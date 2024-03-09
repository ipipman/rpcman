package cn.ipman.rpcman.core.api;

import lombok.Data;

@Data
public class RpcRequest {

    private String service; // 接口: cn.ipman.rpcman.demo.api.UserService
    private String method;
    private Object[] args;
}
