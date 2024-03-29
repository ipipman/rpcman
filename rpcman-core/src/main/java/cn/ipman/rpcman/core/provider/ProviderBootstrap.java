package cn.ipman.rpcman.core.provider;

import cn.ipman.rpcman.core.annotation.RpcProvider;
import cn.ipman.rpcman.core.api.RegistryCenter;
import cn.ipman.rpcman.core.meta.InstanceMeta;
import cn.ipman.rpcman.core.meta.ProviderMeta;
import cn.ipman.rpcman.core.meta.ServiceMeta;
import cn.ipman.rpcman.core.util.MethodUtils;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.Map;


/**
 * Description for this class
 * RPC生产者启动程序,负责Provider类初始化及调用
 *
 * @Author IpMan
 * @Date 2024/3/9 20:07
 */
@Data
@Slf4j
public class ProviderBootstrap implements ApplicationContextAware {

    // 容器上下文
    private ApplicationContext applicationContext;
    // 注册中心
    private RegistryCenter rc;

    // 方法名 -> [sign1, sign2]
    private MultiValueMap<String, ProviderMeta> skeleton = new LinkedMultiValueMap<>();

    private InstanceMeta instance;

    @Value("${server.port}")
    private String port;

    @Value("${server.useNetty}")
    private Boolean useNetty;

    @Value("${app.id}")
    private String app;

    @Value("${app.namespace}")
    private String namespace;

    @Value("${app.env}")
    private String env;

    @Value("${app.version}")
    private String version;

    @PostConstruct
    @SneakyThrows
    public void init() {
        // 寻找@Provider的实现类
        Map<String, Object> providers = applicationContext.getBeansWithAnnotation(RpcProvider.class);
        // 获取注册中心
        this.rc = applicationContext.getBean(RegistryCenter.class);
        providers.forEach((className, classObject)
                -> log.info("@RpcProvider init, className=" + className + ",classObject=" + classObject));
        // 初始化接口列表
        providers.values().forEach(this::genInterface);

    }

    @SneakyThrows
    public void start() {
        // 获取provider实例, 注册到 zookeeper
        String ip = InetAddress.getLocalHost().getHostAddress();
        if (useNetty) {
            this.instance = InstanceMeta.http(ip, Integer.parseInt(port) + 1000);
        } else {
            this.instance = InstanceMeta.http(ip, Integer.parseInt(port));
        }
        // 启动注册中心连接,开始注册
        this.rc.start();
        this.skeleton.keySet().forEach(this::registerService);

    }

    @PreDestroy
    public void stop() {
        log.info(" ===> zk PreDestroy stop: " + this.skeleton);
        // 取消注册,关闭注册中心连接
        skeleton.keySet().forEach(this::unregisterService);
        rc.stop();
    }

    private void unregisterService(String service) {
        ServiceMeta serviceMeta = ServiceMeta.builder()
                .name(service).app(app).namespace(namespace).env(env).version(version)
                .build();
        rc.unregister(serviceMeta, this.instance);
    }

    private void registerService(String service) {
        ServiceMeta serviceMeta = ServiceMeta.builder()
                .name(service).app(app).namespace(namespace).env(env).version(version)
                .build();
        rc.register(serviceMeta, this.instance);
    }

    private void genInterface(Object impl) {
        // 获取注入类的实例,并注册到  skeleton <className, classObject>
        Class<?>[] services = impl.getClass().getInterfaces();
        for (Class<?> service : services) {
            Method[] methods = service.getMethods();
            for (Method method : methods) {
                // 如果是本地方法,就跳过
                if (MethodUtils.checkLocalMethod(method)) {
                    continue;
                }
                // 创建 skeleton
                createProvider(service, impl, method);
            }
        }
    }

    private void createProvider(Class<?> service, Object impl, Method method) {
        ProviderMeta providerMeta = ProviderMeta.builder()
                .method(method).methodSign(MethodUtils.methodSign(method)).serviceImpl(impl)
                .build();
        log.info("create a provider:" + providerMeta);
        this.skeleton.add(service.getCanonicalName(), providerMeta);
    }

    @Deprecated
    @SuppressWarnings("unused")
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
