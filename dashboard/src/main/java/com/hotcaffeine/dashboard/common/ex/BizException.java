package com.hotcaffeine.dashboard.common.ex;


import com.hotcaffeine.dashboard.common.eunm.ResultEnum;

public class BizException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private int code;
    private String msg;

    public BizException(int code, String message) {
        this.code = code;
        this.msg = message;
    }

    public BizException(ResultEnum result) {
        this.code = result.getCode();
        this.msg = result.getMsg();
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    @Override
    public String toString() {
        return "BizException{" +
                "code=" + code +
                ", msg='" + msg + '\'' +
                '}';
    }
}