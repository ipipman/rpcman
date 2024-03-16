package cn.ipman.rpcman.core.cluster;

import cn.ipman.rpcman.core.api.LoadBalancer;

import java.util.List;
import java.util.Random;

/**
 * Description for this class
 *
 * @Author IpMan
 * @Date 2024/3/16 19:53
 */
public class RandomLoadBalancer implements LoadBalancer {

    Random random = new Random();

    @Override
    public String choose(List<String> providers) {
        if (providers == null || providers.isEmpty()) return null;
        if (providers.size() == 1) return providers.get(0);
        // 随机返回
        return providers.get(random.nextInt(providers.size()));
    }
}
