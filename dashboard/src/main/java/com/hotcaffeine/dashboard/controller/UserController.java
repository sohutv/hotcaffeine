package com.hotcaffeine.dashboard.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.hotcaffeine.dashboard.common.base.BaseController;
import com.hotcaffeine.dashboard.common.domain.Constant;
import com.hotcaffeine.dashboard.common.domain.Page;
import com.hotcaffeine.dashboard.common.domain.Result;
import com.hotcaffeine.dashboard.common.domain.req.PageReq;
import com.hotcaffeine.dashboard.model.User;
import com.hotcaffeine.dashboard.service.AppService;
import com.hotcaffeine.dashboard.service.UserAppService;
import com.hotcaffeine.dashboard.service.UserService;


@Controller
@RequestMapping("/user")
public class UserController extends BaseController {

	@Resource
	private UserService userService;

	@Resource
	private UserAppService userAppService;

	@Autowired
	private AppService appService;

	@ResponseBody
	@PostMapping("/info")
    public User info(HttpServletRequest request) {
        User user = getUser();
        Set<String> appNames = null;
        if (user.admin()) {
            appNames = appService.queryAllAppName();
        } else {
            appNames = userAppService.selectUserApp(user.getUserName());
        }
        User newUser = new User();
        newUser.setRole(user.getRole());
        if (appNames != null) {
            newUser.setAppNames(new ArrayList<>(appNames));
        }
        if (request.getParameter("appName") != null) {
            String appName = request.getParameter("appName");
            if (StringUtils.isNotEmpty(appName)) {
                newUser.setAppName(appName);
            }
        }
        return newUser;
    }

	@GetMapping("/view")
    public String view(ModelMap modelMap){
		modelMap.put("title", Constant.USER_MANAGE_VIEW);
        return "admin/user/list";
    }

	@PostMapping("/list")
	@ResponseBody
	public Page<User> list(PageReq page){
		List<User> info = userService.pageUser(page);
		return new Page<>(1, info.size(), info);
	}

	@PostMapping("getUserName")
	@ResponseBody
	public String getUserName(HttpServletRequest request, HttpServletResponse response){
		return getUser().getNickName() == null ? getUser().getUserName() : getUser().getNickName();
	}
	
    @GetMapping("/add")
    public String add(){
        return "admin/user/add";
    }

    @PostMapping("/add")
    @ResponseBody
    public Result<?> add(User user) {
        boolean rst = userService.insertUser(user);
        return rst ? Result.success() : Result.fail();
    }

    @PostMapping("/remove")
    @ResponseBody
    public Result<?> remove(String key) {
        boolean ok = userService.deleteUser(key);
        return ok ? Result.success() : Result.fail();
    }


    @GetMapping("/edit/{userName}")
    public String edit(@PathVariable("userName") String userName, ModelMap modelMap){
        modelMap.put("user", userService.selectByUserName(userName));
        return "admin/user/edit";
    }


    @PostMapping("/edit")
    @ResponseBody
    public Result<?> editSave(User user) {
        return Result.success(userService.updateUser(user));
    }

    @GetMapping("/editPwd/{userName}")
    public String editPwd(@PathVariable("userName") String userName, ModelMap modelMap){
        modelMap.put("user", userService.selectByUserName(userName));
        return "admin/user/editPwd";
    }

    @PostMapping("/editPwd")
    @ResponseBody
    public Result<?> editPwdSave(User user){
        return Result.success(userService.updateUser(user));
    }
}

