package cn.ipman.rpcman.core.util;

/**
 * Description for this class
 *
 * @Author IpMan
 * @Date 2024/3/13 23:07
 */
public class MethodUtils {

    public static boolean checkLocalMethod(final String method) {
        //本地方法不代理
        return "toString".equals(method) ||
                "hashCode".equals(method) ||
                "notifyAll".equals(method) ||
                "equals".equals(method) ||
                "wait".equals(method) ||
                "getClass".equals(method) ||
                "notify".equals(method);
    }


}
