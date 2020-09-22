package com.blog4java.jdbc;

import com.blog4java.common.DbUtils;
import com.blog4java.common.IOUtils;
import org.junit.Test;

import java.sql.*;

public class Example01 {
    @Test
    public void testJdbc() {
        // 初始化数据
        DbUtils.initData();
        try {
            // 加载驱动
           // Class.forName("org.hsqldb.jdbcDriver");
            // 获取Connection对象
            Connection connection = DriverManager.getConnection("jdbc:hsqldb:mem:mybatis",
                    "sa", "");
            Statement statement = connection.createStatement();
             statement.execute("select * from user");
            ResultSet resultSet = statement.getResultSet();
            // 遍历ResultSet
            ResultSetMetaData metaData = resultSet.getMetaData();
            int columCount = metaData.getColumnCount();
            while (resultSet.next()) {
                for (int i = 1; i <= columCount; i++) {
                    String columName = metaData.getColumnName(i);
                    String columVal = resultSet.getString(columName);
                    System.out.println(columName + ":" + columVal);
                }
                System.out.println("--------------------------------------");
            }
            // 关闭连接
            IOUtils.closeQuietly(statement);
            IOUtils.closeQuietly(connection);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void myTest() {
        // 初始化数据
        DbUtils.initData();
        try {
            Connection conn = DriverManager.getConnection("jdbc:hsqldb:mem:mybatis",
                    "sa", "");
            PreparedStatement statement = conn.prepareStatement("select * from user");
            statement.execute();
            ResultSet resultSet = statement.getResultSet();
            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();
            while (resultSet.next()) {
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnName(i);
                    String val = resultSet.getString(metaData.getColumnName(i));
                    System.out.println(columnName + " : " + val);
                }
                System.out.println("--------------------------------------");
            }



        } catch (Exception e) {
            e.printStackTrace();
        }
    }



}
