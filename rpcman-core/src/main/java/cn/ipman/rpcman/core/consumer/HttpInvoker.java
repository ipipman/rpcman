package cn.ipman.rpcman.core.consumer;

import cn.ipman.rpcman.core.api.RpcRequest;
import cn.ipman.rpcman.core.api.RpcResponse;

/**
 * Description for this class
 *
 * @Author IpMan
 * @Date 2024/3/23 12:19
 */
public interface HttpInvoker {

    RpcResponse<?> post(RpcRequest rpcRequest, String url);

}
