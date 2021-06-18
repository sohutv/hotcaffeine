package com.hotcaffeine.dashboard.controller;

import java.util.List;

import javax.annotation.Resource;

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.hotcaffeine.dashboard.common.base.BaseController;
import com.hotcaffeine.dashboard.common.domain.Constant;
import com.hotcaffeine.dashboard.common.domain.Page;
import com.hotcaffeine.dashboard.common.domain.req.PageReq;
import com.hotcaffeine.dashboard.model.Worker;
import com.hotcaffeine.dashboard.service.WorkerService;


@Controller
@RequestMapping("/worker")
public class WorkerController extends BaseController {
	
	private String prefix = "admin/worker";

	@Resource
	private WorkerService workerService;

	@GetMapping("/view")
    public String view(ModelMap modelMap){
		modelMap.put("title", Constant.WORKER_VIEW);
    	return prefix + "/list";
    }

	@PostMapping("/list")
	@ResponseBody
		public Page<Worker> list(PageReq page, String searchText){
		List<Worker> info = workerService.pageWorker(page, param(searchText));
		return new Page<>(1, info.size(), info);
	}

}

