package com.hotcaffeine.dashboard.auth;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.hotcaffeine.dashboard.model.User;
import com.hotcaffeine.dashboard.util.WebUtil;

/**
 * 登录校验
 * 
 * @author yongfeigao
 * @date 2021年6月7日
 */
public abstract class AuthFilter extends OncePerRequestFilter {
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        // 排除/actuator和/static路径
        String requestPath = request.getRequestURI();
        if (StringUtils.startsWithIgnoreCase(requestPath, "/actuator") ||
                StringUtils.startsWithIgnoreCase(requestPath, "/static") ||
                StringUtils.startsWithIgnoreCase(requestPath, "/login") ||
                requestPath.endsWith(".ico") || requestPath.endsWith(".jpg") || requestPath.endsWith(".png")) {
            filterChain.doFilter(request, response);
            return;
        }
        // 获取用户
        User user = getUser(request);
        if (user == null) {
            loginFailed(request, response);
        } else {
            WebUtil.setAttribute(request, "u", user);
            filterChain.doFilter(request, response);
        }
    }

    public abstract User getUser(HttpServletRequest request);

    public abstract void loginFailed(HttpServletRequest request, HttpServletResponse response) throws IOException;
}
