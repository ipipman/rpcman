package cn.ipman.rpcman.core.config;

import cn.ipman.rpcman.core.api.*;
import cn.ipman.rpcman.core.cluster.GrayRouter;
import cn.ipman.rpcman.core.cluster.RoundRibonLoadBalancer;
import cn.ipman.rpcman.core.consumer.ConsumerBootstrap;
import cn.ipman.rpcman.core.filter.ContextParameterFilter;
import cn.ipman.rpcman.core.meta.InstanceMeta;
import cn.ipman.rpcman.core.registry.zk.ZkRegistryCenter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
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
@Import({AppConfigProperties.class, ConsumerConfigProperties.class})
public class ConsumerConfig {

    @Value("${rpcman.providers:}")
    String[] services;

    @Autowired
    private AppConfigProperties appConfigProperties;

    @Autowired
    private ConsumerConfigProperties consumerConfigProperties;

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
        return new GrayRouter(consumerConfigProperties.getGrayRatio());
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public RegistryCenter consumerRc() {
        return new ZkRegistryCenter();
    }

    @Bean
    public Filter filterDefault() {
        return new ContextParameterFilter();
    }

    @Bean
    public RpcContext createContext(@Autowired Router<InstanceMeta> router,
                                    @Autowired LoadBalancer<InstanceMeta> loadBalancer,
                                    @Autowired List<Filter> filters) {
        RpcContext context = new RpcContext();
        context.setRouter(router);
        context.setLoadBalancer(loadBalancer);
        context.setFilters(filters);
        context.getParameters().put("app.id", appConfigProperties.getId());
        context.getParameters().put("app.namespace", appConfigProperties.getNamespace());
        context.getParameters().put("app.env", appConfigProperties.getEnv());
        context.getParameters().put("app.version", appConfigProperties.getVersion());
        context.getParameters().put("app.useNetty", String.valueOf(appConfigProperties.getUseNetty()));

        context.getParameters().put("consumer.retries", String.valueOf(consumerConfigProperties.getRetries()));
        context.getParameters().put("consumer.timeout", String.valueOf(consumerConfigProperties.getTimeout()));
        context.getParameters().put("consumer.faultLimit", String.valueOf(consumerConfigProperties.getFaultLimit()));
        context.getParameters().put("consumer.halfOpenInitialDelay", String.valueOf(consumerConfigProperties.getHalfOpenInitialDelay()));
        context.getParameters().put("consumer.halfOpenDelay", String.valueOf(consumerConfigProperties.getHalfOpenDelay()));
        return context;
    }

}
