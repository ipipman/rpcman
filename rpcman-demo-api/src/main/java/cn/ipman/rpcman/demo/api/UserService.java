package cn.ipman.rpcman.demo.api;

public interface UserService {

    User findById(int id);

    int getId(int id);

    String getName(String name);
}
