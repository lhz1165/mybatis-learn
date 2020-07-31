package com.lhz.test.chapter03;

import org.apache.ibatis.reflection.factory.DefaultObjectFactory;

import java.util.Arrays;
import java.util.List;

/**
 * @author: lhz
 * @date: 2020/7/31
 **/
public class ObjectFactoryTest {
    public static void main(String[] args) {
        DefaultObjectFactory objectFactory = new DefaultObjectFactory();
        List list = objectFactory.create(List.class);
        list.add(1);
        list.add(2);
        list.add(3);
        System.out.println(list);
    }
}
