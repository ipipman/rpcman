package cn.ipman.rpcman.core.consumer;

import cn.ipman.rpcman.core.api.LoadBalancer;
import cn.ipman.rpcman.core.api.Router;
import cn.ipman.rpcman.core.cluster.RandomLoadBalancer;
import cn.ipman.rpcman.core.cluster.RoundRibonLoadBalancer;
import org.springframework.beans.factory.annotation.Autowired;
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
public class ConsumerConfig {

    @Bean
    public ConsumerBootstrap createConsumerBootstrap() {
        return new ConsumerBootstrap();
    }

    @Bean
    @Order(Integer.MIN_VALUE) // 让ProviderBootstrap执行顺序提前,避免Consumer依赖时找不到Provider
    public ApplicationRunner consumerBootstrapRunner(@Autowired ConsumerBootstrap consumerBootstrap) {
        return x -> {
            System.out.println("createConsumerBootstrap starting...");
            consumerBootstrap.start();
            System.out.println("createConsumerBootstrap started...");
        };
    }

    @Bean
    public LoadBalancer loadBalancer() {
        //return LoadBalancer.Default;
        //return new RandomLoadBalancer();
        return new RoundRibonLoadBalancer();
    }

    @Bean
    public Router loadRouter() {
        return Router.Default;
    }

}
