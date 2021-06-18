package com.hotcaffeine.common.etcd;

/**
 * etcd配置
 * 
 * @author yongfeigao
 * @date 2021年6月3日
 */
public interface IEtcdConfig {
    
    public static final String DEFAULT = "default";
    
    /**
     * 初始化路径
     */
    public void init(String user);

    public String getEndpoints();
    
    public void setEndpoints(String endpoints);

    public String getUser();
    
    public void setUser(String user);

    public String getPassword();
    
    public void setPassword(String password);

    public String getRootPath();
    
    public void setRootPath(String rootPath);

    public String getHotKeyPath();
    
    public String getUserHotKeyPath();

    public String getWorkerPath();
    
    public String getUserWorkerPath();
    
    public String getDefaultWorkerPath();

    public String getRulePath();
    
    public String getUserRulePath();

    public String getDashboardPath();
    
    public String getUserDashboardPath();

    public String getTopkPath();
    
    public String getCachePath();
    
    public String getUserCachePath();
    
    public String getWorkerForAppPath();
    
    public String getWorkerForApp();
    
    public boolean isDefaultWorker();
}
