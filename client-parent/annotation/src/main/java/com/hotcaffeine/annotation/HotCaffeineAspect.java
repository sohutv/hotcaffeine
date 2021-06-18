package com.hotcaffeine.annotation;

import java.lang.reflect.Method;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;

import com.hotcaffeine.client.HotCaffeine;
import com.hotcaffeine.client.HotCaffeineDetector;
import com.hotcaffeine.common.model.ValueModel;
import com.hotcaffeine.common.util.ClientLogger;

/**
 * HotCaffeine切面
 * 
 * @author yongfeigao
 * @date 2021年1月21日
 */
@Aspect
public class HotCaffeineAspect {
    @Pointcut("@annotation(com.hotcaffeine.annotation.HotCaffeineSensor)")
    public void hotCaffeineAnnotationPointcut() {
    }

    @Around("hotCaffeineAnnotationPointcut()")
    public Object invokeHotCaffeine(ProceedingJoinPoint pjp) throws Throwable {
        ValueModel valueModel = null;
        try {
            // 解析获取注解
            Method originMethod = resolveMethod(pjp);
            HotCaffeineSensor annotation = originMethod.getAnnotation(HotCaffeineSensor.class);
            if (annotation == null) {
                // Should not go through here.
                throw new IllegalStateException("Wrong state for HotCaffeineSensor annotation");
            }
            String keySource = annotation.value();
            HotCaffeine hotCaffeine = HotCaffeineDetector.getInstance().getHotCaffeine(keySource);
            // 获取第一参数：key
            String key = pjp.getArgs()[0].toString();
            if(annotation.isRemove()) {
                hotCaffeine.remove(key);
            } else {
                valueModel = hotCaffeine.getValueModel(key);
            }
        } catch (Throwable e) { // 发生任何异常不能影响客户端
            ClientLogger.getLogger().error("aspect error:{}", e.toString());
        }
        // 非热点
        if (valueModel == null) {
            return pjp.proceed();
        }
        if (valueModel.isDefaultValue()) {
            // 默认值重设
            valueModel.setValue(pjp.proceed());
        }
        return valueModel.getValue();
    }

    /**
     * 解析方法
     * 
     * @param joinPoint
     * @return
     */
    protected Method resolveMethod(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Class<?> targetClass = joinPoint.getTarget().getClass();
        Method method = getDeclaredMethodFor(targetClass, signature.getName(),
                signature.getMethod().getParameterTypes());
        if (method == null) {
            throw new IllegalStateException("Cannot resolve target method: " + signature.getMethod().getName());
        }
        return method;
    }

    /**
     * Get declared method with provided name and parameterTypes in given class
     * and its super classes. All parameters should be valid.
     *
     * @param clazz class where the method is located
     * @param name method name
     * @param parameterTypes method parameter type list
     * @return resolved method, null if not found
     */
    private Method getDeclaredMethodFor(Class<?> clazz, String name, Class<?>... parameterTypes) {
        try {
            return clazz.getDeclaredMethod(name, parameterTypes);
        } catch (NoSuchMethodException e) {
            Class<?> superClass = clazz.getSuperclass();
            if (superClass != null) {
                return getDeclaredMethodFor(superClass, name, parameterTypes);
            }
        }
        return null;
    }
}
