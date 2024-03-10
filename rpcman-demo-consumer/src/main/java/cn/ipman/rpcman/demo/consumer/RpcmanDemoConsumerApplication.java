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
            User user = userService.findById(1);
            System.out.println(user);

            System.out.println(userService.toString());

            int id = userService.getId(2);
            System.out.println(id);

            Order order = orderService.findById(2);
            System.out.println(order);

            Order order1 = orderService.findById(404);
            System.out.println(order1);
        };
    }
}
