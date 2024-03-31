package cn.ipman.rpcman.core.registry.zk;

import cn.ipman.rpcman.core.api.RegistryCenter;
import cn.ipman.rpcman.core.api.RpcException;
import cn.ipman.rpcman.core.meta.InstanceMeta;
import cn.ipman.rpcman.core.meta.ServiceMeta;
import cn.ipman.rpcman.core.registry.ChangedListener;
import cn.ipman.rpcman.core.registry.Event;
import com.alibaba.fastjson.JSON;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.springframework.beans.factory.annotation.Value;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Description for this class
 * 基于Zookeeper实现的注册中心
 *
 * @Author IpMan
 * @Date 2024/3/17 20:10
 */
@Slf4j
public class ZkRegistryCenter implements RegistryCenter {

    // zk客户端
    private CuratorFramework client = null;
    // zk监听器
    @SuppressWarnings("deprecation")
    private TreeCache cache = null;

    @Value("${rpcman.zkServer}")
    String servers;

    @Value("${rpcman.zkRoot}")
    String root;

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
                .connectString(servers)
                .connectionTimeoutMs(2000)
                .namespace(root)
                .retryPolicy(retryPolicy)
                .build();

        // 启动zk实例
        client.start();
        log.info(" ===> zk client starting to server[ " + servers + "/" + root + " ]");
    }

    @Override
    public void stop() {
        if (cache != null) {
            unsubscribe(); // 关闭订阅
        }
        log.info(" ===> zk client stopped.");
        client.close();
    }

    @Override
    public void register(ServiceMeta service, InstanceMeta instance) {
        // servicePath = rpman/cn.ipman.rpcman.demo.api.UserService
        String servicePath = "/" + service.toPath();
        try {
            // 创建服务的持久化节点
            if (client.checkExists().forPath(servicePath) == null) {
                client.create().withMode(CreateMode.PERSISTENT).forPath(servicePath, "service".getBytes());
            }
            // 创建实例的临时性节点
            // instancePath = rpman/cn.ipman.rpcman.demo.api.UserService/127.0.0.1_8081
            String instancePath = servicePath + "/" + instance.toRcPath();
            log.info(" ===> register to zk:" + instancePath);
            client.create().withMode(CreateMode.EPHEMERAL).forPath(instancePath, instance.toMetas().getBytes());

        } catch (Exception ex) {
            throw new RpcException(ex);
        }
    }

    @Override
    public void unregister(ServiceMeta service, InstanceMeta instance) {
        String servicePath = "/" + service.toPath();
        try {
            // 判断服务是否存在
            if (client.checkExists().forPath(servicePath) == null) {
                return;
            }
            // 容器关停时,删除实例节点
            String instancePath = servicePath + "/" + instance.toRcPath();
            log.info(" ===> unregister to zk:" + instancePath);
            client.delete().quietly().forPath(instancePath);

        } catch (Exception ex) {
            throw new RpcException(ex);
        }
    }

    @Override
    public List<InstanceMeta> fetchAll(ServiceMeta service) {
        String servicePath = "/" + service.toPath();
        try {
            // 根据service接口,获取zk下所有子节点
            List<String> nodes = client.getChildren().forPath(servicePath);
            log.info(" ===> fetchAll to zk:" + servicePath);
            nodes.forEach(x -> log.info("fetchAll nodes={}", x));
            return mapInstances(servicePath, nodes);
        } catch (Exception ex) {
            throw new RpcException(ex);
        }
    }

    @SuppressWarnings("unchecked")
    private List<InstanceMeta> mapInstances(String servicePath, List<String> nodes) {
        return nodes.stream().map(node -> {
            String[] ipPort = node.split("_");
            InstanceMeta instanceMeta = InstanceMeta.http(ipPort[0], Integer.valueOf(ipPort[1]));
            log.debug(" fetchAll instance:{}", instanceMeta.toHttpUrl());
            // 拿到Metas配置, 机房、单元、灰度等
            String nodePath = servicePath + "/" + node;
            byte[] bytes;
            try {
                bytes = client.getData().forPath(nodePath);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            HashMap<String, String> metas = JSON.parseObject(new String(bytes), HashMap.class);
            metas.forEach((k, v) -> System.out.println("providers metas ==> " + k + " -> " + v));
            instanceMeta.setParameters(metas);
            return instanceMeta;
        }).collect(Collectors.toList());
    }

    @Override
    @SneakyThrows
    @SuppressWarnings("deprecation")
    public void subscribe(ServiceMeta service, ChangedListener listener) {
        cache = TreeCache.newBuilder(client, "/" + service.toPath())
                .setCacheData(true)
                .setMaxDepth(2) // 监听节点层级深度为2
                .build();
        cache.getListenable().addListener((curator, event) -> {
            // 监听zookeeper节点变化
            log.info(" ===> zk subscribe event: " + event);
            List<InstanceMeta> nodes = fetchAll(service);
            listener.fire(new Event(nodes));
        });
        cache.start();
    }

    @Override
    public void unsubscribe() {
        log.info(" ===> zk unsubscribe ...");
        // cache.close();
    }
}
