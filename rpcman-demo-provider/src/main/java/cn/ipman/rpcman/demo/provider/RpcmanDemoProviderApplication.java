package cn.ipman.rpcman.demo.provider;

import cn.ipman.rpcman.core.api.RpcRequest;
import cn.ipman.rpcman.core.api.RpcResponse;
import cn.ipman.rpcman.core.provider.ProviderBootstrap;
import cn.ipman.rpcman.core.provider.ProviderConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.*;

@SpringBootApplication
@RestController
@Import({ProviderConfig.class})
public class RpcmanDemoProviderApplication {

    public static void main(String[] args) {
        SpringApplication.run(RpcmanDemoProviderApplication.class, args);
    }

    @Autowired
    ProviderBootstrap providerBootstrap;

    @RequestMapping(value = "/")
    public RpcResponse<?> invoke(@RequestBody RpcRequest request) {
        return providerBootstrap.invoke(request);
    }

    @Bean
    ApplicationRunner providerRun() {
        return x -> {
            RpcRequest request = new RpcRequest();
            request.setService("cn.ipman.rpcman.demo.api.UserService");
            request.setMethod("findById");
            request.setArgs(new Object[]{100});

            RpcResponse rpcResponse = invoke(request);
            System.out.println("return : " + rpcResponse.getData());
        };
    }

}
