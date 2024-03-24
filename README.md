##### 从零开始,手写RPC框架





#### Provider Server IO框架的选择与性能的评估

##### 1.Provider Server 性能分布火焰图

我的框架本身没有任何业务处理逻辑,由此通过火焰图分析框架性能损耗分布,能更好的帮助我优化Latency.

从火焰图表现上看,大部分损耗都在IO上,那么就可以得出一个结论 “一个高性能的RPC框架,必须选择一个高性能的IO通信框架”

<img src="https://ipman-blog-1304583208.cos.ap-nanjing.myqcloud.com/rpcman/2024-03-24-153134.png" alt="image-20240324222246295" style="zoom:67%;" align="left" />



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

<img src="https://ipman-blog-1304583208.cos.ap-nanjing.myqcloud.com/rpcman/2024-03-24-140239.png" alt="image-20240324220234729" />

> 压测性能: 24ms/RT



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

<img src="https://ipman-blog-1304583208.cos.ap-nanjing.myqcloud.com/rpcman/2024-03-24-140101.png" alt="image-20240324220054409"   />

> 压测性能: 28ms/RT





##### 4 总结 Provider Server IO 框架选型

从压测数据来看,花半小时精力手撸个Netty Server的吞吐量,要比 Spring Boot (内嵌Tomcat) 的性能表现好不少. 准确的说, 这次对比是拿了Tomcat 和 Netty 做了一次对比.

其实呢, 两者都是支持NIO的框架, 不管我们去深究Tomcat  (Container、Connector) 的架构 ,还是探索Nettty的 (B/C、EventLoop)的特性,两者对于一个RPC框架来说,意义都没有那么大,除非是性能相差天大地别,已经到了不能忍受的地步.

那么我们应该换个角度看问题, 从RPC框架的角度, 看下他们两者的集成性、扩展性:

- 关于扩展,不用质疑Netty要比Tomcat支持的协议多的多, 因为两者的使命不同, Tomcat主要是作为一个web http容器, 它的战场大部分还是在web开发项目上. 如果我们RPC框架要考虑以下两个问题时,  那么Netty会是首选;
  - 如果我们传输协议不是HTTP, 而是TCP时?
  - 如果我们传输体不是Body, 而是需要自定义编解码时?
- 关于集成, RPC框架的受众是在业务开发同学,  基本上大家业务同学都用的Spring系列的框架. 往往这些框架本身就已经内嵌了Tomcat容器, 那么我们再选择 Spring Boot (内嵌Tomcat) 作为RPC的IO框架, 就会很容易造成Spring的版本冲突、IOC冲突、Servlet冲突等等问题. 所以在RPC框架里, 选择像Netty这种相对独立的IO框架, 也更能被RPC使用者接受.









