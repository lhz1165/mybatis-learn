# Mybatis的缓存（一级和二级）

解析mapper分三步

1. 用XMLconfigBuilder来解析(parse()方法)mybatis的主配置文件里面的<configure>标签
2. 用XMLMapperBuilder来解析（parse()方法）<configure>标签里面的<mapper>标签
3. 用XMLStatementBuilder来解析mapper里的<select|insert|..>（statementParser.parseStatementNode()方法）

## 二级缓存

#### 第一阶段

开启二级缓存需要在configure配置文件里面主动设置

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

