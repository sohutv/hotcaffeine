package com.hotcaffeine.dashboard.controller;

import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.hotcaffeine.common.etcd.EtcdClient;
import com.hotcaffeine.common.etcd.IEtcdConfig;
import com.hotcaffeine.common.model.CacheRule;
import com.hotcaffeine.common.model.KeyRule;
import com.hotcaffeine.common.util.JsonUtil;
import com.hotcaffeine.dashboard.common.base.BaseController;
import com.hotcaffeine.dashboard.common.domain.Constant;
import com.hotcaffeine.dashboard.common.domain.Page;
import com.hotcaffeine.dashboard.common.domain.Result;
import com.hotcaffeine.dashboard.common.domain.req.PageReq;
import com.hotcaffeine.dashboard.common.eunm.ResultEnum;
import com.hotcaffeine.dashboard.model.Rules;
import com.hotcaffeine.dashboard.service.RuleService;


/**
 * @author liyunfeng31
 */
@Controller
@RequestMapping("/rule")
public class RuleController extends BaseController {

	@Resource
	private RuleService ruleService;

	@Resource
	private EtcdClient etcdClient;
	
    @Autowired
    private IEtcdConfig etcdConfig;

	@GetMapping("/viewDetail")
	public String viewDetail(ModelMap modelMap){
		modelMap.put("title", Constant.RULE_CONFIG_VIEW);
		return "admin/rule/json";
	}

	@PostMapping("/getRule")
	@ResponseBody
	public Rules getRule(String app){
		return ruleService.selectRules(app);
	}

	@GetMapping("/appRules")
	@ResponseBody
	public List<KeyRule> getKeyRules(@RequestParam("appName") String appName) {
		return ruleService.keyRules(appName);
	}


	@PostMapping("/save")
	@ResponseBody
	public Result<?> save(Rules rules){
		String app = rules.getApp();
		String keyRuleStr = rules.getRules();
		checkApp(app);
		// ??????: keyRule???cacheName????????????
		String keyCacheStr = etcdClient.get(etcdConfig.getCachePath() + app);
		List<CacheRule> keyCacheList = JsonUtil.toList(keyCacheStr, CacheRule.class);
		if (CollectionUtils.isEmpty(keyCacheList)) {
			return new Result<>(ResultEnum.WEB_ERROR);
		}
		List<KeyRule> keyRuleList = JsonUtil.toList(keyRuleStr, KeyRule.class);
		boolean hasDefaultKeyRule = false;
		for (KeyRule keyRule : keyRuleList) {
			String cacheName = keyRule.getCacheName();
			CacheRule cacheRule = null;
			for (CacheRule keyCache : keyCacheList) {
				if (keyCache.getName().equals(cacheName)) {
				    cacheRule = keyCache;
					break;
				}
			}
			if (cacheRule == null) {
				return new Result<>().setCode(ResultEnum.CACHE_NAME_NOT_EXIST.getCode())
						.setMsg("cacheName: " + cacheName + " ???????????????????????????,????????????");
			}
			if(StringUtils.isEmpty(keyRule.getKey())) {
                return new Result<>().setCode(ResultEnum.PARAM_ERROR.getCode())
                        .setMsg("key???????????????");
            }
			if(keyRule.getInterval() <= 0) {
                return new Result<>().setCode(ResultEnum.PARAM_ERROR.getCode())
                        .setMsg("????????????????????????");
            }
			if(keyRule.getInterval() > com.hotcaffeine.common.util.Constant.MAX_INTERVAL) {
                return new Result<>().setCode(ResultEnum.PARAM_ERROR.getCode())
                        .setMsg("?????????????????????"+com.hotcaffeine.common.util.Constant.MAX_INTERVAL+"???");
            }
			if(keyRule.getTopkCount() < 0) {
                return new Result<>().setCode(ResultEnum.PARAM_ERROR.getCode())
                        .setMsg("topk????????????");
            }
			if(keyRule.getTopkCount() >= 10000) {
                return new Result<>().setCode(ResultEnum.PARAM_ERROR.getCode())
                        .setMsg("topk????????????10000");
            }
			if(keyRule.getKey().equals(KeyRule.DEFAULT_KEY)) {
			    hasDefaultKeyRule = true;
			    if(keyRule.isPrefix()) {
			        return new Result<>().setCode(ResultEnum.PARAM_ERROR.getCode())
	                        .setMsg("?????????keyRule??????????????????????????????");
			    }
			}
            if (keyRule.getNullValueExpire() >= cacheRule.getDuration()) {
                return new Result<>().setCode(ResultEnum.PARAM_ERROR.getCode())
                        .setMsg("??????????????????????????????????????????" + cacheRule.getDuration());
            }
		}
		if(!hasDefaultKeyRule) {
		    return new Result<>().setCode(ResultEnum.CACHE_NAME_NOT_EXIST.getCode())
                    .setMsg("?????????????????????keyRule");
		}
		rules.setUpdateUser(userName());
		int b = ruleService.save(rules);
		return b == 0 ? Result.fail():Result.success();
	}


	@PostMapping("/remove")
	@ResponseBody
	public Result<?> remove(String key){
		checkApp(key);
		int b = ruleService.delRule(key, userName());
		return b == 0 ? Result.fail():Result.success();
	}


	@GetMapping("/view")
	public String view(ModelMap modelMap){
		modelMap.put("title", Constant.RULE_CONFIG_VIEW);
		return "admin/rule/list";
	}

	@PostMapping("/list")
	@ResponseBody
	public Page<Rules> list(PageReq page, String app){
		page.setPageSize(30);
		List<Rules> info = ruleService.pageKeyRule(page, app);
		return new Page<>(1, info.size() ,info);
	}

	@GetMapping("/edit/{app}")
	public String edit(ModelMap modelMap,@PathVariable("app") String app){
		modelMap.put("title", Constant.RULE_CONFIG_VIEW);
		modelMap.put("rules", ruleService.selectRules(app));
		return "admin/rule/view";
	}


	@GetMapping("/add")
	public String add(ModelMap modelMap){
		modelMap.put("title", Constant.RULE_CONFIG_VIEW);
		return "admin/rule/view";
	}


	@PostMapping("/listRules")
	@ResponseBody
	public List<String> rules(){
		return ruleService.listRules(null);
	}
}

