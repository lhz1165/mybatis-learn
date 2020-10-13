[<<<Parent](README.md)
# Mybatis的二级缓存

解析mapper分三步

1. 用XMLconfigBuilder来解析(parse()方法)mybatis的主配置文件里面的<configure>标签
2. 用XMLMapperBuilder来解析（parse()方法）<configure>标签里面的<mapper>标签
3. 用XMLStatementBuilder来解析mapper里的<select|insert|..>（statementParser.parseStatementNode()方法）

## 二级缓存

#### 第一阶段

由于二级缓存默认关闭,开启二级缓存需要在configure配置文件里面主动设置

```
<settings>
    <setting name="localCacheScope" value="SESSION"/>
    <setting name="cacheEnabled" value="true"/>
</settings>
```

获取sqlSession工厂，解析配置文件来初始化Configuration类

```java
SqlSessionFactory factory = new SqlSessionFactoryBuilder().build(Resources.getResourceAsReader("mybatis-config2.xml"));
```

在SqlSessionFactoryBuilder里的使用XMLConfigBuilder来解析

```java
public SqlSessionFactory build(Reader reader, String environment, Properties properties) {
    XMLConfigBuilder parser = new XMLConfigBuilder(reader, environment, properties);
    return build(parser.parse());
}
```

进去parser.parse()，在XMLConfigBuilder类里面，以下给出解析xml的关键代码

```java
//解析xml配置文件
public Configuration parse() {
  // 调用XPathParser.evalNode（）方法，创建表示configuration节点的XNode对象。
  // 调用parseConfiguration（）方法对XNode进行处理
  //创建mapperfactory的map结果集
  parseConfiguration(parser.evalNode("/configuration"));
  return configuration;
}

 private void parseConfiguration(XNode root) {
  	//获取setting节点，比如上面配置的 <setting name="cacheEnabled" value="true"/>
      Properties settings = settingsAsProperties(root.evalNode("settings"));
      loadCustomVfs(settings);
      settingsElement(settings);
      //
      //这是mapper.xml的<mapper标签的解析方法
      //同时解析了mapper接口会生成proxyFactory
      mapperElement(root.evalNode("mappers"));
   
  }


```

------

#### 第二阶段

mapperElement(root.evalNode("mappers"));比较关键，他是生成mapper文件的mappedStatement的对象以及代理类和代理工厂的地方

这里把检测到cacheEnabled，会把cahce对象放入mappedStatement，表示开启二级缓存

我们进去看看

```java
private void mapperElement(XNode parent) throws Exception {
    XMLMapperBuilder mapperParser = new XMLMapperBuilder(inputStream, configuration, resource, configuration.getSqlFragments());
            //解析mapper的关键 进parse看
            mapperParser.parse();
}


public void parse() {
    // 调用XPathParser的evalNode（）方法获取根节点对应的XNode对象
    //这些node节点包括mapper.xml里面的关键节点包括select,insert等
    //这一步也设置缓存
    configurationElement(parser.evalNode("/mapper"));
}
private void configurationElement(XNode context) {
   
      String namespace = context.getStringAttribute("namespace");
    //获取这个mapper的命名空间
      builderAssistant.setCurrentNamespace(namespace);
    //设置cache对象
      cacheElement(context.evalNode("cache"));
    //解析sql
      buildStatementFromContext(context.evalNodes("select|insert|update|delete"));
  } 

//默认使用LRU
builderAssistant.useNewCache(typeClass, evictionClass, flushInterval, size, readWrite, blocking, props);
    
```

#### 第三阶段

```
private void buildStatementFromContext(List<XNode> list, String requiredDatabaseId) {
  for (XNode context : list) {
    final XMLStatementBuilder statementParser = new XMLStatementBuilder(configuration, builderAssistant, context, requiredDatabaseId);
      statementParser.parseStatementNode();
  }
}
```

parseStatementNode() 参数太多给出关键代码

```java
builderAssistant.addMappedStatement(id, sqlSource, statementType, sqlCommandType,
    fetchSize, timeout, parameterMap, parameterTypeClass, resultMap, resultTypeClass,
    resultSetTypeEnum, flushCache, useCache, resultOrdered, 
    keyGenerator, keyProperty, keyColumn, databaseId, langDriver, resultSets);
进去	

  MappedStatement statement = statementBuilder.build();
    configuration.addMappedStatement(statement);
    return statement;
```

```java
MappedStatement.Builder statementBuilder = new MappedStatement.Builder(configuration, id, sqlSource, sqlCommandType)
    .resource(resource)
    .fetchSize(fetchSize)
    .timeout(timeout)
    .statementType(statementType)
    .keyGenerator(keyGenerator)
    .keyProperty(keyProperty)
    .keyColumn(keyColumn)
    .databaseId(databaseId)
    .lang(lang)
    .resultOrdered(resultOrdered)
    .resultSets(resultSets)
    .resultMaps(getStatementResultMaps(resultMap, resultType, id))
    .resultSetType(resultSetType)
    .flushCacheRequired(valueOrDefault(flushCache, !isSelect))
    .useCache(valueOrDefault(useCache, isSelect))
    //设置cache对象
    .cache(currentCache);
```

## 二级缓存使用

```
在CachingExecutor维护了一个
 private final TransactionalCacheManager tcm = new TransactionalCacheManager();
tcm中又维护了一个
// 通过HashMap对象维护二级缓存对应的TransactionalCache实例
  private final Map<Cache, TransactionalCache> transactionalCaches = new HashMap<Cache, TransactionalCache>();

```

  由于sql的执行操作都委托给Executor，包括BaseExucutor，SimpleExecutor，BatchExecutor，当开启了二级缓存，MyBatis使用的就是CachingExecutor。

#### CachingExecutor

CachingExecutor使用了装饰着模式，在上面几种Executor的基础上增加了二级缓存的能力。

Executor delegate;这就是封装的基本的Executor。那么这个CachingExecutor是怎么来的呢?

秘密就藏在Configuration对象的创建Executor对象的方法里,我们发现，如果检测到配置文件的开启缓存功能

```java
public Executor newExecutor(Transaction transaction, ExecutorType executorType) {
  executorType = executorType == null ? defaultExecutorType : executorType;
  executorType = executorType == null ? ExecutorType.SIMPLE : executorType;
  Executor executor;
  // 根据executor类型创建对象的Executor对象
  if (ExecutorType.BATCH == executorType) {
    executor = new BatchExecutor(this, transaction);
  } else if (ExecutorType.REUSE == executorType) {
    executor = new ReuseExecutor(this, transaction);
  } else {
    executor = new SimpleExecutor(this, transaction);
  }
  // 如果cacheEnabled属性为ture，这使用CachingExecutor对上面创建的Executor进行装饰
  //cacheEnabled = false;
  if (cacheEnabled) {
    executor = new CachingExecutor(executor);
  }
  // 执行拦截器链的拦截逻辑
  executor = (Executor) interceptorChain.pluginAll(executor);
  return executor;
}
```

transactionalCaches这就是二级缓存，管理所有的二级缓存对象。里面的

```java
// 通过HashMap对象维护二级缓存对应的TransactionalCache实例
private final Map<Cache, TransactionalCache> transactionalCaches = new HashMap<Cache, TransactionalCache>();
```

Map保存了Cache和用`TransactionalCache`包装后的Cache的映射关系

key代表mappedStatement创建的cache对象是SynchoriezCache，以命名空间为id，

在`getObject`方法中，会把获取值的职责一路传递，最终到`PerpetualCache`。如果没有查到，会把key加入Miss集合，这个主要是为了统计命中率。

### 执行过程

```java
@Override
public <E> List<E> query(MappedStatement ms, Object parameterObject, RowBounds rowBounds, ResultHandler resultHandler, CacheKey key, BoundSql boundSql)
    throws SQLException {
  // 获取MappedStatement对象中维护的二级缓存对象
  Cache cache = ms.getCache();
  if (cache != null) {
    // 判断是否需要刷新二级缓存
    flushCacheIfRequired(ms);
    if (ms.isUseCache() && resultHandler == null) {
      ensureNoOutParams(ms, boundSql);
      // 从MappedStatement对象对应的二级缓存中获取数据
      @SuppressWarnings("unchecked")
      List<E> list = (List<E>) tcm.getObject(cache, key);
      if (list == null) {
        // 如果缓存数据不存在，则从一级缓存里面查找数据库中查询数据
        list = delegate.<E> query(ms, parameterObject, rowBounds, resultHandler, key, boundSql);
        // 將数据存放到MappedStatement对象对应的二级缓存中
        tcm.putObject(cache, key, list); // issue #578 and #116
      }
      return list;
    }
  }
  return delegate.<E> query(ms, parameterObject, rowBounds, resultHandler, key, boundSql);
}
```

首先获取二级缓存对象，然后获取二级缓存，如果不存在则执行普通executor对象的query方法