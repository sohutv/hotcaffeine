package com.hotcaffeine.dashboard.conf;

import java.io.UnsupportedEncodingException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hotcaffeine.common.etcd.DefaultEtcdConfig;
import com.hotcaffeine.common.etcd.EtcdClient;
import com.hotcaffeine.common.etcd.IEtcdConfig;
import com.hotcaffeine.common.model.KeyCount;
import com.hotcaffeine.common.util.MemoryMQ;
import com.hotcaffeine.common.util.ServiceLoaderUtil;
import com.hotcaffeine.dashboard.auth.AuthFilter;
import com.hotcaffeine.dashboard.auth.DefaultAuthFilter;
import com.hotcaffeine.dashboard.consumer.HotKeyConsumer;
import com.hotcaffeine.dashboard.service.ConfigAppService;
import com.hotcaffeine.dashboard.service.ConfigAppService.DefaultConfigAppService;
import com.hotcaffeine.dashboard.util.CipherHelper;

/**
 * 通用配置
 * 
 * @Description:
 * @author yongfeigao
 * @date 2018年6月13日
 */
@Configuration
public class CommonConfiguration {
    
    @Value("${login.ciperKey}")
    private String ciperKey;
    
    /**
     * 初始化密码助手
     * 
     * @return
     * @throws UnsupportedEncodingException
     */
    @Bean
    public CipherHelper cipherHelper() throws UnsupportedEncodingException {
        CipherHelper cipherHelper = new CipherHelper(ciperKey);
        return cipherHelper;
    }
    
    @Bean
    public AuthFilter authFilter() {
        AuthFilter authFilter = ServiceLoaderUtil.loadService(AuthFilter.class, DefaultAuthFilter.class);
        return authFilter;
    }
    
    @Bean
    public ConfigAppService configAppService() {
        return ServiceLoaderUtil.loadService(ConfigAppService.class, DefaultConfigAppService.class);
    }
    
    @Bean
    @ConfigurationProperties(prefix = "etcd")
    public IEtcdConfig etcdConfig() {
        IEtcdConfig etcdConfig = ServiceLoaderUtil.loadService(IEtcdConfig.class, DefaultEtcdConfig.class);
        etcdConfig.init(null);
        return etcdConfig;
    }
    
    @Bean
    public EtcdClient etcdClient(IEtcdConfig etcdConfig) {
        return new EtcdClient(etcdConfig.getEndpoints(), etcdConfig.getUser(), etcdConfig.getPassword());
    }

    /**
     * 热key内存队列
     * 
     * @param hotKeyConsumer
     * @return
     */
    @Bean
    public MemoryMQ<KeyCount> hotKeyMemoryMQ(HotKeyConsumer hotKeyConsumer) {
        MemoryMQ<KeyCount> memoryMQ = new MemoryMQ<>();
        memoryMQ.setConsumerName("hotKeyMQ");
        // 最大缓存量
        memoryMQ.setBufferSize(500000);
        // 最小批量处理数量
        memoryMQ.setMinBatchDealSize(1);
        memoryMQ.setMemoryMQConsumer(hotKeyConsumer);
        memoryMQ.setDestroyOrder(20);
        memoryMQ.init();
        return memoryMQ;
    }
}
