package cn.ipman.rpcman.core.api;

import java.util.List;

/**
 * 注册中心
 *
 * @Author IpMan
 * @Date 2024/3/16 20:39
 */
public interface RegistryCenter {

    void start();

    void stop();

    // Provider侧
    void register(String service, String instance);

    void unregister(String service, String instance);

    // Consumer侧
    List<String> fetchAll(String service);
    // void subscribe();

    /**
     * 静态的注册中心
     */
    class StaticRegistryCenter implements RegistryCenter {

        List<String> providers;

        public StaticRegistryCenter(List<String> providers) {
            this.providers = providers;
        }

        @Override
        public void start() {
            System.out.println("注册中心 start...");
        }

        @Override
        public void stop() {
            System.out.println("注册中心 stop...");
        }

        @Override
        public void register(String service, String instance) {

        }

        @Override
        public void unregister(String service, String instance) {

        }

        @Override
        public List<String> fetchAll(String service) {
            return this.providers;
        }
    }

}
