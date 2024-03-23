package cn.ipman.rpcman.core.api;

import cn.ipman.rpcman.core.meta.InstanceMeta;
import lombok.Data;

import java.util.List;

/**
 * Description for this class
 *
 * @Author IpMan
 * @Date 2024/3/16 20:26
 */
@Data
public class RpcContext {

    List<Filter> filters;

    Router<InstanceMeta> router;

    LoadBalancer<InstanceMeta> loadBalancer;
}
