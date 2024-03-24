##### 从零开始,手写RPC框架





#### Provider Server性能评估

##### 1 测试SpringWeb做为Provider Server的性能

Wrk压测工具

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

![image-20240324190216445](https://ipman-blog-1304583208.cos.ap-nanjing.myqcloud.com/rpcman/2024-03-24-110222.png)

压测性能: 24ms/rt





