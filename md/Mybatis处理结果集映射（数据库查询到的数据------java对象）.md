[<<<README](/README.md)
# Mybatis处理结果集映射（数据库查询到的数据------>java对象）

```
<select id="getOrderByNoWithJoin" resultMap="detailNestMap">
   select o.*,u.* from "order" o left join user u on (u.id = o.userId) where orderNo = #{orderNo}
</select>
-----------------------------------------------------------------------------------------
 <resultMap  id="detailNestMap" type="com.blog4java.mybatis.example.entity.Order">
        <id column="id" property="id"></id>
        <result column="createTime" property="createTime"></result>
        <result column="userId" property="userId"></result>
        <result column="amount" property="amount"></result>
        <result column="orderNo" property="orderNo"></result>
        <result column="address" property="address"></result>
        <association property="user"  javaType="com.blog4java.mybatis.example.entity.User" >
            <id column="userId" property="id"></id>
            <result column="name" property="name"></result>
            <result column="createTime" property="createTime"></result>
            <result column="password" property="password"></result>
            <result column="phone" property="phone"></result>
            <result column="nickName" property="nickName"></result>
        </association>
    </resultMap>
-----------------------------------------------------------------------------------------
        @Test
    public void testGetOrderByNoWithJoin() {
        Order order = orderMapper.getOrderByNoWithJoin("order_2314236");
        System.out.println(JSON.toJSONString(order));
    }
```







## 1.前情提要:查询执行过程

首先sqlSession通过class对象，获取代理工厂(MapperProxyFactory)，来产生代理类(MapperProxy)，执行真正的sql查询代码

```java
@Override
public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
  return mapperMethod.execute(sqlSession, args);
}
//最终走的还是SQlSession的方法
result = sqlSession.selectOne(command.getName(), param);
```

sqlSession的查询方法里面又调用的是Executor的query()方法

```java
// 以MappedStatement对象作为参数，调用Executor的query（）方法
return executor.query(ms, wrapCollection(parameter), rowBounds, Executor.NO_RESULT_HANDLER);
```

Executor(BaseExecutor) 然后就去缓存里查，查不到就去调用更里面的query（）方法

```java
try {
  queryStack++;
  // 从缓存中获取结果
  list = resultHandler == null ? (List<E>) localCache.getObject(key) : null;
  if (list != null) {
    handleLocallyCachedOutputParameters(ms, key, parameter, boundSql);
  } else {
    // 缓存中获取不到，则调用queryFromDatabase（）方法从数据库中查询
    list = queryFromDatabase(ms, parameter, rowBounds, resultHandler, key, boundSql);
  }
```

```
// 调用doQuery（）方法查询
list = doQuery(ms, parameter, rowBounds, resultHandler, boundSql);
```

queryFromDatabase（）是模板方法模式，子类需要重写doQuery()

所以一般我们进入的的SimpleExecutor的doQuery（），这里面通过StatementHandler类来获取结果集（调用prepareStatement底层的execute() ）

```java
@Override
public <E> List<E> doQuery(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) throws SQLException {
  Statement stmt = null;
  try {
    Configuration configuration = ms.getConfiguration();
    // 获取StatementHandler对象
    StatementHandler handler = configuration.newStatementHandler(wrapper, ms, parameter, rowBounds, resultHandler, boundSql);
    // 调用prepareStatement（）方法,创建Statement对象，并进行设置参数等操作
    //把ResultHandler也给附上值
    stmt = prepareStatement(handler, ms.getStatementLog());
    // 调用StatementHandler对象的query（）方法执行查询操作
    return handler.<E>query(stmt, resultHandler);
  } finally {
    closeStatement(stmt);
  }
}
```

进入到PreparedStatementHandler，里面实际调用的是jdbc的PreparedStatement的方法

```java
public <E> List<E> query(Statement statement, ResultHandler resultHandler) throws SQLException {
  PreparedStatement ps = (PreparedStatement) statement;
  // 调用PreparedStatement对象的execute()方法，执行SQL语句
  ps.execute();
  // 调用ResultSetHandler的handleResultSets（）方法处理结果集
  return resultSetHandler.<E> handleResultSets(ps);
}
```

##  2.resultSetHandler.<E> handleResultSets(ps);

以上方法就是本文讨论的如何使用resultSetHandler处理结果集，把数据库对象转化成java对象

DefaultResultSetHandler就是执行这个操作的主角

```java
 //
  // 处理结果集
  //
  @Override
  public List<Object> handleResultSets(Statement stmt) throws SQLException {
    final List<Object> multipleResults = new ArrayList<Object>();
    int resultSetCount = 0;
    // 1、获取ResultSet对象，將ResultSet对象包装为ResultSetWrapper
    //里面有这条记录的所有列的java类型，列名，jdbc类型的集合
    ResultSetWrapper rsw = getFirstResultSet(stmt);
    // 2、获取ResultMap信息，一般只有一个ResultMap
    List<ResultMap> resultMaps = mappedStatement.getResultMaps();
	....
    // 如果指定了多个ResultMap，则对每个ResultMap进行处理
    //一般只有一个结果集，循环一次
    while (rsw != null && resultMapCount > resultSetCount) {
      /**
       * resultMappings 集合是包含原本类型的字段名
       例如再 order----->user 一对一模型中
         Long id;
         Date createTime;
         BigDecimal amount;
         Long userId;
         String orderNo;
         String address;
         User user;
       * 字段 resultMappings就代表order对象的基本属性，id address ... 包括user属性
       *
       
       	
         Long id;
         Date createTime;
         BigDecimal amount;
         Long userId;
         String orderNo;
         String address;
       * mappedColumns就只代表order表的字段，id address ... 不包括user属性
       *
       */
      ResultMap resultMap = resultMaps.get(resultSetCount);
        
      // 3、调用handleResultSet方法处理结果集 重点!!!!!!!!!!!
      handleResultSet(rsw, resultMap, multipleResults, null);
        
      // 获取下一个结果集对象，需要JDBC驱动支持多结果集
        .......
    }
    // 对multipleResults进行处理，如果只有一个结果集，则返回结果集中的元素，否则返回多个结果集
    return collapseSingleResultList(multipleResults);
  }
```

### 2.1 handleResultSet(rsw, resultMap, multipleResults, null);

此方法重点代码这些

```java
// 如果未指定ResultHandler，则创建默认的ResultHandler实现
DefaultResultHandler defaultResultHandler = new DefaultResultHandler(objectFactory);

// 调用handleRowValues（）方法处理 重点！！！！！！！！ 接下来着重介绍这个
handleRowValues(rsw, resultMap, defaultResultHandler, rowBounds, null);


// 获取处理后的结果
multipleResults.add(defaultResultHandler.getResultList());
```

再进去**handleRowValues**(rsw, resultMap, defaultResultHandler, rowBounds, null);

```java
// 如果有嵌套的ResultMap，调用handleRowValuesForNestedResultMap处理嵌套ResultMap
//比如left join 查询 resultmap嵌套了一个collection或者association代表order中的user属性
handleRowValuesForNestedResultMap(rsw, resultMap, resultHandler, rowBounds, parentMapping);
```

再进入**handleRowValuesForNestedResultMap**(rsw, resultMap, resultHandler, rowBounds, parentMapping);

这个方法就是解析resultMap的值和嵌套标签里面的映射对象的属性

分别是Order对象 和它left join的user属性

```java
private void handleRowValuesForNestedResultMap(ResultSetWrapper rsw, ResultMap resultMap, ResultHandler<?> resultHandler, RowBounds rowBounds, ResultMapping parentMapping) throws SQLException {
....
      rowValue = getRowValue(rsw, discriminatedResultMap, rowKey, null, partialObject);
......
}
```

实际上先请求缓存，然后还是委托**getRowValue**来做这件事情

```java
private Object getRowValue(ResultSetWrapper rsw, ResultMap resultMap, CacheKey combinedKey, String columnPrefix, Object partialObject) throws SQLException {
  final String resultMapId = resultMap.getId();
 		......
            // 处理通过<constructor>标签配置的构造器映射 一般返回一个未初始化成员变量的对象，属性全是null
            //1111111 先创造一个未初始化的主对象！！！！！！
            //例如
            //Order={id =null ,amount = null ,.... ,user = null}
    rowValue = createResultObject(rsw, resultMap, lazyLoader, columnPrefix);
    // 判断结果对象是否注册对应的TypeHandler
  
     
      // 处理非<id>,<constructor>指定的映射 //1获取列名 2获取数据库查出来的列值 3获取属性名 4把查出来的列值赋值给属性 
        //除了表以外的字段比如user对象（嵌套映射）
        ////Order={id =1 ,amount = 10 ,.... ,user = null}
      foundValues = applyPropertyMappings(rsw, resultMap, metaObject, lazyLoader, columnPrefix) || foundValues;
      putAncestor(rowValue, resultMapId);
    
      // 处理嵌套的映射
    //处理user关联表的值
      foundValues = applyNestedResultMappings(rsw, resultMap, metaObject, columnPrefix, combinedKey, true) || foundValues;

			....
  return rowValue;
}
```

关键代码就是上面的三个方法 ，以后细说这三个方法

```java
// 处理通过<constructor>标签配置的构造器映射 
rowValue = createResultObject(rsw, resultMap, lazyLoader, columnPrefix);
//一般返回一个未初始化成员变量的对象，属性全是null order{id=null,address=null ... user =null}
```

接着再循环中一次为对应列名赋值,[user属性下一步再处理]（以后再细说如何赋值的）

```java
 // 处理非<id>,<constructor>指定的映射 
 // 1获取列名 2获取数据库查出来的列值 3获取属性名 4把查出来的列值赋值给属性 (除了表以外的字段),比如user对象（嵌套映射）
//Order={id =null ,amount = null ,.... ,user = null}
//处理完之后就是
//// 处理嵌套的映射此时Order={id =1 ,amount = 10 ,.... ,user = null}
        foundValues = applyPropertyMappings(rsw, resultMap, metaObject, lazyLoader, columnPrefix) || foundValues;
```



```java
 // 处理嵌套的映射此时Order={id =1 ,amount = 10 ,.... ,user = null}
    //处理user关联表的值
      foundValues = applyNestedResultMappings(rsw, resultMap, metaObject, columnPrefix, combinedKey, true) || foundValues;
//处理完之后user就有值了
```

他的做法是循环resultMapping，即order对象的所有属性（id,time,amount....user），找出是嵌套对象的哪一个，即user

最终执行下面的方法来初始化user，就是调用上面的getRowValue()，

按照同样的步骤来实例化和初始化user(所有属性为null)，接着初始化user。

```java
// 嵌套ResultMap ，JOIN查询映射
  private boolean applyNestedResultMappings(ResultSetWrapper rsw, ResultMap resultMap, MetaObject metaObject, String parentPrefix, CacheKey parentRowKey, boolean newObject) {
    boolean foundValues = false;
    for (ResultMapping resultMapping : resultMap.getPropertyResultMappings()) {
 			//找出是嵌套对象的哪一个， 即user
          if (anyNotNullColumnHasValue(resultMapping, columnPrefix, rsw)) {
            // 调用getRowValue（）方法，根据嵌套结果集映射信息创建Java实体
              //为user赋值
           rowValue = getRowValue(rsw, nestedResultMap, combinedKey, columnPrefix, rowValue);
          }
           
    return foundValues;
  }
```

到此所有流程就结束了。



### 注意事项

最好不要再实体类里面使用构造函数否则必须再mapper里面也必须配置对应的构造函数映射(<constructor标签>)

如果使用默认无参，

在getRowValue()方法中，会返回一个new出来的对象所有属性未赋值

```
// 处理通过<constructor>标签配置的构造器映射 一般返回一个未初始化成员变量的对象，属性全是null
rowValue = createResultObject(rsw, resultMap, lazyLoader, columnPrefix);
```

createResultObject有多种情况，假如只是有参和无参

```java
if (resultType.isInterface() || metaType.hasDefaultConstructor()) {
    //无参构造，直接使用objectFactory的反射来生成对象
  return objectFactory.create(resultType);
} else if (shouldApplyAutomaticMappings(resultMap, false)) {
    //有参构造，对比mapper里的配置，如果不匹配那么抛出异常
  return createByConstructorSignature(rsw, resultType, constructorArgTypes, constructorArgs, columnPrefix);
}
```

在有参构造，对比mapper里的配置，如果不匹配那么抛出异常中

```java
  for (Constructor<?> constructor : constructors) {
      if (allowedConstructor(constructor, rsw.getClassNames())) {
        return createUsingConstructor(rsw, resultType, constructorArgTypes, constructorArgs, columnPrefix, constructor);
      }
    }
  }
  throw new ExecutorException("No constructor found in " + resultType.getName() + " matching " + rsw.getClassNames());
}
```
