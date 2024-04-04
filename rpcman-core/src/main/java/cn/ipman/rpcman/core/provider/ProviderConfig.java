package cn.ipman.rpcman.core.provider;

import cn.ipman.rpcman.core.api.RegistryCenter;
import cn.ipman.rpcman.core.provider.http.NettyServer;
import cn.ipman.rpcman.core.registry.zk.ZkRegistryCenter;
import cn.ipman.rpcman.core.transport.SpringBootTransport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
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
@Import({SpringBootTransport.class})
public class ProviderConfig {

    @Value("${server.useNetty:false}")
    private Boolean useNetty;

    @Value("${server.port:8081}")
    private String port;

    @Value("${app.id:app1}")
    private String app;

    @Value("${app.namespace:public}")
    private String namespace;

    @Value("${app.env:dev}")
    private String env;

    @Value("${app.version:0.0.1-SNAPSHOT}")
    private String version;

    @Value("#{${app.metas:{dc:'bj',gray:'false',unit:'B001'}}}")
    Map<String, String> metas;

    @Bean
    ProviderBootstrap providerBootstrap() {
        return new ProviderBootstrap(port, app, namespace, env, metas, version, useNetty);
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