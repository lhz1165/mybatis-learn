package com.lhz.test.chapter03;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * @author: lhz
 * @date: 2020/7/31
 * 使用这个工具可以狠优雅的获取和设置属性的值
 **/
public class MetaObjectTest {

    @Test
    public void testMeta() {
        List<Order> orders = new ArrayList<Order>(){{
            add(new Order("11111111111","GoLang"));
            add(new Order("22222222222222","Java"));
        }};
        User user = new User(orders, "lhz",23);
        MetaObject metaObject = SystemMetaObject.forObject(user);

        System.out.println(metaObject.getValue("orders[0].orderNo"));

    }


    @Data
    @AllArgsConstructor
    private static class User {
        List<Order> orders;
        String name;
        Integer age;
    }

    @Data
    @AllArgsConstructor
    private static class Order {
        String orderNo;
        String goodsName;
    }

}
