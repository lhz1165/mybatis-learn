package com.lhz.test.chapter02;

import org.apache.ibatis.datasource.DataSourceFactory;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSourceFactory;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.jdbc.ScriptRunner;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.ServiceLoader;

/**
 * @author: lhz
 * @date: 2020/7/29
 **/
public class WayGetConnectionExample {


    /**
     * 获取connection对象的四种方式
     * @throws IOException
     * @throws SQLException
     */
    @Test
    public void testJndi() throws IOException, SQLException {

        //driver.connect()最后都调用了这个方法
        Connection connByJNID = getConnByDataSourceAndJNID();
        Connection connByDriverManager = getConnByDriverManager();
        Connection connByDataSourceAndProperties = getConnByDataSourceAndProperties();
        Connection connByDataSourceAndParam = getConnByDataSourceAndParam();
        Connection connBySPI = getConnBySPI();
        System.out.println("通过jndi: "+connByJNID);
        System.out.println("通过driverManager: "+connByDriverManager);
        System.out.println("通过DataSource(加载配置文件): "+connByDataSourceAndProperties);
        System.out.println("通过DataSource(填参数): "+connByDataSourceAndParam);
        System.out.println("通过SPI: "+connBySPI);
    }


    public Connection getConnBySPI() throws IOException, SQLException {
        ServiceLoader<Driver> drivers = ServiceLoader.load(Driver.class);
        Properties properties = new Properties();
        InputStream configStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("database2.properties");
        properties.load(configStream);
        for (Driver driver : drivers) {
            System.out.println(driver.getClass().getName());
            Connection connect = driver.connect("jdbc:hsqldb:mem:mybatis", properties);
            return connect;
        }
        return null;
    }

    /**
     * DriverManager最原始的方式获取驱动
     * @return
     */
    public Connection getConnByDriverManager() {
        try {
            // 加载HSQLDB驱动
            Class.forName("org.hsqldb.jdbcDriver");
            // 获取Connection对象
            Connection conn = DriverManager.getConnection("jdbc:hsqldb:mem:mybatis", "sa", "");
            System.out.println(conn.getClass().getName());
            return conn;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 创建DataSource实例，通过dataSource创建connection
     * 读取配置文件的参数
     * @return
     * @throws SQLException
     */
    public Connection getConnByDataSourceAndProperties() throws SQLException, IOException {
        DataSourceFactory dsf = new UnpooledDataSourceFactory();
        Properties properties = new Properties();
        InputStream configStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("database2.properties");
        properties.load(configStream);
        dsf.setProperties(properties);
        DataSource dataSource = dsf.getDataSource();
        // 获取Connection对象
        Connection connection = dataSource.getConnection();
        return connection;
    }

    /**
     * 创建DataSource实例，通过dataSource创建connection
     * 直接写参数
     * @return
     * @throws SQLException
     */
    public Connection getConnByDataSourceAndParam() throws SQLException {
        // 创建DataSource实例
        DataSource dataSource = new UnpooledDataSource("org.hsqldb.jdbcDriver", "jdbc:hsqldb:mem:mybatis", "sa", "");
        // 获取Connection对象
        Connection connection = dataSource.getConnection();
        return connection;
    }


    /**
     *
     * @return
     * @throws IOException
     */
    public Connection getConnByDataSourceAndJNID() throws IOException {
        Properties jndiProps;
        Context ctx;
        Connection conn;
        DataSourceFactory dsf = new UnpooledDataSourceFactory();
        Properties properties = new Properties();
        InputStream configStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("database2.properties");
        properties.load(configStream);
        dsf.setProperties(properties);
        DataSource dataSource = dsf.getDataSource();
        try {
            jndiProps = new Properties();
            jndiProps.put(Context.INITIAL_CONTEXT_FACTORY, "org.apache.naming.java.javaURLContextFactory");
            jndiProps.put(Context.URL_PKG_PREFIXES, "org.apache.naming");
            ctx = new InitialContext(jndiProps);
            //创建服务  之后就可以用来lookup去查询了
            ctx.bind("java:lhzDC", dataSource);


            dataSource = (DataSource) ctx.lookup("java:lhzDC");
            conn = dataSource.getConnection();
            return conn;
        } catch (NamingException | SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
}
