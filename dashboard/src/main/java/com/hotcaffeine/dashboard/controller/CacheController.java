package com.hotcaffeine.dashboard.controller;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.hotcaffeine.common.etcd.EtcdClient;
import com.hotcaffeine.common.etcd.IEtcdConfig;
import com.hotcaffeine.common.model.CacheRule;
import com.hotcaffeine.common.model.KeyRule;
import com.hotcaffeine.common.util.JsonUtil;
import com.hotcaffeine.dashboard.common.domain.Result;
import com.hotcaffeine.dashboard.common.eunm.ResultEnum;
import com.hotcaffeine.dashboard.model.KeyCacheModel;

@Controller
@RequestMapping("/cache")
public class CacheController {

    @Resource
    private EtcdClient etcdClient;
    
    @Autowired
    private IEtcdConfig etcdConfig;

    @RequestMapping("/list")
    @ResponseBody
    public List<?> list(String app) {
        KeyCacheModel keyCacheModel = new KeyCacheModel();
        keyCacheModel.setKeyCacheJson(getKeyCacheJson(app));
        List<KeyCacheModel> list = new ArrayList<>();
        list.add(keyCacheModel);
        return list;
    }
    
    @GetMapping("/edit/{app}")
    public String edit(ModelMap modelMap, @PathVariable("app") String app){
        return "admin/rule/cacheView";
    }
    
    @GetMapping("/viewDetail")
    public String viewDetail(ModelMap modelMap){
        return "admin/rule/cacheJson";
    }
    
    @RequestMapping("/json")
    @ResponseBody
    public KeyCacheModel json(String app){
        KeyCacheModel keyCacheModel = new KeyCacheModel();
        keyCacheModel.setKeyCacheJson(getKeyCacheJson(app));
        return keyCacheModel;
    }
    
    @PostMapping("/save")
    @ResponseBody
    public Result<?> save(String app, String keyCache){
        List<CacheRule> keyCacheList = JsonUtil.toList(keyCache, CacheRule.class);
        if (CollectionUtils.isEmpty(keyCacheList)) {
            return new Result<>(ResultEnum.KEY_CACHE_EMPTY);
        }
        // cacheRule校验
        for(CacheRule cacheRule : keyCacheList) {
            if (StringUtils.isEmpty(cacheRule.getName())) {
                return new Result<>().setCode(ResultEnum.PARAM_ERROR.getCode())
                        .setMsg("cache名不可为空");
            }
            int size = cacheRule.getSize();
            if (size < 0) {
                return new Result<>().setCode(ResultEnum.PARAM_ERROR.getCode())
                        .setMsg("缓存大小不可以为负数");
            }
            int duration = cacheRule.getDuration();
            if (duration < 0) {
                return new Result<>().setCode(ResultEnum.PARAM_ERROR.getCode())
                        .setMsg("缓存过期时间不可以为负数");
            }
        }
        String keyRuleStr = etcdClient.get(etcdConfig.getRulePath() + app);
        List<KeyRule> keyRuleList = JsonUtil.toList(keyRuleStr, KeyRule.class);
        // 校验: 防止keyRule中正在使用的cacheName在keyCache中误删导致不存在
        for (KeyRule keyRule : keyRuleList) {
            String cacheName = keyRule.getCacheName();
            boolean exist = false;
            for (CacheRule value : keyCacheList) {
                if (value.getName().equals(cacheName)) {
                    exist = true;
                }
            }
            if (!exist) {
                return new Result<>().setCode(ResultEnum.CACHE_NAME_STILL_IN_USE.getCode())
                              .setMsg("缓存规则: " + cacheName + " 还在key规则中使用,请先停用");
            }
        }
        etcdClient.put(etcdConfig.getCachePath() + app, keyCache);
        return Result.success();
    }
    
    private String getKeyCacheJson(String app) {
        return etcdClient.get(etcdConfig.getCachePath() + app);
    }
}
