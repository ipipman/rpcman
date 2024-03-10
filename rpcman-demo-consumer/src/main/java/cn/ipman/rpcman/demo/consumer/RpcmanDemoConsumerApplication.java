package cn.ipman.rpcman.demo.consumer;

import cn.ipman.rpcman.core.annotation.RpcConsumer;
import cn.ipman.rpcman.demo.api.User;
import cn.ipman.rpcman.demo.api.UserService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class RpcmanDemoConsumerApplication {

    @RpcConsumer
    UserService userService;

    public static void main(String[] args) {
        SpringApplication.run(RpcmanDemoConsumerApplication.class, args);
    }

    @Bean
    public ApplicationRunner consumerRunner() {
        return x -> {
            User user = userService.findById(1);
            System.out.println(user);
        };
    }
}
