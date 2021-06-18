package com.hotcaffeine.worker.netty.request;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.hotcaffeine.common.model.Message;
import com.hotcaffeine.common.model.MessageType;
import com.hotcaffeine.worker.WorkerApplication;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = WorkerApplication.class)
public class SyncRequestServiceTest {

    @Autowired
    private SyncRequestService<String> syncRequestService;
    
    @Test
    public void test() {
        Message message = new Message(MessageType.APP_NAME, "test");
        Map<String, String> map = syncRequestService.request("core.api-web-mobile.hotkey", message, 1000);
        Assert.assertNotNull(map);
    }

}
