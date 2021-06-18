package com.hotcaffeine.dashboard.service;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.hotcaffeine.common.model.KeyRule;
import com.hotcaffeine.dashboard.DashboardApplication;
import com.hotcaffeine.data.metric.LeapArrayModel;
import com.hotcaffeine.data.util.Result;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = DashboardApplication.class)
public class LeapWindowServiceTest {
    
    @Autowired
    private LeapWindowService leapWindowService;

    @Test
    public void test() {
        String appName = "core.api-web-mobile.hotkey";
        String source = "minor";
        String key = "ugc.play:58245616:::";
        Result<LeapArrayModel> result = leapWindowService.getLeapWindow(appName, KeyRule.buildFullKey(source, key));
        Assert.assertNotNull(result);
    }

}
