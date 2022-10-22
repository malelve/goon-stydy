# Mybatis

原名ibatis(在包名中可以看到仍有不少包名称还是ibatis)，是一个持久层中间件，作用于**事务关系映射**（即对象/类与数据表中的关系记录的映射），极大的简化了数据访问的复杂度，类似的中间件还要jpa等。

## Mybatis的结构

sqlsessionFactoryBuilder ----> sqlsessionFactory ----> sqlsession

## Mybatis配置方法

以maven框架为例（springboot框架直接在application.yml/application.properities中配置，springboot极大的简化了配置）

在resources目录下，新建xml文件，内容如下（主要为数据库连接配置、对象的xml文件位置配置）

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE configuration
  PUBLIC "-//mybatis.org//DTD Config 3.0//EN"
  "http://mybatis.org/dtd/mybatis-3-config.dtd">
<configuration>
  <environments default="development">
    <environment id="development">
      <transactionManager type="JDBC"/>
      <dataSource type="POOLED">
        <property name="driver" value="${driver}"/>
        <property name="url" value="${url}"/>
        <property name="username" value="${username}"/>
        <property name="password" value="${password}"/>
      </dataSource>
    </environment>
  </environments>
  <mappers>
    <mapper resource="org/mybatis/example/BlogMapper.xml"/>
  </mappers>
</configuration>
```



## 原生方法调用

利用sqlsessionFactoryBuilder ----> sqlsessionFactory ----> sqlsession，来获取一个数据库连接对象sqlsession，再通过实体类对应的xml文件依照标签编写的insert、update、delete、select方法（Sql语句），来执行并提交。

```java
String resource = "org/mybatis/example/mybatis-config.xml";
InputStream inputStream = Resources.getResourceAsStream(resource);
SqlSessionFactoryBuilder sqlSessionFactoryBuilder = new SqlSessionFactoryBuilder();
SqlSessionFactory sqlSessionFactory = sqlSessionFactoryBuilder.build(inputStream);
SqlSession sqlsession = new SqlSession();
sqlsession = sqlSessionFactory.openSession();
// sqlsession.方法名();
sqlSession.close();
```





## 接口方法调用

利用Mapper接口，编写接口方法，使用xml来实现方法，以id代表方法名称、resulttype/resultmap代表返回类型等，编写SQL语句来实现方法

```xml
<!--xml文档实现接口-->

```

```java
// java定义接口

```



## 更简单的注解+继承方法调用

编写实体对应的Mapper接口，继承BaseMapper类，加上@Mapper注解，同时编写对应的xml文件（用于实现多表、连表查询），在service层接口继承Iservice<XXMapper>, serviceImpl继承IserviceImpl<XXMapper,XX><!--XX代表实体类名称-->

同时也要编写配置文件，来连接数据库和一些基础配置（如逻辑删除、最大字段、最小字段等）



## 延迟执行

由mybatis根据上下文，自动选择是否执行查询语句，需要先单独实现子查询，然后在父查询的resultmap中采用方法的形式加入子查询





## 动态拼接

由mybatis的mapper中定义标签，查询条件是否需要全部出现，部分未出现也可以执行

常用标签有：https://blog.csdn.net/qq_41617744/article/details/80199357

- when：会自动的给Sql语句添加where关键字，并将第一个and去除

```xml
<select id="selw" resultType="log">
select * from log
<where>
<if test="param1!=null and param1!=''">
and outno=#{param1}
</if>
<if test="param2!=null and param2!=''">
and inno=#{param2}
</if>
</where>
</select>
```

- if：只要成立就拼接在Sql语句中，都成立就全部都拼接（注意：where子句中加上1=1来规避and的风险）

```xml
<select id="selg" resultType="log">
select * from log where 1=1
<if test="param1!=null and param1!=''">
and outno=#{param1}
</if>
<if test="param2!=null and param2!=''">
and inno=#{param2}
</if>
</select>
```

- choose when otherwise：条件只要有一个成立，其他的就不会再判断了。如果没有成立的条件则默认执行otherwise中的内容
- set：产生一个set关键字，自动去除最后一个逗号（注意:在判断条件中最后保持有一个永远成立的条件。避免sql错误。）
- trim标签:
      prefix：在trim的内容前添加指定的内容
      prefixOverrides在trim的内容前去除指定的内容
      suffix：在trim的内容后添加指定的内容
      suffixOverrides:在trim的内容后去除指定的内容
      注意：
      先去除后添加
      添加内容会默认添加一个空格
- bind标签：
     name:参数名
     value:表达式，注意字符串拼接按照变量方式进行拼接
     例如:
      \<bind name="money" value="'$'+money"/> 
   给参数重新赋值
- sql和include标签:
     sql标签：在外部声明公用SQL语句
     id
     include标签：引入声明的公共SQL语句 
     refid：
     优点：便于SQL的整体修改
     缺点：难于阅读
- foreach标签:
     collection:要遍历的集合对象
     item：记录每次遍历的结果
     open：在结果的左边添加内容
     separator：结果和结果之间的内容
     close：在最后添加的内容





## 分页查询

物理分页：相当于执行了limit分页语句，返回部分数据。物理分页只返回部分数据占用内存小，能够获取数据库最新的状态，实施性比较强，一般适用于数据量比较大，数据更新比较频繁的场景。

逻辑分页：一次性把全部的数据取出来，通过程序进行筛选数据。如果数据量大的情况下会消耗大量的内存，由于逻辑分页只需要读取数据库一次，不能获取数据库最新状态，实施性比较差，适用于数据量小，数据稳定的场合。


### RowBounds方法

```java
//接口方法
public List<Honor> getHonorList(HashMap<String, Object> maps，RowBounds rowBounds);

//调用方法
RowBounds rowBounds = new RowBounds(offset, page.getPageSize()); // offset起始行 // limit是当前页显示多少条数据

RowBounds  rowBounds = new RowBounds(2, 2);
    List<Honor> honors = studentMapper.getHonorList(maps，rowBounds);
```

### [pageHelper方法](https://www.cnblogs.com/lukelook/p/11099382.html)

依赖

```xml
<!-- https://mvnrepository.com/artifact/com.github.pagehelper/pagehelper -->
        <!--mybatis pagehelper 分页  -->
        <dependency>
            <groupId>com.github.pagehelper</groupId>
            <artifactId>pagehelper</artifactId>
            <version>5.1.8</version>
        </dependency>
```

拦截器（这个是配置在mybatis-config.xml文件中）：

```xml
<plugins>
        <plugin interceptor="com.github.pagehelper.PageInterceptor">
            <!-- config params as the following -->
            <!--<!–分页参数合理化  –>-->
            <property name="reasonable" value="true"/>
        </plugin>
    </plugins>
```

调用

```java
//第一种，RowBounds方式的调用
List<Country> list = sqlSession.selectList("x.y.selectIf", null, new RowBounds(0, 10));

//第二种，Mapper接口方式的调用，推荐这种使用方式。
PageHelper.startPage(1, 10);
List<Country> list = countryMapper.selectIf(1);

//第三种，Mapper接口方式的调用，推荐这种使用方式。
PageHelper.offsetPage(1, 10);
List<Country> list = countryMapper.selectIf(1);

//第四种，参数方法调用
//存在以下 Mapper 接口方法，你不需要在 xml 处理后两个参数
public interface CountryMapper {
    List<Country> selectByPageNumSize(
            @Param("user") User user,
            @Param("pageNum") int pageNum, 
            @Param("pageSize") int pageSize);
}
//配置supportMethodsArguments=true
//在代码中直接调用：
List<Country> list = countryMapper.selectByPageNumSize(user, 1, 10);

//第五种，参数对象
//如果 pageNum 和 pageSize 存在于 User 对象中，只要参数有值，也会被分页
//有如下 User 对象
public class User {
    //其他fields
    //下面两个参数名和 params 配置的名字一致
    private Integer pageNum;
    private Integer pageSize;
}
//存在以下 Mapper 接口方法，你不需要在 xml 处理后两个参数
public interface CountryMapper {
    List<Country> selectByPageNumSize(User user);
}
//当 user 中的 pageNum!= null && pageSize!= null 时，会自动分页
List<Country> list = countryMapper.selectByPageNumSize(user);

//第六种，ISelect 接口方式
//jdk6,7用法，创建接口
Page<Country> page = PageHelper.startPage(1, 10).doSelectPage(new ISelect() {
    @Override
    public void doSelect() {
        countryMapper.selectGroupBy();
    }
});
//jdk8 lambda用法
Page<Country> page = PageHelper.startPage(1, 10).doSelectPage(()-> countryMapper.selectGroupBy());

//也可以直接返回PageInfo，注意doSelectPageInfo方法和doSelectPage
pageInfo = PageHelper.startPage(1, 10).doSelectPageInfo(new ISelect() {
    @Override
    public void doSelect() {
        countryMapper.selectGroupBy();
    }
});
//对应的lambda用法
pageInfo = PageHelper.startPage(1, 10).doSelectPageInfo(() -> countryMapper.selectGroupBy());

//count查询，返回一个查询语句的count数
long total = PageHelper.count(new ISelect() {
    @Override
    public void doSelect() {
        countryMapper.selectLike(country);
    }
});
//lambda
total = PageHelper.count(()->countryMapper.selectLike(country));
```

