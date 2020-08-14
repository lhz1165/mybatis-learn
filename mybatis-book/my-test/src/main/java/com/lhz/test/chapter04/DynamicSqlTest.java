package com.lhz.test.chapter04;

import com.blog4java.mybatis.example.entity.UserEntity;
import com.blog4java.mybatis.example.mapper.UserMapper;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.jdbc.ScriptRunner;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;

/**
 * @author: lhz
 * @date: 2020/8/4
 **/
public class DynamicSqlTest {
    @Before
    public void initSql() {
        Connection conn = null;
        try {
            Class.forName("org.hsqldb.jdbcDriver");
            conn = DriverManager.getConnection("jdbc:hsqldb:mem:mybatis", "sa", "");
            ScriptRunner scriptRunner = new ScriptRunner(conn);
            // 不输出sql日志
            scriptRunner.setLogWriter(null);
            scriptRunner.runScript(Resources.getResourceAsReader("create-table.sql"));
            scriptRunner.runScript(Resources.getResourceAsReader("init-data.sql"));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testDymicSql() throws Exception {
        SqlSessionFactoryBuilder builder = new SqlSessionFactoryBuilder();
        SqlSessionFactory sessionFactory = builder.build(Resources.getResourceAsReader("mybatis-config2.xml"));
        SqlSession sqlSession = sessionFactory.openSession();
        UserMapper mapper = sqlSession.getMapper(UserMapper.class);
        List<UserEntity> userEntities = mapper.listAllUser();
        for (UserEntity userEntity : userEntities) {
            System.out.println(userEntity);
        }
    }

}
