package cn.ipman.rpcman.core.api;

import cn.ipman.rpcman.core.registry.ChangedListener;

import javax.swing.event.ChangeListener;
import java.util.List;

/**
 * 注册中心
 *
 * @Author IpMan
 * @Date 2024/3/16 20:39
 */
public interface RegistryCenter {

    void start(); // provider || consumer

    void stop(); // provider || consumer

    // Provider侧
    void register(String service, String instance); // provider

    void unregister(String service, String instance); // provider

    // Consumer侧
    List<String> fetchAll(String service); // consumer

    void subscribe(String service, ChangedListener listener); // consumer
    // heartbeat()
    void unsubscribe();

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
        public void subscribe(String service, ChangedListener listener) {

        }

        @Override
        public void unsubscribe() {

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
