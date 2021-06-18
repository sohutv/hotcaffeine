package com.hotcaffeine.dashboard.service;

import java.util.List;

import com.hotcaffeine.common.model.KeyRule;
import com.hotcaffeine.dashboard.common.domain.req.PageReq;
import com.hotcaffeine.dashboard.model.Rules;

/**
 * @ClassName: RuleService
 * @Description: TODO(一句话描述该类的功能)
 * @Author: liyunfeng31
 * @Date: 2020/4/17 16:29
 */
public interface RuleService {

    Rules selectRules(String app);

    int updateRule(Rules rules);

    Integer add(Rules rules);

    int delRule(String key, String updater);

    List<Rules> pageKeyRule(PageReq page, String appName);

    int save(Rules rules);

    List<String> listRules(String app);

    /**
     *为appName设置默认的规则
     */
    int initDefaultRules(String appName, String userName);

    /**
     * 获取某个appName下的所有规则
     */
    List<KeyRule> keyRules(String appName);
}
