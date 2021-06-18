package com.hotcaffeine.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * 日志工具
 * 
 * @author yongfeigao
 * @date 2021年2月1日
 */
public class ClientLogger {

    private static final Logger LOGGER = LoggerFactory.getLogger("hotcaffeine");

    public static Logger getLogger() {
        return LOGGER;
    }
}
