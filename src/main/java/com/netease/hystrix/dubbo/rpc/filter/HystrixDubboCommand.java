package com.netease.hystrix.dubbo.rpc.filter;

import java.lang.reflect.Type;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.DigestUtils;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.RpcResult;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixCommandProperties.ExecutionIsolationStrategy;
import com.netflix.hystrix.HystrixThreadPoolKey;
import com.netflix.hystrix.HystrixThreadPoolProperties;

public class HystrixDubboCommand extends HystrixCommand<Result> {

	private static final int DEFAULT_THREADPOOL_CORE_SIZE = 30;
	/** 分隔符 */
	private static final String SPLITOR = ".";
	private final Invoker<?> invoker;
	private final Invocation invocation;

	public HystrixDubboCommand(Invoker<?> invoker, Invocation invocation) {
		super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey(invoker.getInterface().getName()))
				.andCommandKey(HystrixCommandKey.Factory.asKey(getServiceId(invoker, invocation)))
				.andThreadPoolKey(HystrixThreadPoolKey.Factory.asKey(getServiceId(invoker, invocation)))
				.andCommandPropertiesDefaults(HystrixCommandProperties.Setter()
						/* 设置HystrixCommand.run()的隔离策略 
						 * THREAD —— 在固定大小线程池中，以单独线程执行，并发请求数受限于线程池大小。
						 * SEMAPHORE —— 在调用线程中执行，通过信号量来限制并发量。
						*/
						.withExecutionIsolationStrategy(ExecutionIsolationStrategy.THREAD)
						// 请求容量阈值
						.withCircuitBreakerRequestVolumeThreshold(20)
						// 熔断器中断请求30秒后会进入半打开状态,放部分流量过去重试
						.withCircuitBreakerSleepWindowInMilliseconds(30000)
						// 错误率达到50开启熔断保护
						.withCircuitBreakerErrorThresholdPercentage(50)
						// 使用dubbo的超时，禁用这里的超时
						.withExecutionTimeoutEnabled(false)
						// 设置当使用ExecutionIsolationStrategy.SEMAPHORE时，HystrixCommand.run()方法允许的最大请求数。如果达到最大并发数时，后续请求会被拒绝。
						/*.withExecutionIsolationSemaphoreMaxConcurrentRequests(500)*/
						)
				// 线程池大小不能太大
				.andThreadPoolPropertiesDefaults(
						HystrixThreadPoolProperties.Setter()
						.withCoreSize(getThreadPoolCoreSize(invoker.getUrl()))// 核心线程池大小
						.withQueueSizeRejectionThreshold(20)// 设置队列拒绝的阈值——一个人为设置的拒绝访问的最大队列值，即使maxQueueSize还没有达到。
						.withMaxQueueSize(1024)));// 设置BlockingQueue最大的队列值。

		this.invoker = invoker;
		this.invocation = invocation;
	}

	@Override
	protected Result run() throws Exception {
		return invoker.invoke(invocation);
	}

	@Override
	protected Result getFallback() {
		Throwable throwable = this.getFailedExecutionException();
		if(throwable instanceof RpcException) {
			return fallBackTimeOutResult;
		}
		return fallBackResult;
	}

	private Result fallBackResult = new RpcResult("服务降级..");
	private Result fallBackTimeOutResult = new RpcResult("RPC timeout ..");

	/**
	 * 获取线程池大小
	 * 
	 * @param url
	 * @return
	 */
	private static int getThreadPoolCoreSize(URL url) {
		if (url != null) {
			return url.getParameter("ThreadPoolCoreSize", DEFAULT_THREADPOOL_CORE_SIZE);
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
