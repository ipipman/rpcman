package cn.ipman.rpcman.core.consumer;

import cn.ipman.rpcman.core.api.LoadBalancer;
import cn.ipman.rpcman.core.api.Router;
import cn.ipman.rpcman.core.api.RpcRequest;
import cn.ipman.rpcman.core.api.RpcResponse;
import cn.ipman.rpcman.core.util.MethodUtils;
import cn.ipman.rpcman.core.util.TypeUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import okhttp3.*;

import java.io.IOException;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Description for this class
 * 基于Java动态代理,实现Consumer调用Provider
 *
 * @Author IpMan
 * @Date 2024/3/10 20:03
 */
public class RpcInvocationHandler implements InvocationHandler {

    final static MediaType JSON_TYPE = MediaType.get("application/json; charset=utf-8");

    Class<?> service;
    Router router;
    LoadBalancer loadBalancer;
    String[] providers;


    public RpcInvocationHandler(Class<?> service, Router router,
                                LoadBalancer loadBalancer, String[] providers) {
        this.service = service;
        this.router = router;
        this.loadBalancer = loadBalancer;
        this.providers = providers;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 屏蔽一些Provider接口实现的方法
        if (MethodUtils.checkLocalMethod(method.getName())) {
            return null;
        }

        RpcRequest rpcRequest = new RpcRequest();
        rpcRequest.setService(service.getCanonicalName());
        rpcRequest.setMethodSign(MethodUtils.methodSign(method));
        rpcRequest.setArgs(args);

        // 获取路由,通过负载均衡选取一个代理的url
        List<String> urls = router.route(List.of(this.providers));
        String url = loadBalancer.choose(urls);
        System.out.println("loadBalancer.choose(urls) ==> " + url);

        // 请求 Provider
        RpcResponse<?> rpcResponse = post(rpcRequest, url);

        if (rpcResponse.isStatus()) {
            // 需要处理基础类型
            Object data = rpcResponse.getData();
            Class<?> type = method.getReturnType();
            if (data instanceof JSONObject jsonResult) {
                // 如: Object -> Map<k, v>
                if (Map.class.isAssignableFrom(type)) {
                    Map resultMap = new HashMap();
                    Type genericReturnType = method.getGenericReturnType();
                    System.out.println(genericReturnType);
                    if (genericReturnType instanceof ParameterizedType parameterizedType) {
                        Class<?> keyType = (Class<?>) parameterizedType.getActualTypeArguments()[0];
                        Class<?> valueType = (Class<?>) parameterizedType.getActualTypeArguments()[1];
                        System.out.println("keyType  : " + keyType);
                        System.out.println("valueType: " + valueType);
                        jsonResult.entrySet().stream().forEach(
                                e -> {
                                    Object key = TypeUtils.cast(e.getKey(), keyType);
                                    Object value = TypeUtils.cast(e.getValue(), valueType);
                                    resultMap.put(key, value);
                                }
                        );
                    }
                    return resultMap;
                }
                // 如: jsonObject -> Pojo
                return jsonResult.toJavaObject(type);

            } else if (data instanceof JSONArray jsonArray) {
                Object[] array = jsonArray.toArray();
                if (type.isArray()) {
                    // 如: array -> int[]{1,2,3}
                    Class<?> componentType = type.getComponentType();
                    Object resultArray = Array.newInstance(componentType, array.length);
                    for (int i = 0; i < array.length; i++) {
                        Array.set(resultArray, i, array[i]);
                    }
                    return resultArray;

                } else if (List.class.isAssignableFrom(type)) {
                    // 如: List<?>
                    List<Object> resultList = new ArrayList<>(array.length);
                    Type genericReturnType = method.getGenericReturnType();
                    System.out.println(genericReturnType);
                    if (genericReturnType instanceof ParameterizedType parameterizedType) {
                        // TODO:
                        Type actualType = parameterizedType.getActualTypeArguments()[0];
                        System.out.println(actualType);
                        for (Object o : array) {
                            resultList.add(TypeUtils.cast(o, (Class<?>) actualType));
                        }
                    } else {
                        resultList.addAll(Arrays.asList(array));
                    }
                    return resultList;
                } else {
                    return null;
                }
            } else {
                // 其它基础类型, 如: int, string..
                return TypeUtils.cast(data, method.getReturnType());
            }
        } else {
            // 调用异常时处理
            Exception ex = rpcResponse.getEx();
            throw new RuntimeException(ex);
        }
    }

    // 用okHttp进行远程传输
    OkHttpClient client = new OkHttpClient.Builder()
            .connectionPool(new ConnectionPool(16, 60, TimeUnit.SECONDS))
            .readTimeout(1, TimeUnit.SECONDS)
            .writeTimeout(1, TimeUnit.SECONDS)
            .connectTimeout(1, TimeUnit.SECONDS)
            .build();


    private RpcResponse<?> post(RpcRequest rpcRequest, String url) {
        String reqJson = JSON.toJSONString(rpcRequest);
        System.out.println(" ===> reqJson = " + reqJson);
        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(reqJson, JSON_TYPE))
                .build();
        try {
            String respJson = Objects.requireNonNull(client.newCall(request).execute().body()).string();
            System.out.println(" ===> respJson = " + respJson);
            return JSON.parseObject(respJson, RpcResponse.class);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

}