package cn.ipman.rpcman.core.consumer;

import cn.ipman.rpcman.core.api.LoadBalancer;
import cn.ipman.rpcman.core.api.RegistryCenter;
import cn.ipman.rpcman.core.api.Router;
import cn.ipman.rpcman.core.cluster.RoundRibonLoadBalancer;
import cn.ipman.rpcman.core.meta.InstanceMeta;
import cn.ipman.rpcman.core.registry.zk.ZkRegistryCenter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;


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
        // return new RandomLoadBalancer<>();
        return new RoundRibonLoadBalancer<>();
    }

    @Bean
    @SuppressWarnings("rawtypes")
    public Router loadRouter() {
        return Router.Default;
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public RegistryCenter consumerRc() {
        // return new RegistryCenter.StaticRegistryCenter(List.of(services.split(",")));
        return new ZkRegistryCenter();
    }

}
