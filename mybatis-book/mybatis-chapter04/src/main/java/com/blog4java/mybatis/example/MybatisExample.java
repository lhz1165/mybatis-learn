package com.blog4java.mybatis.example;

import com.alibaba.fastjson.JSON;
import com.blog4java.common.DbUtils;
import com.blog4java.mybatis.example.entity.StudentEntity;
import com.blog4java.mybatis.example.entity.UserEntity;
import com.blog4java.mybatis.example.mapper.StudentMapper;
import com.blog4java.mybatis.example.mapper.UserMapper;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.session.SqlSessionManager;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.List;

public class MybatisExample {

    @Before
    public void initData() {
        DbUtils.initData();
    }


    @Test
    public void testCache() throws IOException {
         SqlSessionFactory factory = new SqlSessionFactoryBuilder().build(Resources.getResourceAsReader("mybatis-config2.xml"));


        SqlSession sqlSession = factory.openSession(true); // 自动提交事务
        StudentMapper studentMapper = sqlSession.getMapper(StudentMapper.class);

        System.out.println(studentMapper.getStudentById(1));
        System.out.println("增加了" + studentMapper.addStudent(buildStudent()) + "个学生");
        System.out.println(studentMapper.getStudentById(1));

        sqlSession.close();
    }

    private StudentEntity buildStudent(){
        StudentEntity studentEntity = new StudentEntity();
        studentEntity.setName("明明");
        studentEntity.setAge(20);
        return studentEntity;
    }











    @Test
    public  void testMybatis () throws IOException {
        // 获取配置文件输入流
        InputStream inputStream = Resources.getResourceAsStream("mybatis-config2.xml");
        // 通过SqlSessionFactoryBuilder的build()方法创建SqlSessionFactory实例
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        // 调用openSession()方法创建SqlSession实例
        SqlSession sqlSession = sqlSessionFactory.openSession(true);
        SqlSession sqlSession2 = sqlSessionFactory.openSession(true);
        // 获取UserMapper代理对象
        UserMapper userMapper = sqlSession.getMapper(UserMapper.class);
        UserMapper userMapper2 = sqlSession2.getMapper(UserMapper.class);

        // 执行Mapper方法，获取执行结果
        List<UserEntity> userList = userMapper.listAllUser();


        List<UserEntity> userList2 = userMapper2.listAllUser();
        sqlSession.commit();

        List<UserEntity> userList3 = userMapper2.listAllUser();
        sqlSession2.commit();


        // 兼容Ibatis，通过Mapper Id执行SQL操作
//        List<UserEntity> userList = sqlSession.selectList(
//                "com.blog4java.mybatis.example.mapper.UserMapper.listAllUser");

        //System.out.println(JSON.toJSONString(userList));
    }

    @Test
    public void testSessionManager() throws IOException {
        Reader mybatisConfig = Resources.getResourceAsReader("mybatis-config2.xml");
        SqlSessionManager sqlSessionManager = SqlSessionManager.newInstance(mybatisConfig);
        sqlSessionManager.startManagedSession();

        UserMapper userMapper = sqlSessionManager.getMapper(UserMapper.class);
        List<UserEntity> userList = userMapper.listAllUser();
        sqlSessionManager.commit();
        List<UserEntity> userList2 = userMapper.listAllUser();
        System.out.println(JSON.toJSONString(userList));
    }
}
