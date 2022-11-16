# SpringSecurity登陆校验



## Spring Security登录

引入spring security后，登录流程主要是通过spring security提供的15个过滤器链实现等

登录流程为：

客户端发送登录请求，包含用户名、密码

服务端对登录用户名、密码进行校验，如果正确，则生成一个jwt，并把jwt返回客户端

客户端之后的其他请求都携带这个token

服务端对请求头中的token进行解析，获取token中的userID，并根据用户id获取用户的相关信息，如果有权限则允许访问相关资源，并把访问的目标资源相应给前端

客户端接收响应



所以我们更改spring security的登录流程，主要是需要更改两点：

1、重写userdetailservice接口，实现我们从数据库中查询用户信息（security默认是从内存中查询）

2、自定义登录接口，实现登录功能，替换掉默认的UserPasswordAuthentication过滤器，调用之后的ProviderManager过滤器，继续执行security流程。

3、自定义一个JWT生成的过滤器，在登录校验成功后，需要依靠用户id，生成一个jwt，返回给前端，同时还需要在redis里，存储当前登录的用户信息。同时我们需要存入SecurityContextHolder中，以便随时随地获取。



### 实现过程

一、**实现UserDetailsService接口**，重写LoadUserByUsername方法（该方法由security过滤器链默认调用，作用和名字相同），我们还需**自定义实现UserDetails类**，作为该方法的返回，用于接收用户信息，包括用户名、密码、权限、是否过期、等等。

二、实现密码加密，默认是使用PasswordEncoder，其在数据库中存储的格式为{id}password，会通过id来判断密码的加密方式（例如我们存储明文密码时，需要在数据库中密码增添{noop}），所以我们自定义一个加密方式，例如自定义一个SecurityConfig类，**来继承WebSecurityConfigurerAdapter类**，并增添@Configuration和@Bean注解注入spring容器，在内部实现PasswordEncoder方法

三、**自定义登录接口**，实现调用security的AuthenticationManager方法中的认证方法，来对用户进行认证，所以我们也要在之前的SecurityConfig中**将AuthenticaitonManager注入容器**（增添@Bean注解），并从写，同时也要**从写configure方法，关闭csrf，并将登录接口权限开放**。接口实现过程如下：调用UserPasswordEncoderAuthenticationManagerToken方法传入id和password获取一个authenticationToken对象，再调用Authentication方法，获取登录的authentication对象，从authentication对象中getPrincipal方法，获取返回的UserDetails对象，之后便可获取ID并利用JWT方法生成JWT，同时将序列化的LoginUser对象存入redis中，最后返回前端jwt.



## Spring Security校验

更改校验流程，需要做到：

首先，我们每次请求需要解析token，并通过token中的userid来获取完整的用户权限信息

其次，我们需要在登录时，就生成token，并返回给前端，才能使后续每次请求，前端都携带这个token。



### 实现过程

一、自定义JWT过滤器，来对每个请求头中的token进行解析，取出userid来去redis中查找，并反序列化出LoginUser对象，然后封装Authentication对象，存入SecurityContextHolder中。

二、把自定义的过滤器，加入到Security的过滤器链中

```
 @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                //关闭csrf
                .csrf().disable()
                //不通过Session获取SecurityContext
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeRequests()
                // 对于登录接口 允许匿名访问
                .antMatchers("/user/login").anonymous()
                // 除上面外的所有请求全部需要鉴权认证
                .anyRequest().authenticated();

        //把token校验过滤器添加到过滤器链中
        http.addFilterBefore(jwtAuthenticationTokenFilter, UsernamePasswordAuthenticationFilter.class);
    }

```



至此，Security的登录校验，全部完成，

权限相关，只需要在方法上增添@PreAuthorize注解，标识权限，然后再LoginUser内新增权限属性和方法，最后再登录的UserDetailsService中，给返回的LoginUser对象加入权限即可（可从数据库中查找权限进行写入）

因为在SpringSecurity中，会使用默认的FilterSecurityInterceptor来进行权限校验。在FilterSecurityInterceptor中会从SecurityContextHolder获取其中的Authentication，然后获取其中的权限信息。当前用户是否拥有访问当前资源所需的权限。

​	所以我们在项目中只需要把当前登录用户的权限信息也存入Authentication。