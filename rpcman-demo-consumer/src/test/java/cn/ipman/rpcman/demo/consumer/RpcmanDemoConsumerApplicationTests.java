//package cn.ipman.rpcman.demo.consumer;
//
//import cn.ipman.rpcman.core.test.TestZKServer;
//import cn.ipman.rpcman.demo.provider.RpcmanDemoProviderApplication;
//import org.junit.jupiter.api.AfterAll;
//import org.junit.jupiter.api.BeforeAll;
//import org.junit.jupiter.api.Test;
//import org.springframework.boot.ExitCodeGenerator;
//import org.springframework.boot.SpringApplication;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.context.ConfigurableApplicationContext;
//
//@SpringBootTest
//class RpcmanDemoConsumerApplicationTests {
//
//    static ConfigurableApplicationContext context;
//
//    static TestZKServer zkServer = new TestZKServer();
//
//    @BeforeAll
//    static void init() {
//        // 开启 Test ZooKeeper
//        zkServer.start();
//        // 开启 Provider
//        context = SpringApplication.run(RpcmanDemoProviderApplication.class,
//                "--server.port=8084", "--server.useNetty=true",
//                "--rpcman.server=localhost:2182", "--logging.level.cn.ipman=debug");
//    }
//
//    @Test
//    void contextLoads() {
//        System.out.println(" ===> RpcmanDemoConsumerApplicationTests ...");
//    }
//
//    @AfterAll
//    static void destroy() {
//        SpringApplication.exit(context, new ExitCodeGenerator() {
//            @Override
//            public int getExitCode() {
//                return 1;
//            }
//        });
//        zkServer.stop();
//    }
//
//}
