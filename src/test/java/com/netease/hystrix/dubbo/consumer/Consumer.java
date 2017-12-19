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
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[] { "classpath:spring/consumer.xml" });
        context.start();
        final HelloService service = context.getBean("helloService", HelloService.class);
        ExecutorService executorService = Executors.newCachedThreadPool();
        int total = 40;
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
        	String result = null;
        	try {
        		result = service.sayHello("steel-" + atomicInteger.incrementAndGet());
			} catch (Exception e) {// dubbo 调用超时或者其他异常捕获
				LOGGER.error(e.getMessage(),e);
			}
        	LOGGER.info(result);
        }
        
        System.in.read(); // 按任意键退出
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
