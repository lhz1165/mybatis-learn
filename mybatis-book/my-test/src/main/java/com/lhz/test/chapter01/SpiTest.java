package com.lhz.test.chapter01;

import org.junit.Test;

import java.sql.Driver;
import java.util.ServiceLoader;

/**
 * @author: lhz
 * @date: 2020/7/29
 **/
public class SpiTest {
    @Test
    public void testSpi() {
        ServiceLoader<Driver> drivers = ServiceLoader.load(Driver.class);
        for (Driver driver : drivers) {
            System.out.println(driver.getClass().getName());
        }
    }
}
