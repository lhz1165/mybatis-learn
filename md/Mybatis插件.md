[<<<Parent](../README.md)

# Mybatis插件

### 前置操作初始化SqlSession

解析xml创建Configuration对象，同时又使用工厂对象创建对应的Executor实例，

```java
// 根据Mybatis主配置文件中指定的Executor类型创建对应的Executor实例
final Executor executor = configuration.newExecutor(tx, execType);
```

然后执行拦截器链的拦截器，来获取Executor代理对象（（由于本插件不拦截executor所以返回的不是代理对象而是原本对）

#### Executor

```java
// 执行拦截器链的拦截逻辑
executor = (Executor) interceptorChain.pluginAll(executor);
```

```java
// 调用所有拦截器对象的plugin（）方法执行拦截逻辑
public Object pluginAll(Object target) {
  for (Interceptor interceptor : interceptors) {
    target = interceptor.plugin(target);
  }
  return target;
}
```

### 创建分页插件对象加入到查询参数，执行拦截逻辑

这是我自定义的拦截参数

```
@Intercepts({
        @Signature(method = "prepare", type = StatementHandler.class, args = {Connection.class, Integer.class})
})
public class PageInterceptor implements Interceptor
```



```java
UserQuery query = new UserQuery();
query.setPageSize(5);
query.setFull(true);
List<UserEntity> users = userMapper.getUserPageable(query);
```

接着在每一个handler来执行拦截器，如果没有配置注解没有配置，就不会返回代理对象（通Executor对象创建），

他们的拦截顺序依次是

#### 1.ParameterHandler

```
public ParameterHandler newParameterHandler(MappedStatement mappedStatement, Object parameterObject, BoundSql boundSql) {
  ParameterHandler parameterHandler = mappedStatement.getLang().createParameterHandler(mappedStatement, parameterObject, boundSql);
  // 执行拦截器链的拦截逻辑
  parameterHandler = (ParameterHandler) interceptorChain.pluginAll(parameterHandler);
  return parameterHandler;
}
```

#### 2.ResultSetHandler

```
public ResultSetHandler newResultSetHandler(Executor executor, MappedStatement mappedStatement, RowBounds rowBounds, ParameterHandler parameterHandler,
    ResultHandler resultHandler, BoundSql boundSql) {
  ResultSetHandler resultSetHandler = new DefaultResultSetHandler(executor, mappedStatement, parameterHandler, resultHandler, boundSql, rowBounds);
  // 执行拦截器链的拦截逻辑
  resultSetHandler = (ResultSetHandler) interceptorChain.pluginAll(resultSetHandler);
  return resultSetHandler;
}
```

#### 3.StatementHandler（分页插件注解拦截它，所以这里返回的是代理对象）

```java
public StatementHandler newStatementHandler(Executor executor, MappedStatement mappedStatement, Object parameterObject, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) {
  StatementHandler statementHandler = new RoutingStatementHandler(executor, mappedStatement, parameterObject, rowBounds, resultHandler, boundSql);
  // 执行拦截器链的拦截逻辑
  statementHandler = (StatementHandler) interceptorChain.pluginAll(statementHandler);
  return statementHandler;
}
```

返回代理对象的代码

```java
public static Object wrap(Object target, Interceptor interceptor) {
  // 调用getSignatureMap（）方法获取自定义插件中，
  // 通过Intercepts注解指定要拦截的方法
  Map<Class<?>, Set<Method>> signatureMap = getSignatureMap(interceptor);
  Class<?> type = target.getClass();
  Class<?>[] interfaces = getAllInterfaces(type, signatureMap);
  if (interfaces.length > 0) {
    return Proxy.newProxyInstance(type.getClassLoader(), interfaces, new Plugin(target, interceptor, signatureMap));
  }
  return target;
```

接着在执行查询操作的时候会获取configuration的StatementHandler的代理对象

```java
@Override
public <E> List<E> doQuery(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) throws SQLException {
  Statement stmt = null;
  try {
    Configuration configuration = ms.getConfiguration();
    // 获取StatementHandler对象(这是代理对象)
    StatementHandler handler = configuration.newStatementHandler(wrapper, ms, parameter, rowBounds, resultHandler, boundSql);
    // 调用prepareStatement（）方法,创建Statement对象，并进行设置参数等操作
    stmt = prepareStatement(handler, ms.getStatementLog());
    // 调用StatementHandler对象的query（）方法执行查询操作
    return handler.<E>query(stmt, resultHandler);
  } finally {
    closeStatement(stmt);
  }
}
```

这里拦截的点是prepare()所以到这里会跳到代理类的 invoke()方法里面去

如果该方法是Intercepts注解指定的方法，则调用拦截器实例的intercept（）方法执行拦截逻辑

```
// 调用StatementHandler的prepare（）方法创建Statement对象
stmt = handler.prepare(connection, transaction.getTimeout());
```

```java
@Override
public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
  try {
    // 如果该方法是Intercepts注解指定的方法，则调用拦截器实例的intercept（）方法执行拦截逻辑
    Set<Method> methods = signatureMap.get(method.getDeclaringClass());
    if (methods != null && methods.contains(method)) {
      return interceptor.intercept(new Invocation(target, method, args));
    }
    return method.invoke(target, args);
  } catch (Exception e) {
    throw ExceptionUtil.unwrapThrowable(e);
  }
}
```

进入intercept方法以后 这就是我定义的，把替换解析的sql替换成应该带有分页的参数的sql

```
public Object intercept(Invocation invocation) throws Throwable {
    // 获取拦截的目标对象
    RoutingStatementHandler handler = (RoutingStatementHandler) invocation.getTarget();
    //获取StatementHandler 的 delegate属性
    StatementHandler delegate = (StatementHandler) ReflectionUtils.getFieldValue(handler, "delegate");
    //获取sql
    BoundSql boundSql = delegate.getBoundSql();
    // 获取参数对象，当参数对象为Page的子类时执行分页操作
    Object parameterObject = boundSql.getParameterObject();
    if (parameterObject instanceof Page<?>) {
        Page<?> page = (Page<?>) parameterObject;
        //获取mappedStatement属性
        MappedStatement mappedStatement = (MappedStatement) ReflectionUtils.getFieldValue(delegate, "mappedStatement");
        //目标方法prepare()的Connection参数
        Connection connection = (Connection) invocation.getArgs()[0];
        //获取sql
        String sql = boundSql.getSql();
        if (page.isFull()) {
            // 获取记录总数
            this.setTotalCount(page, mappedStatement, connection);
        }
        page.setTimestamp(System.currentTimeMillis());
        // 获取分页SQL
        String pageSql = this.getPageSql(page, sql);
        // 将原始SQL语句替换成分页语句
        ReflectionUtils.setFieldValue(boundSql, "sql", pageSql);
    }
    return invocation.proceed();
}
```

最后invocation.proceed()，又会接着执行被代理对象的方法,即

handler.prepare(connection, transaction.getTimeout());正常方法的逻辑
