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
    int resultMapCount = resultMaps.size();
    // 校验ResultMap,如果该ResultMap名称没有配置，则抛出异常
    validateResultMapsCount(rsw, resultMapCount);
    // 如果指定了多个ResultMap，则对每个ResultMap进行处理
    //一般只有一个结果集，循环一次
    while (rsw != null && resultMapCount > resultSetCount) {
      /**
       * resultMappings 集合是包含原本类型的字段名
       例如再 order----->user 一对一模型中
       * resultMappings就代表order对象的基本属性，id address ... 包括user属性
       *
       * mappedColumns就只代表order表的字段，不包括user属性
       *
       */
      ResultMap resultMap = resultMaps.get(resultSetCount);
      // 3、调用handleResultSet方法处理结果集
      handleResultSet(rsw, resultMap, multipleResults, null);
      // 获取下一个结果集对象，需要JDBC驱动支持多结果集
      rsw = getNextResultSet(stmt);
      cleanUpAfterHandlingResultSet();
      resultSetCount++;
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
// 调用handleRowValues（）方法处理
handleRowValues(rsw, resultMap, defaultResultHandler, rowBounds, null);
// 获取处理后的结果
multipleResults.add(defaultResultHandler.getResultList());
```

再进去handleRowValues(rsw, resultMap, defaultResultHandler, rowBounds, null);

```java
// 如果有嵌套的ResultMap，调用handleRowValuesForNestedResultMap处理嵌套ResultMap
//比如left join 查询 resultmap嵌套了一个collection或者association代表order中的user属性
handleRowValuesForNestedResultMap(rsw, resultMap, resultHandler, rowBounds, parentMapping);
```

再进入handleRowValuesForNestedResultMap(rsw, resultMap, resultHandler, rowBounds, parentMapping);

```java
private Object getRowValue(ResultSetWrapper rsw, ResultMap resultMap, CacheKey combinedKey, String columnPrefix, Object partialObject) throws SQLException {
  final String resultMapId = resultMap.getId();
  // 如果缓存了嵌套ResultMap对应的实体对象，则调用applyNestedResultMappings（）方法处理
  Object rowValue = partialObject;
  if (rowValue != null) {
    final MetaObject metaObject = configuration.newMetaObject(rowValue);
    putAncestor(rowValue, resultMapId);
    // 调用applyNestedResultMappings（）方法处理嵌套的映射
    applyNestedResultMappings(rsw, resultMap, metaObject, columnPrefix, combinedKey, false);
    ancestorObjects.remove(resultMapId);
  } else {
    // ResultLoaderMap用于存放懒加载ResultMap信息
    final ResultLoaderMap lazyLoader = new ResultLoaderMap();
    // 处理通过<constructor>标签配置的构造器映射 一般返回一个未初始化成员变量的对象，属性全是null
    rowValue = createResultObject(rsw, resultMap, lazyLoader, columnPrefix);
    // 判断结果对象是否注册对应的TypeHandler
    if (rowValue != null && !hasTypeHandlerForResultObject(rsw, resultMap.getType())) {
      final MetaObject metaObject = configuration.newMetaObject(rowValue);
      // 是否使用构造器映射
      boolean foundValues = this.useConstructorMappings;
      // 是否指定了自动映射
      if (shouldApplyAutomaticMappings(resultMap, true)) {
        // 调用applyAutomaticMappings（）方法处理自动映射
        foundValues = applyAutomaticMappings(rsw, resultMap, metaObject, columnPrefix) || foundValues;
      }
      // 处理非<id>,<constructor>指定的映射 //1获取列名 2获取数据库查出来的列值 3获取属性名 4把查出来的列值赋值给属性 除了表以外的字段比如user对象（嵌套映射）
      foundValues = applyPropertyMappings(rsw, resultMap, metaObject, lazyLoader, columnPrefix) || foundValues;
      putAncestor(rowValue, resultMapId);
      // 处理嵌套的映射
      foundValues = applyNestedResultMappings(rsw, resultMap, metaObject, columnPrefix, combinedKey, true) || foundValues;
      ancestorObjects.remove(resultMapId);
      foundValues = lazyLoader.size() > 0 || foundValues;
      rowValue = foundValues || configuration.isReturnInstanceForEmptyRow() ? rowValue : null;
    }
    if (combinedKey != CacheKey.NULL_CACHE_KEY) {
      nestedResultObjects.put(combinedKey, rowValue);
    }
  }
  return rowValue;
}
```

关键代码

```java
//再while循环中(假如selectList)获取每一行的结果
rowValue = getRowValue(rsw, discriminatedResultMap, rowKey, null, partialObject);
if (partialObject == null) {
  storeObject(resultHandler, resultContext, rowValue, parentMapping, rsw.getResultSet());
}
```

进入getRow()

```java
// 处理通过<constructor>标签配置的构造器映射 
//一般返回一个未初始化成员变量的对象，属性全是null order{id=null,address=null ... user =null}
rowValue = createResultObject(rsw, resultMap, lazyLoader, columnPrefix);
```

接着再循环中一次为对应列名赋值,[user属性下一步再处理]（以后再细说如何赋值的）

```java
 // 处理非<id>,<constructor>指定的映射 
 // 1获取列名 2获取数据库查出来的列值 3获取属性名 4把查出来的列值赋值给属性 (除了表以外的字段),比如user对象（嵌套映射）
        foundValues = applyPropertyMappings(rsw, resultMap, metaObject, lazyLoader, columnPrefix) || foundValues;
```

```java
// 处理<result>标签配置的映射
private boolean applyPropertyMappings(ResultSetWrapper rsw, ResultMap resultMap, MetaObject metaObject, ResultLoaderMap lazyLoader, String columnPrefix)
    throws SQLException {
  // 获取通过<result>标签指定映射的字段名称
  final List<String> mappedColumnNames = rsw.getMappedColumnNames(resultMap, columnPrefix);
  // foundValues变量用于标识是否获取到数据库字段对应的值
  boolean foundValues = false;
  final List<ResultMapping> propertyMappings = resultMap.getPropertyResultMappings();
  // 对所有通过<result>标签配置了映射的字段进行赋值
  for (ResultMapping propertyMapping : propertyMappings) {
    // 获取数据库字段名称
    String column = prependPrefix(propertyMapping.getColumn(), columnPrefix);
 
      // 1获取数据库字段对应的值
      Object value = getPropertyMappingValue(rsw.getResultSet(), metaObject, propertyMapping, lazyLoader, columnPrefix);
      //2 获取Java实体对应的属性名称
     final String property = propertyMapping.getProperty();
      
      // 3调用MetaObject对象的setValue（）方法为返回的实体对象设置属性值
      metaObject.setValue(property, value);
     
    
  }
  return foundValues;
}
```

此时order{id = 2 ,address = "杭州" ... user =null},下一步为user赋值

他的做法是循环resultMapping，即order对象的所有属性，找出是嵌套对象的哪一个，即user

最终执行下面的方法来初始化user，就是递归到上面的getRowValue()，按照新建user(所有属性为null)，接着初始化user

```
// 调用getRowValue（）方法，根据嵌套结果集映射信息创建Java实体
 rowValue = getRowValue(rsw, nestedResultMap, combinedKey, columnPrefix, rowValue);
```

到此所有流程就结束了。



### 注意事项

最好不要再实体类里面使用构造函数否则必须再mapper里面配置构造函数映射(<constructor标签>)

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