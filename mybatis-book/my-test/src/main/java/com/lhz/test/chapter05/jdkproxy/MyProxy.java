package com.lhz.test.chapter05.jdkproxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * @author: lhz
 * @date: 2020/8/12
 **/
public class MyProxy implements InvocationHandler {

    MyTargetInterface targetInterface;

    public MyProxy(MyTargetInterface targetInterface) {
        this.targetInterface = targetInterface;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        System.out.println("-------------");
        Object result = method.invoke(targetInterface, null);
        System.out.println("-------------");
        return result;
    }
}