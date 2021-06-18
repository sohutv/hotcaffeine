package com.hotcaffeine.dashboard.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.hotcaffeine.dashboard.common.domain.Constant;
import com.hotcaffeine.data.metric.LeapArrayModel;
import com.hotcaffeine.data.util.Result;
import com.hotcaffeine.data.util.Status;

/**
 * 滑动窗口服务
 * 
 * @author yongfeigao
 * @date 2021年3月12日
 */
@Component
public class LeapWindowService {
    @Autowired
    private WorkerServer workerServer;

    private RestTemplate restTemplate = new RestTemplate();
    
    @Value("${worker.server.port}")
    private String wokerServerPort;

    public static final String DATA_URL = "http://%s/leap/window?appName={appName}&key={key}";

    public Result<LeapArrayModel> getLeapWindow(String appName, String key) {
        String serverPort = workerServer.getServer(key);
        if (serverPort == null) {
            return Result.getResult(Status.NO_RESULT);
        }
        ResponseEntity<Result<LeapArrayModel>> responseEntity = restTemplate.exchange(
                String.format(DATA_URL, serverPort.split(Constant.SPIT)[0] + ":" + wokerServerPort), HttpMethod.GET, null,
                new ParameterizedTypeReference<Result<LeapArrayModel>>() {}, appName, key);
        return responseEntity.getBody();
    }
}
