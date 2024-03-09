package cn.ipman.rpcman.core.annotation;


import java.lang.annotation.*;

/**
 * Description for this class
 * RPC生产者类标注, 被 @RpcProvider 声明的类会RpcMan框架被自动加载与注入
 *
 * @Author Ip Man
 * @Date 2024/3/9 20:07
 */

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface RpcProvider {
}
