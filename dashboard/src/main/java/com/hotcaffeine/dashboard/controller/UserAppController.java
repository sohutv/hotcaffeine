package com.hotcaffeine.dashboard.controller;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.hotcaffeine.dashboard.common.base.BaseController;
import com.hotcaffeine.dashboard.common.domain.Result;
import com.hotcaffeine.dashboard.common.eunm.ResultEnum;
import com.hotcaffeine.dashboard.model.App;
import com.hotcaffeine.dashboard.service.AppService;
import com.hotcaffeine.dashboard.service.EtcdClientService;
import com.hotcaffeine.dashboard.service.RuleService;
import com.hotcaffeine.dashboard.service.UserAppService;

/**
 * @Author yongweizhao
 * @Date 2021/2/1 15:37
 */
@Controller
@RequestMapping("user/app")
public class UserAppController extends BaseController {

    private String prefix = "admin/app";

    @Autowired
    private AppService appService;

    @Autowired
    private UserAppService userAppService;

    @Autowired
    private EtcdClientService etcdClientService;

    @Autowired
    private RuleService ruleService;

    @RequestMapping(value = "/add", method = RequestMethod.GET)
    public String add(){
        return prefix + "/add";
    }

    /**
     * 手动创建app
     */
    @RequestMapping(value = "/add", method = RequestMethod.POST)
    @ResponseBody
    public Result<?> add(@RequestParam("appName") String appName, @RequestParam( value = "service", required = false) String service) {
        appName = appName.trim();
        // 1.查询appName是否已存在
        App ap = appService.queryByName(appName);
        // 查询成功并且appName已存在
        if (ap != null) {
            return Result.getResult(ResultEnum.APP_CONFLICT);
        }
        // 2.保存user-app
        App app = new App(appName.trim(), service, 2);
        Result<?> saveResult = userAppService.saveUserApp(app, userName());
        if (saveResult.isNotOK()) {
            return saveResult;
        }
        // 3.etcd创建appName
        Result<?> etcdResult = etcdClientService.registerUser(app.getAppName());
        if (etcdResult.isNotOK()) {
            return etcdResult;
        }
        // 4. 创建默认规则
        ruleService.initDefaultRules(app.getAppName(), userName());

        return Result.success();
    }


    @RequestMapping(value = "/association", method = RequestMethod.GET)
    public String association() {
        return prefix + "/association";
    }

    /**
     * 用户关联app,此app可能是从poller实时获取的,也可能是由另外一个用户手动创建的
     */
    @RequestMapping(value = "/association", method = RequestMethod.POST)
    @ResponseBody
    public Result<?> association(@RequestParam("appName") String appName) {
        App app = appService.queryByName(appName);
        // app不存在,说明是从配置获取的,创建并关联
        if (app == null) {
            app = new App(appName, 1);
            // 1.保存user-app
            Result<?> saveResult = userAppService.saveUserApp(app, userName());
            if (saveResult.isNotOK()) {
                return saveResult;
            }
            // 2.etcd创建app
            Result<?> etcdResult = etcdClientService.registerUser(app.getAppName());
            if (etcdResult.isNotOK()) {
                return etcdResult;
            }
            // 3.创建默认规则
            ruleService.initDefaultRules(app.getAppName(), userName());
            return Result.success();
        }
        // 1.查询关联是否已存在
        Set<String> appSet = userAppService.selectUserApp(userName());
        // 查询成功并且关联关系已存在
        if (appSet != null && appSet.contains(app.getAppName())) {
            return Result.getResult(ResultEnum.USER_APP_CONFLICT);
        }
        // 2.保存关联关系
        userAppService.insert(userName(), app.getAppName());
        return Result.success();
    }
}
