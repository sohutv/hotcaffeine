package com.hotcaffeine.dashboard.controller;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.hotcaffeine.dashboard.common.domain.Page;
import com.hotcaffeine.dashboard.common.domain.req.PageReq;
import com.hotcaffeine.data.TopHotKeyStore;
import com.hotcaffeine.data.metric.HotKey;

@Controller
@RequestMapping("/topk")
public class TopkController {
    
    @Autowired
    private TopHotKeyStore topHotKeyStore;
    
    @RequestMapping("/hot")
    @ResponseBody
    public Page<HotKey> hot(PageReq page, String appName, String ruleKey, String startTime){
        Date date = null;
        try {
            date = new SimpleDateFormat("yyyy-MM-dd HH:mm").parse(startTime);
        } catch (Exception e) {
            return new Page<>(1, 0, new ArrayList<>());
        }
        String tm = new SimpleDateFormat("yyyyMMddHHmm").format(date);
        Long count = topHotKeyStore.queryHotKeyCount(appName, ruleKey, tm);
        if(count == null || count == 0) {
            return new Page<>(1, 0, new ArrayList<>());
        }
        int start = (page.getPageNum() - 1) * page.getPageSize();
        int end = start + page.getPageSize() - 1;
        List<HotKey> hotKeyList = topHotKeyStore.queryHotKey(appName, ruleKey, tm, start, end);
        return new Page<HotKey>(page.getPageNum(), count.intValue(), hotKeyList);
    }
}
