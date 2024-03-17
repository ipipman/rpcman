package cn.ipman.rpcman.core.registry;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * Description for this class
 *
 * @Author IpMan
 * @Date 2024/3/17 21:54
 */
@Data
@AllArgsConstructor
public class Event {
    List<String> data;

}

