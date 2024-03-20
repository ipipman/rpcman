package cn.ipman.rpcman.core.provider;

import cn.ipman.rpcman.core.annotation.RpcProvider;
import cn.ipman.rpcman.core.api.RegistryCenter;
import cn.ipman.rpcman.core.api.RpcRequest;
import cn.ipman.rpcman.core.api.RpcResponse;
import cn.ipman.rpcman.core.meta.ProviderMeta;
import cn.ipman.rpcman.core.util.MethodUtils;
import cn.ipman.rpcman.core.util.TypeUtils;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Data;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.Optional;


/**
 * Description for this class
 * RPC生产者启动程序,负责Provider类初始化及调用
 *
 * @Author IpMan
 * @Date 2024/3/9 20:07
 */
@Data
public class ProviderBootstrap implements ApplicationContextAware {

    // 容器上下文
    private ApplicationContext applicationContext;
    // 注册中心
    private RegistryCenter rc;

    // 方法名 -> [sign1, sign2]
    private MultiValueMap<String, ProviderMeta> skeleton = new LinkedMultiValueMap<>();

    private String instance;

    @Value("${server.port}")
    private String port;

    @PostConstruct
    @SneakyThrows
    public void init() {
        // 寻找@Provider的实现类
        Map<String, Object> providers = applicationContext.getBeansWithAnnotation(RpcProvider.class);
        // 获取注册中心
        this.rc = applicationContext.getBean(RegistryCenter.class);
        providers.forEach((className, classObject)
                -> System.out.println("@RpcProvider init, className=" + className + ",classObject=" + classObject));
        // 初始化接口列表
        providers.values().forEach(this::genInterface);

    }

    @SneakyThrows
    public void start() {
        // 获取provider实例, 注册到 zookeeper
        String ip = InetAddress.getLocalHost().getHostAddress();
        this.instance = ip + "_" + port;
        // 启动注册中心连接,开始注册
        this.rc.start();
        this.skeleton.keySet().forEach(this::registerService);

    }

    @PreDestroy
    public void stop() {
        System.out.println(" ===> zk PreDestroy stop: " + this.skeleton);
        // 取消注册,关闭注册中心连接
        this.skeleton.keySet().forEach(this::unregisterService);
        this.rc.stop();
    }

    private void unregisterService(String service) {
        this.rc.unregister(service, this.instance);
    }

    private void registerService(String service) {
        this.rc.register(service, this.instance);
    }

    private void genInterface(Object classObject) {
        // 获取注入类的实例,并注册到  skeleton <className, classObject>
        Class<?>[] itFers = classObject.getClass().getInterfaces();
        for (Class<?> itFer : itFers) {
            Method[] methods = itFer.getMethods();
            for (Method method : methods) {
                // 如果是本地方法,就跳过
                if (MethodUtils.checkLocalMethod(method)) {
                    continue;
                }
                // 创建 skeleton
                createProvider(itFer, classObject, method);
            }
        }
    }

    private void createProvider(Class<?> itFer, Object classObject, Method method) {
        ProviderMeta meta = new ProviderMeta();
        meta.setMethod(method);
        meta.setMethodSign(MethodUtils.methodSign(method));
        meta.setServiceImpl(classObject);
        System.out.println("create a provider:" + meta);
        this.skeleton.add(itFer.getCanonicalName(), meta);
    }


    public RpcResponse<?> invoke(RpcRequest request) {

        RpcResponse<Object> rpcResponse = new RpcResponse<>();
        // 根据类包名,获取容器的类实例
        List<ProviderMeta> providerMetas = this.skeleton.get(request.getService());
        try {
            String methodSign = request.getMethodSign();
            // 从元数据里获取类方法
            ProviderMeta meta = findProviderMeta(providerMetas, methodSign);
            Method method = meta.getMethod();
            // 参数类型转换
            Object[] args = processArgs(request.getArgs(), method.getParameterTypes());
            // 传入方法参数,通过反射 调用目标provider方法
            Object result = method.invoke(meta.getServiceImpl(), args);
            rpcResponse.setStatus(true);
            rpcResponse.setData(result);
        } catch (InvocationTargetException e) {
            // Provider反射时异常处理, TODO 返回反射目标类的异常
            rpcResponse.setEx(new RuntimeException(e.getTargetException().getMessage()));
        } catch (IllegalAccessException e) {
            // Provider反射调用时异常
            rpcResponse.setEx(new RuntimeException(e.getMessage()));
        }
        return rpcResponse;
    }

    private Object[] processArgs(Object[] args, Class<?>[] parameterTypes) {
        if (args == null || args.length == 0) return args;
        // 参数类型转换
        Object[] actualArgs = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            actualArgs[i] = TypeUtils.cast(args[i], parameterTypes[i]);
        }
        return actualArgs;
    }

    private ProviderMeta findProviderMeta(List<ProviderMeta> providerMetas, String methodSign) {
        // 寻找方法签名是否存在
        Optional<ProviderMeta> optional = providerMetas.stream()
                .filter(x -> x.getMethodSign().equals(methodSign)).findFirst();
        return optional.orElse(null);
    }


    private Method findMethod(Class<?> aClass, String methodName) {
        // 根据实现类的方法名字,查找方法实例
        // TODO: 如果有方法重载,这种实现并不可靠,待完善
        for (Method method : aClass.getMethods()) {
            if (method.getName().equals(methodName)) {
                return method;
            }
        }
        return null;
    }

}
