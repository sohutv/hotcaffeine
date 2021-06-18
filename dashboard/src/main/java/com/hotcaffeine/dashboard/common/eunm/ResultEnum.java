package com.hotcaffeine.dashboard.common.eunm;


public enum ResultEnum {

    /**
     * 操作成功
     */
    SUCCESS(200, "操作成功！"),

    NO_RESULT(201, "暂无数据"),

    NO_CHANGE(1001, "操作无影响"),
    
    LOGIN_FAILED(1003, "登录失败"),

    NO_PERMISSION(1004, "没有操作权限"),

    PARAM_ERROR(1005, "参数错误"),

    BIZ_ERROR(1006, "业务异常"),

    CONFLICT_ERROR(1007, "用户名/手机号已存在"),

    TIME_RANGE_LARGE(1022, "查询时间过大"),

    APP_CONFLICT(1050, "app名称重复"),

    USER_APP_CONFLICT(1051, "关联关系已存在"),

    DB_ERROR(1100, "数据库异常"),
    DB_DUPLICATE_KEY(1101, "数据重复"),

    WEB_ERROR(1200, "系统出错，请联系管理员"),

    ETCD_REGISTER_ERROR(1300, "etcd注册用户出错，请联系管理员"),
    ETCD_DELETE_USER_ERROR(1301, "etcd删除用户出错，请联系管理员"),

    KEY_CACHE_EMPTY(1401, "缓存规则不可为空"),
    CACHE_NAME_STILL_IN_USE(1402, ""),
    CACHE_NAME_NOT_EXIST(1403, "");

    private int code;

    private String msg;

    ResultEnum(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public int getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }
}
