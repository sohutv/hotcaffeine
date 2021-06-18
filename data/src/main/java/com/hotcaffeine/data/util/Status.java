package com.hotcaffeine.data.util;
/**
 * 返回状态
 * @Description: 
 * @author yongfeigao
 * @date 2018年6月12日
 */
public enum Status {
    // 2xx代表正常返回
    OK(200, "OK"), 
    NO_RESULT(201, "暂无数据"),
    
    // 3xx代表参数问题
    PARAM_ERROR(300, "参数错误"),
    
    // 5xx代表外部依赖异常
    DB_ERROR(500, "数据库异常"),
    
    // 6xx代表web请求异常
    WEB_ERROR(600, "请求错误"),
    ;

    private int key;
    private String value;

    private Status(int key, String value) {
        this.key = key;
        this.value = value;
    }

    public int getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }
}
