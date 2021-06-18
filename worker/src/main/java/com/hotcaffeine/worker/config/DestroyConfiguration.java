package com.hotcaffeine.worker.config;

import java.util.Collections;
import java.util.List;

import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import com.hotcaffeine.common.model.Destroyable;

/**
 * 销毁配置
 * @Description: 
 * @author yongfeigao
 * @date 2018年3月5日
 */
@Configuration
public class DestroyConfiguration {
    
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());
    
    @Autowired
    private List<Destroyable> list;
    
    @PreDestroy
    public void destroy(){
        Collections.sort(list);
        for(Destroyable destroyable : list) {
            try {
                logger.info("destroy:{}", destroyable);
                destroyable.destroy();
            } catch (Exception e) {
                logger.info("destroy err:{}", destroyable, e);
            }
        }
    }
}
