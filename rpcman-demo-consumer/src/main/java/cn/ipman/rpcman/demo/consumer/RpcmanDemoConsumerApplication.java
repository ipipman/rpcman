package cn.ipman.rpcman.demo.consumer;

import cn.ipman.rpcman.core.annotation.RpcConsumer;
import cn.ipman.rpcman.core.consumer.ConsumerConfig;
import cn.ipman.rpcman.demo.api.Order;
import cn.ipman.rpcman.demo.api.OrderService;
import cn.ipman.rpcman.demo.api.User;
import cn.ipman.rpcman.demo.api.UserService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.util.Arrays;

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

            User user1 = userService.findById(1, "ipman");
            System.out.println(user1);

            // 测试屏幕toString的远程调用
            System.out.println(userService.toString());

            // 测试基础类型 int
            long id = userService.getId(2L);
            System.out.println(id);

            System.out.println("userService.getId(User) ->" + userService.getId(new User(1, "ipman")));

            System.out.println("userService.getId(float) ->" + userService.getId(1.2f));

            // 测试String类型返回
            String name = userService.getName("ipman");
            System.out.println(name);

            // 测试多个Provider的调用
            Order order = orderService.findById(2);
            System.out.println(order);

            String user2 = userService.getName(12);
            System.out.println(user2);

            System.out.println("userService.getId() -> " + Arrays.toString(userService.getIds()));

            System.out.println("userService.getId() -> " + Arrays.toString(userService.getIds(new int[]{4, 5, 6})));

            System.out.println("userService.getLongId() -> " + Arrays.toString(userService.getLongIds()));

            System.out.println("userService.getLongId() -> " +
                    Arrays.toString(userService.getLongIds(new long[]{400L, 500L, 600})));


//            // 测试异常返回
//            Order order1 = orderService.findById(404);
//            System.out.println(order1);
        };
    }
}
