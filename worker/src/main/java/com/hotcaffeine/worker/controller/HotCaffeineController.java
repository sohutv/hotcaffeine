package com.hotcaffeine.worker.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hotcaffeine.common.model.Message;
import com.hotcaffeine.common.model.MessageType;
import com.hotcaffeine.data.util.Result;
import com.hotcaffeine.worker.netty.request.SyncRequestService;

@RestController
@RequestMapping("hotcaffeine")
public class HotCaffeineController {
    
    @Autowired
    private SyncRequestService<String> syncRequestService;
    
    @RequestMapping("/value")
    public Result<?> value(String appName, String key) {
        Message message = new Message(MessageType.REQUEST_HOTKEY_VALUE, key);
        Map<String, String> map = syncRequestService.request(appName, message, 1000);
        return Result.getResult(map);
    }
}
