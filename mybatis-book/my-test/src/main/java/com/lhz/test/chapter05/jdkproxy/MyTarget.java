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

        //用proxy工具生成一个代理对象
        MyTargetInterface target = factory.newInstance(new MyTarget());

        target.test();


    }
}
