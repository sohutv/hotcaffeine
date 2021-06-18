package com.hotcaffeine.dashboard.auth;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;

import com.hotcaffeine.dashboard.model.User;
import com.hotcaffeine.dashboard.service.UserService;
import com.hotcaffeine.dashboard.util.CipherHelper;
import com.hotcaffeine.dashboard.util.WebUtil;

/**
 * 默认登录
 * 
 * @author yongfeigao
 * @date 2021年6月7日
 */
public class DefaultAuthFilter extends AuthFilter {
    @Autowired
    private UserService userService;

    @Autowired
    private CipherHelper cipherHelper;

    public User getUser(HttpServletRequest request) {
        String email = WebUtil.getLoginCookieValue(request);
        if (email == null) {
            return null;
        }
        email = cipherHelper.decrypt(email);
        if (email == null) {
            return null;
        }
        return userService.selectByUserName(email);
    }

    @Override
    public void loginFailed(HttpServletRequest request, HttpServletResponse response) {
        try {
            String url = WebUtil.getUrl(request);
            try {
                url = URLEncoder.encode(url, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                logger.error("url encode:{}", url, e);
            }
            WebUtil.redirect(response, request, "/login?redirect=" + url);
        } catch (IOException e) {
            logger.error("redirect err", e);
        }
    }
}
