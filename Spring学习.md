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

###### IoC xml配置方式

- 编写xml文档

###### IoC xml注入方式

- 通过set方法属性注入
- 通过构造方法注入
- 属性注入和构造方法注入混合使用



**问1**，为什么有了set方法、构造方法等注入来表示依赖关系，为什么还需要在xml文档？

**答1**，实现真正的调用bean，是需要调用set方法才能实现真正的属性注入、xml中利用反射，也或者不是反射，完成了真正调用这个set方法，完成了属性的注入。



**问2**，延迟加载/懒加载不好吗，为什么Spring要在容器初始化时就加载全部Bean

**答2**，Bean错了反正都会报错，不如直接在一开始就报错



**问3**，多个重复id的Bean会报错吗？

**答3**，分两个方面来答，

首先如果是在同一个xml文件中声明了两个重复id的Bean，是肯定会报错的，而且错误发生在Spring容器初始话读取xml配置的时候，

其次如果在不同xml文件里声明了id相同的Bean，那么在容器初始化的时候不会报错，Spring会覆盖掉id相同的Bean；

但在Spring3.0版本，增添了@Configuration + @Bean注解，用于注册第三方Bean，此时如果有两个重复的Bean，那么在容器初始化的时候不会报错，在使用的时候也不会报错，会默认使用第一个Bean，后续id相同的Bean也不会去注册但我们可以使用@Bean("name")或@Qualifier()注解来给Bean取名使其id不同。



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



#### 1.1.5 使用配置类来配置

之前我们使用xml文件配置，在方法中编写set方法/构造方法来注入

现在我们使用@Configuration来编写配置类来配置啦！！！

@Configuration + @Bean方法完全可以取代xml文档的方式。

```Java
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



#### 1.1.6 注入资源

Spring不仅可以注入Bean、也可以注入资源（Resource）

Spring提供了一个`org.springframework.core.io.Resource`（注意不是`javax.annotation.Resource`），它可以像`String`、`int`一样使用`@Value`注入：

通过@Value()注解，注入文件如下：

```java
@Component
public class AppService {
    @Value("classpath:/logo.txt")
    private Resource resource;

    private String logo;

    @PostConstruct
    public void init() throws IOException {
        try (var reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            this.logo = reader.lines().collect(Collectors.joining("\n"));
        }
    }
}
```

注入`Resource`最常用的方式是通过classpath，即类似`classpath:/logo.txt`表示在classpath中搜索`logo.txt`文件，然后，我们直接调用`Resource.getInputStream()`就可以获取到输入流，避免了自己搜索文件的代码。

也可以直接指定文件的路径，例如：

```java
@Value("file:/path/to/logo.txt")
private Resource resource;
```



#### 1.1.7 注入配置

@Value()不仅可以注入资源，也可以注入我们在配置文件中写的资源，如在application.yml，application.properities中添加的配置，一般这些配置都是采用key-value的形式。

如何使用？

@PropertySource("配置文件名") // classpath下

@Value("配置文件里的内容如key")

例子如下：

```java
@Configuration
@ComponentScan
@PropertySource("app.properties") // 表示读取classpath的app.properties
public class AppConfig {
    @Value("${app.zone:Z}")
    String zoneId;

    @Bean
    ZoneId createZoneId() {
        return ZoneId.of(zoneId);
    }
}
```

Spring容器看到`@PropertySource("app.properties")`注解后，自动读取这个配置文件，然后，我们使用`@Value`正常注入：

```java
@Value("${app.zone:Z}")
String zoneId;
```

注意注入的字符串语法，它的格式如下：

- `"${app.zone}"`表示读取key为`app.zone`的value，如果key不存在，启动将报错；
- `"${app.zone:Z}"`表示读取key为`app.zone`的value，但如果key不存在，就使用默认值`Z`。

这样一来，我们就可以根据`app.zone`的配置来创建`ZoneId`。

还可以把注入的注解写到方法参数中：

```java
@Bean
ZoneId createZoneId(@Value("${app.zone:Z}") String zoneId) {
    return ZoneId.of(zoneId);
}
```

可见，先使用`@PropertySource`读取配置文件，然后通过`@Value`以`${key:defaultValue}`的形式注入，可以极大地简化读取配置的麻烦。

另一种注入配置的方式是先通过一个简单的JavaBean持有所有的配置，例如，一个`SmtpConfig`：

```
@Component
public class SmtpConfig {
    @Value("${smtp.host}")
    private String host;

    @Value("${smtp.port:25}")
    private int port;

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }
}
```

然后，在需要读取的地方，使用`#{smtpConfig.host}`注入：

```
@Component
public class MailService {
    @Value("#{smtpConfig.host}")
    private String smtpHost;

    @Value("#{smtpConfig.port}")
    private int smtpPort;
}
```

注意观察`#{}`这种注入语法，它和`${key}`不同的是，`#{}`表示从JavaBean读取属性。`"#{smtpConfig.host}"`的意思是，从名称为`smtpConfig`的Bean读取`host`属性，即调用`getHost()`方法。一个Class名为`SmtpConfig`的Bean，它在Spring容器中的默认名称就是`smtpConfig`，除非用`@Qualifier`指定了名称。

使用一个独立的JavaBean持有所有属性，然后在其他Bean中以`#{bean.property}`注入的好处是，多个Bean都可以引用同一个Bean的某个属性。例如，如果`SmtpConfig`决定从数据库中读取相关配置项，那么`MailService`注入的`@Value("#{smtpConfig.host}")`仍然可以不修改正常运行。

##### 1.1.7.1 注入资源+配置总结

- @Value() + @PropertySource()可注入资源和配置
- 注入方式有两种：
  - 通过直接读取配置文件，如application.yml中的key-value来注入，需要用@PropertySource()声明配置文件，再用@Value()注入，注意@Value()里是$符号，表示从配置文件中读取
  - 通过读取JavaBean来注入，如加了@Component的JavaBean，然后再使用的地方直接@Value()注入，注意@Value()里面是#符号，并且需要JavaBean的名称，表示从JavaBean里读取



#### 1.1.8 条件装配

简单点说，就是决定Bean何时种环境装配，何种环境不装配。

主要使用的注解：

- @Profile() ：决定Bean在什么环境下被装配
- @Conditional ：决定是否创建某个Bean
- @ConditionOnProperty()，@ConditionOnClass()等等·····



**@Profile()**

开发应用程序时，我们会使用开发环境，例如，使用内存数据库以便快速启动。而运行在生产环境时，我们会使用生产环境，例如，使用MySQL数据库。如果应用程序可以根据自身的环境做一些适配，无疑会更加灵活。

Spring为应用程序准备了Profile这一概念，用来表示不同的环境。例如，我们分别定义开发、测试和生产这3个环境：

- native
- test
- production

创建某个Bean时，Spring容器可以根据注解`@Profile`来决定是否创建。例如，以下配置：

```Java
@Configuration
@ComponentScan
public class AppConfig {
    @Bean
    @Profile("!test")
    ZoneId createZoneId() {
        return ZoneId.systemDefault();
    }

    @Bean
    @Profile("test")
    ZoneId createZoneIdForTest() {
        return ZoneId.of("America/New_York");
    }
}
```

如果当前的Profile设置为`test`，则Spring容器会调用`createZoneIdForTest()`创建`ZoneId`，否则，调用`createZoneId()`创建`ZoneId`。注意到`@Profile("!test")`表示非test环境。

**重点：**在运行程序时，加上JVM参数`-Dspring.profiles.active=test`就可以指定以`test`环境启动。



**@Conditional**

除了根据`@Profile`条件来决定是否创建某个Bean外，Spring还可以根据`@Conditional`决定是否创建某个Bean。

例如，我们对`SmtpMailService`添加如下注解：

```java
@Component
@Conditional(OnSmtpEnvCondition.class)
public class SmtpMailService implements MailService {
    ...
}
```

它的意思是，如果满足`OnSmtpEnvCondition`的条件，才会创建`SmtpMailService`这个Bean。`OnSmtpEnvCondition`的条件是什么呢？我们看一下代码：

```java
public class OnSmtpEnvCondition implements Condition {
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        return "true".equalsIgnoreCase(System.getenv("smtp"));
    }
}
```

因此，`OnSmtpEnvCondition`的条件是存在环境变量`smtp`，值为`true`。这样，我们就可以通过环境变量来控制是否创建`SmtpMailService`。

Spring只提供了`@Conditional`注解，具体判断逻辑还需要我们自己实现。Spring Boot提供了更多使用起来更简单的条件注解，例如，如果配置文件中存在`app.smtp=true`，则创建`MailService`：

```java
@Component
@ConditionalOnProperty(name="app.smtp", havingValue="true")
public class MailService {
    ...
}
```

如果当前classpath中存在类`javax.mail.Transport`，则创建`MailService`：

```java
@Component
@ConditionalOnClass(name = "javax.mail.Transport")
public class MailService {
    ...
}
```



### 1.2 AOP

如何理解AOP？

AOP就是一个面向切面编程，可用于处理事务、打印日志等。

在AOP编程中，会遇到的一些概念如下：

- Aspect：切面，即一个横跨多个核心逻辑的功能，或者称之为系统关注点；
- Joinpoint：连接点，即定义在应用程序流程的何处插入切面的执行；
- Pointcut：切入点，即一组连接点的集合；
- Advice：增强，指特定连接点上执行的动作；
- Introduction：引介，指为一个已有的Java对象动态地增加新的接口；
- Weaving：织入，指将切面整合到程序的执行流程中；
- Interceptor：拦截器，是一种实现增强的方式；
- Target Object：目标对象，即真正执行业务的核心逻辑对象；
- AOP Proxy：AOP代理，是客户端持有的增强后的对象引用。

**所以我们可以理解为AOP就是一个[代理模式](https://www.runoob.com/design-pattern/proxy-pattern.html)或一种拦截器！**



#### 1.2.1 装配AOP

要使用AOP，我们首先就是要通过Maven引入Spring对AOP的支持，如下：

```xml
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-aspects</artifactId>
    <version>${spring.version}</version>
</dependency>
```

上述依赖会自动引入AspectJ，使用AspectJ实现AOP比较方便，因为它的定义比较简单。

使用起来非常简单，只需要以下几个步骤：

- 在实现AOP的类上添加`@Aspect`和`@Component`注解
- 在具体AOP类的方法上添加范围，如`@Around()` `@Before()` `@After()` `@AfterReturning()` `@AfterThrowing()`，这也是拦截器的类型
- **开启AOP代理模式**：`@EnableAspectJAutoProxy`
- 实现具体的AOP方法，通常利用反射或者输出日志。

一个例子如下：

我们定义一个`LoggingAspect`：

```java
@Aspect
@Component
public class LoggingAspect {
    // 在执行UserService的每个方法前执行:
    @Before("execution(public * com.itranswarp.learnjava.service.UserService.*(..))")
    public void doAccessCheck() {
        System.err.println("[Before] do access check...");
    }

    // 在执行MailService的每个方法前后执行:
    @Around("execution(public * com.itranswarp.learnjava.service.MailService.*(..))")
    public Object doLogging(ProceedingJoinPoint pjp) throws Throwable {
        System.err.println("[Around] start " + pjp.getSignature());
        Object retVal = pjp.proceed();
        System.err.println("[Around] done " + pjp.getSignature());
        return retVal;
    }
}
```

观察`doAccessCheck()`方法，我们定义了一个`@Before`注解，后面的字符串是告诉AspectJ应该在何处执行该方法，这里写的意思是：执行`UserService`的每个`public`方法前执行`doAccessCheck()`代码。

再观察`doLogging()`方法，我们定义了一个`@Around`注解，它和`@Before`不同，`@Around`可以决定是否执行目标方法，因此，我们在`doLogging()`内部先打印日志，再调用方法，最后打印日志后返回结果。

在`LoggingAspect`类的声明处，除了用`@Component`表示它本身也是一个Bean外，我们再加上`@Aspect`注解，表示它的`@Before`标注的方法需要注入到`UserService`的每个`public`方法执行前，`@Around`标注的方法需要注入到`MailService`的每个`public`方法执行前后。

紧接着，我们需要给`@Configuration`类加上一个`@EnableAspectJAutoProxy`注解：

```java
@Configuration
@ComponentScan
@EnableAspectJAutoProxy
public class AppConfig {
    ...
}
```

Spring的IoC容器看到这个注解，就会自动查找带有`@Aspect`的Bean，然后根据每个方法的`@Before`、`@Around`等注解把AOP注入到特定的Bean中。执行代码，我们可以看到以下输出：

```txt
[Before] do access check...
[Around] start void com.itranswarp.learnjava.service.MailService.sendRegistrationMail(User)
Welcome, test!
[Around] done void com.itranswarp.learnjava.service.MailService.sendRegistrationMail(User)
[Before] do access check...
[Around] start void com.itranswarp.learnjava.service.MailService.sendLoginMail(User)
Hi, Bob! You are logged in at 2020-02-14T23:13:52.167996+08:00[Asia/Shanghai]
[Around] done void com.itranswarp.learnjava.service.MailService.sendLoginMail(User)
```

这说明执行业务逻辑前后，确实执行了我们定义的Aspect（即`LoggingAspect`的方法）。



#### 1.2.2 AOP原理

既然我们说**AOP是一个代理模式**，那么它的原理就非常简单了

AOP实际上是创建了一个**继承于目标方法的子类**（**代理对象**），我们一般称为XXProxy（XX为目标类），然后在该子类中**持有了原始实例的引用**。

基于以上面的例子举例，这个代理对象，如下：

```java
public UserServiceAopProxy extends UserService {
    private UserService target;
    private LoggingAspect aspect;

    public UserServiceAopProxy(UserService target, LoggingAspect aspect) {
        this.target = target;
        this.aspect = aspect;
    }

    public User login(String email, String password) {
        // 先执行Aspect的代码:
        aspect.doAccessCheck();
        // 再执行UserService的逻辑:
        return target.login(email, password);
    }

    public User register(String email, String password, String name) {
        aspect.doAccessCheck();
        return target.register(email, password, name);
    }

    ...
}
```



#### 1.2.3 拦截器类型

顾名思义，拦截器有以下类型：

- @Before：这种拦截器先执行拦截代码，再执行目标代码。如果拦截器抛异常，那么目标代码就不执行了；
- @After：这种拦截器先执行目标代码，再执行拦截器代码。无论目标代码是否抛异常，拦截器代码都会执行；
- @AfterReturning：和@After不同的是，只有当目标代码正常返回时，才执行拦截器代码；
- @AfterThrowing：和@After不同的是，只有当目标代码抛出了异常时，才执行拦截器代码；
- @Around：能完全控制目标代码是否执行，并可以在执行前后、抛异常后执行任意拦截代码，可以说是包含了上面所有功能。



#### 1.2.4 结合注解装配AOP

在上面的例子中，我们采用`@Before(execution(* xxx.Xyz.*(..)))`的方法来装配AOP，但是这种方法写起来比较复杂，且不精确，以下我们将通过注解的的方式来装配AOP。

以下以一个[廖大的例子](https://www.liaoxuefeng.com/wiki/1252599548343744/1310052317134882)来做演示：

为了监控应用程序的性能，我们定义一个性能监控的注解：

```java
@Target(METHOD)
@Retention(RUNTIME)
public @interface MetricTime {
    String value();
}
```

在需要被监控的关键方法上标注该注解：

```java
@Component
public class UserService {
    // 监控register()方法性能:
    @MetricTime("register")
    public User register(String email, String password, String name) {
        ...
    }
    ...
}
```

然后，我们定义`MetricAspect`：

```java
@Aspect
@Component
public class MetricAspect {
    @Around("@annotation(metricTime)")
    public Object metric(ProceedingJoinPoint joinPoint, MetricTime metricTime) throws Throwable {
        String name = metricTime.value();
        long start = System.currentTimeMillis();
        try {
            return joinPoint.proceed();
        } finally {
            long t = System.currentTimeMillis() - start;
            // 写入日志或发送至JMX:
            System.err.println("[Metrics] " + name + ": " + t + "ms");
        }
    }
}
```

注意`metric()`方法标注了`@Around("@annotation(metricTime)")`，它的意思是，符合条件的目标方法是带有`@MetricTime`注解的方法，因为`metric()`方法参数类型是`MetricTime`（**注意**参数名是`metricTime`不是`MetricTime`），我们通过它获取性能监控的名称。

有了`@MetricTime`注解，再配合`MetricAspect`，任何Bean，只要方法标注了`@MetricTime`注解，就可以自动实现性能监控。运行代码，输出结果如下：

```txt
Welcome, Bob!
[Metrics] register: 16ms
```



#### 1.2.5 [AOP避坑](https://www.liaoxuefeng.com/wiki/1252599548343744/1339039378571298)

第一个坑：（主要看链接，廖大讲的很细）

因为AOP是代理模式，持有的是原来实例的引用，由于AOP使用**CGLIB来自动构建原对象的代理对象，是直接生成字节码文件**，而原对象的创建是Java编译器自动加上了super方法继承了父对象的成员变量，要经历**源码-编译-字节码**的过程，所以代理对象的生成没有继承原对象的成员变量。所以我们启动AOP时，注入的是代理对象，那么而我们就没办法直接通过代理对象获取成员变量了，会报错，要使用get方法来获取。

因此，正确使用AOP，我们需要一个避坑指南：

1. 访问被注入的Bean时，总是调用方法而非直接访问字段；
2. 编写Bean时，如果可能会被代理，就不要编写`public final`方法。



由于Spring通过CGLIB实现代理类，我们要避免直接访问Bean的字段（通过方法来访问如get），以及由`final`方法带来的“未代理”问题。

遇到CglibAopProxy的相关日志，务必要仔细检查，防止因为AOP出现NPE异常。



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