package cn.ipman.rpcman.core.api;

import java.util.List;

public interface Router<T> {

    List<T> route(List<T> providers);

    @SuppressWarnings("rawtypes")
    Router Default = p -> p;

}
