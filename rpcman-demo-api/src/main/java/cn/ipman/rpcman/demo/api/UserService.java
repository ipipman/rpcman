package cn.ipman.rpcman.demo.api;

public interface UserService {

    User findById(int id);

    User findById(int id, String name);

    long getId(long id);

    String getName(String name);

    String getName(int id);

    long getId(User user);

    long getId(float id);

    int[] getIds();
}
