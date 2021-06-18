package com.hotcaffeine.dashboard.common.base;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;

import com.alibaba.fastjson.JSON;
import com.hotcaffeine.dashboard.common.domain.req.SearchReq;
import com.hotcaffeine.dashboard.common.eunm.ResultEnum;
import com.hotcaffeine.dashboard.common.ex.BizException;
import com.hotcaffeine.dashboard.model.User;
import com.hotcaffeine.dashboard.service.UserAppService;
import com.hotcaffeine.dashboard.service.UserService;
import com.hotcaffeine.dashboard.util.WebUtil;


public class BaseController {

    @Resource
    protected HttpServletRequest request;
    
    @Resource
    protected UserService userService;

    @Resource
    private UserAppService userAppService;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        dateFormat.setLenient(false);
        binder.registerCustomEditor(Date.class, new CustomDateEditor(dateFormat, true));
    }

    public void checkApp(String app) {
        User user = getUser();
        if (!user.admin()) {
            Set<String> appNames = ownAppList();
            if (CollectionUtils.isEmpty(appNames) || !appNames.contains(app)) {
                throw new BizException(ResultEnum.NO_PERMISSION);
            }
        }
    }

    public String userName() {
        return getUser().getUserName();
    }

    public boolean isAdmin() {
        return getUser().admin();
    }

    public int userId() {
        return getUser().getId();
    }
    
    public User getUser() {
        return WebUtil.getAttribute(request, "u");
    }

    public SearchReq param(String text){
        SearchReq dto = JSON.parseObject(text, SearchReq.class);
        if(dto == null){ dto = new SearchReq(); }
        return dto;
    }

    public Set<String> ownAppList() {
        return userAppService.selectUserApp(userName());
    }

}
