package com.hotcaffeine.dashboard.model;

/**
 * @Author yongweizhao
 * @Date 2021/2/1 17:36
 */
public class App {

    private Integer id;

    private String appName;

    private String service;

    private int source; // 来源, 1:poller 2:用户手动创建

    public App() {}

    public App(String appName, String service, int source) {
        this.appName = appName;
        this.service = service;
        this.source = source;
    }

    public App(String appName, int source) {
        this.appName = appName;
        this.source = source;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public int getSource() {
        return source;
    }

    public void setSource(int source) {
        this.source = source;
    }

    @Override
    public String toString() {
        return "App{" +
                "id=" + id +
                ", appName='" + appName + '\'' +
                ", service='" + service + '\'' +
                ", source=" + source +
                '}';
    }
}
