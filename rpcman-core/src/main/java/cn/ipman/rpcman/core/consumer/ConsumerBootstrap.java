package cn.ipman.rpcman.core.consumer;

import cn.ipman.rpcman.core.annotation.RpcConsumer;
import lombok.Data;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Description for this class
 *
 * @Author IpMan
 * @Date 2024/3/10 19:47
 */
@Data
public class ConsumerBootstrap implements ApplicationContextAware {

    ApplicationContext applicationContext;

    private Map<String, Object> stub = new HashMap<>();

    public void start() {
        // 获取Spring容器中所有的Bean
        String[] names = applicationContext.getBeanDefinitionNames();
        for (String name : names) {
            Object bean = applicationContext.getBean(name);
            // if (!name.contains("rpcmanDemoConsumerApplication")) continue;
            List<Field> fields = findAnnotatedFiled(bean.getClass());
            fields.forEach(f -> {
                Class<?> service = f.getType();
                String serviceName = service.getCanonicalName();
                Object consumer = stub.get(serviceName);
                if (consumer == null) {
                    consumer = createConsumer(service);
                }
                f.setAccessible(true);
                try {
                    f.set(bean, consumer);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            });

        }
    }

    private Object createConsumer(Class<?> service) {
        return Proxy.newProxyInstance(service.getClassLoader(),
                new Class[]{service}, new RpcInvocationHandler(service));
    }

    private List<Field> findAnnotatedFiled(Class<?> aClass) {
        List<Field> result = new ArrayList<>();
        while (aClass != null){
            Field[] fields = aClass.getDeclaredFields();
            for (Field f : fields) {
                if (f.isAnnotationPresent(RpcConsumer.class)) {
                    result.add(f);
                }
            }
            aClass = aClass.getSuperclass();
        }
        return result;
    }

}
