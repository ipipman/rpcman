package cn.ipman.rpcman.core.util;

import java.lang.reflect.Method;
import java.util.Arrays;

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

    public static boolean checkLocalMethod(final Method method) {
        return method.getDeclaringClass().equals(Object.class);
    }

    public static String methodSign(Method method) {
        StringBuffer sb = new StringBuffer(method.getName());
        sb.append("@").append(method.getParameterCount());
        Arrays.stream(method.getParameterTypes()).forEach(
                t -> sb.append("_").append(t.getName())
        );
        return sb.toString();
    }

    public static String methodSign(Method method, Class<?> clazz) {
        return null;
    }

}
