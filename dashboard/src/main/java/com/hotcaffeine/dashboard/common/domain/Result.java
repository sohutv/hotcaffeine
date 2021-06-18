package com.hotcaffeine.dashboard.common.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hotcaffeine.dashboard.common.eunm.ResultEnum;

import java.io.Serializable;


public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer code;

    private String msg;

    private T data;

    private Exception exception;

    public Result() {
    }

    public Result(Integer code, T data) {
        this.code = code;
        this.data = data;
    }

    public Result(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public Result(ResultEnum resultEnum) {
        this.code = resultEnum.getCode();
        this.msg = resultEnum.getMsg();
    }

    public static <T> Result<T> getResult(ResultEnum resultEnum) {
        return new Result<T>()
                .setCode(resultEnum.getCode())
                .setMsg(resultEnum.getMsg());
    }

    @SuppressWarnings("unchecked")
    public static <T> Result<T> getResult(Object data) {
        if (data == null) {
            return getResult(ResultEnum.NO_RESULT);
        }
        return (Result<T>) getResult(ResultEnum.SUCCESS).setData(data);
    }

    public static Result<?> success(){
        return new Result<>(ResultEnum.SUCCESS);
    }

    public static Result<?> success(Object data){
        return new Result<>(200,data);
    }

    public static Result<?> fail(){
        return new Result<>(ResultEnum.NO_CHANGE);
    }

    public static Result<?> error(int code, String msg){
        return new Result<>(code,msg);
    }

    public static Result<?> error(){
        return new Result<>(ResultEnum.BIZ_ERROR);
    }

    public static Result<?> error(ResultEnum result){
        return new Result<>(result);
    }

    /**
     * db异常结果
     * @param e
     */
    public static <T> Result<T> getDBErrorResult(Exception e) {
        return new Result<T>(ResultEnum.DB_ERROR).setException(e);
    }

    @SuppressWarnings("unchecked")
    public static <T> Result<T> getWebErrorResult(Exception e) {
        String error = e.getMessage();
        if(error == null) {
            error = e.getClass().getName();
        }
        return (Result<T>) getResult(ResultEnum.WEB_ERROR).setMsg(error);
    }

    // 请求成功并且有数据
    @JsonIgnore
    public boolean isOK() {
        return ResultEnum.SUCCESS.getCode() == code;
    }

    // 查询db出错
    @JsonIgnore
    public boolean hasDBError() {
        return exception != null;
    }

    @JsonIgnore
    public boolean isNotOK() {
        return !isOK();
    }

    /**
     * 请求成功,数据是否为空
     */
    @JsonIgnore
    public boolean isDataEmpty() {
        return ResultEnum.NO_RESULT.getCode() == code;
    }

    public Integer getCode() {
        return code;
    }

    public Result<T> setCode(Integer code) {
        this.code = code;
        return this;
    }

    public String getMsg() {
        return msg;
    }

    public Result<T> setMsg(String msg) {
        this.msg = msg;
        return this;
    }

    public T getData() {
        return data;
    }

    public Result<T> setData(T data) {
        this.data = data;
        return this;
    }

    public Exception getException() {
        return exception;
    }

    public Result<T> setException(Exception exception) {
        this.exception = exception;
        return this;
    }

}
