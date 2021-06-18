package com.hotcaffeine.dashboard.service.impl;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;
import com.hotcaffeine.dashboard.common.domain.req.PageReq;
import com.hotcaffeine.dashboard.model.User;
import com.hotcaffeine.dashboard.service.UserAppService;
import com.hotcaffeine.dashboard.service.UserService;
import com.hotcaffeine.dashboard.util.CipherHelper;
import com.hotcaffeine.data.store.IRedis;

@Component
public class UserServiceImpl implements UserService {
    private final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);
    
    public static final String USER_KEY = "user";
    
    @Autowired
    private IRedis redis;
    
    @Autowired
    private UserAppService userAppService;
    
    @Autowired
    private CipherHelper cipherHelper;

    @Override
    public List<User> pageUser(PageReq page) {
        Map<String, String> mapString = redis.hgetAll(USER_KEY);
        List<User> users = mapString.entrySet().stream().map(entry -> JSON.parseObject(entry.getValue(), User.class))
                .collect(Collectors.toList());
        return users;
    }

    @Override
    public User selectByUserName(String userName) {
        String userStr = redis.hget(USER_KEY, userName);
        if(userStr == null) {
            return null;
        }
        try {
            return JSON.parseObject(userStr, User.class);
        } catch (Exception e) {
            logger.error(userStr, e);
        }
        return null;
    }

    @Override
    public boolean insertUser(User user) {
        user.setCreateTime(new Date());
        if (user.getPwd() != null) {
            user.setPwd(cipherHelper.encrypt(user.getPwd()));
        }
        redis.hset(USER_KEY, user.getUserName(), JSON.toJSONString(user));
        return true;
    }
    
    @Override
    public boolean updateUser(User user) {
        User oldUser = selectByUserName(user.getUserName());
        if(oldUser == null) {
            return false;
        }
        oldUser.setNickName(user.getNickName());
        oldUser.setPwd(user.getPwd());
        oldUser.setRole(user.getRole());
        return insertUser(oldUser);
    }

    @Override
    public boolean deleteUser(String userName) {
        redis.hdel(USER_KEY, userName);
        userAppService.del(userName);
        return true;
    }
}
