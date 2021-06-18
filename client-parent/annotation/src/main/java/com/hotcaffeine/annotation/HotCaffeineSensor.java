package com.hotcaffeine.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 热键传感器
 * 
 * @author yongfeigao
 * @date 2021年1月20日
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface HotCaffeineSensor {
    /**
     * @return source of key
     */
    String value() default "";
    
    /**
     * remove from cache
     * @return
     */
    boolean isRemove() default false;
}