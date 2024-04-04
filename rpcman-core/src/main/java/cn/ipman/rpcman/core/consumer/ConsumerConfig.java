package cn.ipman.rpcman.core.consumer;

import cn.ipman.rpcman.core.api.*;
import cn.ipman.rpcman.core.cluster.GrayRouter;
import cn.ipman.rpcman.core.cluster.RoundRibonLoadBalancer;
import cn.ipman.rpcman.core.filter.ParameterFilter;
import cn.ipman.rpcman.core.meta.InstanceMeta;
import cn.ipman.rpcman.core.registry.zk.ZkRegistryCenter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.util.List;


/**
 * Description for this class
 * RPC的Consumer端启动时,根据@RpcConsumer注解找到对应的依赖类,通过Java动态代理实现远程调用,并将代理后的Provider注入到容器种
 *
 * @Author IpMan
 * @Date 2024/3/10 19:49
 */
@Configuration
@Slf4j
public class ConsumerConfig {

    @Value("${rpcman.providers}")
    String services;

    @Value("${app.grayRatio:0}")
    private int grayRatio;

    @Value("${app.id:app1}")
    private String app;

    @Value("${app.namespace:public}")
    private String namespace;

    @Value("${app.env:dev}")
    private String env;

    @Value("${app.version:0.0.1-SNAPSHOT}")
    private String version;

    @Value("${app.useNetty:false}")
    private boolean useNetty;

    @Value("${app.retries:1}")
    private int retries;

    @Value("${app.timeout:1000}")
    private int timeout;

    @Value("${app.faultLimit:10}")
    private int faultLimit;

    @Value("${app.halfOpenInitialDelay:10000}")
    private int halfOpenInitialDelay;

    @Value("${app.halfOpenDelay:60000}")
    private int halfOpenDelay;

    @Setter(onMethod_ = {@Autowired})
    ApplicationContext applicationContext;

    @Bean
    public ConsumerBootstrap createConsumerBootstrap() {
        return new ConsumerBootstrap();
    }

    @Bean
    @Order(Integer.MIN_VALUE) // 让ProviderBootstrap执行顺序提前,避免Consumer依赖时找不到Provider
    public ApplicationRunner consumerBootstrapRunner(@Autowired ConsumerBootstrap consumerBootstrap) {
        return x -> {
            log.info("createConsumerBootstrap starting...");
            consumerBootstrap.start();
            log.info("createConsumerBootstrap started...");
        };
    }

    @Bean
    public LoadBalancer<InstanceMeta> loadBalancer() {
        return new RoundRibonLoadBalancer<>();
    }

    @Bean
    public Router<InstanceMeta> loadRouter() {
        return new GrayRouter(grayRatio);
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public RegistryCenter consumerRc() {
        return new ZkRegistryCenter();
    }

    @Bean
    public Filter filterDefault() {
        return new ParameterFilter();
    }

    @Bean
    public RpcContext createContext(@Autowired Router<InstanceMeta> router,
                                    @Autowired LoadBalancer<InstanceMeta> loadBalancer,
                                    @Autowired List<Filter> filters) {
        RpcContext context = new RpcContext();
        context.setRouter(router);
        context.setLoadBalancer(loadBalancer);
        context.setFilters(filters);
        context.getParameters().put("app.id", app);
        context.getParameters().put("app.namespace", namespace);
        context.getParameters().put("app.env", env);
        context.getParameters().put("app.version", version);
        context.getParameters().put("app.retries", String.valueOf(retries));
        context.getParameters().put("app.timeout", String.valueOf(timeout));
        context.getParameters().put("app.useNetty", String.valueOf(useNetty));
        context.getParameters().put("app.halfOpenInitialDelay", String.valueOf(halfOpenInitialDelay));
        context.getParameters().put("app.faultLimit", String.valueOf(faultLimit));
        context.getParameters().put("app.halfOpenDelay", String.valueOf(halfOpenDelay));
        return context;
    }

}
