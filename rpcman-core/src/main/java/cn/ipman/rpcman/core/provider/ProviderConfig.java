package cn.ipman.rpcman.core.provider;

import cn.ipman.rpcman.core.api.RegistryCenter;
import cn.ipman.rpcman.core.consumer.ConsumerBootstrap;
import cn.ipman.rpcman.core.registry.ZkRegistryCenter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.util.List;


/**
 * Description for this class
 * 将Provider启动项,配置到Spring容器中
 *
 * @Author IpMan
 * @Date 2024/3/9 20:07
 */

@Configuration
public class ProviderConfig {

    @Bean
    ProviderBootstrap providerBootstrap() {
        return new ProviderBootstrap();
    }

    @Bean
    @Order(Integer.MIN_VALUE) // 让ProviderBootstrap执行顺序提前,避免Consumer依赖时找不到Provider
    public ApplicationRunner providerBootstrapRunner(@Autowired ProviderBootstrap providerBootstrap) {
        return x -> {
            System.out.println("createProviderBootstrap starting...");
            providerBootstrap.start();
            System.out.println("createProviderBootstrap started...");
        };
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public RegistryCenter consumerRc() {
        return new ZkRegistryCenter();
    }


}
