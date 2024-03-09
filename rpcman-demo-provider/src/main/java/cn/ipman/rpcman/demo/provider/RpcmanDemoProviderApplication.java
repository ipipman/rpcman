package cn.ipman.rpcman.demo.provider;

import cn.ipman.rpcman.core.api.RpcRequest;
import cn.ipman.rpcman.core.api.RpcResponse;
import cn.ipman.rpcman.core.provider.ProviderBootstrap;
import cn.ipman.rpcman.core.provider.ProviderConfig;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
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

    @Setter(onMethod_ = {@Autowired})
    private ProviderBootstrap providerBootstrap;

    @RequestMapping(value = "/")
    public RpcResponse<?> invoke(@RequestBody RpcRequest request) {
        return providerBootstrap.invoke(request);
    }

    /**
     * 在Spring容器启动后,模拟调用Provider
     */
    @Bean
    public ApplicationRunner providerRun() {
        return x -> {
            RpcRequest request = new RpcRequest();
            request.setService("cn.ipman.rpcman.demo.api.UserService");
            request.setMethod("findById");
            request.setArgs(new Object[]{100});

            // 根据接口描述,调用接口
            RpcResponse<?> rpcResponse = invoke(request);
            System.out.println("return : " + rpcResponse.getData());
        };
    }

}
