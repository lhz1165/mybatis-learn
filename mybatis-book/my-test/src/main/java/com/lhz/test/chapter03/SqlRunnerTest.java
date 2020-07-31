package com.lhz.test.chapter03;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.jdbc.SQL;
import org.apache.ibatis.jdbc.ScriptRunner;
import org.apache.ibatis.jdbc.SqlRunner;
import org.junit.Test;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * @author: lhz
 * @date: 2020/7/31
 * mybatis封装的查询语句selectAll，selectOne
 **/
public class SqlRunnerTest {

    @Test
    public void testRunner() throws ClassNotFoundException, SQLException, IOException {
        Class.forName("org.hsqldb.jdbcDriver");
        Connection conn = DriverManager.getConnection("jdbc:hsqldb:mem:mybatis", "sa", "");
        ScriptRunner scriptRunner = new ScriptRunner(conn);
        scriptRunner.setLogWriter(null);
        scriptRunner.runScript(Resources.getResourceAsReader("create-table.sql"));
        scriptRunner.runScript(Resources.getResourceAsReader("init-data.sql"));

        //使用sqlRunner 和 SQL执行sql
        SqlRunner runner = new SqlRunner(conn);
        String sql = new SQL().SELECT("*")
                .FROM("user")
                .WHERE("id = ?").toString();
        Map<String, Object> objectMap = runner.selectOne(sql, Integer.valueOf(1));
        System.out.println(objectMap);


    }
}
