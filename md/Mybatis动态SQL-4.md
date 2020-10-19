[<<<动态sql](/md/Mybatis的动态sql.md)

# Mybatis动态SQL-4

在DynamicSqlSource执行获取BoundSql方法的时候

```
   @Test
    public void testDynamicSql()  {
        UserEntity entity = new UserEntity();
        entity.setPhone("18700001111");
        List<UserEntity> userList =  userMapper.getUserByEntity(entity);
        System.out.println(JSON.toJSONString(userList));
    }

<select id="getUserByEntity"  resultType="com.blog4java.mybatis.example.entity.UserEntity">
    select
    <include refid="userAllField"/>
    from user
    <where>
        <if test="id != null">
            AND id = #{id}
        </if>
        <if test="name != null">
            AND name = #{name}
        </if>
        <if test="phone != null">
            AND phone = #{phone}
        </if>
    </where>
</select>
```

**sqlSourceParser.parse()是一个很重要的一步。**

1. 它包含解析#{}参数，將#{}参数占位符转换为?

2. 解析参数的映射信息。

```java
public BoundSql getBoundSql(Object parameterObject, DynamicContext context) {
  // 以DynamicContext对象作为参数调用SqlNode的apply（）方法
  rootSqlNode.apply(context);
  // 创建SqlSourceBuilder对象
  SqlSourceBuilder sqlSourceParser = new SqlSourceBuilder(configuration);
  Class<?> parameterType = parameterObject == null ? Object.class : parameterObject.getClass();
  // 调用DynamicContext的getSql()方法获取动态SQL解析后的SQL内容，获取参数映射信息
  // 然后调用SqlSourceBuilder的parse（）方法对SQL内容做进一步处理，生成StaticSqlSource对象
  SqlSource sqlSource = sqlSourceParser.parse(context.getSql(), parameterType, context.getBindings());
  // 调用StaticSqlSource对象的getBoundSql（）方法，获得BoundSql实例
    //【parameterObject即为参数信息】
  BoundSql boundSql = sqlSource.getBoundSql(parameterObject);
  // 將<bind>标签绑定的参数添加到BoundSql对象中
  for (Map.Entry<String, Object> entry : context.getBindings().entrySet()) {
    boundSql.setAdditionalParameter(entry.getKey(), entry.getValue());
  }
  return boundSql;
}
```

单独拿出来看一看

```java
public SqlSource parse(String originalSql, Class<?> parameterType, Map<String, Object> additionalParameters) {
  // ParameterMappingTokenHandler为Mybatis参数映射处理器，用于处理SQL中的#{}参数占位符
  ParameterMappingTokenHandler handler = new ParameterMappingTokenHandler(configuration, parameterType, additionalParameters);
    
  // Token解析器，用于解析#{}参数
  GenericTokenParser parser = new GenericTokenParser("#{", "}", handler);
  // 调用GenericTokenParser对象的parse（）方法將#{}参数占位符转换为?
  String sql = parser.parse(originalSql);
  return new StaticSqlSource(configuration, sql, handler.getParameterMappings());
}
```

在调用GenericTokenParser的parse方法中有一步是创建参数映射信息对象,ParameterMapping

```java
builder.append(handler.handleToken(expression.toString()));
```

进入ParameterMappingTokenHandler类里面

把#{}替换成？ 顺便添加参数映射对象

```java
@Override
public String handleToken(String content) {
  parameterMappings.add(buildParameterMapping(content));
  return "?";
}
```





```java
private ParameterMapping buildParameterMapping(String content) {
  // 將#{}占位符内容转换为Map对象
    // properity----->id
  Map<String, String> propertiesMap = parseParameterMapping(content);
  // property 对应的值为参数占位符名称，id
  String property = propertiesMap.get("property");
  // 推断参数类型
    //如果没有指定那么就是Object类型
  Class<?> propertyType;
 if (typeHandlerRegistry.hasTypeHandler(parameterType)) {
    propertyType = parameterType;
    // 如果指定了jdbcType属性，并且为CURSOR类型，则使用ResultSet类型
  } 
  // 使用建造者模式构建ParameterMapping对象
  ParameterMapping.Builder builder = new ParameterMapping.Builder(configuration, property, propertyType);
  Class<?> javaType = propertyType;
  String typeHandlerAlias = null;
  if (typeHandlerAlias != null) {
    builder.typeHandler(resolveTypeHandler(javaType, typeHandlerAlias));
  }
  // 返回ParameterMapping对象
  return builder.build();
}
```

首先获取parameterType，加入我们有设置parameterType="Java.lang.String" ，那么就是StringTypeHandler，否则就是object类型对应unKonwTypeHandler。

有了参数的映射信息，再回到最开始的getBoundSql里面，接着就是

BoundSql boundSql = sqlSource.getBoundSql(parameterObject);

把详细参数添加到BoundSql之中，那么我们这个BoundSql三要素就有了(sql语句，参数映射，参数详情)

sql: select * from user where phone =?

parameterMapping: property->phone,typeHandler -> StringTypeHandler, javaType ->String

parameterObject: UserEntity={phone  : 18700001111 }