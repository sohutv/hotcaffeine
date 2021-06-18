package com.hotcaffeine.dashboard.common.ex;

import com.hotcaffeine.dashboard.common.domain.Result;
import com.hotcaffeine.dashboard.common.eunm.ResultEnum;
import com.hotcaffeine.dashboard.util.WebUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author liyunfeng31
 */
@ControllerAdvice
public class MyExceptionHandler {

	private Logger logger = LoggerFactory.getLogger(getClass());

	@ExceptionHandler(value = BizException.class)
	@ResponseBody
	public Result<?> bizExceptionHandler(BizException e, HttpServletResponse resp){
		resp.setStatus(e.getCode());
		logger.info("业务异常：",e);
		return Result.error(e.getCode(),e.getMsg());
	}

	@ExceptionHandler(value =Exception.class)
	@ResponseBody
	public Result<?> exceptionHandler(Exception e, HttpServletRequest req, HttpServletResponse resp){
	    logger.error("5xx ip:{} url:{}", WebUtil.getIp(req), WebUtil.getUrl(req), e);
		resp.setStatus(500);
		return Result.error(ResultEnum.BIZ_ERROR);
	}
}
