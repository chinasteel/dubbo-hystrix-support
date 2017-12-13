package com.netease.hystrix.dubbo.rpc.filter;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.extension.Activate;
import com.alibaba.dubbo.rpc.Filter;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcException;

@Activate(group = { Constants.CONSUMER, Constants.PROVIDER })
public class HystrixDubboFilter implements Filter {

	@Override
	public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
		HystrixDubboCommand dubboHystrixCommand = HystrixDubboCommand.getInstance(invoker, invocation);
		return dubboHystrixCommand.execute();
	}

}
