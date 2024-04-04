package cn.ipman.rpcman.core.consumer;

import cn.ipman.rpcman.core.annotation.RpcConsumer;
import cn.ipman.rpcman.core.api.*;
import cn.ipman.rpcman.core.meta.InstanceMeta;
import cn.ipman.rpcman.core.meta.ServiceMeta;
import cn.ipman.rpcman.core.util.MethodUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Description for this class
 *
 * @Author IpMan
 * @Date 2024/3/10 19:47
 */
@Data
@Slf4j
public class ConsumerBootstrap implements ApplicationContextAware, EnvironmentAware {

    ApplicationContext applicationContext;
    Environment environment;

    private Map<String, Object> stub = new HashMap<>();

    @Value("${app.id}")
    private String app;

    @Value("${app.namespace}")
    private String namespace;

    @Value("${app.env}")
    private String env;

    @Value("${app.version}")
    private String version;

    @Value("${app.retries}")
    private int retries;

    @Value("${app.timeout}")
    private int timeout;

    @Value("${app.useNetty}")
    private boolean useNetty;

    @Value("${app.grayRatio}")
    private int grayRatio;

    @Value("${app.faultLimit}")
    private int faultLimit;

    @Value("${app.halfOpenInitialDelay}")
    private int halfOpenInitialDelay;

    @Value("${app.halfOpenDelay}")
    private int halfOpenDelay;


    public void start() {

        RegistryCenter rc = applicationContext.getBean(RegistryCenter.class);
        RpcContext rpcContext = createContext();

        // 获取Spring容器中所有的Bean
        String[] names = applicationContext.getBeanDefinitionNames();
        for (String name : names) {
            // 根据Bean的名称,获取实例如: rpcmanDemoConsumerApplication
            Object bean = applicationContext.getBean(name);
            // debug -> if (!name.contains("rpcmanDemoConsumerApplication")) continue;

            // 通过Java反射获取标记 @RpcConsumer 注解的类成员,
            // 如:cn.ipman.rpcman.demo.consumer.RpcmanDemoConsumerApplication.userService
            List<Field> fields = MethodUtils.findAnnotatedFiled(bean.getClass(), RpcConsumer.class);
            fields.forEach(f -> {
                // 获取成员类实例
                Class<?> service = f.getType();
                // 获取成员类实例的类名,如:cn.ipman.rpcman.demo.api.UserService
                String serviceName = service.getCanonicalName();
                log.info(" ===> " + f.getName());
                try {
                    Object consumer = stub.get(serviceName);
                    if (consumer == null) {
                        // 给成员类实例添加Java动态代理
                        consumer = createFromRegistry(service, rpcContext, rc);
                        stub.put(serviceName, consumer);
                    }
                    // 设置可操作权限
                    f.setAccessible(true);
                    // 将动态代理后的Provider类, 重新注入如到Spring容器中
                    // 这样调用Provider时, 通过动态代理实现远程调用
                    f.set(bean, consumer);
                } catch (Exception ex) {
                    log.warn(" ==> Field[{}.{}] create consumer failed.", serviceName, f.getName());
                    log.error("Ignore and print it as: ", ex);
                }
            });
        }
    }

    private RpcContext createContext() {
        // 获取路由和负载均衡Bean
        @SuppressWarnings("unchecked")
        Router<InstanceMeta> router = applicationContext.getBean(Router.class);
        @SuppressWarnings("unchecked")
        LoadBalancer<InstanceMeta> loadBalancer = applicationContext.getBean(LoadBalancer.class);
        List<Filter> filters = applicationContext.getBeansOfType(Filter.class).values().stream().toList();

        RpcContext rpcContext = new RpcContext();
        rpcContext.setRouter(router);
        rpcContext.setLoadBalancer(loadBalancer);
        rpcContext.setFilters(filters);
        rpcContext.getParameters().put("app.retries", String.valueOf(retries));     // client超时重试
        rpcContext.getParameters().put("app.timeout", String.valueOf(timeout));     // client超时时间ms
        rpcContext.getParameters().put("app.useNetty", String.valueOf(useNetty));   // 是否用netty做为client
        // rpcContext.getParameters().put("app.grayRatio", String.valueOf(grayRatio)); // 灰度发布
        rpcContext.getParameters().put("app.halfOpenInitialDelay", String.valueOf(halfOpenInitialDelay)); // 半开重试时间间隔
        rpcContext.getParameters().put("app.faultLimit", String.valueOf(faultLimit)); // client失败重试多少次后进入隔离区
        rpcContext.getParameters().put("app.halfOpenDelay", String.valueOf(halfOpenDelay)); // 半开延迟执行时间
        return rpcContext;
    }


    private Object createFromRegistry(Class<?> service, RpcContext rpcContext, RegistryCenter rc) {
        String serviceName = service.getCanonicalName();
        ServiceMeta serviceMeta = ServiceMeta.builder()
                .name(serviceName).app(app).namespace(namespace).env(env).version(version)
                .build();
        List<InstanceMeta> providers = rc.fetchAll(serviceMeta);
        log.debug("  ===> map to providers");
        providers.forEach(x -> log.debug("InstanceMeta providers={}", x));

        // 新增Provider节点订阅
        rc.subscribe(serviceMeta, event -> {
            providers.clear();
            providers.addAll(event.getData());
        });
        return createConsumer(service, rpcContext, providers);
    }


    private Object createConsumer(Class<?> service, RpcContext rpcContext, List<InstanceMeta> providers) {
        // 通过Java动态代理,实现 Provider 的远程调用
        return Proxy.newProxyInstance(service.getClassLoader(),
                new Class[]{service}, new RpcInvocationHandler(service, rpcContext, providers));
    }


}
