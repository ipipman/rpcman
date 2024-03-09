package cn.ipman.rpcman.core.api;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RpcResponse<T> {

    boolean status; // 状态:true
    T data; // new User

}
