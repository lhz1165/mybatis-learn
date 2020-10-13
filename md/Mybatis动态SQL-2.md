[<<<动态sql](../Mybatis的动态sql.md)
# Mybatis动态SQL-2

## SqlSource和BoundSql

SqlSource接口就一个方法， getBoundSql(Object parameterObject)，真正的sql就放在BoundSq之中

实现类介绍



**DynamicSqlSource**：xml生成的sql资源信息，是动态sql

**ProviderSqlSource**：@select等注解生成的sql资源信息

**RawSqlSource**：xml解析生成的sql资源信息，但是他不是动态sql

**StaticSqlSource**：用来描述上面三者解析后的sql信息，总之，无论那种方式最终都会是用StaticSqlSource来描述sql资源信息.

```java
public class StaticSqlSource implements SqlSource {
  // Mapper解析后的sql内容
  private final String sql;
  // 参数映射信息
  private final List<ParameterMapping> parameterMappings;
  private final Configuration configuration;

  public StaticSqlSource(Configuration configuration, String sql) {
    this(configuration, sql, null);
  }

  public StaticSqlSource(Configuration configuration, String sql, List<ParameterMapping> parameterMappings) {
    this.sql = sql;
    this.parameterMappings = parameterMappings;
    this.configuration = configuration;
  }

  @Override
  public BoundSql getBoundSql(Object parameterObject) {
    return new BoundSql(configuration, sql, parameterMappings, parameterObject);
  }

}
```



显而易见，这个类描述的sql信息，他封装了Mapper解析后的sql内容与参数映射信息，该有的都有了，所以他就是最总描述sql信息的类。

## BoundSql

但是光有了参数映射信息，那参数信息在哪呢，这个秘密就放在BoundSql之中,看看他的数据结构，就明了了，

例如：

**sql** : select * from user where id = ? and phone =?;

**parameterMappings**:   [property=id  typehanler=IntegerTypeHandler   javaType=int jdbctype="numberic"],[property=phonetypehanler=StringTypeHandler   javaType=stringjdbctype="varchar"]

**parameterObject**: User =[id =1 phone ="123456"]

```java
public class BoundSql {

  // Mapper配置解析后的sql语句
  private final String sql;
  // Mapper参数映射信息
  private final List<ParameterMapping> parameterMappings;
  // Mapper接口传入参数对象
  private final Object parameterObject;
  // 额外参数信息，包括<bind>标签绑定的参数，内置参数
  private final Map<String, Object> additionalParameters;
  // 参数对象对应的MetaObject对象
  private final MetaObject metaParameters;
  ....
 }
```

那么我们呢的SqlSource是怎么来的呢？ 这就需要LanguageDriver

## LanguageDriver（mapper配置------>SqlSource）

```java
public interface LanguageDriver {

  ParameterHandler createParameterHandler(MappedStatement mappedStatement, Object parameterObject, BoundSql boundSql);


  SqlSource createSqlSource(Configuration configuration, XNode script, Class<?> parameterType);


  SqlSource createSqlSource(Configuration configuration, String script, Class<?> parameterType);

}
```

实现类**XMLLanguageDriver**提供了解析标签（etc,<if>)来完成动态sql的目的。

仔细观察两个重载方法的参数有一个地方不同String script和 XNode script 这分别代表@select的动态sql标签和xml里面动态标签的意思。



## SqlNode

sqlNode就是sql的节点，是动态 sql的基石，就代表trim if  foreach....等节点的对象。

根据动态的参数信息，生成静态的sql

apply一般来说就是拼接sql，把各种node构建成一个完整的sql

**补充**:${}就是通过TextSqlNode的apply方法把${}处替换为对应值的sql

```java
public interface SqlNode {
  boolean apply(DynamicContext context);
}
```



一个mapper一般来说有多个sqlNode，他们一起构成了sql语句,这个最终的对象就是MixedSqlNode。

```java
public class MixedSqlNode implements SqlNode {
  private final List<SqlNode> contents;

  public MixedSqlNode(List<SqlNode> contents) {
    this.contents = contents;
  }

  @Override
  public boolean apply(DynamicContext context) {
    for (SqlNode sqlNode : contents) {
      sqlNode.apply(context);
    }
    return true;
  }
}
```

