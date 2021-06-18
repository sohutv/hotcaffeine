package com.hotcaffeine.dashboard.service;

import java.util.List;

import com.hotcaffeine.dashboard.common.domain.req.PageReq;
import com.hotcaffeine.dashboard.common.domain.req.SearchReq;
import com.hotcaffeine.dashboard.model.Worker;

/**
 * @Author: liyunfeng31
 * @Date: 2020/4/17 16:28
 */
public interface WorkerService {
    List<Worker> pageWorker(PageReq page, SearchReq param);
}
