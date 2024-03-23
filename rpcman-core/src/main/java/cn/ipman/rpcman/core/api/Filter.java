package cn.ipman.rpcman.core.api;

/**
 * 过滤器
 */
public interface Filter {

    Object preFilter(RpcRequest request);

    Object postFilter(RpcRequest request, RpcResponse<?> response, Object result);


    Filter Default = new Filter() {
        @Override
        public Object preFilter(RpcRequest request) {
            return null;
        }

        @Override
        public Object postFilter(RpcRequest request, RpcResponse<?> response, Object result) {
            return null;
        }
    };

}
