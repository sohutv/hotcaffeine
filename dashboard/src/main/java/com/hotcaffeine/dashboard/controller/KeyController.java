package com.hotcaffeine.dashboard.controller;

import com.hotcaffeine.common.model.KeyRule;
import com.hotcaffeine.dashboard.common.base.BaseController;
import com.hotcaffeine.dashboard.common.domain.Constant;
import com.hotcaffeine.dashboard.common.domain.Page;
import com.hotcaffeine.dashboard.common.domain.Result;
import com.hotcaffeine.dashboard.common.domain.req.PageReq;
import com.hotcaffeine.dashboard.common.domain.req.SearchReq;
import com.hotcaffeine.dashboard.model.KeyTimely;
import com.hotcaffeine.dashboard.service.HotValueService;
import com.hotcaffeine.dashboard.service.KeyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;


/**
 * @author liyunfeng31
 */
@Controller
@RequestMapping("/key")
public class KeyController extends BaseController {
	
	private String prefix = "admin/key";

	@Resource
	private KeyService keyService;

	@Autowired
	private HotValueService hotValueService;

	@GetMapping("/viewTimely")
	public String viewTimely(ModelMap modelMap){
		modelMap.put("title", Constant.TIMELY_KEY_VIEW);
		return prefix + "/listtimely";
	}


	@PostMapping("/listTimely")
	@ResponseBody
	public Page<KeyTimely> listTimely(PageReq page, SearchReq searchReq) {
		return keyService.pageKeyTimely(page, searchReq, userName());
	}

	@PostMapping("/remove")
	@ResponseBody
	public Result<?> remove(String key) {
		String[] arr = key.split("/");
		checkApp(arr[0]);
		int b = keyService.delKeyByUser(new KeyTimely(key,userName()));
		return b == 0 ? Result.fail():Result.success();
	}
	
    @RequestMapping("/value")
    @ResponseBody
    public com.hotcaffeine.data.util.Result<?> value(String appName, String ruleKey, String key) {
        return hotValueService.getValue(appName, KeyRule.buildFullKey(ruleKey, key));
    }
}

