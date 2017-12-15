package com.netease.hystrix.dubbo.consumer;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.netease.hystrix.dubbo.service.HelloService;

public class Consumer {
	private static final Logger LOGGER = LoggerFactory.getLogger(Consumer.class);

    public static void main(String[] args) throws IOException, InterruptedException {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[] { "consumer.xml" });
        context.start();
        final HelloService service = context.getBean("helloService", HelloService.class);
        ExecutorService executorService = Executors.newCachedThreadPool();
        int total = 50;
        final AtomicInteger atomicInteger = new AtomicInteger(0);
        for (int i = 0; i < total; i++) {
            executorService.submit(new Runnable() {

                @Override
                public void run() {
                	LOGGER.info(service.sayHello("steel-" + atomicInteger.incrementAndGet()));
                }
            });

        }
        System.in.read(); // 按任意键退出
        
        for (int i = 0; i < total; i++) {
        	LOGGER.info(service.sayHello("steel-" + atomicInteger.incrementAndGet()));
        }
        
        Thread.sleep(10000);
        LOGGER.info("withCircuitBreakerSleepWindowInMilliseconds after 10000");
        for (int i = 0; i < total; i++) {
            executorService.submit(new Runnable() {

                @Override
                public void run() {
                	LOGGER.info(service.sayHello("steel-" + atomicInteger.incrementAndGet()));
                }
            });

        }
        
        System.in.read(); // 按任意键退出
        
        context.close();
    }

}
