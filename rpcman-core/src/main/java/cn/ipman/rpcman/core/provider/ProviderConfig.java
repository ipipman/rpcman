package cn.ipman.rpcman.core.provider;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


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

}
