##### 从零开始,手写RPC框架





#### Provider Server IO框架的选择与性能的评估

##### 1.Provider Server 性能分布火焰图

我的框架本身没有任何业务处理逻辑,由此通过火焰图分析框架性能损耗分布,能更好的帮助我优化Latency.

从火焰图表现上看,大部分损耗都在IO上,那么就可以得出一个结论 “一个高性能的RPC框架,必须选择一个高性能的IO通信框架”

<img src="https://ipman-blog-1304583208.cos.ap-nanjing.myqcloud.com/rpcman/2024-03-24-153134.png" alt="image-20240324222246295" style="width:600px;"  />



##### 2 用SpringBoot做Provider Server的性能

wrk压测工具

> wrk http://localhost:8088/?id=101

```java
Running 10s test @ http://localhost:8088/?id=101
  2 threads and 10 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency    10.83ms   27.35ms 284.50ms   94.92%
    Req/Sec     0.98k   428.99     2.09k    69.90%
  19210 requests in 10.02s, 3.10MB read
Requests/sec:   1916.85
Transfer/sec:    316.70KB
```

压测结果: 1900/qps



Arthas分析工具: 

>monitor -c 5 cn.ipman.rpcman.core.consumer.RpcInvocationHandler invoke "#cost>10"

<img src="https://ipman-blog-1304583208.cos.ap-nanjing.myqcloud.com/rpcman/2024-03-24-140239.png" alt="image-20240324220234729"  />

`压测性能: 24ms/RT`



##### 3. 用Netty做Provider Server的性能

wrk压测工具

> wrk http://localhost:8088/?id=101

```java
Running 10s test @ http://localhost:8088/?id=101
  2 threads and 10 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency     8.78ms   32.87ms 334.38ms   96.57%
    Req/Sec     1.82k   599.35     2.78k    70.10%
  35218 requests in 10.02s, 5.68MB read
Requests/sec:   3516.08
Transfer/sec:    580.95KB
```



Arthas分析工具: 

> monitor -c 5 cn.ipman.rpcman.core.consumer.RpcInvocationHandler invoke "#cost>10"

<img src="https://ipman-blog-1304583208.cos.ap-nanjing.myqcloud.com/rpcman/2024-03-24-140101.png" alt="image-20240324220054409"  />

`压测性能: 28ms/RT`



##### 4 总结 Provider Server IO 框架选型

从压测数据来看,用Netty Server的吞吐量,要比 Spring Boot (内嵌Tomcat) 的性能表现好不少. 准确的说, 这次对比是拿了Tomcat 和 Netty 做了一次对比.

其实呢, 两者都是支持NIO的框架, 不管我们去深究Tomcat  (Container、Connector) 的架构 ,还是探索Nettty的 (B/C、EventLoop)的特性,两者对于一个RPC框架来说,意义都没有那么大,除非是性能相差天大地别,已经到了不能忍受的地步.

那么我们应该换个角度看问题, 从RPC框架的角度, 看下他们两者的集成性、扩展性:

- 关于扩展,不用质疑Netty要比Tomcat支持的协议多的多, 因为两者的使命不同, Tomcat主要是作为一个web http容器, 它的战场大部分还是在web开发项目上. 如果我们RPC框架要考虑以下两个问题时,  那么Netty会是首选;
  - 如果我们传输协议不是HTTP, 而是TCP时?
  - 如果我们传输体不是Body, 而是需要自定义编解码时?
- 关于集成, RPC框架的受众是在业务开发同学,  基本上大家业务同学都用的Spring系列的框架. 往往这些框架本身就已经内嵌了Tomcat容器, 那么我们再选择 Spring Boot (内嵌Tomcat) 作为RPC的IO框架, 就会很容易造成Spring的版本冲突、IOC冲突、Servlet冲突等等问题. 所以在RPC框架里, 选择像Netty这种相对独立的IO框架, 也更能被RPC使用者接受.



####  模拟全流程的单元测试代码覆盖率

##### 1. 首先需要模拟一个ZK Server,便于测试

因为注册中心需要用到zokeeper, 这里用的`curator`自带的`curator-test` 来模拟,  添加 `maven` 依赖:

```java
<dependency>
    <groupId>org.apache.curator</groupId>
    <artifactId>curator-test</artifactId>
    <version>5.1.0</version>
</dependency>
```

具体模拟 `zookeeper` server 的代码如下:

```java
package cn.ipman.rpcman.core.test;

import lombok.SneakyThrows;
import org.apache.curator.test.InstanceSpec;
import org.apache.curator.test.TestingCluster;
import org.apache.curator.utils.CloseableUtils;

/**
 * 模拟ZooKeeper服务端
 *
 * @Author IpMan
 * @Date 2024/3/25 22:42
 */
public class TestZKServer {

    TestingCluster cluster;

    @SneakyThrows
    public void start() {
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
    public void stop() {
        System.out.println("TestingZooKeeperServer stopping ...");
        cluster.stop();
        CloseableUtils.closeQuietly(cluster);
        System.out.println("TestingZooKeeperServer stopped.");
    }
}
```



##### 2.编写Consumer测试启动类

在`consumer测试类`启动前, 先通过 `@Before` 启动`TestZKServer`和`Provider`程序.  目的是测试一个从 `consumer`端 -> `provider`端 完整的闭环链路

```java
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
 * MockRpcManDemoConsumerApplicationTests
 *
 * @Author IpMan
 * @Date 2024/3/26 22:22
 */
@SpringBootTest(classes = {RpcmanDemoConsumerApplication.class})
public class MockRpcManDemoConsumerApplicationTests {

    static ApplicationContext context;

    static TestZKServer zkServer = new TestZKServer();

    @BeforeAll
    static void init() {
        System.out.println(" ================================ ");
        System.out.println(" ================================ ");
        System.out.println(" ================================ ");
        System.out.println(" ================================ ");

        zkServer.start();
        context = SpringApplication.run(RpcmanDemoProviderApplication.class,
                "--server.port=8084", "--server.useNetty=true",
                "--rpcman.server=localhost:2182", "--logging.level.cn.ipman=debug");
    }

    @Test
    void contextLoads() {
        System.out.println("consumer running ... ");
    }

    @AfterAll
    static void destroy() {
        SpringApplication.exit(context, () -> 1);
        zkServer.stop();
    }
}
```




##### 3.安装 `jaCoCo` 的依赖和插件, 并运行统计代码覆盖率

添加maven依赖

```java
<dependency>
    <groupId>org.jacoco</groupId>
    <artifactId>org.jacoco.agent</artifactId>
    <version>0.8.7</version>
    <scope>test</scope>
</dependency>

<build>   
  <plugins>
    <plugin>
        <groupId>org.jacoco</groupId>
        <artifactId>jacoco-maven-plugin</artifactId>
        <version>0.8.7</version>
        <executions>
            <execution>
                <goals>
                    <goal>prepare-agent</goal>
                </goals>
            </execution>
            <execution>
                <id>report</id>
                <phase>test</phase>
                <goals>
                    <goal>report</goal>
                </goals>
            </execution>
        </executions>
    </plugin>  
  </plugins>
</build>  
```




运行`mvn test`，将生成JaCoCo代码覆盖率报告`target/site/jacoco/*`

$ mvn clean test  启动测试

![image-20240330125633711](https://ipman-blog-1304583208.cos.ap-nanjing.myqcloud.com/rpcman/2024-03-30-045640.png)......


<img src="https://ipman-blog-1304583208.cos.ap-nanjing.myqcloud.com/rpcman/2024-03-30-051111.png" alt="image-20240330131105677" style="width:400px;" />



最终查看测试代码覆盖率报告:  `xx/rpcman/rpcman-demo-consumer/target/site/jacoco/index.html`

![image-20240330130139092](https://ipman-blog-1304583208.cos.ap-nanjing.myqcloud.com/rpcman/2024-03-30-050141.png)

