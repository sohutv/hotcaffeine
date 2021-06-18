package com.hotcaffeine.dashboard.helper;

import com.hotcaffeine.dashboard.common.domain.Constant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @Author yongweizhao
 * @Date 2021/3/29 14:25
 */
@Component
public class EnvHelper {

    @Value("${spring.profiles.active:local}")
    private String profile;

    public boolean isOnline() {
        return Constant.ONLINE.equals(profile);
    }

    public String getEnv() {
        return profile;
    }
}
