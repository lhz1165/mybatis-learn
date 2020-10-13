[<<<Parent](../Mybatis的动态sql.md)
# Mybatis动态SQL-3

有了之前的学习，我们对动态sql的基本配件有了一定的了解，现在来了解以下动态sql的解析过程.

## 第一步从LanguageDriver出发

```java
@Override
public SqlSource createSqlSource(Configuration configuration, XNode script, Class<?> parameterType) {
  // 该方法用于解析XML文件中配置的SQL信息
  // 创建XMLScriptBuilder对象
  XMLScriptBuilder builder = new XMLScriptBuilder(configuration, script, parameterType);
  // 调用 XMLScriptBuilder对象parseScriptNode（）方法解析SQL资源
  return builder.parseScriptNode();
}
```

解析工作委托给XMLScriptBuilder 来做

```java
public SqlSource parseScriptNode() {
  // 调用parseDynamicTags（）方法將SQL配置转换为SqlNode对象
  MixedSqlNode rootSqlNode = parseDynamicTags(context);
  SqlSource sqlSource = null;
  // 判断Mapper SQL配置中是否包含动态SQL元素，如果是创建DynamicSqlSource对象，否则创建RawSqlSource对象
  if (isDynamic) {
    sqlSource = new DynamicSqlSource(configuration, rootSqlNode);
  } else {
    sqlSource = new RawSqlSource(configuration, rootSqlNode, parameterType);
  }
  return sqlSource;
}
```

 **主要由parseDynamicTags(context);这个方法把mapper的配置转化为sqlNode对象，然后再创建SqlSource对象。**

parseDynamicTags()就是遍历所有的node对象来往MixedSqlNode添加node的

最终MixedSqlNode对象包含了一个所有sqlNode对象的集合

## sqlNode根据参数生成动态sql

上面的过程是把mapper的配置生成sqlNode，那么如果把传递过来的参数给生成sql语句呢？



因为解析后的SqlNode封装再SqlSource中，由于MappedStatment中又封装了SqlSource，当MappedStatment调用getBoundSql的时候

```java
public BoundSql getBoundSql(Object parameterObject) {
  BoundSql boundSql = sqlSource.getBoundSql(parameterObject);
  List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
  if (parameterMappings == null || parameterMappings.isEmpty()) {
    boundSql = new BoundSql(configuration, boundSql.getSql(), parameterMap.getParameterMappings(), parameterObject);
  }

  // check for nested result maps in parameter mappings (issue #30)
  for (ParameterMapping pm : boundSql.getParameterMappings()) {
    String rmId = pm.getResultMapId();
    if (rmId != null) {
      ResultMap rm = configuration.getResultMap(rmId);
      if (rm != null) {
        hasNestedResultMaps |= rm.hasNestedResultMaps();
      }
    }
  }

  return boundSql;
}
```

进入看SqlSource的getBound方法，具体过程写的很清晰了

```java
@Override
public BoundSql getBoundSql(Object parameterObject) {
  // 通过参数对象，创建动态SQL上下文对象
  DynamicContext context = new DynamicContext(configuration, parameterObject);
  // 以DynamicContext对象作为参数调用SqlNode的apply（）方法
  rootSqlNode.apply(context);
  // 创建SqlSourceBuilder对象
  SqlSourceBuilder sqlSourceParser = new SqlSourceBuilder(configuration);
  Class<?> parameterType = parameterObject == null ? Object.class : parameterObject.getClass();
   
  // 调用DynamicContext的getSql()方法获取动态SQL解析后的SQL内容，
  // 然后调用SqlSourceBuilder的parse（）方法对SQL内容做进一步处理，生成StaticSqlSource对象,
  //把#{}变成？替换操作
    
     //select * from t where t.id =#{id}===>select * from t where t.id =?
  SqlSource sqlSource = sqlSourceParser.parse(context.getSql(), parameterType, context.getBindings());
  // 调用StaticSqlSource对象的getBoundSql（）方法，获得BoundSql实例
  BoundSql boundSql = sqlSource.getBoundSql(parameterObject);
  // 將<bind>标签绑定的参数添加到BoundSql对象中
  for (Map.Entry<String, Object> entry : context.getBindings().entrySet()) {
    boundSql.setAdditionalParameter(entry.getKey(), entry.getValue());
  }
  return boundSql;
}
```



## 总结

在解析configuration生成初始化过程之中（SqlSessionFactory的build()），把Mapper通过**LanguageDriver**被解析成**MixedSqlNode**，然后利用**MixedSqlNode**作为构造函数的初始值，构建**SqlSource**，然后把**SqlSource**添加到**MappedStatement**对象之中，要读取sql，就调用**MappedStatement**的**getBoundSql**()方法，这样就会间接的调用**sqlSource**的**getBoundSql**()方法(就是上面的方法)，从而获取动态的sql语句。