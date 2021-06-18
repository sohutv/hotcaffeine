package com.hotcaffeine.common.util;

import java.util.Iterator;
import java.util.ServiceLoader;

/**
 * 服务加载
 * 
 * @author yongfeigao
 * @date 2021年6月3日
 */
public class ServiceLoaderUtil {

    /**
     * 加载服务
     * 
     * @param clz
     * @return
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T> T loadService(Class<T> interfaceClass, Class defaultImplClass) {
        ServiceLoader<T> loadedClass = ServiceLoader.load(interfaceClass);
        Iterator<T> iterator = loadedClass.iterator();
        if (iterator.hasNext()) {
            T t = iterator.next();
            ClientLogger.getLogger().info("ServiceLoader load service:{}", t);
            return t;
        }
        try {
            ClientLogger.getLogger().info("ServiceLoader load service2:{}", defaultImplClass);
            return (T) defaultImplClass.newInstance();
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }
}
