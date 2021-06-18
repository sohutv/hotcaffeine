package com.hotcaffeine.dashboard.util;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletRequest;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.web.util.WebUtils;

/**
 * web相关工具
 * @Description: 
 * @author yongfeigao
 * @date 2018年6月12日
 */
public class WebUtil {

    public static final String LOGIN_TOKEN = "HC_TOKEN";

    /**
     * 从request中获取客户端ip
     * 
     * @param request
     * @return
     */
    public static String getIp(ServletRequest request) {
        HttpServletRequest req = (HttpServletRequest) request;
        String addr = getHeaderValue(req, "X-Forwarded-For");
        if (StringUtils.isNotEmpty(addr) && addr.contains(",")) {
            addr = addr.split(",")[0];
        }
        if (StringUtils.isEmpty(addr)) {
            addr = getHeaderValue(req, "X-Real-IP");
        }
        if (StringUtils.isEmpty(addr)) {
            addr = req.getRemoteAddr();
        }
        return addr;
    }
    
    /**
     * 获取请求的完整url
     * @param request
     * @return
     */
    public static String getUrl(HttpServletRequest request) {
        String url = request.getRequestURL().toString();
        String queryString = request.getQueryString();
        if(queryString != null) {
            url += "?" + request.getQueryString();
        }
        return url;
    }
    
    /**
     * 获取ServletRequest header value
     * @param request
     * @param name
     * @return
     */
    public static String getHeaderValue(HttpServletRequest request, String name) {
        String v = request.getHeader(name);
        if(v == null) {
            return null;
        }
        return v.trim();
    }
    
    /**
     * 从request属性中获取对象
     * @param request
     * @return
     */
    public static void setEmailAttribute(ServletRequest request, String email) {
        request.setAttribute("email", email);
    }
    
    /**
     * 从request属性中获取对象
     * @param request
     * @return
     */
    public static String getEmailAttribute(ServletRequest request) {
        Object email = request.getAttribute("email");
        if(email == null) {
            return null;
        }
        return email.toString();
    }
    
    /**
     * 设置对象到request属性中
     * @param request
     * @return
     */
    public static void setAttribute(ServletRequest request, String name, Object obj) {
        request.setAttribute(name, obj);
    }
    
    /**
     * 从request属性中获取对象
     * @param request
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> T getAttribute(ServletRequest request, String name) {
        return (T) request.getAttribute(name);
    }
    
    /**
     * 输出内容到页面
     * @param response
     * @param result
     * @throws IOException
     */
    public static void print(HttpServletResponse response, String result) throws IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.print(result);
        out.flush();
        out.close();
        out = null;
    }
    
    /**
     * 获取登录的cookie的值
     * 
     * @param request
     * @return
     */
    public static String getLoginCookieValue(HttpServletRequest request) {
        Cookie cookie = WebUtils.getCookie(request, LOGIN_TOKEN);
        if(cookie != null) {
            return cookie.getValue();
        }
        return null;
    }
    
    /**
     * 获取登录的cookie
     * 
     * @param request
     * @return
     */
    public static Cookie getLoginCookie(HttpServletRequest request) {
        return WebUtils.getCookie(request, LOGIN_TOKEN);
    }

    /**
     * 设置登录的cookie
     * 
     * @param request
     */
    public static void setLoginCookie(HttpServletResponse response, String value) {
        Cookie cookie = new Cookie(LOGIN_TOKEN, value);
        cookie.setPath("/");
        response.addCookie(cookie);
    }

    /**
     * 移除登录的cookie
     * 
     * @param request
     */
    public static void deleteLoginCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(LOGIN_TOKEN, "");
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }
    
    /**
     * 跳转
     * @param response
     * @param request
     * @param path
     * @throws IOException 
     */
    public static void redirect(HttpServletResponse response, HttpServletRequest request, String path) throws IOException {
        response.sendRedirect(request.getContextPath() + path);
    }
    
    /**
     * count格式化
     * @param value
     * @return
     */
    public static String countFormat(long value) {
        if (value >= 100000000) {
            return format(value / 100000000F) + "亿";
        }
        if (value >= 10000) {
            return format(value / 10000F) + "万";
        }
        return format(value);
    }
    
    /**
     * size格式化
     * @param value
     * @return
     */
    public static String sizeFormat(long value) {
        if (value >= 1073741824) {
            return format(value / 1073741824F) + "g";
        }
        if (value >= 1048576) {
            return format(value / 1048576F) + "m";
        }
        if (value >= 1024) {
            return format(value / 1024F) + "k";
        }
        return format(value) + "b";
    }

    public static String format(float value) {
        long v = (long) (value * 10);
        if (v % 10 == 0) {
            return String.valueOf(v / 10);
        }
        return String.valueOf(v / 10.0);
    }
}
