package cn.ipman.rpcman.core.registry;

@FunctionalInterface
public interface ChangedListener {

    void fire(Event event);

}
