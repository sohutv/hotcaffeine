package com.hotcaffeine.dashboard.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hotcaffeine.common.etcd.EtcdClient;
import com.hotcaffeine.common.util.DigestUtil;
import com.hotcaffeine.dashboard.common.domain.Result;
import com.hotcaffeine.dashboard.common.eunm.ResultEnum;

/**
 * @Author yongweizhao
 * @Date 2021/2/4 14:22
 */
@Service
public class EtcdClientService {

    private final Logger logger = LoggerFactory.getLogger(EtcdClientService.class);

    @Autowired
    private EtcdClient etcdClient;

    public Result<?> registerUser(String user) {
        try {
            // 添加user
            etcdClient.userAdd(user, DigestUtil.encode(user));
            // 赋予角色
            etcdClient.userGrantRole(user, "client");
        } catch (Exception e) {
            logger.error("register etcd user err, user:{}", user, e);
            return new Result<>(ResultEnum.ETCD_REGISTER_ERROR);
        }
        return Result.success();
    }

    public Result<?> deleteUser(String user) {
        try {
            etcdClient.delete(user);
        } catch (Exception e) {
            logger.error("register etcd user err, user:{}", user, e);
            return new Result<>(ResultEnum.ETCD_DELETE_USER_ERROR);
        }
        return Result.success();
    }

}
