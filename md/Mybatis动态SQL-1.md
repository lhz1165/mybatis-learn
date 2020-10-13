[<<<动态sql](/md/Mybatis的动态sql.md)
# Mybatis动态SQL-1

## 标签

<if|>，<where|>,<choose|when|otherwise>

<foreach>用例

```java
List<UserEntity> getUserByPhones(@Param("phones") List<String> phones);
```

```java
select
<include refid="userAllField"/>
from user
where phone in
<foreach item="phone" index="index" collection="phones" open="(" separator="," close=")">
    #{phone}
</foreach>
```

<trim|set>

```java
<select id="getUserByPhones" resultType="com.blog4java.mybatis.example.entity.UserEntity">
    select
    *
    from user
    where 
    <if test="id != null">
        AND id = #{id}
    </if>
    <if test="name != null">
        AND name = #{name}
    </if>
    <if test="phone != null">
         AND phone = #{phone}
     </if>
  
</select>
为了防止出现 select * from user where ;或者select * from user where and name =?;
可以用
    
        select
    *
    from user
    <trim prefix="where" prefixOverrides ="AND|OR">
    <if test="id != null">
        AND id = #{id}
    </if>
    <if test="name != null">
        AND name = #{name}
    </if>
    <if test="phone != null">
         AND phone = #{phone}
     </if>
     </TRIM>
         
         
```

其实为了不出现上述情况可以直接使用where 1=1或者<where>标签

推荐<where>
