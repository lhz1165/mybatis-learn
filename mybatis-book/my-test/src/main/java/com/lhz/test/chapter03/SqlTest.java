package com.lhz.test.chapter03;

import org.apache.ibatis.jdbc.SQL;
import org.junit.Test;

/**
 * @author: lhz
 * @date: 2020/7/31
 * mybatis拼接sql的工具类
 **/
public class SqlTest {


    @Test
    public void testSql() {
        String newSql = new SQL()
                .SELECT("P.ID, P.USERNAME, P.PASSWORD, P.FULL_NAME")
                .SELECT("P.LAST_NAME, P.CREATED_ON, P.UPDATED_ON")
                .FROM("PERSON P")
                .FROM("ACCOUNT A")
                .INNER_JOIN("DEPARTMENT D on D.ID = P.DEPARTMENT_ID")
                .INNER_JOIN("COMPANY C on D.COMPANY_ID = C.ID")
                .WHERE("P.ID = A.ID")
                .WHERE("P.FIRST_NAME like ?")
                .OR()
                .WHERE("P.LAST_NAME like ?")
                .GROUP_BY("P.ID")
                .HAVING("P.LAST_NAME like ?")
                .OR()
                .HAVING("P.FIRST_NAME like ?")
                .ORDER_BY("P.ID")
                .ORDER_BY("P.FULL_NAME")
                .toString();

        System.out.println(newSql);

    }
}