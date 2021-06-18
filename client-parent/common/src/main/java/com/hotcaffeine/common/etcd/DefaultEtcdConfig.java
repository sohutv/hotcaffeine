package com.hotcaffeine.common.etcd;

import com.hotcaffeine.common.util.DigestUtil;

/**
 * etcd配置
 * 
 * @author yongfeigao
 * @date 2021年6月3日
 */
public class DefaultEtcdConfig implements IEtcdConfig {
    // 链接
    protected String endpoints;
    // 用户
    protected String user;
    // 密码
    protected String password;
    // 根路径
    protected String rootPath;
    // 手工添加的热键
    protected String hotKeyPath;
    // worker
    protected String workerPath;
    // 规则
    protected String rulePath;
    // dashboard ip
    protected String dashboardPath;
    // topk
    protected String topkPath;
    // 缓存
    protected String cachePath;
    // 只为某个app服务的worker
    protected String workerForApp;

    public DefaultEtcdConfig() {
        this.endpoints = "http://127.0.0.1:2379";
        this.rootPath = "/hotcaffeine/";
    }

    public void init(String user) {
        this.user = user;
        this.hotKeyPath = this.rootPath + "hotkey/";
        this.workerPath = this.rootPath + "worker/";
        this.rulePath = this.rootPath + "rule/";
        this.dashboardPath = this.rootPath + "dashboard/";
        this.topkPath = this.rootPath + "topk/";
        this.cachePath = this.rootPath + "cache/";
    }

    public String getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(String endpoints) {
        this.endpoints = endpoints;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        if (password == null && user != null) {
            return DigestUtil.encode(getUser());
        }
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRootPath() {
        return rootPath;
    }

    public void setRootPath(String rootPath) {
        this.rootPath = rootPath;
    }

    public String getHotKeyPath() {
        return hotKeyPath;
    }

    public void setHotKeyPath(String hotKeyPath) {
        this.hotKeyPath = hotKeyPath;
    }

    public String getWorkerPath() {
        return workerPath;
    }

    public void setWorkerPath(String workerPath) {
        this.workerPath = workerPath;
    }

    public String getRulePath() {
        return rulePath;
    }

    public void setRulePath(String rulePath) {
        this.rulePath = rulePath;
    }

    public String getDashboardPath() {
        return dashboardPath;
    }

    public void setDashboardPath(String dashboardPath) {
        this.dashboardPath = dashboardPath;
    }

    public String getTopkPath() {
        return topkPath;
    }

    public void setTopkPath(String topkPath) {
        this.topkPath = topkPath;
    }

    public String getCachePath() {
        return cachePath;
    }

    public void setCachePath(String cachePath) {
        this.cachePath = cachePath;
    }

    @Override
    public String getUserHotKeyPath() {
        return hotKeyPath + user;
    }

    @Override
    public String getUserWorkerPath() {
        return workerPath + user;
    }

    @Override
    public String getUserRulePath() {
        return rulePath + user;
    }

    @Override
    public String getUserDashboardPath() {
        return dashboardPath + user;
    }

    @Override
    public String getUserCachePath() {
        return cachePath + user;
    }

    @Override
    public boolean isDefaultWorker() {
        return DEFAULT.equals(workerForApp);
    }

    public String getWorkerForAppPath() {
        return workerPath + workerForApp;
    }
    
    public String getWorkerForApp() {
        return workerPath;
    }

    public void setWorkerForApp(String workerForApp) {
        this.workerForApp = workerForApp;
    }

    @Override
    public String getDefaultWorkerPath() {
        return workerPath + DEFAULT;
    }
}
