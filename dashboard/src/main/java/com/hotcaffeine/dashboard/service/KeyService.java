package com.hotcaffeine.dashboard.service;

import com.hotcaffeine.dashboard.common.domain.Page;
import com.hotcaffeine.dashboard.common.domain.req.PageReq;
import com.hotcaffeine.dashboard.common.domain.req.SearchReq;
import com.hotcaffeine.dashboard.model.KeyTimely;

/**
 * @Author: liyunfeng31
 * @Date: 2020/4/17 16:28
 */
public interface KeyService {

    int delKeyByUser(KeyTimely keyTimely);

    Page<KeyTimely> pageKeyTimely(PageReq page, SearchReq param, String userName);
}
