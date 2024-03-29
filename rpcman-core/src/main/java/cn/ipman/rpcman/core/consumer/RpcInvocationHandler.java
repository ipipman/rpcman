package cn.ipman.rpcman.core.consumer;

import cn.ipman.rpcman.core.api.*;
import cn.ipman.rpcman.core.consumer.http.OkHttpInvoker;
import cn.ipman.rpcman.core.meta.InstanceMeta;
import cn.ipman.rpcman.core.util.MethodUtils;
import cn.ipman.rpcman.core.util.TypeUtils;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.*;
import java.util.*;


/**
 * Description for this class
 * 基于Java动态代理,实现Consumer调用Provider
 *
 * @Author IpMan
 * @Date 2024/3/10 20:03
 */
@Slf4j
public class RpcInvocationHandler implements InvocationHandler {

    Class<?> service;
    RpcContext rpcContext;
    List<InstanceMeta> providers;

    HttpInvoker httpInvoker = new OkHttpInvoker();


    public RpcInvocationHandler(Class<?> service, RpcContext rpcContext, List<InstanceMeta> providers) {
        this.service = service;
        this.rpcContext = rpcContext;
        this.providers = providers;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        // 屏蔽一些Provider接口实现的方法
        if (MethodUtils.checkLocalMethod(method.getName())) {
            return null;
        }

        RpcRequest rpcRequest = new RpcRequest();
        rpcRequest.setService(this.service.getCanonicalName());
        rpcRequest.setMethodSign(MethodUtils.methodSign(method));
        rpcRequest.setArgs(args);

        // 前置过滤器
        for (Filter filter : this.rpcContext.getFilters()) {
            Object preResult = filter.preFilter(rpcRequest);
            // preResult == null 代表被过滤
            if (preResult != null) {
                log.info(filter.getClass().getName() + " ==> preFilter:" + preResult);
                return preResult;
            }
        }

        // 获取路由,通过负载均衡选取一个代理的url
        List<InstanceMeta> instances = rpcContext.getRouter().route(this.providers);
        InstanceMeta instance = rpcContext.getLoadBalancer().choose(instances);
        log.debug("loadBalancer.choose(urls) ==> " + instance);

        // 请求 Provider
        RpcResponse<?> rpcResponse = this.httpInvoker.post(rpcRequest, instance.toHttpUrl());
        Object result = castResponseToResult(method, rpcResponse);

        // 后置过滤器, 这里拿到的可能不是最终值, 需要再设计一下
        for (Filter filter : this.rpcContext.getFilters()) {
            Object filterResult = filter.postFilter(rpcRequest, rpcResponse, result);
            // filterResult == null 代表不过滤
            if (filterResult != null) {
                log.info(filter.getClass().getName() + " ==> postFilter:" + filterResult);
                return filterResult;
            }
        }

        return result;
    }

    @Nullable
    private static Object castResponseToResult(Method method, RpcResponse<?> rpcResponse) {
        if (rpcResponse.isStatus()) {
            // 处理方法,返回类型
            Object data = rpcResponse.getData();
            return TypeUtils.castMethodResult(method, data);
        } else {
            // 调用异常时处理
            Exception ex = rpcResponse.getEx();
            throw new RuntimeException(ex);
        }
    }

}