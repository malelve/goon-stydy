# Spring

Spring是一个支持快速开发java EE（java Enterprise Edition）应用程序的框架。它提供了一些列**底层容器**和**基础设施**，并可以和大量常用的开源框架无缝集成，可以说是开发Java EE应用程序的必备。

在Spring Framework的基础上，又诞生了Spring Boot、Spring Cloud、Spring Data、Spring Security等一系列基于Spring Framework的项目。

以下逐一介绍Spring内容。



## 1 Spring Framework

Spring Framework主要包括几个模块：

- 支持IoC和AOP的容器；
- 支持JDBC和ORM的数据访问模块；
- 支持声明式事务的模块；
- 支持基于Servelt的MVC开发；
- 支持Reactive的Web开发；
- 以及集成JMS、JavaMail、JMX、缓存等其他模块。

### 1.1 [IoC](https://www.liaoxuefeng.com/wiki/1252599548343744/1282381977747489)

#### 1.1.1 IoC原理

又称IoC容器，Inversion of Control，直译为控制反转。

用于解决：

- 谁负责创建组件？
- 谁负责根据依赖关系组装组件？
- 销毁时，如何按依赖顺序正确销毁？

在Java中，所有组件都需要通过应用程序自己创建、配置和使用（典型的new+init）如：

```java
public class BookService {
    private HikariConfig config = new HikariConfig();
    private DataSource dataSource = new HikariDataSource(config);

    public Book getBook(long bookId) {
        try (Connection conn = dataSource.getConnection()) {
            ...
            return book;java
        }
    }
}
```



在IoC模式下，控制权发生了反转，即从应用程序转移到了IoC容器，**所有组件不再由应用程序自己创建和配置，而是由IoC容器负责**，这样，**应用程序只需要直接使用已经创建好并且配置好的组件**。

为了能让组件在IoC容器中被“装配”出来，需要某种**“注入”机制**，例如，`BookService`自己并不会创建`DataSource`，而是等待外部通过`setDataSource()`方法来注入一个`DataSource`：

```
public class BookService {
    private DataSource dataSource;

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }
}
```

不直接`new`一个`DataSource`，而是注入一个`DataSource`，这个小小的改动虽然简单，却带来了一系列好处：

1. `BookService`不再关心如何创建`DataSource`，因此，不必编写读取数据库配置之类的代码；
2. `DataSource`实例被注入到`BookService`，同样也可以注入到`UserService`，因此，共享一个组件非常简单；
3. 测试`BookService`更容易，因为注入的是`DataSource`，可以使用内存数据库，而不是真实的MySQL配置。

因此，IoC又称为**依赖注入**（DI：Dependency Injection），它解决了一个最主要的问题：将组件的创建+配置与组件的使用相分离，并且，由IoC容器负责管理组件的生命周期。

因为IoC容器要负责实例化所有的组件，因此，有必要告诉容器如何创建组件，以及各组件的依赖关系。**一种最简单的配置是通过XML文件来实现**，例如：

```xml
<beans>
    <bean id="dataSource" class="HikariDataSource" />
    <bean id="bookService" class="BookService">
        <property name="dataSource" ref="dataSource" />
    </bean>
    <bean id="userService" class="UserService">
        <property name="dataSource" ref="dataSource" />xml
    </bean>
</beans>
```

 `property = ` 是需要注入的对象的类中的属性，`ref = ` 是其依赖的对象的类（注意大小写）,通过`<property name="..." ref="..." />`注入了另一个Bean。

上述XML配置文件指示IoC容器创建3个JavaBean组件，并把id为`dataSource`的组件通过属性`dataSource`（即调用`setDataSource()`方法）注入到另外两个组件中。

在Spring的IoC容器中，我们把所有组件统称为JavaBean，即配置一个组件就是配置一个Bean。



**依赖注入方式**

我们从上面的代码可以看到，依赖注入可以通过`set()`方法实现。但依赖注入也可以通过构造方法实现。

很多Java类都具有带参数的构造方法，如果我们把`BookService`改造为通过构造方法注入，那么实现代码如下：

```
public class BookService {
    private DataSource dataSource;

    public BookService(DataSource dataSource) {
        this.dataSource = dataSource;
    }
}
```

Spring的IoC容器同时支持**属性注入**和**构造方法注入**，并**允许混合使用**。



**无侵入容器**

在设计上，Spring的IoC容器是一个高度可扩展的无侵入容器。所谓无侵入，是指应用程序的组件无需实现Spring的特定接口，或者说，组件根本不知道自己在Spring的容器中运行。这种无侵入的设计有以下好处：

1. 应用程序组件既可以在Spring的IoC容器中运行，也可以自己编写代码自行组装配置；
2. 测试的时候并不依赖Spring容器，可单独进行测试，大大提高了开发效率。

##### 1.1.1.1 原理总结

###### IoC配置方式

- 编写xml文档

###### IoC注入方式

- 通过set方法属性注入
- 通过构造方法注入
- 属性注入和构造方法注入混合使用



**问1**，为什么有了set方法、构造方法等注入来表示依赖关系，为什么还需要在xml文档？

**答1**，实现真正的调用bean，是需要调用set方法才能实现真正的属性注入、xml中利用反射，也或者不是反射，完成了真正调用这个set方法，完成了属性的注入。



**问2**，延迟加载/懒加载不好吗，为什么Spring要在容器初始化时就加载全部Bean

**答2**，Bean错了反正都会报错，不如直接在一开始就报错



#### 1.1.2 装配[Bean](https://www.liaoxuefeng.com/wiki/1252599548343744/1282382145519649)

在Spring的IoC容器中，我们把所有组件统称为JavaBean，即配置一个组件就是配置一个Bean。

装配Bean有两种方式

- set方法/构造方法 + xml文档
- configuration+bean、component、service、controller注解

调用方式：

- 一、获取一个ApplicationContext对象，传入**xml文档**或者**配置类**
- 二、调用ApplicationContext对象的getBean方法，可以传入id或者类型。



#### 1.1.3  使用注解装配Bean

作用：**简化Bean配置方式**

使用Spring的IoC容器，实际上就是通过类似XML这样的配置文件，把我们自己的Bean的依赖关系描述出来，然后让容器来创建并装配Bean。一旦容器初始化完毕，我们就直接从容器中获取Bean使用它们。

使用XML配置的优点是所有的Bean都能**一目了然**地列出来，并通过配置注入能直观地看到每个Bean的依赖。它的**缺点是写起来非常繁琐，每增加一个组件，就必须把新的Bean配置到XML中**。

通过以下几个注解来实现：

- `@Component`
- `@Configuration` + `@Bean`（等同于`@Component`）
- `@Autowired`
- `@Configuration`
- `@ComponentScan`

`@Component`注解就相当于定义了一个Bean，它有一个可选的名称，默认是`xxService`，即小写开头的类名。

`@Configuration`表示该类是配置类。

`@ConponentScan`自动搜索当前类所在的包以及子包，把所有标注为`@Component`的Bean自动创建出来，并根据`@Autowired`进行装配

`@Autowired`就相当于把指定类型的Bean注入到指定的字段中。和XML配置相比，`@Autowired`大幅简化了注入，因为它不但可以写在`set()`方法上，还可以直接写在字段上，甚至可以写在构造方法中：

```
@Component
public class UserService {
    MailService mailService;

    public UserService(@Autowired MailService mailService) {
        this.mailService = mailService;
    }
    ...
}
```

我们一般把`@Autowired`写在字段上，通常使用package权限的字段，便于测试。

最后也是**通过ApplicationContext来获取Bean**



#### 1.1.4 Bean的更多操作方式

- **@Configuration + @ Bean**：**创建第三方Bean**，注入未在package管理范围内的Bean
- @Autowired(**required = false**)：**可选注入**，找得到的时候就注入，找不到就不忽略（适合有定义就使用，没定义就使用默认值）
- Scpoe

对于Spring容器来说，当我们把一个Bean标记为`@Component`后，它就会自动为我们创建**一个单例（Singleton）**，即容器初始化时创建Bean，容器关闭前销毁Bean。在容器运行期间，我们调用`getBean(Class)`获取到的Bean总是同一个实例。

还有一种Bean，我们每次调用`getBean(Class)`，容器都返回一个**新的实例**，这种**Bean称为Prototype**（原型），它的生命周期显然和Singleton不同。声明一个Prototype的Bean时，需要添加一个额外的`@Scope`注解：

```java
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE) // @Scope("prototype")
public class MailSession {
    ...
}
```

- 注入List：当一个类/接口被多个Bean实现时，可以通过**List+泛型**的方式，将全部实现Bean**自动注入到List**中，还可以增添**@Order()注解来给Bean排序**。
- @PostConstruct 和 @PreDestroy

有些时候，一个Bean在注入必要的依赖后，需要进行初始化（监听消息等）。在容器关闭时，有时候还需要清理资源（关闭连接池等）。我们通常会定义一个`init()`方法进行初始化，定义一个`shutdown()`方法进行清理，然后，引入JSR-250定义的Annotation：

```xml
<dependency>
    <groupId>javax.annotation</groupId>
    <artifactId>javax.annotation-api</artifactId>
    <version>1.3.2</version>
</dependency>
```

在Bean的初始化和清理方法上标记`@PostConstruct`和`@PreDestroy`：

```java
@Component
public class MailService {
    @Autowired(required = false)
    ZoneId zoneId = ZoneId.systemDefault();

    @PostConstruct
    public void init() {
        System.out.println("Init mail service with zoneId = " + this.zoneId);
    }

    @PreDestroy
    public void shutdown() {
        System.out.println("Shutdown mail service");
    }
}
```

Spring容器会对上述Bean做如下初始化流程：

- 调用构造方法创建`MailService`实例；
- 根据`@Autowired`进行注入；
- 调用标记有`@PostConstruct`的`init()`方法进行初始化。

而销毁时，容器会首先调用标记有`@PreDestroy`的`shutdown()`方法。

**Spring只根据Annotation查找*无参数*方法，对方法名不作要求**。

- @Bean("name")指定别名
- @Bean+@Qualifier("name")指定别名

```Java
@Configuration
@ComponentScan
public class AppConfig {
    @Bean("z")
    ZoneId createZoneOfZ() {
        return ZoneId.of("Z");
    }

    @Bean
    @Qualifier("utc8")
    ZoneId createZoneOfUTC8() {
        return ZoneId.of("UTC+08:00");
    }
}
```

可以用`@Bean("name")`指定别名，也可以用`@Bean`+`@Qualifier("name")`指定别名。

存在多个同类型的Bean时，注入`ZoneId`又会报错：

```
NoUniqueBeanDefinitionException: No qualifying bean of type 'java.time.ZoneId' available: expected single matching bean but found 2
```

意思是期待找到唯一的`ZoneId`类型Bean，但是找到两。因此，注入时，要指定Bean的名称：

```Java
@Component
public class MailService {
	@Autowired(required = false)
	@Qualifier("z") // 指定注入名称为"z"的ZoneId
	ZoneId zoneId = ZoneId.systemDefault();
    ...
}
```

- @Qualifier：在配置Bean和使用Bean时，指定Bean名称
- @Primary： 指定主要Bean，即存在重复时，优先采用这个

```Java
@Configuration
@ComponentScan
public class AppConfig {
    @Bean
    @Primary // 指定为主要Bean
    @Qualifier("z")
    ZoneId createZoneOfZ() {
        return ZoneId.of("Z");
    }

    @Bean
    @Qualifier("utc8")
    ZoneId createZoneOfUTC8() {
        return ZoneId.of("UTC+08:00");
    }
}
```

这样，在注入时，如果没有指出Bean的名字，Spring会注入标记有`@Primary`的Bean。这种方式也很常用。例如，对于主从两个数据源，通常将主数据源定义为`@Primary`：

```Java
@Configuration
@ComponentScan
public class AppConfig {
    @Bean
    @Primary
    DataSource createMasterDataSource() {
        ...
    }

    @Bean
    @Qualifier("slave")
    DataSource createSlaveDataSource() {
        ...
    }
}
```

其他Bean默认注入的就是主数据源。如果要注入从数据源，那么只需要指定名称即可。

- 使用FactoryBean

  我们在设计模式的[工厂方法](https://www.liaoxuefeng.com/wiki/1252599548343744/1281319170474017)中讲到，很多时候，可以通过工厂模式创建对象。Spring也提供了工厂模式，允许定义一个工厂，然后由工厂创建真正的Bean。

  用工厂模式创建Bean需要实现`FactoryBean`接口。我们观察下面的代码：

  ```
  @Component
  public class ZoneIdFactoryBean implements FactoryBean<ZoneId> {
  
      String zone = "Z";
  
      @Override
      public ZoneId getObject() throws Exception {
          return ZoneId.of(zone);
      }
  
      @Override
      public Class<?> getObjectType() {
          return ZoneId.class;
      }
  }
  ```

  当一个Bean实现了`FactoryBean`接口后，Spring会先实例化这个工厂，然后调用`getObject()`创建真正的Bean。`getObjectType()`可以指定创建的Bean的类型，因为指定类型不一定与实际类型一致，可以是接口或抽象类。

  因此，如果定义了一个`FactoryBean`，要注意Spring创建的Bean实际上是这个`FactoryBean`的`getObject()`方法返回的Bean。为了和普通Bean区分，我们通常都以`XxxFactoryBean`命名。





### 1.2 AOP





### 1.3 容器和上下文

#### 1.3.1 容器

如何理解容器？

我们在java中最熟悉的容器莫过于Tomcat，它是一个运行于Servlet上的Web容器。。而Spring要想**实现依赖注入功能**，就离不开对象生产的容器。**容器是Spring框架实现功能的核心**，容器不只是帮我们创建了对象那么简单，它负责了对象整个的生命周期的管理——**创建、装配、销毁**。

这里我们可以理解Spring容器就是IoC容器



#### 1.3.2 上下文

Spring的应用上下文可以简单的理解成是将你需要Spring帮你管理的对象放入容器的容器对象，应用上下文即是Spring容器的一种抽象化表述；而我们常见的**ApplicationContext**本质上说就是一个维护Bean定义以及对象之间协作关系的高级接口。



#### 1.3.3 容器和上下文的联系

我们必须明确，**Spring的核心是容器**，而**容器并不唯一**，框架本身就提供了很多个容器的实现，大概分为**两种类型**：

- 一种是不常用的**BeanFactory**，这是最简单的容器，只能提供基本的DI功能；
- 还有一种就是**继承了BeanFactory后派生而来的应用上下文**，其抽象接口也就是我们上面提到的的**ApplicationContext**，它能提供更多企业级的服务，例如解析配置文本信息等等，这也是应用上下文实例对象最常见的应用场景。

有了上下文对象，我们就能向容器注册需要Spring管理的对象了。对于上下文抽象接口，Spring也为我们提供了多种类型的容器实现，供我们在不同的应用场景选择。

- `AnnotationConfigApplicationContext`:从基于一个或多个**java的配置类**中加载上下文，适用于java注解的方式；
- `ClassPathXmlApplicationContext`:从**类路径**下的一个或多个**xml配置文件**中加载上下文，适用于xml配置的方式；
- `FileSystemXmlApplicationContext`:从**文件系统**下的一个或多个**xml配置文件**中加载上下文，从系统盘符中加载xml配置文件；
- `AnnotationConfigWebApplicationContext`:专门为**web**应用准备的，适用于**注解方式**；
- `XmlWebApplicationContext`:从**web**应用下的一个或多个**xml配置文件**加载上下文，适用于xml配置方式。

无论需要IOC容器管理的对象是基于xml配置也好，java注解配置也罢，总之要将需要IOC容器管理的对象（bean）、bean之间的协作关系配置好，然后利用应用上下文对象加载进我们的Spring容器，容器就能为程序提供对象管理服务了。

##### 1.3.3.1 使用XML的方式

```xml
<bean id="man" class="com.xxx.xxx.Man">
        <constructor-arg ref="person" />
</bean>
<bean  id="person" class="com.xxx.xxx.Person"/>
```

然后通过应用上下文将配置加载到IOC容器，让Spring替我们管理对象，待我们需要使用对象的时候，再从容器中获取bean就ok。

```java
 //加载项目中的spring配置文件到容器
 //ApplicationContext context = new ClassPathXmlApplicationContext("resouces/applicationContext.xml");
 //加载系统盘中的配置文件到容器
 ApplicationContext context = new FileSystemXmlApplicationContext("C:/spring/applicationContext.xml");
 //从容器中获取对象实例
 Man man = context.getBean(Man.class);
 man.say();
```

以上测试中，配置文件**applicationContext.xml**分别放在项目中或任意的系统盘符下，只需要使用相应的上下文对象去加载配置文件。

##### 1.3.3.2 使用注解的方式

```java
@Configuration
public class ManConfig {
    @Bean
    public Man man() {
        return new Man(person());
    }
    @Bean
    public Person person() {
        return new Person();
    }
}

//从java注解的配置中加载配置到容器
ApplicationContext context = new AnnotationConfigApplicationContext(ManConfig.class);
//从容器中获取对象实例
Man man = context.getBean(Man.class);
man.say();
```



#### 1.3.4 容器和上下文总结

**容器作为spring的核心**，需要配合上下文使用，spring提供了**多种容器**，而这些**容器的抽象，也就是我们如何操作管理容器就是上下文**，如BeanFactory、ApplicationContext。

Spring也提供了多种向上下文注册的方式，如从**Xml中注册**，和基于**@Configuration和@Bean注解的注册方式**。使用时，我们只需要从ApplicationContext对象中获取对象即可（获取也可以通过类型和名称等多种方式），其使用是利用了**Java的反射**机制。