package cn.ipman.rpcman.core.consumer;

import cn.ipman.rpcman.core.annotation.RpcConsumer;
import cn.ipman.rpcman.core.api.LoadBalancer;
import cn.ipman.rpcman.core.api.RegistryCenter;
import cn.ipman.rpcman.core.api.Router;
import cn.ipman.rpcman.core.api.RpcContext;
import cn.ipman.rpcman.core.registry.ChangedListener;
import cn.ipman.rpcman.core.registry.Event;
import lombok.Data;
import org.apache.logging.log4j.util.Strings;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Description for this class
 *
 * @Author IpMan
 * @Date 2024/3/10 19:47
 */
@Data
public class ConsumerBootstrap implements ApplicationContextAware, EnvironmentAware {

    ApplicationContext applicationContext;
    Environment environment;

    private Map<String, Object> stub = new HashMap<>();

    public void start() {

        // 获取路由和负载均衡Bean
        Router router = applicationContext.getBean(Router.class);
        LoadBalancer loadBalancer = applicationContext.getBean(LoadBalancer.class);
        RegistryCenter rc = applicationContext.getBean(RegistryCenter.class);


        RpcContext rpcContext = new RpcContext();
        rpcContext.setRouter(router);
        rpcContext.setLoadBalancer(loadBalancer);


        // 获取Spring容器中所有的Bean
        String[] names = applicationContext.getBeanDefinitionNames();
        for (String name : names) {
            // 根据Bean的名称,获取实例如: rpcmanDemoConsumerApplication
            Object bean = applicationContext.getBean(name);
            if (!name.contains("rpcmanDemoConsumerApplication")) continue;

            // 通过Java反射获取标记 @RpcConsumer 注解的类成员,
            // 如:cn.ipman.rpcman.demo.consumer.RpcmanDemoConsumerApplication.userService
            List<Field> fields = findAnnotatedFiled(bean.getClass());
            fields.forEach(f -> {
                // 获取成员类实例
                Class<?> service = f.getType();
                // 获取成员类实例的类名,如:cn.ipman.rpcman.demo.api.UserService
                String serviceName = service.getCanonicalName();
                Object consumer = stub.get(serviceName);
                if (consumer == null) {
                    // 给成员类实例添加Java动态代理
                    consumer = createFromRegistry(service, rpcContext, rc);
                }
                // 设置可操作权限
                f.setAccessible(true);
                try {
                    // 将动态代理后的Provider类, 重新注入如到Spring容器中
                    // 这样调用Provider时, 通过动态代理实现远程调用
                    f.set(bean, consumer);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            });

        }
    }

    private Object createFromRegistry(Class<?> service, RpcContext rpcContext, RegistryCenter rc) {
        String serviceName = service.getCanonicalName();
        List<String> providers = mapUrls(rc.fetchAll(serviceName));
        System.out.println("  ===> map to providers");
        providers.forEach(System.out::println);

        // 新增Provider节点订阅
        rc.subscribe(serviceName, event -> {
            providers.clear();
            providers.addAll(mapUrls(event.getData()));
        });
        return createConsumer(service, rpcContext, providers);
    }

    private List<String> mapUrls(List<String> nodes) {
        return nodes.stream()
                .map(x -> "http://" + x.replace("_", ":")).collect(Collectors.toList());
    }


    private Object createConsumer(Class<?> service, RpcContext rpcContext, List<String> providers) {
        // 通过Java动态代理,实现 Provider 的远程调用
        return Proxy.newProxyInstance(service.getClassLoader(),
                new Class[]{service}, new RpcInvocationHandler(service, rpcContext, providers));
    }

    private List<Field> findAnnotatedFiled(Class<?> aClass) {
        List<Field> result = new ArrayList<>();
        while (aClass != null) {
            // 获取实例的所有成员
            Field[] fields = aClass.getDeclaredFields();
            for (Field f : fields) {
                // 判断成员是否被标记为 @Consumer 注解
                if (f.isAnnotationPresent(RpcConsumer.class)) {
                    result.add(f);
                }
            }
            // 找到被Spring容器CGLIB代理过的父类,避免找不到标记成 @RpcConsumer 的成员
            aClass = aClass.getSuperclass();
        }
        return result;
    }

}
