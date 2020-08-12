package com.lhz.test.chapter05.jdkproxy;

/**
 * @author: lhz
 * @date: 2020/8/12
 **/
public class MyTarget implements MyTargetInterface{

    @Override
    public void test() {
        System.out.println("hello world");
    }

    public static void main(String[] args) {
        MyProxyFactory<MyTargetInterface> factory = new MyProxyFactory<>(MyTargetInterface.class);

        //首先使用目标接口产生一个代理工具类
        MyProxy myProxy = new MyProxy(new MyTarget());

        //用proxy工具生成一个代理对象
        MyTargetInterface target = factory.getMyProxy(myProxy);

        target.test();


    }
}
