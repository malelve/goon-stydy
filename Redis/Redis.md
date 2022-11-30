# Redis进阶！！！！！

TMD，我倒要看看，对一个校招的本科生，Redis还能问出什么花！！！！

我一本科应届生，为什么要问这么细？？？动不动就百万并发项目该怎么做！！！！动不动就设计秒杀系统！！！！

我真的会谢！！！！



## 一、Redis可以用来做什么？

- 缓存
- 数据库
- 排行榜
- 商品列表
- 评论列表
- 消息队列等

### Redis的特点

Redis以**内存**作为数据存储介质，读写数据的效率极高。

Redis跟memcache不同的是，储存在Redis中的数据是**持久化**的，断电或重启，数据也不会丢失。

Redis的**存储分为内存存储、磁盘存储和log文件**。

Redis可以从磁盘重新将数据加载到内存中，也可以通过配置文件对其进行配置，因此，redis才能实现持久化。

Redis**支持主从模式**，可以**配置集群**，更利于支撑大型的项目。



## 二、Redis的两层数据结构

Redis给我们提供了5种可操控的数据结构，分别是string、hash、list、set、sorted set

但是，redis底层其实采用了两层数据结构，还有第二层数据结构来实现第一层数据结构，并依赖这些灵活的数据结构，来**提升读取和写入的性能**。



开始之前，我们先了解，**为什么Reids要采用第二层数据结构**？

- **存储效率**。Redis是专用于存储数据的，它对计算机资源的**主要消耗就在于内存，因此节省内存是它非常非常重要的一个方面**。这意味着Redis一定是非常精细地考虑了压缩数据、减少内存碎片等问题。
- **快速响应时间**。与快速响应时间相对的，是高吞吐量。Redis是用于提供在线访问的，对于单个请求的响应时间要求很高，因此，**快速响应时间是比高吞吐量更重要的目标**。
- **单线程**。Redis的性能瓶颈不在于CPU资源，而在于内存访问和网络IO。而采用单线程的设计带来的好处是，极大简化了数据结构和算法的实现。相反，**Redis通过异步IO和pipelining等机制来实现高速的并发访问**。显然，单线程的设计，对于单个请求的快速响应时间也提出了更高的要求。



### RedisObject是什么？

redisObject：**两层数据结构的桥梁**
redisObject数据结构详解：http://zhangtielei.com/posts/blog-redis-robj.html

**1、什么是redisObject：**

        从Redis的使用者的角度来看，一个Redis节点包含多个database（非cluster模式下默认是16个，cluster模式下只能是1个），而一个database维护了从key space到object space的映射关系，这个映射关系的key是string类型，而value可以是多种数据类型，比如：string, list, hash、set、sorted set等。
    
        而从Redis内部实现的角度来看，database内的这个映射关系是用一个dict来维护的。dict的key固定用一种数据结构来表达就够了，这就是动态字符串sds；而value则比较复杂，为了在同一个dict内能够存储不同类型的value，这就需要一个通用的数据结构，这个通用的数据结构就是robj，全名是redisObject

举个例子：

如果value是list类型，那么它的内部存储结构是一个quicklist或者是一个ziplist
如果value是string类型，那么它的内部存储结构一般情况下是一个sds。但如果string类型的value的值是一个数字，那么Redis内部还会把它转成long型来存储，从而减小内存使用。
所以，一个robj既能表示一个sds，也能表示一个quicklist，甚至还能表示一个long型。

**2、redis的数据结构定义：（基于Redis3.2版本）**

```
/* Object types */
#define OBJ_STRING 0
#define OBJ_LIST 1
#define OBJ_SET 2
#define OBJ_ZSET 3
#define OBJ_HASH 4

/* Objects encoding. Some kind of objects like Strings and Hashes can be

 * internally represented in multiple ways. The 'encoding' field of the object
 * is set to one of this fields for this object. */
   #define OBJ_ENCODING_RAW 0     /* Raw representation */
   #define OBJ_ENCODING_INT 1     /* Encoded as integer */
   #define OBJ_ENCODING_HT 2      /* Encoded as hash table */
   #define OBJ_ENCODING_ZIPMAP 3  /* Encoded as zipmap */
   #define OBJ_ENCODING_LINKEDLIST 4 /* Encoded as regular linked list */
   #define OBJ_ENCODING_ZIPLIST 5 /* Encoded as ziplist */
   #define OBJ_ENCODING_INTSET 6  /* Encoded as intset */
   #define OBJ_ENCODING_SKIPLIST 7  /* Encoded as skiplist */
   #define OBJ_ENCODING_EMBSTR 8  /* Embedded sds string encoding */
   #define OBJ_ENCODING_QUICKLIST 9 /* Encoded as linked list of ziplists */

#define LRU_BITS 24
typedef struct redisObject {
    unsigned type:4;
    unsigned encoding:4;
    unsigned lru:LRU_BITS; /* lru time (relative to server.lruclock) */
    int refcount;
    void *ptr;
} robj;
```

（1）一个robj包含如下5个字段：

- type: 对象的数据类型。占4个bit。可能的取值有5种：OBJ_STRING, OBJ_LIST, OBJ_SET, OBJ_ZSET, OBJ_HASH，分别对应Redis对外暴露的5种数据结构

- encoding: 对象的内部表示方式（也可以称为编码）。占4个bit。可能的取值有10种，即前面代码中的10个OBJ_ENCODING_XXX常量。
- lru: 做LRU替换算法用，占24个bit。这个不是我们这里讨论的重点，暂时忽略。
- refcount: 引用计数。它允许robj对象在某些情况下被共享。
- ptr: 数据指针。指向真正的数据。比如，一个代表string的robj，它的ptr可能指向一个sds结构；一个代表list的robj，它的ptr可能指向一个quicklist。

（2）encoding字段的说明：

        这里特别需要仔细察看的是encoding字段。对于同一个type，还可能对应不同的encoding，这说明同样的一个数据类型，可能存在不同的内部表示方式。而不同的内部表示，在内存占用和查找性能上会有所不同。

当type = OBJ_STRING的时候，表示这个robj存储的是一个string，这时encoding可以是下面3种中的一种：

OBJ_ENCODING_RAW: string采用原生的表示方式，即用sds来表示。
OBJ_ENCODING_INT: string采用数字的表示方式，实际上是一个long型。
OBJ_ENCODING_EMBSTR: string采用一种特殊的嵌入式的sds来表示。
当type = OBJ_HASH的时候，表示这个robj存储的是一个hash，这时encoding可以是下面2种中的一种：

OBJ_ENCODING_HT: hash采用一个dict来表示
OBJ_ENCODING_ZIPLIST: hash采用一个ziplist来表示
（3）10种encoding的取值说明：

OBJ_ENCODING_RAW: 最原生的表示方式。其实只有string类型才会用这个encoding值（表示成sds）。
OBJ_ENCODING_INT: 表示成数字。实际用long表示。
OBJ_ENCODING_HT: 表示成dict。
OBJ_ENCODING_ZIPMAP: 是个旧的表示方式，已不再用。在小于Redis 2.6的版本中才有。
OBJ_ENCODING_LINKEDLIST: 也是个旧的表示方式，已不再用。
OBJ_ENCODING_ZIPLIST: 表示成ziplist。
OBJ_ENCODING_INTSET: 表示成intset。用于set数据结构。
OBJ_ENCODING_SKIPLIST: 表示成skiplist。用于sorted set数据结构。
OBJ_ENCODING_EMBSTR: 表示成一种特殊的嵌入式的sds。
OBJ_ENCODING_QUICKLIST: 表示成quicklist。用于list数据结构。

**3、robj的作用：**

- redisObject就是Redis对外暴露的第一层面的数据结构：string, list, hash, set, sorted set，而每一种数据结构的底层实现所对应的是哪些第二层面的数据结构（dict, sds, ziplist, quicklist, skiplist等），则通过不同的encoding来区分。可以说，**robj是联结两个层面的数据结构的桥梁**。
- 为**多种数据类型提供一种统一的表示方式**。
- **允许同一类型的数据采用不同的内部表示，从而在某些情况下尽量节省内存**。
- **支持对象共享和引用计数**。当对象被共享的时候，**只占用一份内存拷贝，进一步节省内存**。



### Redis一层数据结构

- String
  - 底层采用string+long形式
- Hash
  - 底层采用zipList+dict形式
- List
  - 底层采用zipList+quickList形式
- Set
  - 底层采用intSet+dict形式
- Sorted Set
  - 底层采用zipList+zSet（dict+skipList）形式

下面我们来详细了解Reids的一层数据结构：

#### string

String是最简单的数据类型，一般用于复杂的计数功能的缓存：微博数，粉丝数等。

- 底层实现方式：动态字符串sds或者long

String的内部存储结构一般是sds（Simple Dynamic String），但是**如果一个String类型的value的值是数字，那么Redis内部会把它转成long类型来存储，从而减少内存的使用**。

确切地说，String在Redis中是用一个**robj**来表示的。
用来表示String的robj可能编码成3种内部表示：OBJ_ENCODING_RAW，OBJ_ENCODING_EMBSTR，OBJ_ENCODING_INT。其中前两种编码使用的是sds来存储，最后一种OBJ_ENCODING_INT编码直接把string存成了long型。
在对string进行incr, decr等操作的时候，如果它内部是OBJ_ENCODING_INT编码，那么可以直接进行加减操作；如果它内部是OBJ_ENCODING_RAW或OBJ_ENCODING_EMBSTR编码，那么Redis会先试图把sds存储的字符串转成long型，如果能转成功，再进行加减操作。
对一个内部表示成long型的string执行append, setbit, getrange这些命令，针对的仍然是string的值（即十进制表示的字符串），而不是针对内部表示的long型进行操作。比如字符串”32”，如果按照字符数组来解释，它包含两个字符，它们的ASCII码分别是0x33和0x32。当我们执行命令setbit key 7 0的时候，相当于把字符0x33变成了0x32，这样字符串的值就变成了”22”。而如果将字符串”32”按照内部的64位long型来解释，那么它是0x0000000000000020，在这个基础上执行setbit位操作，结果就完全不对了。因此，在这些命令的实现中，会把long型先转成字符串再进行相应的操作。

#### list

list 的实现为一个**双向链表**，经常被用作队列使用，支持在链表**两端进行push和pop操作**，时间复杂度为O(1)；同时也支持在链表中的任意位置的存取操作，但是需要对list进行遍历，时间复杂度为O(n)。

        list 的应用场景非常多，比如微博的关注列表，粉丝列表，消息列表等功能都可以用Redis的 list 结构来实现。可以利用lrange命令，做基于redis的分页功能。

- Redis3.2之前的底层实现方式：压缩列表ziplist 或者 双向循环链表linkedlist

  当list存储的数据量较少时，会使用ziplist存储数据，也就是同时满足下面两个条件：

  - 列表中数据个数少于512个
  - list中保存的每个元素的长度小于 64 字节

  当不能同时满足上面两个条件的时候，list就通过双向[循环链表](https://so.csdn.net/so/search?q=循环链表&spm=1001.2101.3001.7020)linkedlist来实现了

- Redis3.2及之后的底层实现方式：**quicklist**

  quicklist是一个双向链表，而且是一个基于ziplist的双向链表，quicklist的每个节点都是一个ziplist，结合了双向链表和ziplist的优点

#### hash

 Hash适合用于存储对象，因为一个对象的各个属性，正好对应一个hash结构的各个field，可以方便地操作对象中的某个字段。

- 底层实现方式：压缩列表ziplist 或者 字典dict

当数据量较少的情况下，hash底层会使用压缩列表ziplist进行存储数据，也就是同时满足下面两个条件的时候：

hash-max-ziplist-entries 512：当hash中的数据项（即filed-value对）的数目小于512时
hash-max-ziplist-value 64：当hash中插入的任意一个value的长度小于64字节
        当不能同时满足上面两个条件的时候，底层的ziplist就会转成dict，之所以这样设计，是因为当ziplist变得很大的时候，它有如下几个**缺点**：

每次插入或修改引发的realloc操作会有更大的概率造成内存拷贝，从而降低性能。
一旦发生内存拷贝，内存拷贝的成本也相应增加，因为要拷贝更大的一块数据。
当ziplist数据项过多的时候，在它上面查找指定的数据项就会性能变得很低，因为ziplist上的查找需要进行遍历。
总之，**ziplist本来就设计为各个数据项挨在一起组成连续的内存空间，这种结构并不擅长做修改操作**。一旦数据发生改动，就会引发内存realloc，可能导致内存拷贝。（**注：realloc是从新分配**）

#### set

set是一个存放不重复值的无序集合，可做全局去重的功能，提供了判断某个元素是否在set集合内的功能，这个也是list所不能提供的。基于set可以实现交集、并集、差集的操作，计算共同喜好，全部的喜好，自己独有的喜好等功能。

- 底层实现方式：有序整数集合intset 或者 字典dict

  当存储的数据同时满足下面这样两个条件的时候，Redis 就采用整数集合intset来实现set这种数据类型：

  - 存储的数据都是整数

  - 存储的数据元素个数小于512个

  当不能同时满足这两个条件的时候，Redis 就使用dict来存储集合中的数据

#### sorted set

Sorted set 相比 set 多了一个权重参数score，集合中的元素能够按score进行排列。可以做排行榜应用，取TOP N操作。另外，sorted set可以用来做延时任务。最后一个应用就是可以做范围查找。

- 底层实现方式：压缩列表ziplist 或者 zset

  当 sorted set 的数据同时满足下面这两个条件的时候，就使用压缩列表ziplist实现sorted set

  - 元素个数要小于 128 个，也就是**ziplist数据项小于256个**
  - 集合中每个数据大小都小于 **64 字节**

  当不能同时满足这两个条件的时候，Redis 就使用zset来实现sorted set，这个**zset包含一个dict + 一个skiplist**。**dict用来查询数据到分数(score)的对应关系，而skiplist用来根据分数查询数据（可能是范围查找）**。

#### 做个总结

- string：非数字—>sds；数字—>：long
- Hash：512内+64内—>zipList；不满足则dict
- List：
  - 3.2之前：512内+64内—>zipList；不满足则LinkedList
  - 3.2之后：quickList
- set：512内+都是整数—>intset；不满足则dict
- sorted set：128内+64内—>ziplist；不满足则zset（dict+skiplist）

![img](https://img-blog.csdnimg.cn/720b2e10a6cc4f009963d0af153a2351.png)

### Redis二层数据结构

- sds
- dict
- zipList
- quickList
- intset
- skipList

下面问我们来详细了解下**Redis的内部数据结构**：

#### sds

sds数据结构详解：http://zhangtielei.com/posts/blog-redis-sds.html

**（1）什么是sds**：

sds全称是Simple Dynamic String，具有如下显著的特点：

① **可动态扩展内存**。sds表示的字符串其内容可以修改，也可以追加。

② 采用**预分配冗余空间的方式来减少内存的频繁分配**，从而优化字符串的增长操作

③ **二进制安全**（Binary Safe）。sds能存储任意二进制数据，而不仅仅是可打印字符。

④ **与传统的C语言字符串类型兼容**。



**（2）sds的数据结构：**

sds是Binary Safe的，它可以存储任意二进制数据，不能像C语言字符串那样以字符’\0’来标识字符串的结束，因此它必然有个长度字段。但这个长度字段在哪里呢？实际上sds还包含一个header结构：

```
struct __attribute__ ((__packed__)) sdshdr5 {
    unsigned char flags; /* 3 lsb of type, and 5 msb of string length */
    char buf[];
};
struct __attribute__ ((__packed__)) sdshdr8 {
    uint8_t len; /* used */
    uint8_t alloc; /* excluding the header and null terminator */
    unsigned char flags; /* 3 lsb of type, 5 unused bits */
    char buf[];
};
struct __attribute__ ((__packed__)) sdshdr16 {
    uint16_t len; /* used */
    uint16_t alloc; /* excluding the header and null terminator */
    unsigned char flags; /* 3 lsb of type, 5 unused bits */
    char buf[];
};
struct __attribute__ ((__packed__)) sdshdr32 {
    uint32_t len; /* used */
    uint32_t alloc; /* excluding the header and null terminator */
    unsigned char flags; /* 3 lsb of type, 5 unused bits */
    char buf[];
};
struct __attribute__ ((__packed__)) sdshdr64 {
    uint64_t len; /* used */
    uint64_t alloc; /* excluding the header and null terminator */
    unsigned char flags; /* 3 lsb of type, 5 unused bits */
    char buf[];
};
```

sds一共有5种类型的header。之所以有5种，是为了能让不同长度的字符串可以使用不同大小的header。这样，短字符串就能使用较小的header，从而节省内存。

所以，**sds字符串的完整结构，由在内存地址上前后相邻的header和字符数组两部分组成**：

① **header**：除了sdshdr5，一个header结构都包含3个字段：**字符串的真正长度len**、**字符串的最大容量alloc**和**flags**，flags总是占用一个字节。其中的最低3个bit用来表示header的类型。

② **字符数组**：字符数组的长度等于最大容量+1，存放了真正有效的字符串数据。在真正的字符串数据之后，是空余未用的字节（一般以字节0填充），允许在不重新分配内存的前提下让字符串数据向后做有限的扩展。在真正的字符串数据之后，还有一个NULL结束符，即ASCII码为0的’\0’字符。这是为了和传统C字符串兼容。之所以字符数组的长度比最大容量多1个字节，就是为了在字符串长度达到最大容量时仍然有1个字节存放NULL结束符。

header的类型共有5种，在sds.h中有常量定义：

```
#define SDS_TYPE_5  0
#define SDS_TYPE_8  1
#define SDS_TYPE_16 2
#define SDS_TYPE_32 3
#define SDS_TYPE_64 4
```

**sds字符串的header，其实隐藏在真正的字符串数据的前面（低地址方向），这样的一个定义，有如下几个好处**：

header和数据相邻，从而不需要分成两块内存空间来单独分配，有利于减少内存碎片，提高存储效率
虽然header有多个类型，但sds可以用统一的char *来表达。且它与传统的C语言字符串保持类型兼容。如果一个sds里面存储的是可打印字符串，那么我们可以直接把它传给C函数，比如使用strcmp比较字符串大小，或者使用printf进行打印。



**（3）String robj的编码与解码过程：**

OBJ_STRING类型的字符串对象的编码和解码过程在Redis里非常重要，应用广泛。

当我们执行Redis的set命令的时候，Redis首先将接收到的value值（string类型）表示成一个type = OBJ_STRING并且encoding = OBJ_ENCODING_RAW的robj对象，然后在存入内部存储之前先执行一个编码过程，试图将它表示成另一种更节省内存的encoding方式。这一过程的核心代码，是object.c中的tryObjectEncoding函数。

当我们需要获取字符串的值，比如执行get命令的时候，我们需要执行与前面讲的编码过程相反的操作——解码。这一解码过程的核心代码，是object.c中的getDecodedObject函数。

对于编码的核心代码tryObjectEncoding函数和解码的核心代码getDecodedObject函数的详细说明，样请读者移步这篇文章：Redis内部数据结构详解(3)——robj - 铁蕾的个人博客



**（4）为什么 Redis 的 String 底层数据结构使用 sds：**

**① 性能高：**

 sds 数据结构主要由 len、alloc、buf[] 三个属性组成，其中 buf[] 为实际保存字符串的 char 类型数组；len 表示 buf[] 数组所保存的字符串的长度。由于使用 len 记录了保存的字符串长度，所以在获取字符串长度的时候，不需要从前往后遍历数组，直接获取 len 的值就可以了

**② 内存预分配，优化字符串的增长操作：**

当需要修改数据时，首先会检查 sds 空间 len 是否满足，不满足则自动扩容空间，然后再执行修改。sds 在分配空间时，还会分配额外的未使用空间 free，下次再修改就先检查未使用空间 free 是否满足，如果满足则不用再扩展空间。通过空间预分配策略，有效较少在字符串连续增长情况下产生的内存重分配次数。

额外分配 free 空间的规则：

如果 sds 字符串修改后，len 值小于 1M，则额外分配未使用空间 free 的大小为 len 
如果 sds 字符串修改后，len 值大于等于 1M，则额外分配未使用空间 free 的大小为1M

**③ 惰性空间回收，优化字符串的缩短操作：**

当缩短 SDS 字符串后，并不会立即执行内存重分配来回收多余的空间，如果后续有增长操作，则可直接使用。



#### dict

dict数据结构详解：http://zhangtielei.com/posts/blog-redis-dict.html

**dict是一个用于维护key-value映射关系的数据结构**。Redis的一个database中所有key到value的映射，就是使用一个dict来维护的。不过，这只是它在Redis中的一个用途而已，它在Redis中被使用的地方还有很多。比如，Redis的hash结构，当它的field较多时，便会采用dict来存储。再比如，Redis配合使用dict和skiplist来共同维护一个sorted set。

**dict本质上是为了解决算法中的查找问题**，是一个基于哈希表的算法，在不要求数据有序存储，且能保持较低的哈希值冲突概率的前提下，查询的时间复杂度接近O(1)。它采用某个哈希函数并通过计算key从而找到在哈希表中的位置，采用拉链法解决冲突，并在装载因子（load factor）超过预定值时自动扩展内存，引发重哈希（rehashing），为了避免扩容时一次性对所有key进行重哈希，Redis采用了一种称为增量式重哈希（incremental rehashing）的方法，将重哈希的操作分散到对于dict的各个增删改查的操作中去。这种方法能做到每次只对一小部分key进行重哈希，而每次重哈希之间不影响dict的操作。dict之所以这样设计，是为了避免重哈希期间单个请求的响应时间剧烈增加，这与前面提到的“快速响应时间”的设计原则是相符的。

当装载因子大于 1 的时候，Redis 会触发扩容，将散列表扩大为原来大小的 2 倍左右；当数据动态减少之后，为了节省内存，当装载因子小于 0.1 的时候，Redis 就会触发缩容，缩小为字典中数据个数的大约2 倍大小。



#### ziplist

ziplist数据结构详解：http://zhangtielei.com/posts/blog-redis-ziplist.html

**ziplist是一个经过特殊编码的双向链表**，可以用于存储字符串或整数，其中整数是按真正的二进制表示进行编码的，而不是编码成字符串序列。它能以O(1)的时间复杂度在表的两端提供push和pop操作。

一个普通的双向链表，链表中每一项都占用独立的一块内存，并通过地址指针连接起来，但这种方式会带来大量的内存碎片，而且地址指针也会占用额外的内存。而**ziplist使用了一整块连续的内存，将表中每一项存放在前后连续的地址空间内，类似于一个数组**。另外，**ziplist为了在细节上节省内存，对于值的存储采用了变长的编码方式，大概意思是说，对于大的整数，就多用一些字节来存储，而对于小的整数，就少用一些字节来存储**。

总的来说，**ziplist使用一块连续的内存空间来存储数据，并采用可变长的编码方式，支持不同类型和大小的数据的存储，更加节省内存，而且数据存储在一片连续的内存空间，读取的效率也非常高**。



#### quicklist

quicklist数据结构详解：http://zhangtielei.com/posts/blog-redis-quicklist.html

**（1）什么是quicklist：**

**quicklist是一个基于ziplist的双向链表**，**quicklist的每个节点都是一个ziplist**，比如，一个包含3个节点的quicklist，如果每个节点的ziplist又包含4个数据项，那么对外表现上，这个list就总共包含12个数据项。quicklist的结构为什么这样设计呢？总结起来，大概又是一个空间和时间的折中：

双向链表便于在表的两端进行push和pop操作，但是它的内存开销比较大。首先，它在每个节点上除了要保存数据之外，还要额外保存两个指针；其次，双向链表的各个节点是单独的内存块，地址不连续，节点多了容易产生内存碎片。
ziplist由于是一整块连续内存，所以存储效率很高。但是，它不利于修改操作，每次数据变动都会引发一次内存的realloc。特别是当ziplist长度很长的时候，一次realloc可**能会导致大批量的数据拷贝，进一步降低性能。**
**于是，**结合了双向链表和ziplist的优点，quicklist就应运而生了。

**（2）quicklist中每个ziplist长度的配置：**

不过，这也带来了一个新问题：到底一个quicklist节点包含多长的ziplist合适呢？比如，同样是存储12个数据项，既可以是一个quicklist包含3个节点，而每个节点的ziplist又包含4个数据项，也可以是一个quicklist包含6个节点，而每个节点的ziplist又包含2个数据项。

这又是一个**需要找平衡点的难题**。我们只从存储效率上分析一下：

**每个quicklist节点上的ziplist越短，则内存碎片越多。**内存碎片多了，有可能在内存中产生很多无法被利用的小碎片，从而降低存储效率。这种情况的极端是每个quicklist节点上的ziplist只包含一个数据项，这就蜕化成一个普通的双向链表了。
**每个quicklist节点上的ziplist越长，则为ziplist分配大块连续内存空间的难度就越大。**有可能出现内存里有很多小块的空闲空间（它们加起来很多），但却找不到一块足够大的空闲空间分配给ziplist的情况。这同样会降低存储效率。这种情况的极端是整个quicklist只有一个节点，所有的数据项都分配在这仅有的一个节点的ziplist里面。这其实蜕化成一个ziplist了
可见，一个quicklist节点上的ziplist要保持一个合理的长度。那到底多长合理呢？这可能取决于具体应用场景。实际上，**Redis提供了一个配置参数list-max-ziplist-size，就是为了让使用者可以来根据自己的情况进行调整。**

`list-max-ziplist-size -2`

这个参数可以取正值，也可以取负值。

**当取正值的时候，表示按照数据项个数来限定每个quicklist节点上的ziplist长度。比如，当这个参数配置成5的时候，表示每个quicklist节点的ziplist最多包含5个数据项。**

当取负值的时候，表示按照占用字节数来限定每个quicklist节点上的ziplist长度。这时，它只能取-1到-5这五个值，每个值含义如下：

```
-5: 每个quicklist节点上的ziplist大小不能超过64 Kb。（注：1kb => 1024 bytes）
-4: 每个quicklist节点上的ziplist大小不能超过32 Kb。
-3: 每个quicklist节点上的ziplist大小不能超过16 Kb。
-2: 每个quicklist节点上的ziplist大小不能超过8 Kb。（-2是Redis给出的默认值）
-1: 每个quicklist节点上的ziplist大小不能超过4 Kb。
```



#### intset

intset数据结构详解：http://zhangtielei.com/posts/blog-redis-intset.html

**（1）什么是intset：**

intset是一个**由整数组成的有序集合**，从而**便于进行二分查找**，用于快速地判断一个元素是否属于这个集合。它在内存分配上与ziplist有些类似，**是连续的一整块内存空间，而且对于大整数和小整数（按绝对值）采取了不同的编码，尽量对内存的使用进行了优化。**



**（2）intset的数据结构：**

```
typedef struct intset {
    uint32_t encoding;
    uint32_t length;
    int8_t contents[];
} intset;

#define INTSET_ENC_INT16 (sizeof(int16_t))
#define INTSET_ENC_INT32 (sizeof(int32_t))
#define INTSET_ENC_INT64 (sizeof(int64_t))
```

各个字段含义如下：

encoding: 数据编码，表示intset中的每个数据元素用几个字节来存储。它有三种可能的取值：INTSET_ENC_INT16表示每个元素用2个字节存储，INTSET_ENC_INT32表示每个元素用4个字节存储，INTSET_ENC_INT64表示每个元素用8个字节存储。因此，intset中存储的整数最多只能占用64bit。
length: 表示intset中的元素个数。encoding和length两个字段构成了intset的头部（header）。
contents: 是一个柔性数组（flexible array member），表示intset的header后面紧跟着数据元素。这个数组的总长度（即总字节数）等于encoding * length。柔性数组在Redis的很多数据结构的定义中都出现过（例如sds, quicklist, skiplist），用于表达一个偏移量。contents需要单独为其分配空间，这部分内存不包含在intset结构当中。
其中需要注意的是，intset可能会随着数据的添加而改变它的数据编码：

最开始，新创建的intset使用占内存最小的INTSET_ENC_INT16（值为2）作为数据编码。
每添加一个新元素，则根据元素大小决定是否对数据编码进行升级。



**（3）intset与ziplist相比：**

ziplist可以存储任意二进制串，而intset只能存储整数。
ziplist是无序的，而intset是从小到大有序的。因此，在ziplist上查找只能遍历，而在intset上可以进行二分查找，性能更高。
ziplist可以对每个数据项进行不同的变长编码（每个数据项前面都有数据长度字段len），而intset只能整体使用一个统一的编码（encoding）。



#### skiplist

skiplist数据结构详解：http://zhangtielei.com/posts/blog-redis-skiplist.html

（1）什么是跳表：

        跳表是一种可以进行二分查找的有序链表，采用空间换时间的设计思路，跳表在原有的有序链表上面增加了多级索引（例如每两个节点就提取一个节点到上一级），通过索引来实现快速查找。跳表是一种动态数据结构，支持快速的插入、删除、查找操作，时间复杂度都为O(logn)，空间复杂度为 O(n)。跳表非常灵活，可以通过改变索引构建策略，有效平衡执行效率和内存消耗。

① 跳表的删除操作：**除了要删除原始链表中的节点，还要删除索引中的节点**。

② **插入元素后，索引的动态更新**：不停的往跳表里面插入数据，如果不更新索引，就有可能出现某两个索引节点之间的数据非常多的情况，甚至退化成单链表。针对这种情况，我们在添加元素的时候，通过一个随机函数，同时选择将这个数据插入到部分索引层。比如随机函数生成了值 K，那我们就将这个结点添加到第一级到第K级这K级的索引中。

<img src="https://upload-images.jianshu.io/upload_images/19063731-8899cf09c97fd229.jpeg?imageMogr2/auto-orient/strip|imageView2/2/w/1142/format/webp" alt="img" style="zoom:67%;" />



## 三、Redis怎么持久化？

首先，持久化是什么？

因为Redis是直接在内存中操作数据的，那么如果关闭计算机，数据直接丢失了，所以持久化就是将内存上的数据保存下来存储在磁盘中。

Redis官方提供了不同级别的持久化方式：

* RDB持久化：能够在指定的时间间隔能对你的数据进行快照存储。
* AOF持久化：记录每次对服务器写的操作，当服务器重启的时候会重新执行这些命令来恢复原始的数据，AOF命令以redis协议追加保存每次写的操作到文件末尾。Redis还能对AOF文件进行后台重写，使得AOF文件的体积不至于过大。
* 不使用持久化：如果你只希望你的数据在服务器运行的时候存在，你也可以选择不使用任何持久化方式。
* 同时开启RDB和AOF：你也可以同时开启两种持久化方式，在这种情况下当redis重启的时候会优先载入AOF文件来恢复原始的数据，因为在通常情况下AOF文件保存的数据集要比RDB文件保存的数据集要完整。

这么多持久化方式我们应该怎么选？在选择之前我们需要搞清楚每种持久化方式的区别以及各自的优劣势。

### RDB持久化

RDB(Redis Database)持久化是把当前内存数据生成快照保存到硬盘的过程，触发RDB持久化过程分为**手动触发**和**自动触发**。

（1）**手动触发**

手动触发对应**save命令**，会阻塞当前Redis服务器，直到RDB过程完成为止，对于内存比较大的实例会造成长时间阻塞，**线上环境不建议使用。**

（2）**自动触发**

自动触发对应**bgsave命令**，Redis进程执行fork操作创建子进程，RDB持久化过程由子进程负责，完成后自动结束。**阻塞只发生在fork阶段，一般时间很短。**

在redis.conf配置文件中可以配置：

```plain
save <seconds> <changes>
```

表示xx秒内数据修改xx次时自动触发bgsave。
如果想关闭自动触发，可以在save命令后面加一个空串，即：

```plain
save ""
```

还有其他常见可以触发bgsave，如：

* 如果从节点执行全量复制操作，主节点自动执行bgsave生成RDB文件并发送给从节点。
* 默认情况下执行shutdown命令时，如果没有开启AOF持久化功能则 自动执行bgsave。

**bgsave工作机制**

<div align="center">  <img src="https://cdn.jsdelivr.net/gh/SmileLionCoder/assets@main/202010/20201025211619.png" width="300"/> </div><br>

（1）执行bgsave命令，Redis父进程判断当前是否存在正在执行的子进 程，**如RDB/AOF子进程，如果存在，bgsave命令直接返回。**

（2）**父进程执行fork操作创建子进程**，fork操作过程中父进程会阻塞，通 过info stats命令查看latest_fork_usec选项，可以获取最近一个fork操作的耗时，单位为微秒

（3）**父进程fork完成后，bgsave命令返回“Background saving started”信息并不再阻塞父进程**，可以继续响应其他命令。

（4）**子进程创建RDB文件，根据父进程内存生成临时快照文件，完成后对原有文件进行原子替换**。执行lastsave命令可以获取最后一次生成RDB的 时间，对应info统计的rdb_last_save_time选项。

（5）进程发送信号给父进程表示完成，父进程更新统计信息，具体见 info Persistence下的rdb_*相关选项。



### AOF持久化

AOF（append only file）持久化：以独立日志的方式记录每次写命令， 重启时再重新执行AOF文件中的命令达到恢复数据的目的。AOF的主要作用是解决了数据持久化的实时性，目前已经是Redis持久化的主流方式。

**AOF持久化工作机制**

开启AOF功能需要配置：appendonly yes，默认不开启。

AOF文件名 通过appendfilename配置设置，默认文件名是appendonly.aof。保存路径同 RDB持久化方式一致，通过dir配置指定。

AOF的工作流程操作：命令写入 （append）、文件同步（sync）、文件重写（rewrite）、重启加载 （load）。

<div align="center">  <img src="https://cdn.jsdelivr.net/gh/SmileLionCoder/assets@main/202010/20201025211644.png" width="200"/> </div><br>

（1）所有的写入命令会追加到aof_buf（缓冲区）中。

（2）AOF缓冲区根据对应的策略向硬盘做同步操作。

AOF为什么把命令追加到aof_buf中？Redis使用单线程响应命令，如果每次写AOF文件命令都直接追加到硬盘，那么性能完全取决于当前硬盘负载。先写入缓冲区aof_buf中，还有另一个好处，Redis可以提供多种缓冲区同步硬盘的策略，在性能和安全性方面做出平衡。

（3）随着AOF文件越来越大，需要定期对AOF文件进行重写，达到压缩的目的。

（4）当Redis服务器重启时，可以加载AOF文件进行数据恢复。

**AOF重写（rewrite）机制**

重写的目的：

* 减小AOF文件占用空间；
* 更小的AOF 文件可以更快地被Redis加载恢复。

AOF重写可以分为手动触发和自动触发：

* 手动触发：直接调用bgrewriteaof命令。
* 自动触发：根据auto-aof-rewrite-min-size和auto-aof-rewrite-percentage参数确定自动触发时机。

auto-aof-rewrite-min-size：表示运行AOF重写时文件最小体积，默认 为64MB。

auto-aof-rewrite-percentage：代表当前AOF文件空间 （aof_current_size）和上一次重写后AOF文件空间（aof_base_size）的比值。

自动触发时机

当aof_current_size>auto-aof-rewrite-minsize 并且（aof_current_size-aof_base_size）/aof_base_size>=auto-aof-rewritepercentage。

其中aof_current_size和aof_base_size可以在info Persistence统计信息中查看。

<div align="center">  <img src="https://cdn.jsdelivr.net/gh/SmileLionCoder/assets@main/202010/20201025211707.png" width="300"/> </div><br>

AOF文件重写后为什么会变小？

（1）旧的AOF文件含有无效的命令，如：del key1， hdel key2等。重写只保留最终数据的写入命令。

（2）多条命令可以合并，如lpush list a，lpush list b，lpush list c可以直接转化为lpush list a b c。

**AOF文件数据恢复**

<div align="center">  <img src="https://cdn.jsdelivr.net/gh/SmileLionCoder/assets@main/202010/20201025211835.png" width="300"/> </div><br>

数据恢复流程说明：

（1）AOF持久化开启且存在AOF文件时，优先加载AOF文件。

（2）AOF关闭或者AOF文件不存在时，加载RDB文件。

（3）加载AOF/RDB文件成功后，Redis启动成功。

（4）AOF/RDB文件存在错误时，Redis启动失败并打印错误信息。

### RDB和AOF的优缺点

**RDB优点**

* RDB 是一个非常紧凑的文件,它保存了某个时间点的数据集,非常适用于数据集的备份,比如你可以在每个小时报保存一下过去24小时内的数据,同时每天保存过去30天的数据,这样即使出了问题你也可以根据需求恢复到不同版本的数据集。
* RDB 是一个紧凑的单一文件,很方便传送到另一个远端数据中心，非常适用于灾难恢复。
* RDB 在保存 RDB 文件时父进程唯一需要做的就是 fork 出一个子进程,接下来的工作全部由子进程来做，父进程不需要再做其他 IO 操作，所以 RDB 持久化方式可以最大化 Redis 的性能。
* 与AOF相比,在恢复大的数据集的时候，RDB 方式会更快一些。

**AOF优点**

* 你可以使用不同的 fsync 策略：无 fsync、每秒 fsync 、每次写的时候 fsync .使用默认的每秒 fsync 策略, Redis 的性能依然很好( fsync 是由后台线程进行处理的,主线程会尽力处理客户端请求),一旦出现故障，你最多丢失1秒的数据。
* AOF文件是一个只进行追加的日志文件,所以不需要写入seek,即使由于某些原因(磁盘空间已满，写的过程中宕机等等)未执行完整的写入命令,你也也可使用redis-check-aof工具修复这些问题。
* Redis 可以在 AOF 文件体积变得过大时，自动地在后台对 AOF 进行重写： 重写后的新 AOF 文件包含了恢复当前数据集所需的最小命令集合。 整个重写操作是绝对安全的，因为 Redis 在创建新 AOF 文件的过程中，会继续将命令追加到现有的 AOF 文件里面，即使重写过程中发生停机，现有的 AOF 文件也不会丢失。 而一旦新 AOF 文件创建完毕，Redis 就会从旧 AOF 文件切换到新 AOF 文件，并开始对新 AOF 文件进行追加操作。
* AOF 文件有序地保存了对数据库执行的所有写入操作， 这些写入操作以 Redis 协议的格式保存， 因此 AOF 文件的内容非常容易被人读懂， 对文件进行分析（parse）也很轻松。 导出（export） AOF 文件也非常简单： 举个例子， 如果你不小心执行了 FLUSHALL 命令， 但只要 AOF 文件未被重写， 那么只要停止服务器， 移除 AOF 文件末尾的 FLUSHALL 命令， 并重启 Redis ， 就可以将数据集恢复到 FLUSHALL 执行之前的状态。

**RDB缺点**

* Redis 要完整的保存整个数据集是一个比较繁重的工作,你通常会每隔5分钟或者更久做一次完整的保存,万一在 Redis 意外宕机,你可能会丢失几分钟的数据。
* RDB 需要经常 fork 子进程来保存数据集到硬盘上,当数据集比较大的时候, fork 的过程是非常耗时的,可能会导致 Redis 在一些毫秒级内不能响应客户端的请求。

**AOF缺点**

* 对于相同的数据集来说，AOF 文件的体积通常要大于 RDB 文件的体积。
* 数据恢复（load）时AOF比RDB慢，通常RDB 可以提供更有保证的最大延迟时间。

**RDB和AOF简单对比总结**

RDB优点：

* RDB 是紧凑的二进制文件，比较适合备份，全量复制等场景
* RDB 恢复数据远快于 AOF

RDB缺点：

* RDB 无法实现实时或者秒级持久化；
* 新老版本无法兼容 RDB 格式。



AOF优点：

* 可以更好地保护数据不丢失；
* appen-only 模式写入性能比较高；
* 适合做灾难性的误删除紧急恢复。

AOF缺点：

* 对于同一份文件，AOF 文件要比 RDB 快照大；
* AOF 开启后，会对写的 QPS 有所影响，相对于 RDB 来说 写 QPS 要下降；
* 数据库恢复比较慢， 不合适做冷备。



## 四、Redis线程模型

总所周知，Redis是单线程的，那么为什么Redis被称为单线程模型？

redis 内部使用<u>**文件事件处理器 file event handler**，它是单线程的，所以redis才叫做单线程模型</u>。它采用**IO多路复用机制**同时监听多个 socket，将产生事件的 socket 压入内存队列中，事件分派器根据 socket 上的事件类型来选择对应的事件处理器进行处理。

**文件事件处理起的结构如下：**

- 多个 socket
- IO 多路复用程序
- 文件事件分派器
- 事件处理器（连接应答处理器、命令请求处理器、命令回复处理器）

**线程模型**

多个 socket 可能会并发产生不同的操作，每个操作对应不同的文件事件，但是 IO多路复用程序会监听多个 socket，会将产生事件的 socket 放入队列中排队，事件分派器每次从队列中取出一个 socket，根据 socket 的事件类型交给对应的事件处理器进行处理。

![img](https://img2020.cnblogs.com/blog/2136379/202008/2136379-20200830233359116-1537526582.png)

下面我们看一次客户端与Redis通信的完整通信过程

##### 建立连接

1. 首先，redis 服务端进程初始化的时候，会将 server socket 的 AE_READABLE 事件与连接应答处理器关联。
2. 客户端 socket01 向 redis 进程的 server socket 请求建立连接，此时 server socket 会产生一个 AE_READABLE 事件，IO 多路复用程序监听到 server socket 产生的事件后，将该 socket 压入队列中。
3. 文件事件分派器从队列中获取 socket，交给连接应答处理器。
4. 连接应答处理器会创建一个能与客户端通信的 socket01，并将该 socket01 的 AE_READABLE 事件与命令请求处理器关联。

##### 执行一个set请求

1. 客户端发送了一个 set key value 请求，此时 redis 中的 socket01 会产生 AE_READABLE 事件，IO 多路复用程序将 socket01 压入队列，
2. 此时事件分派器从队列中获取到 socket01 产生的 AE_READABLE 事件，由于前面 socket01 的 AE_READABLE 事件已经与命令请求处理器关联，
3. 因此事件分派器将事件交给命令请求处理器来处理。命令请求处理器读取 socket01 的 key value 并在自己内存中完成 key value 的设置。
4. 操作完成后，它会将 socket01 的 AE_WRITABLE 事件与命令回复处理器关联。
5. 如果此时客户端准备好接收返回结果了，那么 redis 中的 socket01 会产生一个 AE_WRITABLE 事件，同样压入队列中，
6. 事件分派器找到相关联的命令回复处理器，由命令回复处理器对 socket01 输入本次操作的一个结果，比如 ok，之后解除 socket01 的 AE_WRITABLE 事件与命令回复处理器的关联。

[![img](https://img2020.cnblogs.com/blog/2136379/202008/2136379-20200830233422768-1205881078.png)](https://img2020.cnblogs.com/blog/2136379/202008/2136379-20200830233422768-1205881078.png)



## 五、[Redis分布式锁](https://www.bilibili.com/video/BV1wW4y177WN/?spm_id_from=333.337.search-card.all.click&vd_source=cb5cce1bdc5ab99fce812684e0c31a2b)



### ①SETNX + expire

`setnx`+ `expire`命令。即先用`setnx`来抢锁，如果抢到之后，再用`expire`给锁设置一个过期时间，防止锁忘记了释放。

> SETNX 是SET IF NOT EXISTS的简写.日常命令格式是SETNX key value，如果 key不存在，则SETNX成功返回1，如果这个key已经存在了，则返回0。

假设某电商网站的某商品做秒杀活动，key可以设置为key_resource_id,value设置任意值，伪代码如下：

```csharp
// jedis为c#中操控redis的一个库
if（jedis.setnx(key_resource_id,lock_value) == 1）{ //加锁
    expire（key_resource_id，100）; //设置过期时间
    try {
        do something  //业务请求
    }catch(){
　　}
　　finally {
       jedis.del(key_resource_id); //释放锁
    }
}
```

**问题**：

> 但是这个方案中，`setnx`和`expire`两个命令分开了，**不是原子操作**。如果执行完`setnx`加锁，正要执行`expire`设置过期时间时，进程crash或者要重启维护了，那么这个锁就“长生不老”了，**别的线程永远获取不到锁啦**

**问题：故障导致锁不能释放，尝试解决：改原子性操作**



### ②SETNX + value

即：系统时间+过期时间

这个方案的优点是，巧妙移除`expire`单独设置过期时间的操作，把**过期时间放到setnx的value值**里面来。解决了方案一发生异常，锁得不到释放的问题。但是这个方案还有别的缺点：

**问题**：

> - 过期时间是客户端自己生成的（System.currentTimeMillis()是当前系统的时间），必须要求分布式环境下，每个客户端的时间必须同步。
> - 如果锁过期的时候，并发多个客户端同时请求过来，都执行jedis.getSet()，最终只能有一个客户端加锁成功，但是该客户端锁的过期时间，可能被别的客户端覆盖
> - 该锁没有保存持有者的唯一标识，可能被别的客户端释放/解锁。

**问题：锁被别人释放，或释放了别人的锁， 尝试解决：不用SETNX了，因为方案一和方案二暴露的问题，没办法用SETNX解决**



### ③SET NX EX扩展命令

（`SET key value[EX seconds][PX milliseconds][NX|XX]`），它也是原子性的！

> SET key value[EX seconds][PX milliseconds][NX|XX]
>
> - NX :表示key不存在的时候，才能set成功，也即保证只有第一个客户端请求才能获得锁，而其他客户端请求只能等其释放锁，才能获取。
> - EX seconds :设定key的过期时间，时间单位是秒。
> - PX milliseconds: 设定key的过期时间，单位为毫秒
> - XX: 仅当key存在时设置值

伪代码demo如下：

```csharp
if（jedis.set(key_resource_id, lock_value, "NX", "EX", 100s) == 1）{ //加锁
    try {
        do something  //业务处理
    }catch(){
　　}
　　finally {
       jedis.del(key_resource_id); //释放锁
    }
}
复制代码
```

但是呢，这个方案还是可能存在问题：

- 问题一：**锁过期释放了，业务还没执行完**。假设线程a获取锁成功，一直在执行临界区的代码。但是100s过去后，它还没执行完。但是，这时候锁已经过期了，此时线程b又请求过来。显然线程b就可以获得锁成功，也开始执行临界区的代码。那么问题就来了，临界区的业务代码都不是严格串行执行的啦。
- 问题二：**锁被别的线程误删**。假设线程a执行完后，去释放锁。但是它不知道当前的锁可能是线程b持有的（线程a去释放锁时，有可能过期时间已经到了，此时线程b进来占有了锁）。那线程a就把线程b的锁释放掉了，但是线程b临界区业务代码可能都还没执行完呢。

**问题一：锁被别人释放，尝试解决：添加请求ID来标识当前锁，自能释放自己请求ID的锁**

**问题二：未执行完，锁自己释放了，尝试解决：开启异步监视线程，检测锁时间与线程运行状态，自动续杯**



### ④Redisson

上一个方案还是可能存在**锁过期释放，业务没执行完**的问题。有些小伙伴认为，稍微把锁过期时间设置长一些就可以啦。其实我们设想一下，是否可以给获得锁的线程，开启一个定时守护线程，每隔一段时间检查锁是否还存在，存在则对锁的过期时间延长，防止锁过期提前释放。

当前开源框架Redisson解决了这个问题。我们一起来看下Redisson底层原理图吧：

<img src="https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/367cd1a7a3fb4d398988e4166416d71d~tplv-k3u1fbpfcp-zoom-in-crop-mark:4536:0:0:0.image" alt="img" style="zoom:50%;" />

只要线程一加锁成功，就会启动一个`watch dog`看门狗，它是一个后台线程，会每隔10秒检查一下，如果线程1还持有锁，那么就会不断的延长锁key的生存时间。因此，Redisson就是使用Redisson解决了**锁过期释放，业务没执行完**问题。

**完美！但美中不足！Redisson解决了上述的问题：锁被别人释放了+未执行完自动释放的问题，但是现在新的问题来了，集群状态下，如何保证分布式锁的安全，因为master负责写+加锁，slave负责读，万一master挂了，加的锁不久丢了吗？**

**尝试解决：利用下面的RedLock+Redisson模式解决**



### ⑤RedLock+Redisson

###### 多机实现的分布式锁Redlock+Redisson

Redis一般都是集群部署的：

<img src="https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/7349794feeee458aa71c27f27a0b2428~tplv-k3u1fbpfcp-zoom-in-crop-mark:4536:0:0:0.image" alt="img" style="zoom:50%;" />

如果线程一在Redis的master节点上拿到了锁，但是加锁的key还没同步到slave节点。恰好这时，master节点发生故障，一个slave节点就会升级为master节点。线程二就可以获取同个key的锁啦，但线程一也已经拿到锁了，锁的安全性就没了。

为了解决这个问题，Redis作者 antirez提出一种高级的分布式锁算法：Redlock。Redlock核心思想是这样的：

> 搞多个Redis master部署，以保证它们不会同时宕掉。并且这些master节点是完全相互独立的，相互之间不存在数据同步。同时，需要确保在这多个master实例上，是与在Redis单实例，使用相同方法来获取和释放锁。

我们假设当前有5个Redis master节点，在5台服务器上面运行这些Redis实例。

<img src="https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/0df0a36c7ccd439291a8a869ff4ddad3~tplv-k3u1fbpfcp-zoom-in-crop-mark:4536:0:0:0.image" alt="img" style="zoom:50%;" />

RedLock的实现步骤:如下

> - 1.获取当前时间，以毫秒为单位。
> - 2.按顺序向5个master节点请求加锁。客户端设置网络连接和响应超时时间，并且超时时间要小于锁的失效时间。（假设锁自动失效时间为10秒，则超时时间一般在5-50毫秒之间,我们就假设超时时间是50ms吧）。如果超时，跳过该master节点，尽快去尝试下一个master节点。
> - 3.客户端使用当前时间减去开始获取锁时间（即步骤1记录的时间），得到获取锁使用的时间。当且仅当超过一半（N/2+1，这里是5/2+1=3个节点）的Redis master节点都获得锁，并且使用的时间小于锁失效时间时，锁才算获取成功。（如上图，10s> 30ms+40ms+50ms+4m0s+50ms）
> - 如果取到了锁，key的真正有效时间就变啦，需要减去获取锁所使用的时间。
> - 如果获取锁失败（没有在至少N/2+1个master实例取到锁，有或者获取锁时间已经超过了有效时间），客户端要在所有的master节点上解锁（即便有些master节点根本就没有加锁成功，也需要解锁，以防止有些漏网之鱼）。

简化下步骤就是：

- 按顺序向5个master节点请求加锁
- 根据设置的超时时间来判断，是不是要跳过该master节点。
- 如果大于等于三个节点加锁成功，并且使用的时间小于锁的有效期，即可认定加锁成功啦。
- 如果获取锁失败，解锁！



### 补：lua

方案一二三的分布式锁出现的问题，可以尝试利用lua脚本结局

先挖个坑，以后学习lua脚本再来填坑。



### ⑥其他分布式锁：Zookeeper

先挖个坑



### 分布式锁总结：

分布式系统离不开CAP理论，我们最多只能达到CP或AP

- CP：一致性
- AP：可用性

在分布式锁的场景下

- Redis分布式锁就是AP，保证了高可用（在内存中），但损失了一致性

- Zookeeper就是CP，保证了一致性（在磁盘中），但可用性有一定损失
- 其实在日常开发中，我们推荐使用AP即Redis，因为就算不一致，我们可以通过最终一致性方案来解决，但是不可用的导致系统问题，那就是没办法解决了。



## 六、Redis作为缓存要注意什么问题？

### 缓存降级

缓存降级是指缓存失效或缓存服务器挂掉的情况下，不去访问数据库，直接返回默认数据或访问服务的内存数据。

在项目实战中通常会将部分热点数据缓存到服务的内存中，这样一旦缓存出现异常，可以直接使用服务的内存数据，从而避免数据库遭受巨大压力。

降级一般是有损的操作，所以尽量减少降级对于业务的影响程度。



### 缓存预热

缓存预热就是系统上线后，将相关的缓存数据直接加载到缓存系统，这样就可以避免在用户请求的时候，先查询数据库，然后再将数据回写到缓存。

如果不进行预热， 那么 Redis 初始状态数据为空，系统上线初期，对于高并发的流量，都会访问到数据库中， 对数据库造成流量的压力。

#### 缓存预热的操作方法

* 数据量不大的时候，工程启动的时候进行加载缓存动作；
* 数据量大的时候，设置一个定时任务脚本，进行缓存的刷新；
* 数据量太大的时候，优先保证热点数据进行提前加载到缓存。



### 缓存雪崩

#### 什么是缓存雪崩？

缓存雪崩是指缓存中数据大批量到过期时间，而查询数据量巨大，请求直接落到数据库上，引起数据库压力过大甚至宕机。和缓存击穿不同的是，缓存击穿指并发查同一条数据，缓存雪崩是不同数据都过期了，很多数据都查不到从而查数据库。

#### 缓存雪崩解决方案

常用的解决方案有：

* 均匀过期
* 加互斥锁
* 缓存永不过期
* 双层缓存策略

（1）均匀过期

设置不同的过期时间，让缓存失效的时间点尽量均匀。通常可以为有效期增加随机值或者统一规划有效期。

（2）加互斥锁

跟缓存击穿解决思路一致，同一时间只让一个线程构建缓存，其他线程阻塞排队。

（3）缓存永不过期

跟缓存击穿解决思路一致，缓存在物理上永远不过期，用一个异步的线程更新缓存。

（4）双层缓存策略

使用主备两层缓存：

主缓存：有效期按照经验值设置，设置为主读取的缓存，主缓存失效后从数据库加载最新值。

备份缓存：有效期长，获取锁失败时读取的缓存，主缓存更新时需要同步更新备份缓存。



### 缓存穿透

#### 什么是缓存穿透？

缓存穿透是指用户请求的数据在**缓存中不存在即没有命中，同时在数据库中也不存在，导致用户每次请求该数据都要去数据库中查询一遍，然后返回空**。

如果有恶意攻击者不断请求系统中不存在的数据，会导致短时间大量请求落在数据库上，造成数据库压力过大，甚至击垮数据库系统。

#### 缓存穿透常用的解决方案

**（1）布隆过滤器（推荐）**

布隆过滤器（Bloom Filter，简称BF）由Burton Howard Bloom在1970年提出，是一种空间效率高的概率型数据结构。

**布隆过滤器专门用来检测集合中是否存在特定的元素。**

如果在平时我们要判断一个元素是否在一个集合中，通常会采用查找比较的方法，下面分析不同的数据结构查找效率：

* 采用线性表存储，查找时间复杂度为O(N)
* 采用平衡二叉排序树（AVL、红黑树）存储，查找时间复杂度为O(logN)
* 采用哈希表存储，考虑到哈希碰撞，整体时间复杂度也要O[log(n/m)]

当需要判断一个元素是否存在于海量数据集合中，不仅查找时间慢，还会占用大量存储空间。接下来看一下布隆过滤器如何解决这个问题。

**布隆过滤器设计思想**

布隆过滤器由一个长度为m比特的位数组（bit array）与k个哈希函数（hash function）组成的数据结构。位数组初始化均为0，所有的哈希函数都可以分别把输入数据尽量均匀地散列。

当要向布隆过滤器中插入一个元素时，该元素经过k个哈希函数计算产生k个哈希值，以哈希值作为位数组中的下标，将所有k个对应的比特值由0置为1。

当要查询一个元素时，同样将其经过哈希函数计算产生哈希值，然后检查对应的k个比特值：如果有任意一个比特为0，表明该元素一定不在集合中；如果所有比特均为1，表明该集合有可能性在集合中。为什么不是一定在集合中呢？因为不同的元素计算的哈希值有可能一样，会出现哈希碰撞，导致一个不存在的元素有可能对应的比特位为1，这就是所谓“假阳性”（false positive）。相对地，“假阴性”（false negative）在BF中是绝不会出现的。

总结一下：**布隆过滤器认为不在的，一定不会在集合中；布隆过滤器认为在的，可能在也可能不在集合中**。

举个例子：下图是一个布隆过滤器，共有18个比特位，3个哈希函数。集合中三个元素x，y，z通过三个哈希函数散列到不同的比特位，并将比特位置为1。当查询元素w时，通过三个哈希函数计算，发现有一个比特位的值为0，可以肯定认为该元素不在集合中。

<div align="center">  <img src="https://cdn.jsdelivr.net/gh/SmileLionCoder/assets@main/202010/20201025213820.png" width="500"/> </div><br>

**布隆过滤器优缺点**

优点：

* 节省空间：不需要存储数据本身，只需要存储数据对应hash比特位
* 时间复杂度低：插入和查找的时间复杂度都为O(k)，k为哈希函数的个数

缺点：

* 存在假阳性：布隆过滤器判断存在，可能出现元素不在集合中；判断准确率取决于哈希函数的个数
* 不能删除元素：如果一个元素被删除，但是却不能从布隆过滤器中删除，这也是造成假阳性的原因了

**布隆过滤器适用场景**

* 爬虫系统url去重
* 垃圾邮件过滤
* 黑名单

**（2）返回空对象**

当缓存未命中，查询持久层也为空，可以将返回的空对象写到缓存中，这样下次请求该key时直接从缓存中查询返回空对象，请求不会落到持久层数据库。为了避免存储过多空对象，通常会给空对象设置一个过期时间。

这种方法会存在两个问题：

* 如果有大量的key穿透，缓存空对象会占用宝贵的内存空间。
* 空对象的key设置了过期时间，在这段时间可能会存在缓存和持久层数据不一致的场景。



### 缓存击穿

#### 什么是缓存击穿？

缓存击穿，是指一个key非常热点，在不停的扛着大并发，大并发集中对这一个点进行访问，当这个key在失效的瞬间，持续的大并发就穿破缓存，直接请求数据库，就像在一个屏障上凿开了一个洞。



#### 缓存击穿危害

数据库瞬时压力骤增，造成大量请求阻塞。

#### 如何解决

**使用互斥锁（mutex key）**

这种思路比较简单，就是让一个线程回写缓存，其他线程等待回写缓存线程执行完，重新读缓存即可。

<div align="center">  <img src="https://cdn.jsdelivr.net/gh/SmileLionCoder/assets@main/202010/20201025213939.png" width="500"/> </div><br>

同一时间只有一个线程读数据库然后回写缓存，其他线程都处于阻塞状态。如果是高并发场景，大量线程阻塞势必会降低吞吐量。这种情况如何解决？大家可以在留言区讨论。

如果是分布式应用就需要使用分布式锁。

**热点数据永不过期**

永不过期实际包含两层意思：

* 物理不过期，针对热点key不设置过期时间
* 逻辑过期，把过期时间存在key对应的value里，如果发现要过期了，通过一个后台的异步线程进行缓存的构建

<div align="center">  <img src="https://cdn.jsdelivr.net/gh/SmileLionCoder/assets@main/202010/20201025213959.png" width="500"/> </div><br>

从实战看这种方法对于性能非常友好，唯一不足的就是构建缓存时候，其余线程(非构建缓存的线程)可能访问的是老数据，对于不追求严格强一致性的系统是可以接受的。

## 





## 七、Redis与数据库的一致性问题



### ①先删除缓存，再更新数据库

先删除后更新：指先将redis中的数据删除，再更新数据库中的数据，借助下次redis读取时没有，从数据库读入的过程，来保证redis的数据一致性

### ②先更新数据库，再更新缓存

先更新再更新：指先更新数据库，在更新redis

### ③延迟双删

先删除redis缓存，再更新mysql，延迟几百毫秒后再删除redis缓存，这样及时再我们更新mysql时，有其他线程读取了mysql中未修改的数据缓存到redis中，redis中的数据也会被删除，保持了数据的一致性

### ④最终一致性方案

- 借助第三方，在redis更新失败的时候，自旋更新
- 或者监控MySQL日志，持续更新redis





## 八、主从复制

主从复制，是指将一台Redis服务器的数据，复制到其他的Redis服务器。前者称为主节点(master)，后者称为从节点(slave)

数据的复制是单向的，只能由主节点到从节点。

默认情况下，每台Redis服务器都是主节点，且一个主节点可以有多个从节点(或没有从节点)，但一个从节点只能有一个主节点。

目前很多中小企业都没有使用到 Redis 的集群，但是至少都做了主从。有了主从，当 master 挂掉的时候，运维让从库过来接管，服务就可以继续，否则 master 需要经过数据恢复和重启的过程，这就可能会拖很长的时间，影响线上业务的持续服务。

Redis主从复制服务器架构图如下：

![img](https://pic4.zhimg.com/80/v2-734db98a54d6141470ebd3d367f90793_720w.webp)

### 主从复制的作用

主从复制的作用主要包括：

- **数据冗余**：主从复制实现了数据的热备份，是**持久化之外的一种数据冗余方式**。
- **故障恢复**：当主节点出现问题时，可以由从节点提供服务，**实现快速的故障恢复**；实际上是一种服务的冗余。
- **负载均衡**：在主从复制的基础上，配合**读写分离**，可以由**主节点提供写服务，由从节点提供读服务**（即写Redis数据时应用连接主节点，读Redis数据时应用连接从节点），分担服务器负载；尤其是在写少读多的场景下，通过多个从节点分担读负载，可以大大提高Redis服务器的并发量。
- **高可用基石**：主从复制还是哨兵和集群能够实施的基础，因此说**主从复制是Redis高可用的基础**。



**Redis主从复制是最求最终一致性的**：

Redis 的主从数据是异步同步的，所以分布式的 Redis 系统并不满足「**一致性**」要求。当客户端在 Redis 的主节点修改了数据后，立即返回，即使在主从网络断开的情况下，主节点依旧可以正常对外提供修改服务，所以 Redis 满足「**可用性**」。

Redis 保证「**最终一致性**」，从节点会努力追赶主节点，最终从节点的状态会和主节点的状态将保持一致。如果网络断开了，主从节点的数据将会出现大量不一致，一旦网络恢复，从节点会采用多种策略努力追赶上落后的数据，继续尽力保持和主节点一致



### 主从复制的实现原理

总的来说主从复制功能的详细步骤可以分为7个步骤：

- 设置主节点的地址和端口
- 建立套接字连接
- 发送PING命令
- 权限验证
- 同步
- 命令传播

整个流程图如下：



![img](https://pic3.zhimg.com/80/v2-95d0e5d7b2f6223f93b53c5ac30b7196_720w.webp)

基本步骤不再详细介绍，详细可以查看链接：https://zhuanlan.zhihu.com/p/151740247

主要介绍最后两个步骤：

#### **同步**

同步就是将从节点的数据库状态更新成主节点当前的数据库状态。具体执行的方式是：从节点向主节点发送psync命令（Redis2.8以前是sync命令），开始同步。 数据同步阶段是主从复制最核心的阶段，根据主从节点当前状态的不同，可以分为**全量复制**和**部分复制**



##### **全量复制和部分复制**

在Redis2.8以前，从节点向主节点发送sync命令请求同步数据，此时的同步方式是全量复制；在Redis2.8及以后，从节点可以发送psync命令请求同步数据，此时根据主从节点当前状态的不同，同步方式可能是全量复制或部分复制。后文介绍以Redis2.8及以后版本为例。

1. **全量复制**：用于**初次复制**或**其他无法进行部分复制**的情况，将主节点中的所有数据都发送给从节点，是一个非常重型的操作。
2. **部分复制**：用于**网络中断等情况后的复制**，只将中断期间主节点执行的写命令发送给从节点，与全量复制相比更加高效。需要注意的是，**如果网络中断时间过长，导致主节点没有能够完整地保存中断期间执行的写命令，则无法进行部分复制，仍使用全量复制**。

 

##### **全量复制**

Redis通过psync命令进行全量复制的过程如下：

（1）从节点判断无法进行部分复制，向主节点发送全量复制的请求；或从节点发送部分复制的请求，但主节点判断无法进行部分复制；具体判断过程需要在讲述了部分复制原理后再介绍。

（2）主节点收到全量复制的命令后，执行bgsave，在后台生成RDB文件，并使用一个缓冲区（称为复制缓冲区）记录从现在开始执行的所有写命令

（3）主节点的bgsave执行完成后，将RDB文件发送给从节点；从节点首先清除自己的旧数据，然后载入接收的RDB文件，将数据库状态更新至主节点执行bgsave时的数据库状态

（4）主节点将前述复制缓冲区中的所有写命令发送给从节点，从节点执行这些写命令，将数据库状态更新至主节点的最新状态

（5）如果从节点开启了AOF，则会触发bgrewriteaof的执行，从而保证AOF文件更新至主节点的最新状态



![img](https://pic1.zhimg.com/80/v2-ac90b3fed515a99b8517b578d3c61994_720w.webp)



通过全量复制的过程可以看出，全量复制是非常重型的操作：

（1）主节点通过bgsave命令fork子进程进行RDB持久化，该过程是非常消耗CPU、内存(页表复制)、硬盘IO的；关于bgsave的性能问题，可以参考我另外一篇文章： 深入剖析Redis高可用系列：持久化 AOF和RDB

（2）主节点通过网络将RDB文件发送给从节点，对主从节点的带宽都会带来很大的消耗

（3）从节点清空老数据、载入新RDB文件的过程是阻塞的，无法响应客户端的命令；如果从节点执行bgrewriteaof，也会带来额外的消耗



##### **部分复制**

由于全量复制在主节点数据量较大时效率太低，因此Redis2.8开始提供部分复制，用于处理网络中断时的数据同步。

部分复制的实现，依赖于三个重要的概念：

1. 复制偏移量
2. 复制积压缓冲区
3. 服务器运行ID(runid)
   下面我们分别讲解一下这三个概念：

1. **复制偏移量**：

执行复制的双方，主从节点，分别会维护一个复制偏移量offset： 主节点每次向从节点同步了N字节数据后，将修改自己的复制偏移量offset+N 从节点每次从主节点同步了N字节数据后，将修改自己的复制偏移量offset+N

offset用于判断主从节点的数据库状态是否一致： 如果二者offset相同，则一致； 如果offset不同，则不一致，此时可以根据两个offset找出从节点缺少的那部分数据。

例如，如果主节点的offset是1000，而从节点的offset是500，那么部分复制就需要将offset为501-1000的数据传递给从节点。而offset为501-1000的数据存储的位置，就是下面要介绍的复制积压缓冲区。

 2.**复制积压缓冲区**：

主节点内部维护了一个**固定长度的、先进先出(FIFO)队列** 作为复制积压缓冲区，其默认大小为1MB 在主节点进行命令传播时，不仅会将写命令同步到从节点，还会将写命令写入复制积压缓冲区。

由于复制积压缓冲区定长且是先进先出，所以它保存的是主节点最近执行的写命令；时间较早的写命令会被挤出缓冲区。因此，**当主从节点offset的差距过大超过缓冲区长度时，将无法执行部分复制，只能执行全量复制。**

为了提高网络中断时**部分复制**执行的概率，可以根据需要增大复制积压缓冲区的大小(通过配置repl-backlog-size)；例如如果网络中断的平均时间是60s，而主节点平均每秒产生的写命令(特定协议格式)所占的字节数为100KB，则复制积压缓冲区的平均需求为6MB，保险起见，可以设置为12MB，来保证绝大多数断线情况都可以使用部分复制。

从节点将offset发送给主节点后，主节点根据offset和缓冲区大小决定能否执行部分复制：

- 如果offset偏移量之后的数据，仍然都在复制积压缓冲区里，则执行**部分复制**；
- 如果offset偏移量之后的数据已不在复制积压缓冲区中（数据已被挤出），则执行**全量复制**。

复制积压缓冲区示意图：



![img](https://pic4.zhimg.com/80/v2-3aa9eb4881fb69142204c764ceae7b5f_720w.webp)



3. **服务器运行ID(runid)**：

每个Redis节点，都有其运行ID，运行ID由节点在启动时自动生成，主节点会将自己的运行ID发送给从节点，从节点会将主节点的运行ID存起来。 从节点Redis断开重连的时候，就是根据运行ID来判断同步的进度：

- 如果从节点保存的runid与主节点现在的runid相同，说明主从节点之前同步过，主节点会继续尝试使用部分复制(到底能不能部分复制还要看offset和复制积压缓冲区的情况)；
- 如果从节点保存的runid与主节点现在的runid不同，说明从节点在断线前同步的Redis节点并不是当前的主节点，只能进行全量复制。



#### **命令传播**

经过上面同步操作，此时主从的数据库状态其实已经一致了，但这种一致的状态的并不是一成不变的。 在完成同步之后，也许主服务器马上就接受到了新的写命令，执行完该命令后，主从的数据库状态又不一致。

数据同步阶段完成后，主从节点进入命令传播阶段；在这个阶段主节点将自己执行的写命令发送给从节点，从节点接收命令并执行，从而保证主从节点数据的一致性。

另外命令转播我们需要关注两个点： **延迟与不一致** 和 **心跳机制** 我们下面介绍一下



##### **延迟与不一致** 

需要注意的是，命令传播是**异步**的过程，即主节点发送写命令后并不会等待从节点的回复；因此实际上主从节点之间很难保持实时的一致性，延迟在所难免。数据不一致的程度，与主从节点之间的网络状况、主节点写命令的执行频率、以及主节点中的**repl-disable-tcp-nodelay配置**等有关。

- repl-disable-tcp-nodelay 配置如下：

  - 假如设置成yes，则redis会合并小的TCP包从而节省带宽，但会增加同步延迟（40ms），造成master与slave数据不一致

  - 假如设置成no，则redis master会立即发送同步数据，没有延迟

- 概括来说就是：**前者关注性能，后者关注一致性**

- 具体发送频率与Linux内核的配置有关，默认配置为40ms。当设置为no时，TCP会立马将主节点的数据发送给从节点，带宽增加但延迟变小。

- 一般来说，只有当应用对Redis数据不一致的容忍度较高，且主从节点之间网络状况不好时，才会设置为yes；多数情况使用默认值no



**Redis是如何保证主从服务器一致处于连接状态以及命令是否丢失？** 

答：命令传播阶段，从服务器会利用心跳检测机制定时的向主服务发送消息。 



##### **心跳机制**

- 心跳检测机制的作用有三个：
  - 检查主从服务器的网络连接状态
  - 辅助实现min-slaves选项
  - 检测命令丢失



**1.检查主从服务器的网络连接状态**

主节点信息中可以看到所属的从节点的连接信息：

- state 表示从节点状态
- offset 表示复制偏移量
- lag 表示延迟值（几秒之前有过心跳检测机制）



**2.辅助实现min-slaves选项**

Redis.conf配置文件中有下方两个参数

```text
# 未达到下面两个条件时，写操作就不会被执行
# 最少包含的从服务器
# min-slaves-to-write 3
# 延迟值
# min-slaves-max-lag 10
```

如果将两个参数的注释取消，那么如果从服务器的数量少于3个，或者三个从服务器的延迟（lag）大于等于10秒时，主服务器都会拒绝执行写命令。



**3.检测命令丢失**

在从服务器的连接信息中可以看到复制偏移量，如果此时主服务器的复制偏移量与从服务器的复制偏移量不一致时，主服务器会补发缺失的数据。





## 九、哨兵模式

哨兵模式是一种特殊的模式，首先[Redis](https://cloud.tencent.com/product/crs?from=10680)提供了哨兵的命令，哨兵是一个独立的进程，作为进程，它会独立运行。其原理是`哨兵通过发送命令`，`等待Redis服务器响应`，`从而监控运行的多个Redis实例`

<img src="https://ask.qcloudimg.com/http-save/2736799/7efc1b19ffb9a5577c37f04c2b6471ce.png?imageView2/2/w/1620" alt="img" style="zoom:67%;" />

这里的哨兵有两个作用

- 通过发送命令，让Redis服务器返回监控其运行状态，包括主服务器和从服务器。
- 当哨兵监测到master宕机，会自动将slave切换成master，然后通过发布订阅模式通知其他的从服务器，修改配置文件，让它们切换主机。

然而一个哨兵进程对Redis服务器进行监控，可能会出现问题，为此，我们可以使用多个哨兵进行监控。各个哨兵之间还会进行监控，这样就形成了多哨兵模式。

**故障切换（failover）的过程**：

假设主服务器宕机，哨兵1先检测到这个结果，系统并不会马上进行failover过程，仅仅是哨兵1主观的认为主服务器不可用，这个现象成为主观下线。当后面的哨兵也检测到主服务器不可用，并且数量达到一定值时，那么哨兵之间就会进行一次投票，投票的结果由一个哨兵发起，进行failover操作。切换成功后，就会通过发布订阅模式，让各个哨兵把自己监控的从服务器实现切换主机，这个过程称为客观下线。这样对于客户端而言，一切都是透明的。



### 优点

- 哨兵集群, 基于主从复制模式, 所有主从配置优点, 它全有
- 主从可以切换, 故障可以转移, 系统可用性就会很好
- 哨兵模式就是主从模式的升级, 手动到自动, 更加健壮

### 缺点

- redis 不好在线扩容, 集群容量一旦到达上限, 在线扩容就十分的麻烦
- 实现哨兵模式的配置其实是很麻烦的, 里面有很多选择



## 十、Redis如何做到集群

### 集群是什么

Redis 集群是 Redis 提供的分布式[数据库](https://cloud.tencent.com/solution/database?from=10680)方案，集群通过分片( sharding )来实现数据共享，并提供复制和故障转移。

可以说上面这句话是对 redis 集群的高度概括了，redis 集群提供[分布式数据库](https://cloud.tencent.com/product/tddbms?from=10680)方案，前面我们将的主从复制和哨兵模式可以知道，只会有一个主[服务器](https://cloud.tencent.com/product/cvm?from=10680)( master )。**主从复制，只会有一个 master ，可以有多个 slave**。而**哨兵模式是在主从复制的基础上，发现 master 挂掉，会自动的将其中一个 salve 升级成 master** 。但是最终结果还是只有一个 master。所以如果系统很大，对Redis 写操作的压力就会很大，所以才出现的集群的模式。**集群模式可以有多个 master** 。

<img src="https://ask.qcloudimg.com/http-save/6834658/odibwi7mbw.png?imageView2/2/w/1620" alt="img" style="zoom: 80%;" />



### 怎么做到集群

#### 方法一：修改config文件

参考链接：https://cloud.tencent.com/developer/article/1592432

#### 项目中使用Redis集群

参考链接：https://cloud.tencent.com/developer/article/1592432











## 九、刁钻面试题

### ①Redis的Sorted的zset，为什么要采用dict + skipList?

- dict用于通过数据查询排名
- skipList用于通过排名查询数据（包括范围查询）
- 之所以使用skipList的原因是因为，skipList相当于是二分查找，查询速度更快，而dict是一个key-value映射，查询需要遍历全部数据
- 既然我们说skipList用于二分查找，那么我们为什么不采用红黑树呢？
  - 这里我们就需要考虑Redis的Sorted Set需要实现哪些功能：
    - 排序
    - 查询
    - 插入
    - 修改
    - 删除
    - 去重
    - 顺序输出全部数据
    - 范围输出
  - 那么我们可以得出，除了范围输出功能外，红黑树的效率其实与跳表是一样的，但是在范围查询这个功能上，我们可以很容易就得知，跳表的范围查询效率更高，红黑树不能实现范围查询。

### ②Redis的效率为什么这么高？

- 单线程，避免多线程频繁的上下文切换问题，预防了多线程可能的竞争问题
- IO多路复用
- 内存操作
- C语言编写，语言更接近操作系统，执行速度相对较快



# 一些参考文章

Redis应用场景：https://www.cnblogs.com/cye9971-/p/11267431.html

Redis两层数据结构：https://blog.csdn.net/a745233700/article/details/113449889

Redis线程模型：https://juejin.cn/post/6844903970511519758 ， https://www.cnblogs.com/mrmirror/p/13587311.html

Redis分布式锁：https://www.bilibili.com/video/BV1wW4y177WN/?spm_id_from=333.337.search-card.all.click&vd_source=cb5cce1bdc5ab99fce812684e0c31a2b

Redis主从复制：https://zhuanlan.zhihu.com/p/151740247

Redis哨兵模式：https://cloud.tencent.com/developer/article/1865872

Redis集群：https://cloud.tencent.com/developer/article/1592432