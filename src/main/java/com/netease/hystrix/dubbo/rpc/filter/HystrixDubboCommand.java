package com.netease.hystrix.dubbo.rpc.filter;

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
	private final Invoker<?> invoker;
	private final Invocation invocation;

	public HystrixDubboCommand(Invoker<?> invoker, Invocation invocation, String serviceId) {
		super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey(invoker.getInterface().getName()))
				.andCommandKey(HystrixCommandKey.Factory.asKey(serviceId))
				.andThreadPoolKey(HystrixThreadPoolKey.Factory.asKey(serviceId))
				.andCommandPropertiesDefaults(HystrixCommandProperties.Setter()
						/* 设置HystrixCommand.run()的隔离策略 
						 * THREAD —— 在固定大小线程池中，以单独线程执行，并发请求数受限于线程池大小。
						 * SEMAPHORE —— 在调用线程中执行，通过信号量来限制并发量。
						*/
						.withExecutionIsolationStrategy(ExecutionIsolationStrategy.THREAD)
						// 请求容量阈值
						.withCircuitBreakerRequestVolumeThreshold(20)
						// 熔断器中断请求30秒后会进入半打开状态,放部分流量过去重试 默认5s
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
	protected Result getFallback() {// 可以根据不同接口配置不同的Result策略
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

}
