package cn.ipman.rpcman.core.test;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.test.InstanceSpec;
import org.apache.curator.test.TestingCluster;
import org.apache.curator.test.TestingServer;
import org.apache.curator.utils.CloseableUtils;

/**
 * Description for this class
 *
 * @Author IpMan
 * @Date 2024/3/25 22:42
 */
public class TestZKServer {

    TestingCluster cluster;

    @SneakyThrows
    public void start(){
        // 模拟ZooKeeper服务端
        InstanceSpec instanceSpec = new InstanceSpec(null, 2182,
                -1, -1, true,
                -1, -1, -1);
        cluster = new TestingCluster(instanceSpec);
        System.out.println("TestingZooKeeperServer starting ...");
        cluster.start();
        cluster.getServers().forEach(s -> System.out.println(s.getInstanceSpec()));
        System.out.println("TestingZooKeeperServer started.");
    }

    @SneakyThrows
    public void stop(){
        System.out.println("TestingZooKeeperServer stopping ...");
        cluster.stop();
        CloseableUtils.closeQuietly(cluster);
        System.out.println("TestingZooKeeperServer stopped.");
    }
}
