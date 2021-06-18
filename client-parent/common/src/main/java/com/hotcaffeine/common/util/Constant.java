package com.hotcaffeine.common.util;

/**
 * @author wuweifeng wrote on 2019-12-05
 * @version 1.0
 */
public class Constant {
    /**
     * 当客户端要删除某个key时，就往etcd里赋值这个value，设置1秒过期，就算删除了
     */
    public static final String DEFAULT_DELETE_VALUE = "#[DELETE]#";

    //单次包最大2M
    public static final int MAX_LENGTH = 2 * 1024 * 1024;

    public static final int MAX_INTERVAL = 10;
}
