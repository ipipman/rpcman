package cn.ipman.rpcman.demo.provider;

import cn.ipman.rpcman.core.annotation.RpcProvider;
import cn.ipman.rpcman.demo.api.User;
import cn.ipman.rpcman.demo.api.UserService;
import org.springframework.stereotype.Component;


/**
 * Description for this class
 *
 * @Author IpMan
 * @Date 2024/3/9 20:07
 */

@Component
@RpcProvider
public class UserServiceImpl implements UserService {

    @Override
    public User findById(int id) {
        return new User(id, "RpcMan-" + System.currentTimeMillis() + ", id=" + id);
    }

    @Override
    public User findById(int id, String name) {
        return new User(id, "RpcMan-" + System.currentTimeMillis() + ", id=" + id + ", name=" + name);
    }

    @Override
    public int getId(int id) {
        return id;
    }

    @Override
    public String getName(String name) {
        return name;
    }

    @Override
    public String getName(int id) {
        return "ipman -> " + id;
    }
}

