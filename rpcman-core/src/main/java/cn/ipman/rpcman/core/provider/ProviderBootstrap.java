package cn.ipman.rpcman.core.provider;

import cn.ipman.rpcman.core.annotation.RpcProvider;
import cn.ipman.rpcman.core.api.RpcRequest;
import cn.ipman.rpcman.core.api.RpcResponse;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;


/**
 * Description for this class
 * RPC生产者启动程序,负责Provider类初始化及调用
 *
 * @Author IpMan
 * @Date 2024/3/9 20:07
 */
@Data
public class ProviderBootstrap implements ApplicationContextAware {

    ApplicationContext applicationContext;

    private Map<String, Object> skeleton = new HashMap<>();

    @PostConstruct
    public void buildProviders() {
        // 寻找@Provider的实现类
        Map<String, Object> providers = applicationContext.getBeansWithAnnotation(RpcProvider.class);
        providers.forEach((className, classObject)
                -> System.out.println("@RpcProvider init, className=" + className + ",classObject=" + classObject));
        // 初始化接口列表
        providers.values().forEach(this::genInterface);

    }

    private void genInterface(Object classObject) {
        // 获取注入类的实例,并注册到  skeleton <className, classObject>
        Class<?> itFer = classObject.getClass().getInterfaces()[0];
        skeleton.put(itFer.getCanonicalName(), classObject);
    }


    public RpcResponse<?> invoke(RpcRequest request) {
        // 屏蔽一些Provider接口实现的方法
        String name = request.getMethodSign();
        if (name.equals("toString") || name.equals("hashCode")) {
            return null;
        }

        RpcResponse<Object> rpcResponse = new RpcResponse<>();
        // 根据类包名,获取容器的类实例
        Object bean = skeleton.get(request.getService());
        try {
            Class<?> aClass = bean.getClass();
            // 根据类和方法名,找到对应方法实例
            Method method = findMethod(aClass, request.getMethodSign());

            // 传入方法参数,通过反射 调用目标provider方法
            assert method != null;
            Object result = method.invoke(bean, request.getArgs());
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
