package cn.ipman.rpcman.core.consumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.SimpleThreadScope;
import org.springframework.core.annotation.Order;


/**
 * Description for this class
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

}
