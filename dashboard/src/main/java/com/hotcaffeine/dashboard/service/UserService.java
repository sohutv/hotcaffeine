package com.hotcaffeine.dashboard.service;

import java.util.List;

import com.hotcaffeine.dashboard.common.domain.req.PageReq;
import com.hotcaffeine.dashboard.model.User;

/**
 * @Author: liyunfeng31
 * @Date: 2020/4/16 20:37
 */
public interface UserService {

    List<User> pageUser(PageReq page);

    User selectByUserName(String userName);
    
    boolean insertUser(User user);
    
    boolean deleteUser(String userName);

    boolean updateUser(User user);
}
