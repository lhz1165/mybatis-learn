package com.lhz.test.chapter05.jdkproxy;

import java.lang.reflect.Proxy;

/**
 * @author: lhz
 * @date: 2020/8/12
 **/
public class MyProxyFactory<T> {


    Class<T> targetClass;

    public MyProxyFactory(Class<T> target) {
        this.targetClass = target;
    }

    public T getMyProxy(MyProxy proxy) {

        return (T)Proxy.newProxyInstance(targetClass.getClassLoader(),new Class[]{targetClass},proxy);
    }

}
