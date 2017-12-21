package com.netease.hystrix.dubbo.rpc.filter;

import java.lang.reflect.Type;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.DigestUtils;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.extension.Activate;
import com.alibaba.dubbo.rpc.Filter;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcException;

@Activate(group = { Constants.CONSUMER, Constants.PROVIDER })
public class HystrixDubboFilter implements Filter {
	/** 分隔符 */
	private static final String SPLITOR = ".";
	
	@Override
	public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
		String serviceId = this.getServiceId(invoker, invocation);
		return new HystrixDubboCommand(invoker, invocation, serviceId).execute();
	}
	
	/**
	 * 取得服务唯一标识，serviceId会加上方法参数类型摘要前8位，支持方法重载
	 * 
	 * @author gengchaogang
	 * @dateTime 2017年12月13日 上午10:55:02
	 * @param invoker
	 * @param invocation
	 * @return
	 */
	private String getServiceId(Invoker<?> invoker, Invocation invocation) {
		Type[] paramTypes = invocation.getParameterTypes();
		StringBuilder serviceIdBuilder = new StringBuilder(invoker.getInterface().getName()).append(SPLITOR)
				.append(invocation.getMethodName()).append(SPLITOR);
		StringBuilder builder = new StringBuilder("");
		if (ArrayUtils.isNotEmpty(paramTypes)) {
			for (Type type : paramTypes) {
				builder.append(type.toString());
			}
		}
		String hexStr = DigestUtils.md5DigestAsHex(builder.toString().getBytes());
		serviceIdBuilder.append(StringUtils.substring(hexStr, 0, 8));
		return serviceIdBuilder.toString();
	}

}
