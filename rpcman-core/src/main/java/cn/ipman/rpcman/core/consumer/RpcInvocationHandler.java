package cn.ipman.rpcman.core.consumer;

import cn.ipman.rpcman.core.api.*;
import cn.ipman.rpcman.core.consumer.http.OkHttpInvoker;
import cn.ipman.rpcman.core.governance.SlidingTimeWindow;
import cn.ipman.rpcman.core.meta.InstanceMeta;
import cn.ipman.rpcman.core.util.MethodUtils;
import cn.ipman.rpcman.core.util.TypeUtils;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.*;
import java.net.SocketTimeoutException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


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
    final List<InstanceMeta> providers;
    final List<InstanceMeta> isolateProviders = new ArrayList<>();
    final List<InstanceMeta> halfOpenProviders = new ArrayList<>();
    HttpInvoker httpInvoker;
    final Map<String, SlidingTimeWindow> windows = new HashMap<>();
    ScheduledExecutorService executorService;


    public RpcInvocationHandler(Class<?> service, RpcContext rpcContext, List<InstanceMeta> providers) {
        this.service = service;
        this.rpcContext = rpcContext;
        this.providers = providers;
        int timeout = Integer.parseInt(rpcContext.getParameters()
                .getOrDefault("app.timeout", "1000"));
        this.httpInvoker = new OkHttpInvoker(timeout);
        // 定时探活Provider的运行状态 , 单线程, 延迟10s执行, 每60s执行一次
        this.executorService = Executors.newScheduledThreadPool(1);
        this.executorService.scheduleWithFixedDelay(this::halfOpen, 10, 60, TimeUnit.SECONDS);
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

        // 重试次数
        int retries = Integer.parseInt(rpcContext.getParameters()
                .getOrDefault("app.retries", "1"));

        while (retries-- > 0) {
            log.info(" ===> retries: " + retries);
            try {
                // [Filter Before] 前置过滤器
                for (Filter filter : this.rpcContext.getFilters()) {
                    Object preResult = filter.preFilter(rpcRequest);
                    // preResult == null 代表被过滤
                    if (preResult != null) {
                        log.info(filter.getClass().getName() + " ==> preFilter:" + preResult);
                        return preResult;
                    }
                }

                InstanceMeta instance;
                synchronized (halfOpenProviders) {
                    if (halfOpenProviders.isEmpty()) {
                        // 获取路由,通过负载均衡选取一个代理的url
                        List<InstanceMeta> instances = rpcContext.getRouter().route(this.providers);
                        instance = rpcContext.getLoadBalancer().choose(instances);
                        log.debug("loadBalancer.choose(urls) ==> {}", instance);
                    } else {
                        // 如果有半开的Provider节点, 需要做探活
                        instance = halfOpenProviders.remove(0);
                        log.debug("check alive instance ==> {}", instance);
                    }
                }

                RpcResponse<?> rpcResponse;
                Object result;
                String url = instance.toHttpUrl();
                try {
                    // 请求 Provider
                    rpcResponse = this.httpInvoker.post(rpcRequest, url);
                    result = castResponseToResult(method, rpcResponse);
                } catch (Exception e) {
                    // 故障的规则统计和隔离
                    // 每一次异常, 记录一次, 统计30s的异常数.
                    synchronized (windows) {
                        SlidingTimeWindow window = windows.computeIfAbsent(url, k -> new SlidingTimeWindow());
                        window.record(System.currentTimeMillis());
                        log.debug("instance {} in windows with {}", url, window.getSum());
                        // 规则发生10次, 就做故障隔离, 摘除节点
                        if (window.getSum() >= 10) {
                            isolate(instance);
                        }
                    }
                    throw e;
                }

                synchronized (providers) {
                    // 如果Provider实例调用成功, 但不在providers里,证明是探活成功了,需要在providers中恢复这个节点
                    if (!providers.contains(instance)) {
                        isolateProviders.remove(instance);
                        providers.add(instance);
                        log.debug("instance {} is recovered, isolatedProviders={}, providers={}",
                                instance, isolateProviders, providers);
                    }
                }

                // [Filter After] 后置过滤器, 这里拿到的可能不是最终值, 需要再设计一下
                for (Filter filter : this.rpcContext.getFilters()) {
                    Object filterResult = filter.postFilter(rpcRequest, rpcResponse, result);
                    // filterResult == null 代表不过滤
                    if (filterResult != null) {
                        log.info(filter.getClass().getName() + " ==> postFilter:" + filterResult);
                        return filterResult;
                    }
                }
                return result;
            } catch (RuntimeException ex) {
                // 如果不是超时异常,就直接throw
                if (!(ex.getCause() instanceof SocketTimeoutException)) {
                    throw ex;
                }
            }
        }
        return null;
    }


    private void isolate(InstanceMeta instance) {
        // 故障隔离, 服务拆除
        log.debug(" ==>  providers isolate instance: " + instance);
        providers.remove(instance);
        log.debug(" ==>  providers = {}", providers);
        isolateProviders.add(instance);
        log.debug(" ==>  isolateProviders = {}", isolateProviders);
    }


    private void halfOpen() {
        // 故障半开, 服务探活
        log.debug(" ==> providers half open isolateProviders:" + isolateProviders);
        halfOpenProviders.clear();
        halfOpenProviders.addAll(isolateProviders);
    }


    @Nullable
    private static Object castResponseToResult(Method method, RpcResponse<?> rpcResponse) {
        if (rpcResponse.isStatus()) {
            // 处理方法,返回类型
            return TypeUtils.castMethodResult(method, rpcResponse.getData());
        } else {
            Exception exception = rpcResponse.getEx();
            if (exception instanceof RpcException ex) {
                throw ex; // RpcException是Runtime异常, 可以直接throw
            }
            // 调用未知异常时
            throw new RpcException(rpcResponse.getEx(), RpcException.UnknownEx);
        }
    }

}