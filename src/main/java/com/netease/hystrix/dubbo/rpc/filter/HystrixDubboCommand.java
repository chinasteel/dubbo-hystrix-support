package com.netease.hystrix.dubbo.rpc.filter;

import java.lang.reflect.Type;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.DigestUtils;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcResult;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixThreadPoolProperties;

public class HystrixDubboCommand extends HystrixCommand<Result> {
	private static final Logger LOGGER = LoggerFactory.getLogger(HystrixDubboCommand.class);

	private static final int DEFAULT_THREADPOOL_CORE_SIZE = 30;
	/** 分隔符 */
	private static final String SPLITOR = ".";
	private final Invoker<?> invoker;
	private final Invocation invocation;

	public HystrixDubboCommand(Invoker<?> invoker, Invocation invocation) {
		super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey(invoker.getInterface().getName()))
				.andCommandKey(HystrixCommandKey.Factory.asKey(getServiceId(invoker, invocation)))
				.andCommandPropertiesDefaults(HystrixCommandProperties.Setter()
						// 请求容量阈值
						.withCircuitBreakerRequestVolumeThreshold(200)
						// 熔断器中断请求10秒后会进入半打开状态,放部分流量过去重试
						.withCircuitBreakerSleepWindowInMilliseconds(10000)
						// 错误率达到50开启熔断保护
						.withCircuitBreakerErrorThresholdPercentage(50)
						// 使用dubbo的超时，禁用这里的超时
						.withExecutionTimeoutEnabled(false)
						// 允许最大并发执行的数量
						.withExecutionIsolationSemaphoreMaxConcurrentRequests(500)
						.withFallbackIsolationSemaphoreMaxConcurrentRequests(500))
				.andThreadPoolPropertiesDefaults(
						HystrixThreadPoolProperties.Setter().withCoreSize(getThreadPoolCoreSize(invoker.getUrl()))// 线程池为30
								.withMaxQueueSize(1000)));

		this.invoker = invoker;
		this.invocation = invocation;
	}

	@Override
	protected Result run() throws Exception {
		return invoker.invoke(invocation);
	}

	@Override
	protected Result getFallback() {
		return fallBackResult;
	}

	private Result fallBackResult = new RpcResult("服务降级..");

	/**
	 * 获取线程池大小
	 * 
	 * @param url
	 * @return
	 */
	private static int getThreadPoolCoreSize(URL url) {
		if (url != null) {
			int size = url.getParameter("ThreadPoolCoreSize", DEFAULT_THREADPOOL_CORE_SIZE);
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("ThreadPoolCoreSize:" + size);
			}
			return size;
		}

		return DEFAULT_THREADPOOL_CORE_SIZE;

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
	private static String getServiceId(Invoker<?> invoker, Invocation invocation) {
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
