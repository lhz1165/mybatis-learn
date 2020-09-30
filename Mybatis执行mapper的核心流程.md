[<<<Parent](README.md)
# Mybatis执行mapper的核心流程

包含两个过程，

<u>**1.一个是解析mapper.xml成为mappedStatement对象，**</u>

<u>**2.另一个是创建代理，执行mapper接口的方法时，调用mapper的代理对象的方法**</u>

首先来看下代码

```java
// 获取配置文件输入流
InputStream inputStream = Resources.getResourceAsStream("mybatis-config.xml");
// 通过SqlSessionFactoryBuilder的build()方法创建SqlSessionFactory实例
SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
// 调用openSession()方法创建SqlSession实例
SqlSession sqlSession = sqlSessionFactory.openSession();
// 获取UserMapper代理对象
UserMapper userMapper = sqlSession.getMapper(UserMapper.class);
// 执行Mapper方法，获取执行结果
List<UserEntity> userList = userMapper.listAllUser();
```

解析mapper.xml是在SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream); 里面的

**SqlSessionFactory**的**build**方法

```
public SqlSessionFactory build(InputStream inputStream, String environment, Properties properties) {
    XMLConfigBuilder parser = new XMLConfigBuilder(inputStream, environment, properties);
    //在build方法里创建Configuration对象，通过解析流来创建
    Configuration configuration = parser.parse();
    return build(configuration);
```

往下进去 **XMLConfigBuilder** 的 **parseConfiguration**()发现从Mybatis配置文件中来解析mappers标签

```
//这是mapper.xml的<mapper标签的解析方法
//同时解析了mapper接口会生成proxyFactory
mapperElement(root.evalNode("mappers"));
```

这是一个比较关键的代码，for循环遍历所有的xml，一个一个解析，主要包括mapper对象mappedStatement，以及mapper的代理

```java
private void mapperElement(XNode parent) throws Exception {
  if (parent != null) {
    //获取到mybatis-config.xml的mapper配置文件的位置   <mapper resource="com/blog4java/mybatis/example/mapper/UserMapper.xml"/>
    for (XNode child : parent.getChildren()) {
        //这是child <mapper resource="com/blog4java/mybatis/example/mapper/UserMapper.xml"/>
        //xml的全路径名 com/blog4java/mybatis/example/mapper/UserMapper.xml
        String resource = child.getStringAttribute("resource");
        //解析mapper为null
        String url = child.getStringAttribute("url");
        //null
        String mapperClass = child.getStringAttribute("class");
        // 通过resource属性指定XML文件路径
        if (resource != null && url == null && mapperClass == null) {
          ErrorContext.instance().resource(resource);
          InputStream inputStream = Resources.getResourceAsStream(resource);
          XMLMapperBuilder mapperParser = new XMLMapperBuilder(inputStream, configuration, resource, configuration.getSqlFragments());
          //解析mapper的关键
          mapperParser.parse();
        } 
      }
    }
  }
}
```



进入会发现

```java
//这一步mapperStatements被填充上了
mapperParser.parse();
```

最终走到**XMLMapperBuilder**的**parse()**方法

核心代码是

**1.configurationElement()对应着mapper.xml成为mappedStatement对象**(namespase---->mapperStatement)映射

 **2.bindMapperForNamespace()对应着创建（class-------->MapperProxyFactory）映射，用来创建调用mapper方法的代理对象**

```java
public void parse() {
    // 调用XPathParser的evalNode（）方法获取根节点对应的XNode对象
    //这些node节点包括mapper.xml里面的关键节点包括select,insert等
    //这一步也设置缓存
    configurationElement(parser.evalNode("/mapper"));
    // 將资源路径添加到Configuration对象中
    configuration.addLoadedResource(resource);
    //获取产生mapper.java文件代理对象的 MapperProxyFactory
    bindMapperForNamespace();

}
```

解析之后我们的configuration对象的

```java
//key为 <select id="xx" xx，MappedStatement为mapper里面的sql
protected final Map<String, MappedStatement> mappedStatements
```

以及

```java
protected final MapperRegistry mapperRegistry里面的

```

里面的

```java
// 用于注册Mapper接口Class对象，和MapperProxyFactory对象对应关系
private final Map<Class<?>, MapperProxyFactory<?>> knownMappers = new HashMap<Class<?>, MapperProxyFactory<?>>();
```

初具规模，我们就可以使用

UserMapper userMapper = sqlSession.getMapper(UserMapper.class);来获取knownMappers里面的代理对象

```java
public <T> T getMapper(Class<T> type, SqlSession sqlSession) {
  //这里代理对象以及创建好了
  final MapperProxyFactory<T> mapperProxyFactory = (MapperProxyFactory<T>) knownMappers.get(type);
    return mapperProxyFactory.newInstance(sqlSession);
}
```

List<UserEntity> userList = userMapper.listAllUser();来执行代理对象的的select方法，而执行这个查询方法又需要MappedStatement的协助,因为这里面封装了sql语句以及参数等信息

```java
@Override
public <E> List<E> selectList(String statement, Object parameter, RowBounds rowBounds) {
  try {
    // 根据Mapper的Id，获取对应的MappedStatement对象
    MappedStatement ms = configuration.getMappedStatement(statement);
    // 以MappedStatement对象作为参数，调用Executor的query（）方法
    return executor.query(ms, wrapCollection(parameter), rowBounds, Executor.NO_RESULT_HANDLER);
  } catch (Exception e) {
    throw ExceptionFactory.wrapException("Error querying database.  Cause: " + e, e);
  } finally {
    ErrorContext.instance().reset();
  }

}
```

有了这两个我们就查询出结果了 ，具体细节的东西以后讨论
