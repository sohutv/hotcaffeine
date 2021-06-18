package com.hotcaffeine.client;

import com.hotcaffeine.client.listener.IKeyListener;
import com.hotcaffeine.common.model.KeyRule;

/**
 * 热键缓存
 * 
 * @author yongfeigao
 * @date 2021年1月15日
 */
public class PrefixHotCaffeine extends HotCaffeine {

    public PrefixHotCaffeine(String ruleKey, IKeyListener hotCaffeineListener, HotCaffeineDetector hotCaffeineDetector) {
        super(ruleKey, hotCaffeineListener, hotCaffeineDetector);
    }

    @Override
    protected KeyRule finKeyRule(String key) {
        return hotCaffeineDetector.getKeyRuleCacher().findPrefixRule(key);
    }
}
