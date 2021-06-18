package com.hotcaffeine.dashboard.controller;

import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.hotcaffeine.dashboard.common.domain.Result;
import com.hotcaffeine.dashboard.common.eunm.ResultEnum;
import com.hotcaffeine.dashboard.model.User;
import com.hotcaffeine.dashboard.service.UserService;
import com.hotcaffeine.dashboard.util.CipherHelper;
import com.hotcaffeine.dashboard.util.WebUtil;

/**
 * 登录相关
 * 
 * @author yongfeigao
 * @date 2018年10月11日
 */
@Controller
@RequestMapping("/login")
public class LoginController {
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private UserService userService;

    @Autowired
    private CipherHelper cipherHelper;

    @RequestMapping
    public String index(Map<String, Object> map) {
        return "admin/login/index";
    }

    /**
     * 用户检查
     * 
     * @return
     * @throws Exception
     */
    @ResponseBody
    @RequestMapping(value = "/check", method = RequestMethod.POST)
    public Result<?> check(@RequestParam("email") String email,
            @RequestParam(value = "password") String password,
            HttpServletResponse response) throws Exception {
        logger.info("user:{} login", email);
        User user = userService.selectByUserName(email);
        if (user == null) {
            return Result.getResult(ResultEnum.LOGIN_FAILED);
        }
        if (!user.getPwd().equals(cipherHelper.encrypt(password))) {
            return Result.getResult(ResultEnum.LOGIN_FAILED);
        }
        // 设置到cookie中
        WebUtil.setLoginCookie(response, cipherHelper.encrypt(email));
        return Result.success();
    }
    
    /**
     * 用户注册
     * 
     * @return
     * @throws Exception
     */
    @ResponseBody
    @RequestMapping(value = "/register", method = RequestMethod.POST)
    public Result<?> register(@RequestParam("email") String email,
            @RequestParam(value = "password") String password,
            @RequestParam(value = "nickName", required = false) String nickName,
            HttpServletResponse response) throws Exception {
        logger.info("user:{} register", email);
        if (StringUtils.isBlank(email) || StringUtils.isBlank(password)) {
            return Result.getResult(ResultEnum.PARAM_ERROR);
        }
        User user = new User();
        user.setUserName(email);
        user.setPwd(password);
        user.setRole("APPUSER");
        if (!StringUtils.isBlank(nickName)) {
            user.setNickName(nickName);
        }
        try {
            userService.insertUser(user);
        } catch (Exception e) {
            return Result.getWebErrorResult(e);
        }
        return Result.success();
    }
}
