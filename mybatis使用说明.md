# Mybatis

原名ibatis(在包名中可以看到仍有不少包名称还是ibatis)，是一个持久层中间件，作用于**事务关系映射**（即对象/类与数据表中的关系记录的映射），极大的简化了数据访问的复杂度，类似的中间件还要jpa等。

## Mybatis的结构

sqlsessionFactoryBuilder ----> sqlsessionFactory ----> sqlsession

## Mybatis配置方法

以maven框架为例（springboot框架直接在application.yml/application.properities中配置，springboot极大的简化了配置）

在resources目录下，新建xml文件，内容如下（主要为数据库连接配置、对象的xml文件位置配置）

```xml

```



## 原生方法调用

利用sqlsessionFactoryBuilder ----> sqlsessionFactory ----> sqlsession，来获取一个数据库连接对象sqlsession，再通过实体类对应的xml文件依照标签编写的insert、update、delete、select方法（Sql语句），来执行并提交。

```xml

```



## 

## 接口方法调用

利用Mapper接口，编写接口方法，使用xml来实现方法，以id代表方法名称、resulttype/resultmap代表返回类型等，编写SQL语句来实现方法

```xml

```



## 更简单的注解+继承方法调用

编写实体对应的Mapper接口，继承BaseMapper类，加上@Mapper注解，同时编写对应的xml文件（用于实现多表、连表查询），在service层接口继承Iservice<XXMapper>, serviceImpl继承IserviceImpl<XXMapper,XX><!--XX代表实体类名称-->

同时也要编写配置文件，来连接数据库和一些基础配置（如逻辑删除、最大字段、最小字段等）