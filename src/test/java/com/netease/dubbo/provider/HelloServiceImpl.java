package com.netease.dubbo.provider;

import org.apache.commons.lang.math.RandomUtils;

import com.netease.dubbo.service.HelloService;

public class HelloServiceImpl implements HelloService {

    @Override
    public String sayHello(String name) {
    	try {
			Thread.sleep(RandomUtils.nextInt(10000));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
        return "Hello " + name;
    }

}
