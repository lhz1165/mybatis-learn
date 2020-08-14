package com.lhz.test.chapter04;

import com.alibaba.fastjson.JSON;
import com.blog4java.common.DbUtils;
import com.blog4java.mybatis.example.entity.UserEntity;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.*;
import org.apache.ibatis.transaction.jdbc.JdbcTransaction;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.util.List;

/**
 * @author: lhz
 * @date: 2020/8/5
 **/
public class TestExecutor {
    @Before
    public void initData() {
        DbUtils.initData();
    }

    @Test
    public void testExecutor() throws Exception{
        InputStream inputStream = Resources.getResourceAsStream("mybatis-config2.xml");
        SqlSessionFactory sessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        SqlSession sqlSession = sessionFactory.openSession();
        Configuration configuration = sqlSession.getConfiguration();
        // 从Configuration对象中获取描述SQL配置的MappedStatement对象
        MappedStatement mappedStatement = configuration
                .getMappedStatement("com.blog4java.mybatis.example.mapper.UserMapper.listAllUser");
        //创建ReuseExecutor实例
        Executor reuseExecutor = configuration.newExecutor(
                new JdbcTransaction(sqlSession.getConnection()),
                ExecutorType.REUSE
        );
        // 调用query()方法执行查询操作
        List<UserEntity> userList =  reuseExecutor.query(mappedStatement,
                null,
                RowBounds.DEFAULT,
                Executor.NO_RESULT_HANDLER);
        System.out.println(JSON.toJSON(userList));


    }
}
