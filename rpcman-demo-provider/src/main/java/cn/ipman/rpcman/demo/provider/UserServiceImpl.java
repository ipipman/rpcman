package cn.ipman.rpcman.demo.provider;

import cn.ipman.rpcman.core.annotation.RpcProvider;
import cn.ipman.rpcman.demo.api.User;
import cn.ipman.rpcman.demo.api.UserService;
import org.springframework.stereotype.Component;

@Component
@RpcProvider
public class UserServiceImpl implements UserService {


    @Override
    public User findById(int id) {
        return new User(id, "RpcMan-" + System.currentTimeMillis());
    }
}
