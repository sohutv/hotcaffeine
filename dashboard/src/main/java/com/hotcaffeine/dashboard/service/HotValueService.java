package com.hotcaffeine.dashboard.service;

import java.util.Map;
import java.util.TreeMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.alibaba.fastjson.JSON;
import com.hotcaffeine.dashboard.common.domain.Constant;
import com.hotcaffeine.dashboard.model.ValueModel;
import com.hotcaffeine.data.util.Result;
import com.hotcaffeine.data.util.Status;

/**
 * 热值服务
 * 
 * @author yongfeigao
 * @date 2021年4月9日
 */
@Component
public class HotValueService {
    @Autowired
    private WorkerServer workerServer;
    
    private RestTemplate restTemplate = new RestTemplate();
    
    @Value("${worker.server.port}")
    private String wokerServerPort;

    public static final String DATA_URL = "http://%s/hotcaffeine/value?appName={appName}&key={key}";

    @SuppressWarnings({"rawtypes", "unchecked"})
    public Result<?> getValue(String appName, String key) {
        String serverPort = workerServer.getServer(key);
        if (serverPort == null) {
            return Result.getResult(Status.NO_RESULT);
        }
        ResponseEntity<Result<Map>> responseEntity = restTemplate.exchange(
                String.format(DATA_URL, serverPort.split(Constant.SPIT)[0] + ":" + wokerServerPort), HttpMethod.GET, null,
                new ParameterizedTypeReference<Result<Map>>() {
                }, appName, key);
        Result<Map> result = responseEntity.getBody();
        Map<String, String> map = result.getResult();
        if(map == null) {
            return result;
        }
        Map<String, ValueModel> resultMap = new TreeMap<>();
        map.forEach((k,v)->{
            resultMap.put(k, JSON.parseObject(v, ValueModel.class));
        });
        return Result.getResult(resultMap);
    }
}
