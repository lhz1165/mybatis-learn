[<<<README](/md/MyBatis的级联映射以及延迟加载.md)
# MyBatis延迟加载

Mybatis可以通过，<collection//>或者<association//>标签来嵌套一个外部mapper，来达到关联查询的目的

例如

```xml
<resultMap id="detailMap" type="com.blog4java.mybatis.example.entity.Order">
    <association property="user" javaType="com.blog4java.mybatis.example.entity.User"
                 fetchType="lazy"
                 select="com.blog4java.mybatis.example.mapper.UserMapper.getUserById" column="userId">
    </association>
</resultMap>

<select id="getUserById" resultType="com.blog4java.mybatis.example.entity.User">
    select * from user where id = #{userId}
</select>

<select id="getOrderByNo" resultMap="detailMap">
    select * from "order" where orderNo = #{orderNo}
</select>

Order order =  orderMapper.getOrderByNo("order_2314234");

```

执行这段查询代码 可以同时运行两个sql



在**DefaultResultSetHandler**中.之前所说的一对多属性会让**hasNestedResultMaps**()为true，所以走第一个判断，

只要是resultMap嵌套外部mapper那么为false，但是**hasNestQueries**属性为true

```java
public void handleRowValues(ResultSetWrapper rsw, ResultMap resultMap, ResultHandler<?> resultHandler, RowBounds rowBounds, ResultMapping parentMapping) throws SQLException {
  // 是否有嵌套ResultMap
  if (resultMap.hasNestedResultMaps()) {
   。。。。。
       //association或者collection 直接嵌套列属性走这一步
    // 如果有嵌套的ResultMap，调用handleRowValuesForNestedResultMap处理嵌套ResultMap
    handleRowValuesForNestedResultMap(rsw, resultMap, resultHandler, rowBounds, parentMapping);
  } else {
     //association或者collection 嵌套外部mapper走这一步
    // 如果无嵌套的ResultMap，调用handleRowValuesForSimpleResultMap处理简单非嵌套ResultMap
    handleRowValuesForSimpleResultMap(rsw, resultMap, resultHandler, rowBounds, parentMapping);
  }
}
```



**进入handleRowValuesForSimpleResultMap()**

```java
private void handleRowValuesForSimpleResultMap(ResultSetWrapper rsw, ResultMap resultMap, ResultHandler<?> resultHandler, RowBounds rowBounds, ResultMapping parentMapping)
    throws SQLException {
  DefaultResultContext<Object> resultContext = new DefaultResultContext<Object>();
  // 遍历处理每一行记录
  while (shouldProcessMoreRows(resultContext, rowBounds) && rsw.getResultSet().next()) {
    // 调用getRowValue（）把一行数据转换为Java实体对象
    Object rowValue = getRowValue(rsw, discriminatedResultMap);
    storeObject(resultHandler, resultContext, rowValue, parentMapping, rsw.getResultSet());
  }
}
```

最终还是使用的是getRowValue()方法来设置java对象的属性的，这是之前的重载方法。





```java
// 处理非嵌套ResultMap
private Object getRowValue(ResultSetWrapper rsw, ResultMap resultMap) throws SQLException {
  // 创建ResultLoaderMap对象，用于存放懒加载属性信息
  final ResultLoaderMap lazyLoader = new ResultLoaderMap();
  // 创建ResultMap指定的类型实例，通常为<resultMap>标签的type属性指定的类型
    //实例化，还没有对他初始化
    //order = {id=null,amount =null ... user=mull}
  Object rowValue = createResultObject(rsw, resultMap, lazyLoader, null);
  // 判断该类型是否注册了TypeHandler
  if (rowValue != null && !hasTypeHandlerForResultObject(rsw, resultMap.getType())) {
    final MetaObject metaObject = configuration.newMetaObject(rowValue);
    boolean foundValues = this.useConstructorMappings;
    // 判断是否需要处理自动映射
    if (shouldApplyAutomaticMappings(resultMap, false)) {
      // 调用applyAutomaticMappings（）方法处理自动映射的字段
      foundValues = applyAutomaticMappings(rsw, resultMap, metaObject, null) || foundValues;
    }
    // 处理<result>标签配置映射的字段
    foundValues = applyPropertyMappings(rsw, resultMap, metaObject, lazyLoader, null) || foundValues;
    foundValues = lazyLoader.size() > 0 || foundValues;
    rowValue = foundValues || configuration.isReturnInstanceForEmptyRow() ? rowValue : null;
  }
  return rowValue;
}
```



### 重点关注一下上面的createResultObject()

```java
// 初始化返回的实体对象，并处理构造方法映射
private Object createResultObject(ResultSetWrapper rsw, ResultMap resultMap, ResultLoaderMap lazyLoader, String columnPrefix) throws SQLException {

  // 调用createResultObject（）方法创建结果对象
  Object resultObject = createResultObject(rsw, resultMap, constructorArgTypes, constructorArgs, columnPrefix);
    //.....
      if (propertyMapping.getNestedQueryId() != null && propertyMapping.isLazy()) {
          
        // 调用ProxyFactory实例的createProxy（）方法创建代理对象
        resultObject = configuration.getProxyFactory().createProxy(resultObject, lazyLoader, configuration, objectFactory, constructorArgTypes, constructorArgs);
		//......
  return resultObject;
}
```

首先调用重载的createResultObject()实例化一个对象。

然后判断是否右懒加载机制，如果有那么用getProxyFactory()来创建代理对象。

当我们开启了懒加载，实际查询到的是代理对象。

如果调用了getUser()方法，那么就会执行代理方法，进行嵌套查询。就是之前的内容了。

