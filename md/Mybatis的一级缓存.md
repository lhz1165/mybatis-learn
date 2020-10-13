# Mybatis的一级缓存

**概述:**Mybatis的一级缓存是默认开始的，并且不能关闭，因为Mybatis的核心特性，例如<collection>和<association>建立级联映射，以及避免循环引用都是基于一级缓存实现的，可以配置作用域，STATEMENT和SESSION,分别是执行完一条语句就清除缓存和对于当前SqlSession都有效。

## 基础

PerpetualCache类是Cache接口的基本实现，通过一个map的数据结构来保存id和查询结果。

其他cache对象都嵌套着这个对象，这就是装饰器模式。一级缓存就是基于这个实现类来做的，在我们的BaseExecutor维护着这个对象

## 使用

```java
 @Override
 public <E> List<E> query(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler) throws SQLException {
   // 获取BoundSql对象，BoundSql是对动态SQL解析生成的SQL语句和参数映射信息的封装
   BoundSql boundSql = ms.getBoundSql(parameter);
   // 创建CacheKey，用于缓存Key
   CacheKey key = createCacheKey(ms, parameter, rowBounds, boundSql);
   // 调用重载的query（）方法
   return query(ms, parameter, rowBounds, resultHandler, key, boundSql);
}
```

每次查询之前，通过sql来创建一个缓存Id，然后再去缓存的Map中去取，取到对象就直接返回，没有就执行

queryFromDatabase()调用数据库的查询方法。

```java
public <E> List<E> query(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, CacheKey key, BoundSql boundSql) throws SQLException {
  ErrorContext.instance().resource(ms.getResource()).activity("executing a query").object(ms.getId());
  if (closed) {
    throw new ExecutorException("Executor was closed.");
  }
  if (queryStack == 0 && ms.isFlushCacheRequired()) {
    clearLocalCache();
  }
  List<E> list;
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
  } finally {
    queryStack--;
  }
  if (queryStack == 0) {
    for (DeferredLoad deferredLoad : deferredLoads) {
      deferredLoad.load();
    }
    // issue #601
    deferredLoads.clear();
    if (configuration.getLocalCacheScope() == LocalCacheScope.STATEMENT) {
      // issue #482
      clearLocalCache();
    }
  }
  return list;
}
```

### 注意：

分布式环境下，Mybatis一级缓存的localCacheScope属性应设置为STATEMENT，不然当其他服务update之后，本服务还是基于session的缓存查询，是结果不一致。