package com.hotcaffeine.dashboard.controller;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.hotcaffeine.common.model.KeyRule;
import com.hotcaffeine.common.model.KeyRuleCacher;
import com.hotcaffeine.dashboard.rule.AppKeyRuleCacher;
import com.hotcaffeine.dashboard.service.LeapWindowService;
import com.hotcaffeine.data.metric.LeapArrayModel;
import com.hotcaffeine.data.metric.LeapArrayModel.WindowWrapModel;
import com.hotcaffeine.data.util.Result;
import com.hotcaffeine.data.util.Status;

@Controller
@RequestMapping("/leap")
public class LeapWindowController {

    @Autowired
    private LeapWindowService leapWindowService;

    @Autowired
    private AppKeyRuleCacher appKeyRuleCacher;

    @RequestMapping("/window")
    @ResponseBody
    public Result<?> window(String appName, String ruleKey, String key) {
        KeyRuleCacher keyRuleCacher = appKeyRuleCacher.getKeyRuleCacher(appName);
        if (keyRuleCacher == null) {
            return Result.getResult(Status.NO_RESULT);
        }
        KeyRule keyRule = keyRuleCacher.findKeyRuleByKey(ruleKey);
        if (keyRule == null) {
            return Result.getResult(Status.NO_RESULT);
        }
        String tmpKey = key;
        if (!keyRule.isPrefix()) {
            tmpKey = KeyRule.buildFullKey(ruleKey, key);
        }
        Result<LeapArrayModel> result = leapWindowService.getLeapWindow(appName, tmpKey);
        LeapArrayModel leapArrayModel = result.getResult();
        if (leapArrayModel != null) {
            leapArrayModel.setSurvivalTime(leapArrayModel.getSurvivalTime() / 1000);
            leapArrayModel.setLiveTime(leapArrayModel.getLiveTime() / 1000);
            List<WindowWrapModel> list = leapArrayModel.getWindowList();
            if (list != null) {
                Collections.sort(list);
            }
        }
        return result;
    }
}
