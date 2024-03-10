package cn.ipman.rpcman.demo.consumer;

import cn.ipman.rpcman.core.annotation.RpcConsumer;
import cn.ipman.rpcman.core.consumer.ConsumerConfig;
import cn.ipman.rpcman.demo.api.Order;
import cn.ipman.rpcman.demo.api.OrderService;
import cn.ipman.rpcman.demo.api.User;
import cn.ipman.rpcman.demo.api.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import({ConsumerConfig.class})
public class RpcmanDemoConsumerApplication {

    @RpcConsumer
    UserService userService;

    @RpcConsumer
    OrderService orderService;

    public static void main(String[] args) {
        SpringApplication.run(RpcmanDemoConsumerApplication.class, args);
    }

    @Bean
    public ApplicationRunner consumerRunner() {
        return x -> {
            // 测试返回一个Java Object
            User user = userService.findById(1);
            System.out.println(user);

            // 测试屏幕toString的远程调用
            System.out.println(userService.toString());

            // 测试基础类型 int
            int id = userService.getId(2);
            System.out.println(id);

            // 测试String类型返回
            String name = userService.getName("ipman");
            System.out.println(name);

            // 测试多个Provider的调用
            Order order = orderService.findById(2);
            System.out.println(order);

            // 测试异常返回
//            Order order1 = orderService.findById(404);
//            System.out.println(order1);
        };
    }
}
