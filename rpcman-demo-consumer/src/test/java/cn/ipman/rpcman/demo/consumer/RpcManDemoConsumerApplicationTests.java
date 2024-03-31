package cn.ipman.rpcman.demo.consumer;

import cn.ipman.rpcman.core.test.TestZKServer;
import cn.ipman.rpcman.demo.provider.RpcmanDemoProviderApplication;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

/**
 * Description for this class
 *
 * @Author IpMan
 * @Date 2024/3/26 22:22
 */
@SpringBootTest(classes = {RpcmanDemoConsumerApplication.class})
public class RpcManDemoConsumerApplicationTests {

    static ApplicationContext context1;

    static ApplicationContext context2;

    static TestZKServer zkServer = new TestZKServer();

    @BeforeAll
    static void init() {
        System.out.println(" ================================ ");
        System.out.println(" =========== Mock ZK ============ ");
        System.out.println(" ================================ ");
        System.out.println(" ================================ ");

        zkServer.start();

        System.out.println(" ================================ ");
        System.out.println(" ============  8084 ============= ");
        System.out.println(" ================================ ");
        System.out.println(" ================================ ");
        context1 = SpringApplication.run(RpcmanDemoProviderApplication.class,
                "--server.port=8084", "--server.useNetty=true",
                "--rpcman.server=localhost:2182", "--logging.level.cn.ipman=debug",
                "--app.metas={dc:'bj',gray:'false',unit:'B001'}");

        System.out.println(" ================================ ");
        System.out.println(" ============  8085 ============= ");
        System.out.println(" ================================ ");
        System.out.println(" ================================ ");
        context2 = SpringApplication.run(RpcmanDemoProviderApplication.class,
                "--server.port=8086", "--server.useNetty=true",
                "--rpcman.server=localhost:2182", "--logging.level.cn.ipman=debug",
                "--app.metas={dc:'bj',gray:'false',unit:'B001'}");


    }

    @Test
    void contextLoads() {
        System.out.println("consumer running ... ");
    }

    @AfterAll
    static void destroy() {
        SpringApplication.exit(context1, () -> 1);
//        SpringApplication.exit(context2, () -> 1);
        zkServer.stop();
    }
}
