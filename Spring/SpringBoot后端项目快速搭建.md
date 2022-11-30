# 一、项目模板创建

## Maven模板

空白模板，仅包含maven工程格式

![maven模板格式](\IMG\maven模板格式.png)

## Spring Initializr模板

可在项目创建时选择一些基础架包，如Spring-boot-web、lombok等

同时该模板，自动创建启动项（src/main/java目录下的项目名+application.java文件）、配置文件（resources目录下的application.properities）等



------



# 二、配置依赖

## pom.xml配置

POM( Project Object Model，项目对象模型 ) 是 Maven 工程的基本工作单元，是一个XML文件，包含了项目的基本信息，用于描述项目如何构建，声明项目依赖，等等。

执行任务或目标时，Maven 会在当前目录中查找 POM。它读取 POM，获取所需的配置信息，然后执行目标。

以下罗列一些基础常用：

```xml
<parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>2.7.3</version>
    </parent>
```

<parent></parent>标签内是依赖的父依赖，常放于<dependencies></dependencies>标签上方，其版本可直接作用于下方<dependency></dependency>标签中的子依赖

```xml
<dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
	
    	<!-- lombok，常用于实体，如@Data注解执get\set方法等 -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
    	<!-- spring boot test -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    	<!-- mybatis plus常用于mapper\数据增、删、改、查等各种操作 -->
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-boot-starter</artifactId>
            <version>3.3.2</version>
        </dependency>
    </dependencies>
```

除了上述依赖外，还有很多其他的依赖，如mysql数据库、json等等，根据需要可随时添加（注：导入依赖后不起效，可通过clean maven等操作进行重新加载）

![maven操作](IMG\maven操作.png)

## application.yml/properties配置

application.yml文件与application.properties作用相同，同时可复制相互转换（深入了解后补充）,以下是基本的application.yml配置（深入学习后扩展补充）

```yaml
# 端口号
server:
  port: 8080
```

## 编写启动项

通过Spring Initializr创建的模板项自带启动项，一般放在src/main/java目录下，Maven创建的模板项目直接在同位置新建java文件，内容如下：

```java
package com.example.test_book;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
# 项目中扫描不到mapper可通过以下注解传入mapper包路径
//@MapperScan("com.example.test_book.mapper")
public class TestBookApplication {
    public static void main(String[] args) {
        SpringApplication.run(TestBookApplication.class, args);
    }

}

```

注：

@SpringBootApplication 标注的类为 Spring Boot 的主配置类，Spring Boot 会运行这个类的 main 方法来启动 Spring Boot 应用。

@SpringBootApplication 注解等价于以同时使  用 @SpringBootConfiguration,@EnableAutoConfiguration 和@ComponentScan 其中	@SpringBootConfiguration 跟进去发现，其就等价于@Configuration，一个是在 Spring Boot 的名称，一起是在 Spring 中的名称。

​		@Configuration 本质上也就是一个@Component，也是一个组件而已。

​	@EnableAutoConfiguration: 打开 Spring Boot 的自动配置机制  

​	@ComponentScan: 允许程序自动扫描包，扫描当前包及其子包下标注了@Component，@Controller，@Service，@Repository 类并纳入到 spring [容器](https://cloud.tencent.com/product/tke?from=10680)中进行管理。

## 尝试第一次启动项目

运行启动项，打开浏览器，输入：localhost:8080(默认8080，同时在application.yml文件配置过)



![第一次启动](IMG\第一次启动.png)

当看到以上内容，则项目顺利启动，因无任何请求返回，所以看到的是错误页



------



# 三、连接数据库、建立数据表

## pom.xml和application.yml增添数据库配置

在application增添如下配置，本次演示采用h2数据库，mysql数据库同样格式，需要填写对应的用户名、密码、url和驱动

```yaml
spring:
  # h2数据库，mysql同样
  datasource:
    username: test
    password: 123456
    url: jdbc:h2:~/test
    driver-class-name: org.h2.Driver
  # 启用h2控制台
  h2:
    console:
      enabled: true

```

在pom.xml的<dependencies></dependencies>标签中添加数据库依赖如下（MySql同理）：

```xml
	<!-- h2数据库 -->
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>runtime</scope>
        </dependency>
	<!-- mysql数据库 -->
<dependency>
            <groupId>mysql</groupId>
            <artifactId>mysql-connector-java</artifactId>
            <version>8.0.29</version>
        </dependency>
```

## 编写sql文件

以下操作为项目启动自动建表操作，也可手动在数据库建立好表格省去此步骤

首先在src/main/resources目录下建立一个包用以存储sql文件（名字不做要求），包内建立sql文件如下：

```sql
create table if not exists book(
    id integer auto_increment not null primary key ,
    create_at time null,
    update_at time null,
    title varchar(255) null,
    summary text null,
    author varchar(255) null
    );
```

在application.yml文件内，添加配置，说明sql文件地址（如本次的地址为src/main/resouces/sql/init.sql写作classpath:sql/init.sql）

```yaml
spring: 
# 启动项目自动建表的sql文件地址
  sql:
    init:
      mode: always
      schema-locations: classpath:sql/init.sql
```

## 启动项目，查看数据库

h2数据库的查看方式为：

打开浏览器，输入localhost:8080/h2-console（我们之前在application.yml文件夹中打开了h2控制台）

连接查看，已建立数据表

![h2数据库](IMG\h2数据库.png)

可通过select语句查看表属性

mysql查看方式：

navicat查看，或通过idea右侧数据库连接工具（database，需要连接）查看

注：

建立失败多数原因是sql文件写错，请仔细检查，特别是分号“；”



------



# 四、项目框架搭建

### entity

src/main/java/com.example.test_book下建立包entity（命名不做要求）注：com.example.test_book为项目包，Spring Initializr自带，maven自建

在entity内建立Book.java，内容如下：

```java
package com.example.test_book.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("book")
public class Book{
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    @TableField(value = "create_at", fill = FieldFill.INSERT)
    private LocalDateTime createAt;
    @TableField(value = "update_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateAt;
    private String title;
    private String summary;
    private String author;
}
```

注：

@Data注解为get、set方法

@NoArgsConstructor：无参构造

@AllArgsConstructor：全参构造

@TableName("book")： 映射数据库中表名

@TableId(value = "id", type = IdType.AUTO)：映射主键，类型自增

 @TableField(value = "create_at", fill = FieldFill.INSERT)：映射表的其他属性，设置为插入时写入数据

 @TableField(value = "update_at", fill = FieldFill.INSERT_UPDATE)：映射表的其他属性，设置为插入和更新时写入数据

### mapper

src/main/java/com.example.test_book下建立包mapper（命名不做要求）注：com.example.test_book为项目包，Spring Initializr自带，maven自建

在mapper包内建立BookMapper.java，如下：

```java
package com.example.test_book.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.test_book.entity.Book;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Component;

@Mapper
@Component
public interface BookMapper extends BaseMapper<Book> {
}

```

### service

src/main/java/com.example.test_book下建立包service（命名不做要求）注：com.example.test_book为项目包，Spring Initializr自带，maven自建

在service包内建立BookService.java，如下：

```java
package com.example.test_book.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.test_book.entity.Book;
import com.example.test_book.vo.AddInfo;

import java.util.List;

public interface BookService extends IService<Book> {
    // 新增书籍方法
    Book addBook(AddInfo req);
}
```

### service.Impl

src/main/java/com.example.test_book/service下建立包Impl（命名不做要求）注：com.example.test_book为项目包，Spring Initializr自带，maven自建

在service.Impl包内建立BookServiceImp.java，如下：

```java
package com.example.test_book.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.test_book.entity.Book;
import com.example.test_book.mapper.BookMapper;
import com.example.test_book.service.BookService;
import com.example.test_book.vo.AddInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BookServiceImp extends ServiceImpl<BookMapper, Book> implements BookService {

    @Autowired
    private BookMapper bookMapper;

    // 新增书籍的具体实现方法
    @Override
    public Book addBook(AddInfo req) {
        Book book = new Book();
        book.setAuthor(req.author);
        book.setSummary(req.summary);
        book.setTitle(req.title);
        bookMapper.insert(book);
        return bookMapper.selectById(book.getId());
    }
}

```

注:

@Autowired：它可以对类成员变量、方法及构造函数进行标注，完成自动装配的工作。 通过 @Autowired的使用来消除 set ，get方法。

### controller

src/main/java/com.example.test_book下建立包controller（命名不做要求）注：com.example.test_book为项目包，Spring Initializr自带，maven自建

在service包内建立BookController.java，如下：

```java
package com.example.test_book.controller;

import com.example.test_book.service.BookService;
import com.example.test_book.vo.AddInfo;
import com.example.test_book.vo.ResponseInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/books")
public class BookController {
    @Autowired
    private BookService bookService;
	
    // 调用BookService接口的方法，其中ResponseInfo为封装的返回格式
    @PostMapping("")
    public ResponseInfo addBook(@RequestBody AddInfo req){
        return ResponseInfo.success(bookService.addBook(req));
    }
}

```

注：

@RestController = @Controller + @ResponseBody组成，等号右边两位同志简单介绍两句，就明白我们@RestController的意义了：

​	@Controller 将当前修饰的类注入SpringBoot IOC容器，使得从该类所在的项目跑起来的过程中，这个类就被实例化。当然也有语义化的作用，即代表该类是充当Controller的作用

​	@ResponseBody 它的作用简短截说就是指该类中所有的API接口返回的数据，甭管你对应的方法返回Map或是其他Object，它会以Json字符串的形式返回给客户端，本人尝试了一下，如果返回的是String类型，则仍然是String。

@RequestMapping("/books")：映射http请求地址

@PostMapping("")：post请求地址（这里为空，其他地址也可以写为“/abcd”等等）

@RequestBody：请求体（body）

### 工具类

#### 统一返回类型封装

一个通用的简单的封装类，可自行扩展到code等复杂返回结构

```java
package com.example.test_book.vo;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ResponseInfo<T> {
    private int code;
    private String msg;
    private T data;

    public static <T> ResponseInfo<T> success(T data){
        return new ResponseInfo<>(data);
    }

    public static <T> ResponseInfo<T> success(String msg){
        return new ResponseInfo<>(msg);
    }

    public static <T> ResponseInfo<T> faild(int code ,String msg){
        return new ResponseInfo<>(code, msg);
    }

    private ResponseInfo(int code, String msg){
        this.code = code;
        this.msg = msg;
    }

    private ResponseInfo(String msg){
        this.msg = msg;
    }

    public ResponseInfo(T data){
        this.code = 200;
        this.msg = "成功";
        this.data = data;
    }
}

```

#### 请求类型封装

为接口的请求体类型，同时也存在响应体类型（本次采用实体类型直接返回，未封装响应体，主要用于封装响应结果）：

```java
package com.example.test_book.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddInfo {
    public String title;
    public String author;
    public String summary;
}

```

#### 自动填充时间、用户等

还记得我们上面数据库实体类，给一些属性增添的fill类型吗

```java
package com.example.test_book.utils;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        // 可继续扩展，字符串为实体类中我们指定的新增时插入的属性
        setFieldValByName("createAt", now, metaObject);
        setFieldValByName("updateAt", now, metaObject);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        // 可继续扩展，字符串为实体类中我们指定的更新时插入的属性
        setFieldValByName("updateAt", now, metaObject);
    }
}

```

### 启动项目，访问接口

方法一：点击BookController中接口上方注解内的工具，生成http文件，后输入如下内容

![测试接口](IMG\测试接口.png)

```http
POST http://localhost:8080/books
Content-Type: application/json

{
  "title" : "接口一",
  "author" : "接口一作者",
  "summary" : "接口一总结"
}
```

方法二：打开postman，输入localhost:8080/books，并输入以下请求体

```http
{
  "title" : "接口一",
  "author" : "接口一作者",
  "summary" : "接口一总结"
}
```

注：post请求因需要携带请求体，所以不可通过浏览器直接请求，需要借助工具，不携带请求体的http请求如get可通过浏览器测试

查看接口返回：

```json
POST http://localhost:8080/books

HTTP/1.1 200 
Content-Type: application/json
Transfer-Encoding: chunked
Date: Wed, 21 Sep 2022 08:29:19 GMT
Keep-Alive: timeout=60
Connection: keep-alive

{
  "code": 200,
  "msg": "成功",
  "data": {
    "id": 2,
    "createAt": "2022-09-21T16:29:19",
    "updateAt": "2022-09-21T16:29:19",
    "title": "接口一",
    "summary": "接口一总结",
    "author": "接口一作者"
  }
}

```

# 其他

get、put、delete方法和post类似，可自行学习，本次教程结束