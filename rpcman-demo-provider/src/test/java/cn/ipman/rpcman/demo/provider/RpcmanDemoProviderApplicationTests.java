package cn.ipman.rpcman.demo.provider;

import cn.ipman.rpcman.core.test.TestZKServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class RpcmanDemoProviderApplicationTests {

    static TestZKServer zkServer = new TestZKServer();

    @BeforeAll
    static void init() {
        System.out.println(" ====================================== ");
        System.out.println(" ====================================== ");
        System.out.println(" =============     ZK2182    ========== ");
        System.out.println(" ====================================== ");
        System.out.println(" ====================================== ");
        zkServer.start();
    }

    @Test
    void contextLoads() {
        System.out.println(" ===> RpcmanDemoProviderApplicationTests ...");
    }

    @AfterAll
    static void destroy() {
        zkServer.stop();
    }

}
