package cn.ipman.rpcman.core.meta;

import lombok.Builder;
import lombok.Data;

/**
 * 描述服务的元数据
 *
 * @Author IpMan
 * @Date 2024/3/23 14:42
 */
@Data
@Builder
public class ServiceMeta {

    private String app;
    private String namespace;
    private String env;
    private String name; //cn.ip
    private String version;

    public String toPath() {
        return String.format("%s_%s_%s_%s_%s", app, namespace, env, name, version);
    }

}
