package com.netease.hystrix.dubbo.provider;

import java.io.IOException;

import org.springframework.context.support.ClassPathXmlApplicationContext;

public class Provider {

    public static void main(String[] args) throws IOException {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[] { "classpath:spring/provider.xml" });
        context.start();

        System.in.read(); // 按任意键退出
        context.close();

    }

}
