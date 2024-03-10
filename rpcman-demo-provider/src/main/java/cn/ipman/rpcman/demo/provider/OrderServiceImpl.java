package cn.ipman.rpcman.demo.provider;

import cn.ipman.rpcman.core.annotation.RpcProvider;
import cn.ipman.rpcman.demo.api.Order;
import cn.ipman.rpcman.demo.api.OrderService;
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
public class OrderServiceImpl implements OrderService {

    @Override
    public Order findById(int id) {
        // test
        if (id == 404) {
            throw new RuntimeException("404 exception");
        }
        return new Order(id, "RpcMan-" + System.currentTimeMillis() + ", id=" + id);
    }

}
