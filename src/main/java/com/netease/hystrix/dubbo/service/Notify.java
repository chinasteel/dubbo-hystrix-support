package com.netease.hystrix.dubbo.service;

public class Notify {

    public void onreturn(String rv, String name) {
        System.out.println("Notify:" + rv);
    }

    public void onthrow(Throwable ex, String name) {
        ex.printStackTrace();
    }
}
