package cn.ipman.rpcman.demo.consumer;

import cn.ipman.rpcman.demo.provider.RpcmanDemoProviderApplication;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootTest
class RpcmanDemoConsumerApplicationTests {

    static ConfigurableApplicationContext context;

    @BeforeAll
    static void init(){
        context = SpringApplication.run(RpcmanDemoProviderApplication.class,
                "--server.port=8084", "--server.useNetty=true", "--logging.level.cn.ipman=debug");
    }

    @Test
    void contextLoads() {

    }

    @AfterAll
    static void destroy(){
        SpringApplication.exit(context, new ExitCodeGenerator() {
            @Override
            public int getExitCode() {
                return 1;
            }
        });
    }

}
