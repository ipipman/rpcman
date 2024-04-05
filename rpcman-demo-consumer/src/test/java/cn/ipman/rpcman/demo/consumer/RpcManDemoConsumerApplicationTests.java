package cn.ipman.rpcman.demo.consumer;

import cn.ipman.rpcman.core.test.TestZKServer;
import cn.ipman.rpcman.demo.provider.RpcmanDemoProviderApplication;
import lombok.Setter;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

/**
 * Description for this class
 *
 * @Author IpMan
 * @Date 2024/3/26 22:22
 */
@SpringBootTest(classes = {RpcmanDemoConsumerApplication.class},
        properties = {"rpcman.zk.zkServer=localhost:2183"})
public class RpcManDemoConsumerApplicationTests {

    static ApplicationContext context1;

    static ApplicationContext context2;

    static TestZKServer zkServer = new TestZKServer(2183);

    @Setter(onMethod_ = {@Autowired})
    private Environment environment;

    @BeforeAll
    static void init() {

        System.out.println(" ================================ ");
        System.out.println(" =========== Mock ZK 2183 ======= ");
        System.out.println(" ================================ ");
        System.out.println(" ================================ ");

        zkServer.start();

        System.out.println(" ================================ ");
        System.out.println(" ============  8085 ============= ");
        System.out.println(" ================================ ");
        System.out.println(" ================================ ");
        context1 = SpringApplication.run(RpcmanDemoProviderApplication.class,
                "--server.port=8085",
                "--logging.level.cn.ipman=debug",
                "--rpcman.app.useNetty=true",
                "--rpcman.zk.zkServer=localhost:2183",
                "--rpcman.provider.metas.dc=bj",
                "--rpcman.provider.metas.gray=false",
                "--rpcman.provider.metas.unit=B002"
        );

        System.out.println(" ================================ ");
        System.out.println(" ============  8087 ============= ");
        System.out.println(" ================================ ");
        System.out.println(" ================================ ");
        context2 = SpringApplication.run(RpcmanDemoProviderApplication.class,
                "--server.port=8087",
                "--logging.level.cn.ipman=debug",
                "--rpcman.app.useNetty=true",
                "--rpcman.zk.zkServer=localhost:2183",
                "--rpcman.provider.metas.dc=bj",
                "--rpcman.provider.metas.gray=false",
                "--rpcman.provider.metas.unit=B002"
        );
    }

    @Test
    void contextLoads() {
        System.out.println("rpcman.zk.zkServer=>" + environment.getProperty("rpcman.zk.zkServer"));
        System.out.println("consumer running ... ");
    }

    @AfterAll
    static void destroy() {
        SpringApplication.exit(context1, () -> 1);
        SpringApplication.exit(context2, () -> 1);
        zkServer.stop();
    }
}
