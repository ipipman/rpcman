package cn.ipman.rpcman.core.util;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

/**
 * Description for this class
 *
 * @Author IpMan
 * @Date 2024/3/14 22:31
 */
public class TypeUtils {

    public static Object castMethodResult(Method method, Object data) {
        Class<?> type = method.getReturnType();
        if (data instanceof JSONObject jsonResult) {
            // 如: Object -> Map<k, v>
            if (Map.class.isAssignableFrom(type)) {
                Map resultMap = new HashMap();
                Type genericReturnType = method.getGenericReturnType();
                System.out.println(genericReturnType);
                if (genericReturnType instanceof ParameterizedType parameterizedType) {
                    Class<?> keyType = (Class<?>) parameterizedType.getActualTypeArguments()[0];
                    Class<?> valueType = (Class<?>) parameterizedType.getActualTypeArguments()[1];
                    System.out.println("keyType  : " + keyType);
                    System.out.println("valueType: " + valueType);
                    jsonResult.entrySet().stream().forEach(
                            e -> {
                                Object key = cast(e.getKey(), keyType);
                                Object value = cast(e.getValue(), valueType);
                                resultMap.put(key, value);
                            }
                    );
                }
                return resultMap;
            }
            // 如: jsonObject -> Pojo
            return jsonResult.toJavaObject(type);

        } else if (data instanceof JSONArray jsonArray) {
            Object[] array = jsonArray.toArray();
            if (type.isArray()) {
                // 如: array -> int[]{1,2,3}
                Class<?> componentType = type.getComponentType();
                Object resultArray = Array.newInstance(componentType, array.length);
                for (int i = 0; i < array.length; i++) {
                    if (componentType.isPrimitive() || componentType.getPackageName().startsWith("java")) {
                        Array.set(resultArray, i, array[i]);
                    } else {
                        Object castObject = cast(array[i], componentType);
                        Array.set(resultArray, i, castObject);
                    }
                }
                return resultArray;

            } else if (List.class.isAssignableFrom(type)) {
                // 如: List<?>
                List<Object> resultList = new ArrayList<>(array.length);
                Type genericReturnType = method.getGenericReturnType();
                System.out.println(genericReturnType);
                if (genericReturnType instanceof ParameterizedType parameterizedType) {
                    // TODO:
                    Type actualType = parameterizedType.getActualTypeArguments()[0];
                    System.out.println(actualType);
                    for (Object o : array) {
                        resultList.add(cast(o, (Class<?>) actualType));
                    }
                } else {
                    resultList.addAll(Arrays.asList(array));
                }
                return resultList;
            } else {
                return null;
            }
        } else {
            // 其它基础类型, 如: int, string..
            return cast(data, method.getReturnType());
        }
    }

    public static Object cast(Object origin, Class<?> type) {
        if (origin == null) return null;
        Class<?> aClass = origin.getClass();

        // 如果要转换的类型,已经是它的原始类型,就直接返回
        if (type.isAssignableFrom(aClass)) {
            return origin;
        }

        // 参数序列化, List -> int[]
        if (type.isArray()) {
            if (origin instanceof List<?> list) {
                origin = list.toArray();
            }
            int length = Array.getLength(origin);
            Class<?> componentType = type.getComponentType();
            Object resultArray = Array.newInstance(componentType, length);
            for (int i = 0; i < length; i++) {
                if (componentType.isPrimitive() || componentType.getPackageName().startsWith("java")) {
                    Array.set(resultArray, i, Array.get(origin, i));
                } else {
                    Object castObject = cast(Array.get(origin, i), componentType);
                    Array.set(resultArray, i, castObject);
                }
            }
            return resultArray;
        }

        // 参数序列化,Map -> Object
        if (origin instanceof HashMap map) {
            JSONObject jsonObject = new JSONObject(map);
            return jsonObject.toJavaObject(type);
        }

        // 序列换, Object -> Pojo
        if (origin instanceof JSONObject jsonObject) {
            return jsonObject.toJavaObject(type);
        }

        // 基础类型解析
        if (type.equals(Long.class) || type.equals(Long.TYPE)) {
            return Long.valueOf(origin.toString());
        } else if (type.equals(Integer.class) || type.equals(Integer.TYPE)) {
            return Integer.valueOf(origin.toString());
        } else if (type.equals(Float.class) || type.equals(Float.TYPE)) {
            return Float.valueOf(origin.toString());
        } else if (type.equals(Double.class) || type.equals(Double.TYPE)) {
            return Double.valueOf(origin.toString());
        } else if (type.equals(Byte.class) || type.equals(Byte.TYPE)) {
            return Byte.valueOf(origin.toString());
        } else if (type.equals(Short.class) || type.equals(Short.TYPE)) {
            return Short.valueOf(origin.toString());
        } else if (type.equals(Character.class) || type.equals(Character.TYPE)) {
            return origin.toString().charAt(0);
        } else if (type.equals(Boolean.class) || type.equals(Boolean.TYPE)) {
            return Boolean.valueOf(origin.toString());
        }

        return null;
    }
}
