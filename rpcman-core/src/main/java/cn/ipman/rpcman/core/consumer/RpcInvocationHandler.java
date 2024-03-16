package cn.ipman.rpcman.core.consumer;

import cn.ipman.rpcman.core.api.RpcRequest;
import cn.ipman.rpcman.core.api.RpcResponse;
import cn.ipman.rpcman.core.util.MethodUtils;
import cn.ipman.rpcman.core.util.TypeUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import okhttp3.*;

import javax.print.attribute.standard.JobKOctets;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Objects;
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

    public RpcInvocationHandler(Class<?> service) {
        this.service = service;
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

        // 请求 Provider
        RpcResponse<?> rpcResponse = post(rpcRequest);
        if (rpcResponse.isStatus()) {
            // 需要处理基础类型
            Object data = rpcResponse.getData();
            if (data instanceof JSONObject) {
                // 反序列化,json对象类型,如:Map -> Object
                JSONObject jsonResult = (JSONObject) rpcResponse.getData();
                return jsonResult.toJavaObject(method.getReturnType());

            } else if (data instanceof JSONArray jsonArray) {
                // 反序列化,json数组类型,如:List -> int[]
                Object[] array = jsonArray.toArray();
                Class<?> componentType = method.getReturnType().getComponentType();
                //System.out.println("componentType => " + componentType);
                Object resultArray = Array.newInstance(componentType, array.length);
                for (int i = 0; i < array.length; i++) {
                    Array.set(resultArray, i, array[i]);
                }
                return resultArray;

            } else {
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


    private RpcResponse<?> post(RpcRequest rpcRequest) {
        String reqJson = JSON.toJSONString(rpcRequest);
        System.out.println(" ===> reqJson = " + reqJson);
        Request request = new Request.Builder()
                .url("http://localhost:8080")
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