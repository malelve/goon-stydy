# Nginx

了解Nginx之前，我们先了解下什么是负载均衡？



## 1.负载均衡

### 1.1什么是负载均衡？

我们直接看下面这张图：

![img](https://pic3.zhimg.com/80/v2-6aa2607e04cc9d2f0d448f9fa80b2ae2_720w.webp)

从图片中我们不难看出，负载均衡是处于客户端和服务器之间，用户访问负载均衡器，再由负载均衡器将请求转发给后端服务器。



### 1.2负载均衡有哪些实现方式？

现在我们对负载均衡有了初步了解，那么既然负载均衡涉及到客户端和服务器之间的负载均衡器，我们有哪些方式实现呢？

目前有两种方案：**硬件实现**和**软件实现**



#### 1.2.1硬件方案

采用硬件的方式实现负载均衡，一般是**单独的负载均衡服务器**，价格昂贵，常用的有：**F5、A10、Citrix Netscaler**。

- **优点：**

  1、硬件负载均衡**稳定性更强**，**双机或集群的效果更佳**，**可以应对高并发、高吞吐的网络环境**中。

  2、在**策略配置方面**，可以**实现深度的健康检查方法**，而不是简单的ping或tcp的方式，而是可以针对业务层进行健康检查，整体的策略调度更灵活、**配置更方便，在七层负载方面更具优势**。

- **缺点：**

  1、**价格昂贵**；

  2、**扩展能力差，无法进行扩展和定制**；

  3、**调试和维护比较麻烦，需要专业人员**。

- **选择：**

  核心系统必须使用硬件负载均衡设备；

  测试系统和一般系统可以使用软件负载均衡设备。



#### 1.2.2软件方案

硬件负载均衡价格昂贵，在实际应用中远不如**软件负载均衡普遍**。常用的软件负载均衡软件有**Nginx、LVS、HaProxy、ats、perlbal、pound**等。

**Nginx**、**LVS**、**HAProxy**是目前使用最广泛的三种负载均衡软件。

- **对比：**

  LVS：是基于四层的转发（只能做端口转发，不能做基于URL、目录的转发）

  HAproxy：是基于四层和七层的转发，是专业的代理服务器

  Nginx：是WEB服务器，缓存服务器，又是反向代理服务器，可以做七层的转发

- **选择：**

  1、HAproxy和Nginx可做七层转发，URL和目录转发都可以；

  2、中小型企业推荐使用HAproxy（配置简单）；

  3、在很大并发量的时候选择LVS。

  

### 1.3负载均衡涉及哪些算法？

- 轮询
- 随机
- 加权轮询
- 加权随机
- 最小链接
- 源地址哈希法



## 2.Nginx负载均衡

有了上述负载均衡的了解，我们知道负载均衡其实就是一种调度策略，Nginx也就是一种软件方案实现的负载均衡，那么Nginx做负载均衡，有什么特点呢？Nginx又是如何实现负载均衡的呢？

要搞清楚上面的问题，我们就要先了解什么是代理，代理又分为哪些种类？

### 2.1代理是什么？

#### 2.1.1正向代理

正向代理（Forward Proxy）最大的特点是，**客户端非常明确要访问的服务器地址**，它代理客户端，**替客户端发出请求**。比如：棵学上网，俗称 FQ（警告⚠️：FQ 操作违反相关法律规定，本文只是为了解释正向代理向读者举个例子，仅供学习参考，切勿盲目 FQ）。(**也就是我们常说的梯子**)

![正向代理](C:\Users\pc\Desktop\知识累积\Nginx\正向代理.jpg)



#### 2.1.2反向代理

那么，随着请求量的爆发式增长，服务器觉得自己一个人始终是应付不过来，需要兄弟服务器们帮忙，于是它喊来了自己的兄弟以及代理服务器朋友。

此时，来自**不同客户端的所有请求实际上都发到了代理服务器**处，**再由代理服务器按照一定的规则将请求分发给各个服务器**。

这就是反向代理（Reverse Proxy），反向代理隐藏了服务器的信息，它代理的是服务器端，代其接收请求。换句话说，反向代理的过程中，**客户端并不知道具体是哪台服务器处理了自己的请求**。如此一来，**既提高了访问速度，又为安全性提供了保证**。

![反向代理](C:\Users\pc\Desktop\知识累积\Nginx\反向代理.jpg)

在这之中，反向代理需要考虑的问题是，**如何进行均衡分工**，**控制流量**，**避免出现局部节点负载过大**的问题。**通俗的讲，就是如何为每台服务器合理的分配请求，使其整体具有更高的工作效率和资源利用率。**



### 2.2Nginx是什么？

有了上述负载均衡的知识和代理的知识，我们现在可以思考，Nginx是什么？

**Nginx 作为一个基于 C 实现的高性能 Web 服务器，可以通过系列算法解决上述的负载均衡问题。并且由于它具有高并发、高可靠性、高扩展性、开源等特点，成为开发人员常用的反向代理工具。**

[Niginx](https://zhuanlan.zhihu.com/p/364588916):*Nginx* (engine x) 是一个**高性能**的[HTTP](https://link.zhihu.com/?target=https%3A//baike.baidu.com/item/HTTP)和[反向代理](https://link.zhihu.com/?target=https%3A//baike.baidu.com/item/%E5%8F%8D%E5%90%91%E4%BB%A3%E7%90%86/7793488)web服务器（其也可以做正向代理，但是运用很少）



### 2.3Nginx有哪些负载均衡算法？

大致上，还是上述的负载均衡算法



#### 2.3.1 轮询

轮询为负载均衡中较为基础也较为简单的算法，它不需要配置额外参数。假设配置文件中共有 **M** 台服务器，该算法遍历服务器节点列表，并按节点次序每轮选择一台服务器处理请求。当所有节点均被调用过一次后，该算法将从第一个节点开始重新一轮遍历。

**特点**：由于该算法中每个请求按时间顺序逐一分配到不同的服务器处理，因此**适用于服务器性能相近的集群情况，其中每个服务器承载相同的负载**。但对于**服务器性能不同的集群**而言，该算法**容易引发资源分配不合理等问题**。



#### 2.3.2 加权轮询

为了避免普通轮询带来的弊端，加权轮询应运而生。在加权轮询中，每个服务器会有各自的 `weight`。一般情况下，`weight` 的值越大意味着该服务器的性能越好，可以承载更多的请求。该算法中，客户端的请求按权值比例分配，当一个请求到达时，优先为其分配权值最大的服务器。

**特点**：加权轮询可以应用于服务器性能不等的集群中，使资源分配更加合理化。

加权轮询的**核心思想**是：遍历各服务器节点，计算节点权值，计算规则为`current_weight` 与其对应的 `effective_weight` 之和，每轮遍历中选出权值最大的节点作为最优服务器节点。其中`effective_weight`会在算法的执行过程中随资源情况和响应情况而改变。

这里的`current_weight`和`effective_weight` 借用一位大佬的理解如下：

> 每个结点就像一位勇者，他的**hp为current_weight**。每次轮询就像一次回合制战斗，会**选出当前hq最高的那位勇者前去攻击**。被选择的这位勇者成功击中怪物一次（被选择成为best最佳结点），自己就会被怪物反击，需从**hq中扣除一定的血量（即total**）。此时这位勇者的**hq太低，下次就不会被选取作战了**。但每位勇者**每回合都会回复一定hq（即effective_weight）**，也就是说effective_weight越高，其恢复能力越强，因而其没过多久hq又会变的最高，最后总的看下来，的确权值越高，该结点被选择的几率越大！



#### 2.3.3 IP Hash 

`ip_hash` 依据发出请求的**客户端 IP 的 hash 值来分配服务器**，该算法可以保证同 IP 发出的请求映射到同一服务器，或者具有相同 hash 值的不同 IP 映射到同一服务器。

**特点**：该算法在**一定程度上解决了集群部署环境下 Session 不共享的问题**。

> Session 不共享问题是说，假设用户已经登录过，此时发出的请求被分配到了 A 服务器，但 A 服务器突然宕机，用户的请求则会被转发到 B 服务器。但由于 Session 不共享，B 无法直接读取用户的登录信息来继续执行其他操作。

实际应用中，我们可以利用 `ip_hash`，将一部分 IP 下的请求转发到运行新版本服务的服务器，另一部分转发到旧版本服务器上，实现灰度发布。再者，如遇到文件过大导致请求超时的情况，也可以利用 `ip_hash` 进行文件的分片上传，它可以保证同客户端发出的文件切片转发到同一服务器，利于其接收切片以及后续的文件合并操作。



#### 2.3.4 URL Hash

`url_hash` 是根据请求的 URL 的 hash 值来分配服务器。该算法的特点是，相同 URL 的请求会分配给固定的服务器，**当存在缓存的时候，效率一般较高**。然而 Nginx 默认不支持这种负载均衡算法，需要依赖第三方库。



#### 2.3.5 Fair

按后端服务器的响应时间来分配请求，响应时间短的优先分配



#### 2.3.6 最小连接数	

假设共有 ![M](https://juejin.cn/equation?tex=M) 台服务器，当有新的请求出现时，遍历服务器节点列表并选取其中连接数最小的一台服务器来响应当前请求。连接数可以理解为当前处理的请求数。





## 3.Nginx实操

OK，上述理论讲了那么多，我们现在来实际实现下Nginx负载均衡。

以SpringBoot项目为例，采用Nginx加权轮询算法，实现如下：



第一步，准备好两个web项目，分别设置端口号为8080、8081



第二步，下载docker，拉去nginx镜像



第三步，将docker容器中的niginx的`\etc\nginx\nginx.conf`和`\etc\nginx\conf.d\default.conf`拷贝到本地



第四步：本地创建nginx文件夹，内部创建logs、html、conf三个文件，nginx.conf放在nginx目录下，default.conf文件放在conf目录下。



第五步：新建nginx容器，并通过`-v`命令挂载本地nginx.conf和default.nginx文件，替换容器中文件，shell命令如下：

以下命令，将本机目录挂载到docker容器中，并赋予权限

```shell
docker run --name Nginx -p 80:80 -d -v D://Nginx/nginx/nginx.conf:/etc/nginx/nginx.conf -v D://Nginx/nginx/conf/default.conf:/etc/nginx/conf.d/default.conf -v D://Nginx/nginx/logs:/var/log/nginx/ --privileged=true nginx
```

- -v：挂载目录
- --privileged：设置权限

注意：**所启动的镜像，一定要写在最后**！！！！

```
/etc/nginx/nginx.conf/  # niginx.conf目录
/etc/nginx/conf.d/default.conf/  # default.conf目录 
/usr/share/nginx/html/  # 静态页面目录
/var/log/nginx/  # 日志目录

-v # 挂载目录
--privileged = true  # 开放容器权限
```





第六步：修改本地的nginx.conf和default.conf文件，可同步到docker容器内，本次配置如下：

```conf
upstream testServer {
        server 10.130.146.83:8080 weight=10;
        server 10.130.146.83:8081 weight=2;
}

server {
    listen        80;
    server_name  10.130.146.83;
    location / {	
        root   /html;
        index  index.html index.htm;
        proxy_pass http://testServer; 
    }
}

```

```conf
http{
	upstream testServer {
            server localhost:8080 weight=10;
            server localhost:8081 weight=2;
    }
    server {
        location / {
            root   html;
            index  index.html index.htm;
            proxy_pass http://testServer; 
        }
    }
}
```

- `upstream`：负载均衡
- `weight`：权重
- `proxy_pass`代理

所以，我们这里采用的是加权轮询算法，默认是轮询算法。



### 一个小bug

在本次实操中，配置在nginx.conf中的端口不生效，因为，在nginx.conf文件最下方有这么一句话：`include/etc/nginx/conf.d/*.conf;`

所以我们在default目录下配置端口，经过测试，生效

这里挖个坑，以后来填。



# 参考链接

Nginx负载均衡：https://juejin.cn/post/6844904106541203464

Nginx正向代理与反向代理：https://zhuanlan.zhihu.com/p/366343222

Nginx加权轮询源码：https://github.com/nginx/nginx/blob/master/src/http/ngx_http_upstream_round_robin.c

Nginx的加权轮询算法：https://blog.csdn.net/BlacksunAcheron/article/details/84439302

Nginx服务器docker部署：https://juejin.cn/post/7029348407609131015

Nginx.conf文件的结构及配置方式：https://www.runoob.com/w3cnote/nginx-setup-intro.html

更多配置信息:https://www.bilibili.com/video/BV1934y1p7WG/?spm_id_from=333.337.search-card.all.click&vd_source=cb5cce1bdc5ab99fce812684e0c31a2b

docker部署nginx踩坑：https://blog.csdn.net/weixin_41474364/article/details/121427352