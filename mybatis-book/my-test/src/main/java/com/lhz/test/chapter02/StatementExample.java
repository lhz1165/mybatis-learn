package com.lhz.test.chapter02;

import com.blog4java.common.IOUtils;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.jdbc.ScriptRunner;
import org.junit.Test;

import java.io.*;
import java.sql.*;
import java.util.Arrays;

/**
 * @author: lhz
 * @date: 2020/7/30
 **/
public class StatementExample {

    @Test
    public void testStatement() throws Exception {

        Connection connection = getConnByDriverManager();
        // 使用Mybatis的ScriptRunner工具类执行数据库脚本
        ScriptRunner scriptRunner = new ScriptRunner(connection);
        // 不输出sql日志
        //scriptRunner.setLogWriter(null);
        System.out.println(System.out.getClass());
        scriptRunner.runScript(Resources.getResourceAsReader("create-table.sql"));
        Statement statement = connection.createStatement();
        statement.addBatch("insert into  " +
                "user(create_time, name, password, phone, nick_name) " +
                "values('2010-10-24 10:20:30', 'User1', 'test', '18700001111', 'User1');");
        statement.addBatch("insert into " +
                "user (create_time, name, password, phone, nick_name) " +
                "values('2010-10-24 10:20:30', 'User2', 'test', '18700002222', 'User2');");
        statement.executeBatch();
        statement.execute("select * from user");

        ResultSet result = statement.getResultSet();
        printResult(result);
        // 关闭连接
        IOUtils.closeQuietly(statement);
        IOUtils.closeQuietly(connection);

    }

    @Test
    public void testInsert() throws Exception {
        Connection connection = getConnByDriverManager();
        // 使用Mybatis的ScriptRunner工具类执行数据库脚本
        ScriptRunner scriptRunner = new ScriptRunner(connection);
        // 不输出sql日志
        PrintWriter writer = new PrintWriter(System.out);

        //scriptRunner.setLogWriter(writer);
        scriptRunner.runScript(Resources.getResourceAsReader("create-table.sql"));
        Statement statement = connection.createStatement();
        String sql = "insert into user(create_time, name, password, phone, nick_name) " +
                "values('2010-10-24 10:20:30','User1','test','18700001111','User1');";
        statement.executeUpdate(sql, Statement.RETURN_GENERATED_KEYS);
        //插入返回主键
        ResultSet genKeys = statement.getGeneratedKeys();
        if(genKeys.next()) {
            System.out.println("自增长主键：" + genKeys.getInt(1));
        }
        //查询
        statement.execute("select * from user");
        ResultSet resultSet = statement.getResultSet();
        ResultSetMetaData metaData = resultSet.getMetaData();
        int columnCount = metaData.getColumnCount();
        while (resultSet.next()) {
            for (int i = 1; i <= columnCount; i++) {
                String columName = metaData.getColumnName(i);
                String columVal = resultSet.getString(i);
                System.out.println(columName + ":" + columVal);
            }
            System.out.println("-------------------------------------");
        }


        IOUtils.closeQuietly(statement);
        IOUtils.closeQuietly(connection);

    }




    public Connection getConnByDriverManager() {
        try {
            // 加载HSQLDB驱动
            Class.forName("org.hsqldb.jdbcDriver");
            // 获取Connection对象
            Connection conn = DriverManager.getConnection("jdbc:hsqldb:mem:mybatis", "sa", "");
            return conn;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void printResult(ResultSet resultSet) throws Exception {
        ResultSetMetaData metaData = resultSet.getMetaData();
        int columCount = metaData.getColumnCount();
        while (resultSet.next()) {
            for (int i = 1; i <= columCount; i++) {
                String columName = metaData.getColumnName(i);
                String columVal = resultSet.getString(columName);
                System.out.println(columName + ":" + columVal);
            }
            System.out.println("-------------------------------------");
        }
    }
}
