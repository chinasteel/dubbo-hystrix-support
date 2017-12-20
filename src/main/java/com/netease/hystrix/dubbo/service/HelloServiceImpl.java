package com.netease.hystrix.dubbo.service;

import org.apache.commons.lang.math.RandomUtils;

public class HelloServiceImpl implements HelloService {

    @Override
    public String sayHello(String name) {
    	try {
			Thread.sleep(RandomUtils.nextInt(1000));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
        return "Hello " + name;
    }

}
