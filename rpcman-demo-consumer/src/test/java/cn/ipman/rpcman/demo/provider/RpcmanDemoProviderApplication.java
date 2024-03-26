package cn.ipman.rpcman.demo.provider;

import cn.ipman.rpcman.core.api.RpcRequest;
import cn.ipman.rpcman.core.api.RpcResponse;
import cn.ipman.rpcman.core.provider.ProviderConfig;
import cn.ipman.rpcman.core.provider.ProviderInvoker;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
@Import({ProviderConfig.class})
@Slf4j
public class RpcmanDemoProviderApplication {

//    public static void main(String[] args) {
//        SpringApplication.run(RpcmanDemoProviderApplication.class, args);
//    }

    @Setter(onMethod_ = {@Autowired})
    private ProviderInvoker providerInvoker;

    @RequestMapping(value = "/")
    public RpcResponse<?> invoke(@RequestBody RpcRequest request) {
        return providerInvoker.invoke(request);
    }

    /**
     * 在Spring容器启动后,模拟调用Provider
     */
    @Bean
    public ApplicationRunner providerRun() {
        return x -> {
            RpcRequest request = new RpcRequest();
            request.setService("cn.ipman.rpcman.demo.api.UserService");
            request.setMethodSign("findById@1_int");
            request.setArgs(new Object[]{100});

            // 根据接口描述,调用接口
            RpcResponse<?> rpcResponse = invoke(request);
            log.info("return : " + rpcResponse.getData());

            RpcRequest request1 = new RpcRequest();
            request1.setService("cn.ipman.rpcman.demo.api.UserService");
            request1.setMethodSign("findById@2_int_java.lang.String");
            request1.setArgs(new Object[]{100, "ipman"});

            // 根据接口描述,调用接口
            RpcResponse<?> rpcResponse1 = invoke(request1);
            log.info("return : " + rpcResponse1.getData());
        };


    }

}
