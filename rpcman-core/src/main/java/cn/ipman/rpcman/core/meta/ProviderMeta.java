package cn.ipman.rpcman.core.meta;

import lombok.Data;

import java.lang.reflect.Method;

/**
 * Description for this class
 *
 * @Author IpMan
 * @Date 2024/3/13 23:22
 */
@Data
public class ProviderMeta {

    Method method; // 实现类的方法
    String methodSign; // 方法签名
    Object serviceImpl; // 实现类

}
