package com.hotcaffeine.data.store;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
/**
 * redis配置
 * 
 * @author yongfeigao
 * @date 2021年5月28日
 */
public class RedisConfiguration {
    private GenericObjectPoolConfig<?> poolConfig;
    private String host;
    private int port;
    private int connectionTimeout;
    private int soTimeout;
    private String password;

    public GenericObjectPoolConfig<?> getPoolConfig() {
        return poolConfig;
    }

    public void setPoolConfig(GenericObjectPoolConfig<?> poolConfig) {
        this.poolConfig = poolConfig;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public int getSoTimeout() {
        return soTimeout;
    }

    public void setSoTimeout(int soTimeout) {
        this.soTimeout = soTimeout;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
