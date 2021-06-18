package com.hotcaffeine.dashboard.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.hotcaffeine.dashboard.DashboardApplication;
import com.hotcaffeine.dashboard.rule.AppKeyRuleCacher;

/**
 * @Author yongweizhao
 * @Date 2021/3/3 18:37
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = DashboardApplication.class)
public class RuleUtilTest {

    @Autowired
    private AppKeyRuleCacher appKeyRuleCacher;

    @Test
    public void test() {
        appKeyRuleCacher.getAppKeyRuleCacherMap().forEach((appName, keyRuleCacher) -> {
            keyRuleCacher.getKeyRuleList().forEach(keyRule -> {
                System.out.println("appName:" + appName + ";keyRule:" + keyRule.toString());
            });
        });
    }
}
