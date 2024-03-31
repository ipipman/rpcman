package cn.ipman.rpcman.core.provider;

import cn.ipman.rpcman.core.api.RpcException;
import cn.ipman.rpcman.core.api.RpcRequest;
import cn.ipman.rpcman.core.api.RpcResponse;
import cn.ipman.rpcman.core.meta.ProviderMeta;
import cn.ipman.rpcman.core.util.TypeUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.MultiValueMap;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;

/**
 * Description for this class
 *
 * @Author IpMan
 * @Date 2024/3/23 11:52
 */
@Slf4j
public class ProviderInvoker {

    private final MultiValueMap<String, ProviderMeta> skeleton;

    public ProviderInvoker(ProviderBootstrap providerBootstrap) {
        this.skeleton = providerBootstrap.getSkeleton();
    }

    public RpcResponse<?> invoke(RpcRequest request) {
        log.debug("RpcRequest request={}", request);
        RpcResponse<Object> rpcResponse = new RpcResponse<>();
        // 根据类包名,获取容器的类实例
        List<ProviderMeta> providerMetas = this.skeleton.get(request.getService());
        try {
            String methodSign = request.getMethodSign();
            // 从元数据里获取类方法
            ProviderMeta meta = findProviderMeta(providerMetas, methodSign);
            Method method = meta.getMethod();

            // 参数类型转换
            Object[] args = processArgs(request.getArgs(), method.getParameterTypes(), method.getGenericParameterTypes());
            // 传入方法参数,通过反射 调用目标provider方法
            Object result = method.invoke(meta.getServiceImpl(), args);

            rpcResponse.setStatus(true);
            rpcResponse.setData(result);
        } catch (InvocationTargetException e) {
            // Provider反射时异常处理, TODO 返回反射目标类的异常
            rpcResponse.setEx(new RpcException(e.getTargetException().getMessage()));
        } catch (IllegalAccessException e) {
            // Provider反射调用时异常
            rpcResponse.setEx(new RpcException(e.getMessage()));
        } catch (Exception e) {
            rpcResponse.setEx(new RpcException(e.getMessage()));
        }
        log.debug("RpcResponse rpcResponse={}", request);
        return rpcResponse;
    }

    private Object[] processArgs(Object[] args, Class<?>[] parameterTypes, Type[] genericParameterTypes) {
        if (args == null || args.length == 0) return args;
        // 参数类型转换
        Object[] actualArgs = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            actualArgs[i] = TypeUtils.cast(args[i], parameterTypes[i], genericParameterTypes[i]);
        }
        return actualArgs;
    }

    private ProviderMeta findProviderMeta(List<ProviderMeta> providerMetas, String methodSign) {
        // 寻找方法签名是否存在
        Optional<ProviderMeta> optional = providerMetas.stream()
                .filter(x -> x.getMethodSign().equals(methodSign)).findFirst();
        return optional.orElse(null);
    }

}
