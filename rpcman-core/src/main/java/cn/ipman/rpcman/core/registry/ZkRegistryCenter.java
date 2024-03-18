package cn.ipman.rpcman.core.registry;

import cn.ipman.rpcman.core.api.RegistryCenter;
import lombok.SneakyThrows;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;

import java.util.List;

/**
 * Description for this class
 * 基于Zookeeper实现的注册中心
 *
 * @Author IpMan
 * @Date 2024/3/17 20:10
 */
public class ZkRegistryCenter implements RegistryCenter {

    private CuratorFramework client = null;

    @Override
    public void start() {
        // baseSleepTimeMs：初始的sleep时间，用于计算之后的每次重试的sleep时间，
        //          计算公式：当前sleep时间=baseSleepTimeMs*Math.max(1, random.nextInt(1<<(retryCount+1)))
        // maxRetries：最大重试次数
        // maxSleepMs：最大sleep时间，如果上述的当前sleep计算出来比这个大，那么sleep用这个时间
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);

        // connectString：zk的server地址，多个server之间使用英文逗号分隔开
        // connectionTimeoutMs：连接超时时间，如上是30s，默认是15s
        // sessionTimeoutMs：会话超时时间，如上是50s，默认是60s
        // retryPolicy：失败重试策略
        client = CuratorFrameworkFactory.builder()
                .connectString("localhost:2181")
                .connectionTimeoutMs(2000)
                .namespace("rpcman")
                .retryPolicy(retryPolicy)
                .build();

        // 启动zk实例
        client.start();
        System.out.println(" ===> zk client starting.");
    }

    @Override
    public void stop() {
        System.out.println(" ===> zk client stopped.");
        client.close();
    }

    @Override
    public void register(String service, String instance) {
        // servicePath = rpman/cn.ipman.rpcman.demo.api.UserService
        String servicePath = "/" + service;
        try {
            // 创建服务的持久化节点
            if (client.checkExists().forPath(servicePath) == null) {
                client.create().withMode(CreateMode.PERSISTENT).forPath(servicePath, "service".getBytes());
            }
            // 创建实例的临时性节点
            // instancePath = rpman/cn.ipman.rpcman.demo.api.UserService/127.0.0.1_8081
            String instancePath = servicePath + "/" + instance;
            System.out.println(" ===> register to zk:" + instancePath);
            client.create().withMode(CreateMode.EPHEMERAL).forPath(instancePath, "provider".getBytes());

        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void unregister(String service, String instance) {
        String servicePath = "/" + service;
        try {
            // 判断服务是否存在
            if (client.checkExists().forPath(servicePath) == null) {
                return;
            }
            // 容器关停时,删除实例节点
            String instancePath = servicePath + "/" + instance;
            System.out.println(" ===> unregister to zk:" + instancePath);
            client.delete().quietly().forPath(instancePath);

        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public List<String> fetchAll(String service) {
        String servicePath = "/" + service;
        try {
            // 根据service接口,获取zk下所有子节点
            List<String> nodes = client.getChildren().forPath(servicePath);
            System.out.println(" ===> fetchAll to zk:" + servicePath);
            nodes.forEach(System.out::println);
            return nodes;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    @SneakyThrows
    public void subscribe(String service, ChangedListener listener) {
        final TreeCache cache = TreeCache.newBuilder(client, "/" + service)
                .setCacheData(true).setMaxDepth(2).build();
        cache.getListenable().addListener((curator, event) -> {
            // 监听zookeeper节点变化
            System.out.println(" ===> zk subscribe event: " + event);
            List<String> nodes = fetchAll(service);
            listener.fire(new Event(nodes));
        });
        cache.start();
    }
}
