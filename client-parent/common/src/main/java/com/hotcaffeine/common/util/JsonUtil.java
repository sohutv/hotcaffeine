package com.hotcaffeine.common.util;

import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

/**
 * @author wuweifeng wrote on 2018/11/23.
 */
public class JsonUtil {

    public static String toJSON(Object object) {
        return JSON.toJSONString(object);
    }

    public static <T> T toBean(String text, Class<T> clazz) {
        return JSON.parseObject(text, clazz);
    }

    // 转换为List
    public static <T> List<T> toList(String text, Class<T> clazz) {
        return JSON.parseArray(text, clazz);
    }
    
    /**
     * json字符串转化为map
     */
    @SuppressWarnings("unchecked")
    public static <K, V> Map<K, V> toCollect(String s) {
        return (Map<K, V>) JSONObject.parseObject(s);
    }
}

