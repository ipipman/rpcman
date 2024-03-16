package cn.ipman.rpcman.core.api;

import java.util.List;

public interface LoadBalancer {

    /**
     * 选择provider
     */
    String choose(List<String> providers);

    LoadBalancer Default = p -> (p == null || p.isEmpty()) ? null : p.get(0);


}
