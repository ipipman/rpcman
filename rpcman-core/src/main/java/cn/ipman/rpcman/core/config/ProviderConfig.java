package cn.ipman.rpcman.core.config;

import cn.ipman.rpcman.core.api.RegistryCenter;
import cn.ipman.rpcman.core.config.AppConfigProperties;
import cn.ipman.rpcman.core.config.ProviderConfigProperties;
import cn.ipman.rpcman.core.provider.ProviderBootstrap;
import cn.ipman.rpcman.core.provider.ProviderInvoker;
import cn.ipman.rpcman.core.provider.http.NettyServer;
import cn.ipman.rpcman.core.registry.zk.ZkRegistryCenter;
import cn.ipman.rpcman.core.transport.SpringBootTransport;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;

import java.util.Map;


/**
 * Description for this class
 * 将Provider启动项,配置到Spring容器中
 *
 * @Author IpMan
 * @Date 2024/3/9 20:07
 */

@Configuration
@Slf4j
@Import({AppConfigProperties.class, ProviderConfigProperties.class, SpringBootTransport.class})
public class ProviderConfig {

    @Value("${server.useNetty:false}")
    private Boolean useNetty;

    @Value("${server.port:8081}")
    private String port;

    @Setter(onMethod_ = {@Autowired})
    private AppConfigProperties appConfigProperties;

    @Setter(onMethod_ = {@Autowired})
    private ProviderConfigProperties providerConfigProperties;

    @Bean
    ProviderBootstrap providerBootstrap() {
        return new ProviderBootstrap(port, appConfigProperties, providerConfigProperties);
    }

    @Bean
    ProviderInvoker providerInvoker(@Autowired ProviderBootstrap providerBootstrap) {
        return new ProviderInvoker(providerBootstrap);
    }

    @Bean
    @Order(Integer.MIN_VALUE) // 让ProviderBootstrap执行顺序提前,避免Consumer依赖时找不到Provider
    public ApplicationRunner providerBootstrapRunner(@Autowired ProviderBootstrap providerBootstrap) {
        return x -> {
            log.info("createProviderBootstrap starting...");
            providerBootstrap.start();
            log.info("createProviderBootstrap started...");
        };
    }

    @Bean
    @ConditionalOnMissingBean
    public RegistryCenter consumer_rc() {
        return new ZkRegistryCenter();
    }

    @Bean(initMethod = "start")
    public NettyServer nettyServer(@Autowired ProviderInvoker providerInvoker) {
        if (useNetty)
            return new NettyServer(Integer.parseInt(port) + 1000, providerInvoker);
        return null;
    }

}