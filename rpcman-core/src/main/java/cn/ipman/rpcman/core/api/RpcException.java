package cn.ipman.rpcman.core.api;

import lombok.Data;

/**
 * RPC框架统一异常类
 *
 * @Author IpMan
 * @Date 2024/3/30 14:00
 */
@Data
public class RpcException extends RuntimeException {

    private String errCode;

    public RpcException() {
    }

    public RpcException(String message) {
        super(message);
    }

    public RpcException(String message, Throwable cause) {
        super(message, cause);
    }

    public RpcException(Throwable cause) {
        super(cause);
    }

    public RpcException(Throwable cause, String errCode) {
        super(cause);
        this.errCode = errCode;
    }

    // X => 技术类异常
    // Y => 业务类异常
    // Z => unknown异常,后续再归类到X或Y上
    public static final String SocketTimeoutEx = "X001-" + "http_invoke_timeout";
    public static final String NoSuchMethodEx = "X201-" + "method_not_exists";
    public static final String UnknownEx = "Z001-" + "unknown";

}