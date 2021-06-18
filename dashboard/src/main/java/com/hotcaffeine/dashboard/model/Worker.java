package com.hotcaffeine.dashboard.model;

import com.hotcaffeine.dashboard.common.domain.Constant;

import java.util.Date;

public class Worker {
    private Integer id;

    private String name;

    private String ip;

    private Integer port;

    private Integer cliCount;

    private long totalCount;

    private Date updateTime;

    private String updateUser;

    private Integer state = 1;

    private String uuid;

    public Worker() {
    }

    public Worker(String name, String ipPort) {
        this.name = name;
        String[] arr = ipPort.split(Constant.SPIT);
        this.ip = arr[0];
        this.port = Integer.valueOf(arr[1]);
    }


    public Worker(String name, String ipPort, Integer cliCount) {
        this.name = name;
        String[] arr = ipPort.split(Constant.SPIT);
        this.ip = arr[0];
        this.port = Integer.valueOf(arr[1]);
        this.state = 1;
        this.cliCount = cliCount;
    }

    public Worker(String name, String ipPort, String uuid) {
        this.name = name;
        String[] arr = ipPort.split(Constant.SPIT);
        this.ip = arr[0];
        this.port = Integer.valueOf(arr[1]);
        this.state = 1;
        this.uuid = uuid;
    }

    public Worker(String name, Integer state,String updateUser) {
        this.name = name;
        this.updateUser = updateUser;
        this.state = state;
    }

    public long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(long totalCount) {
        this.totalCount = totalCount;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name == null ? null : name.trim();
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip == null ? null : ip.trim();
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public Integer getCliCount() {
        return cliCount;
    }

    public void setCliCount(Integer cliCount) {
        this.cliCount = cliCount;
    }
    
    public void addCliCount(int count) {
        if (this.cliCount == null) {
            this.cliCount = 0;
        }
        this.cliCount += count;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public String getUpdateUser() {
        return updateUser;
    }

    public void setUpdateUser(String updateUser) {
        this.updateUser = updateUser;
    }

    public Integer getState() {
        return state;
    }

    public void setState(Integer state) {
        this.state = state;
    }
}