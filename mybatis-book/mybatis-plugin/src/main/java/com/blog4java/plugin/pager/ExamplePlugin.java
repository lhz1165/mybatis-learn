/**
 *    Copyright 2009-2016 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.blog4java.plugin.pager;

import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.ResultHandler;

import java.sql.Statement;
import java.util.Properties;

@Intercepts({
        @Signature(type = StatementHandler.class, method = "query", args = {Statement.class, ResultHandler.class})
})
public class ExamplePlugin implements Interceptor {
  private Properties properties;
  @Override
  public Object intercept(Invocation invocation) throws Throwable {
    // TODO:自定义拦截逻辑
    System.out.println("我的拦截器拦截执行");
    return invocation.proceed();
  }

  @Override
  public Object plugin(Object target) {
    // 调用Plugin类的wrap（）方法返回一个动态代理对象
    return Plugin.wrap(target, this);
  }

  @Override
  public void setProperties(Properties properties) {
    // 设置插件的属性信息
    this.properties = properties;
  }

  public Properties getProperties() {
    return properties;
  }

}
