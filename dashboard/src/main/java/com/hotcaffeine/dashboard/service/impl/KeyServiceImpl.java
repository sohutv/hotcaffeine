package com.hotcaffeine.dashboard.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.hotcaffeine.common.etcd.EtcdClient;
import com.hotcaffeine.common.etcd.IEtcdConfig;
import com.hotcaffeine.common.model.CacheRule;
import com.hotcaffeine.common.model.KeyCount;
import com.hotcaffeine.common.model.KeyRule;
import com.hotcaffeine.common.model.KeyRuleCacher;
import com.hotcaffeine.dashboard.common.domain.Constant;
import com.hotcaffeine.dashboard.common.domain.Page;
import com.hotcaffeine.dashboard.common.domain.req.PageReq;
import com.hotcaffeine.dashboard.common.domain.req.SearchReq;
import com.hotcaffeine.dashboard.model.KeyTimely;
import com.hotcaffeine.dashboard.model.User;
import com.hotcaffeine.dashboard.rule.AppKeyRuleCacher;
import com.hotcaffeine.dashboard.service.HotKeyService;
import com.hotcaffeine.dashboard.service.KeyService;
import com.hotcaffeine.dashboard.service.UserAppService;
import com.hotcaffeine.dashboard.service.UserService;
import com.hotcaffeine.dashboard.util.PageUtil;


/**
 * @ClassName: KeyServiceImpl
 * @Author: liyunfeng31
 * @Date: 2020/4/17 17:53
 */
@Service
public class KeyServiceImpl implements KeyService {

    @Resource
    private EtcdClient etcdClient;
    
    @Autowired
    private IEtcdConfig etcdConfig;

    @Autowired
    private UserService userService;

    @Autowired
    private UserAppService userAppService;
    
    @Autowired
    private AppKeyRuleCacher appKeyRuleCacher;
    
    @Autowired
    private HotKeyService hotKeyService;
    
    private Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public Page<KeyTimely> pageKeyTimely(PageReq page, SearchReq param, String userName) {
        List<KeyTimely> keyTimelies = list(param);
        User user = userService.selectByUserName(userName);
        if (user != null && !user.admin()) {
            Set<String> appNames = userAppService.selectUserApp(userName);
            if (appNames != null) {
                keyTimelies = keyTimelies.stream().filter(keyTimely -> appNames.contains(keyTimely.getAppName()))
                        .collect(Collectors.toList());
            } else { // ??????????????????????????????
                keyTimelies = new ArrayList<>();
            }
        }
        // ????????????
        return PageUtil.pagination(keyTimelies, page.getPageSize(), page.getPageNum()-1);
    }

    @Override
    public int delKeyByUser(KeyTimely keyTimely) {
        //app + "_" + key
        String[] arr = keyTimely.getKey().split("/");
        logger.info("delete key:{}", keyTimely.getKey());
        //??????client???????????????key
        String etcdKey = etcdConfig.getHotKeyPath() + arr[0] + "/" + arr[1];
        if (etcdClient.get(etcdKey) == null) {
            //???????????????????????????client???????????????????????????key????????????????????????????????????????????????????????????client???????????????????????????
            etcdClient.put(etcdKey, com.hotcaffeine.common.util.Constant.DEFAULT_DELETE_VALUE, 10);
        }
        etcdClient.delete(etcdKey);

        //??????redis????????????key
        delete(arr[0] + "/" + arr[1]);

        return Constant.SUCCESS;
    }
    
    /**
     * ???????????????key,?????????????????????
     * 1. appName + rule + ???key?????????????????????????????????
     * 2. appName + rule????????????appName???????????????????????????key??????
     */
    public List<KeyTimely> list(SearchReq searchReq) {
        // appName????????????
        String appName = searchReq.getApp();
        if (StringUtils.isEmpty(appName)) {
            return new ArrayList<>();
        }
        String rule = searchReq.getRule();
        String key = searchReq.getKey();
        List<KeyCount> keyCountList = null;
        // ????????????1
        if (!StringUtils.isEmpty(searchReq.getKey())) {
            keyCountList = hotKeyService.getKeyCount(appName, rule, key);
        } else {
            keyCountList = hotKeyService.getKeyCountList(appName, rule);
        }
        if (keyCountList != null) {
            return keyCountList.stream().map(keyCount -> parse(keyCount, System.currentTimeMillis()))
                    .filter(item -> item != null).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    /**
     * ???keyCount???????????????????????????
     */
    private KeyTimely parse(KeyCount keyCount, long now) {
        if (keyCount == null) {
            return null;
        }
        KeyRuleCacher keyRuleCacher = appKeyRuleCacher.getKeyRuleCacher(keyCount.getAppName());
        KeyRule keyRule = keyRuleCacher.findRule(keyCount.getKey());
        if (keyRule == null) {
            return null;
        }
        CacheRule keyCache = keyRuleCacher.getCacheRule(keyRule);
        if(keyCache == null) {
            return null;
        }
        long remainTime = keyCache.getDuration() * 1000 - (now - keyCount.getCreateTime());
        if (remainTime <= 0) {
            return null;
        }
        return KeyTimely.aKeyTimely()
                .key(keyCount.getKey())
                .realHotKey(keyRule.stripRuleKey(keyCount.getKey()))
                .appName(keyCount.getAppName())
                .rule(keyRule.getKey())
                .duration(remainTime / 1000)
                .createTime(new Date(keyCount.getCreateTime())).build();
    }

    /**
     * ???????????????key
     */
    public boolean delete(String appNameKey) {
        String[] array = appNameKey.split("/");
        String appName = array[0];
        KeyRuleCacher keyRuleCacher = appKeyRuleCacher.getKeyRuleCacher(appName);
        KeyRule keyRule = keyRuleCacher.findRule(array[1]);
        return hotKeyService.removeKeyCount(appName, keyRule.getKey(), array[1]);
    }

}


