package com.hotcaffeine.dashboard.controller;

import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

import com.hotcaffeine.dashboard.common.base.BaseController;
import com.hotcaffeine.dashboard.model.User;

/**
 * @Author yongweizhao
 * @Date 2021/1/29 11:23
 */
@Controller
@RequestMapping("/")
public class IndexController extends BaseController {
    @RequestMapping
    public String index(HttpServletRequest request, ModelMap modelMap) {
        User user = getUser();
        if (user != null) {
            modelMap.put("name", user.getNickName() == null ? user.getUserName() : user.getNickName());
            modelMap.put("role", user.getRole());
        }
        return "admin/index";
    }
}
