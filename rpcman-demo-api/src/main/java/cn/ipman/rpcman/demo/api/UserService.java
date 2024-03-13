package cn.ipman.rpcman.demo.api;

public interface UserService {

    User findById(int id);

    User findById(int id, String name);

    int getId(int id);

    String getName(String name);

    String getName(int id);
}
