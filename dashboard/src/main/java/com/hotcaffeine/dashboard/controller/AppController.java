package com.hotcaffeine.dashboard.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.hotcaffeine.dashboard.common.base.BaseController;
import com.hotcaffeine.dashboard.common.domain.Result;
import com.hotcaffeine.dashboard.model.App;
import com.hotcaffeine.dashboard.service.AppService;
import com.hotcaffeine.dashboard.service.ConfigAppService;

/**
 * @Author yongweizhao
 * @Date 2021/2/1 15:37
 */
@Controller
@RequestMapping("app")
public class AppController extends BaseController {

    @Autowired
    private AppService appService;

    @Autowired
    private ConfigAppService configAppService;

    // 获取所有的app列表
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    @ResponseBody
    public List<App> list() {
        Result<List<App>> result =  appService.queryAll();
        return result.getData();
    }

    /**
     * 获取当前用户可以关联的appName列表,包括两部分：
     * 1.poller里关联的服务appName
     * 2.poller中没有关联,用户手动创建的appName
     */
    @RequestMapping(value = "/canRelate", method = RequestMethod.GET)
    @ResponseBody
    public List<String> canRelate() throws Exception {
        List<String> appNameList = new ArrayList<>();
        String userName = userName();
        // 配置的app列表
        List<String> configAppList = configAppService.getApp(userName);
        if (!CollectionUtils.isEmpty(configAppList)) {
            appNameList.addAll(configAppList);
        }
        // 获取用户手动创建的appName列表
        List<String> appNames = appService.AppNameManuallyCreated();
        if (!CollectionUtils.isEmpty(appNames)) {
            appNameList.addAll(appNames);
        }
        return appNameList;
    }
}
