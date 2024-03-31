package cn.ipman.rpcman.demo.provider;

import cn.ipman.rpcman.core.annotation.RpcProvider;
import cn.ipman.rpcman.demo.api.User;
import cn.ipman.rpcman.demo.api.UserService;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;


/**
 * Description for this class
 *
 * @Author IpMan
 * @Date 2024/3/9 20:07
 */

@Component
@RpcProvider
public class UserServiceImpl implements UserService {

    @Setter(onMethod_ = {@Autowired})
    Environment environment;

    @Override
    public User findById(int id) {
        return new User(id, "II-V10-"
                + environment.getProperty("server.port")
                + " ipman-" + System.currentTimeMillis());
    }

    @Override
    public User findById(int id, String name) {
        return new User(id, "ipman-" + name + "_" + System.currentTimeMillis());
    }

    @Override
    public long getId(long id) {
        return id;
    }

    @Override
    public long getId(User user) {
        return user.getId().longValue();
    }

    @Override
    public long getId(float id) {
        return 1L;
    }

    @Override
    public String getName() {
        return "ipman123";
    }

    @Override
    public String getName(int id) {
        return "ipman-" + id;
    }

    @Override
    public int[] getIds() {
        return new int[]{100, 200, 300};
    }

    @Override
    public long[] getLongIds() {
        return new long[]{1, 2, 3};
    }

    @Override
    public int[] getIds(int[] ids) {
        return ids;
    }

    @Override
    public User[] findUsers(User[] users) {
        return users;
    }

    @Override
    public List<User> getList(List<User> userList) {
        User user = userList.get(0);
        System.out.println(user.getId());
        System.out.println(user.getName());
        return userList;
    }

    @Override
    public Map<String, User> getMap(Map<String, User> userMap) {
        if (userMap.containsKey("A200")) {
            User user = userMap.get("A200");
            System.out.println(user.getId());
            System.out.println(user.getName());
        }
        return userMap;
    }

    @Override
    public Boolean getFlag(boolean flag) {
        return !flag;
    }

    @Override
    public User findById(long id) {
        return new User(Long.valueOf(id).intValue(), "ipman");
    }

    @Override
    public User ex(boolean flag) {
        if (flag) throw new RuntimeException("just throw an exception");
        return new User(100, "ipman-100");
    }

    String timeoutPorts = "8081";

    @Override
    public User find(int timeout) {
        String port = environment.getProperty("server.port");
        if (Arrays.asList(timeoutPorts.split(",")).contains(port)) {
            try {
                Thread.sleep(timeout);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        return new User(1001, "ipman-" + port);
    }

    @SuppressWarnings("all")
    public void setTimeoutPorts(String timeoutPorts) {
        this.timeoutPorts = timeoutPorts;
    }

}

