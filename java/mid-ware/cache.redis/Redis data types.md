# An introduction to Redis data types

Redis并非一个单纯的key-value存储工具，官方说实际上它是一个数据结构服务器(*data-structure server*)，支持不同数据类型的值。意思是，在传统的key-value存储中，我们存储字符串类型的key和字符串类型的value关联起来组成的键值对，但是在redis中，值并不局限于字符串类型，redis可以支持更复杂的数据结构。例如：

- Binary-safe strings - 二进制安全的字符串

- Lists - 列表

  根据插入顺序的有序字符串集合。

- Sets - 集合

  无序但是不重复的字符串集合。

- Sorted sets - 有序集合

  与Set类似，但是每个字符串元素都和一个浮点数类型的score关联。读取数据时数据元素按照score。所以相比Set，Sorted set支持我们读取指定范围内的数据。

- Hashes - 哈希

  属性和属性值对组成的映射(maps composed of fields associated with values)。属性和属性值都是字符串。与Ruby或者Python的hash很类似。

- Bit arrays (or simply bitmaps)

  支持使用特殊命令像操作位数组一样操作字符串数据。例如我们可以设置或者清空某一位，统计所有设置为1的位数，查找第一个设置为1或0的位等等。

- HyperLogLogs

  This is a probabilistic data structure which is used in order to estimate the cardinality of a set.

## Redis keys

Redis的key值是二进制安全的，这意味着可以用任何二进制序列作为key值，从形如"foo"的简单字符串到一个JPEG文件的内容都可以。空字符串也是有效的key值。key的一些其他规则：

- 不推荐太长的key。例如1024字节的键值就不是个好主意，不仅因为消耗内存，而且在数据集中查找这类key值的计算成本很高。如果我们确实需要查找一个很大的值，可以使用hash函数计算其散列值，以节省内存和带宽。
- 也不推荐太短的key。例如用"user:1000:password"来代替"u:1000:pwd"，但后者更易阅读，由此增加的空间消耗相对于key object和value object本身来说很小。我们需要在这两者之间做权衡。
- 采用固定的命名模式。例如在多个单词之间使用:或者-等分隔符。
- Redis支持的key最大为512MB。

## Redis Strings

String是Redis支持的最简单的数据类型，Memcached只支持String。String数据类型可以用于很多场景，例如缓存HTML片段或者整个网页。最简单的使用样例：

```
127.0.0.1:6379> set mykey myvalue
OK
127.0.0.1:6379> get mykey
"myvalue"
```

```
127.0.0.1:6379> mset mykey1 myvalue1 mykey2 myvalue2 mykey3 myvalue3
OK
127.0.0.1:6379> mget mykey1 mykey2 mykey3
1) "myvalue1"
2) "myvalue2"
3) "myvalue3"
```

如果string类型的value可以被转换为数字，则可以对其进行原子加减运算。incr/incrby/decr/decrby命令都是原子性的，例如当有N个线程并发使用incr命令操作key的原始值M时，得到的最终结果可以保证为M+N：

```
127.0.0.1:6379> set numkey 100
OK
127.0.0.1:6379> incr numkey
(integer) 101
127.0.0.1:6379> incr numkey
(integer) 102
127.0.0.1:6379> incrby numkey 100
(integer) 202
127.0.0.1:6379> decrby numkey 100
(integer) 102
127.0.0.1:6379> decr numkey
(integer) 101
127.0.0.1:6379> decr numkey
(integer) 100
```

## Redis Lists

从最一般的定义上来说，List仅仅只是有序元素的一个序列，但是和Java中经典的两种List实现一样，使用数组和链表实现的List在表现上会有很大不同。Redis的List由链表实现，这意味着无论对于多大的链表，在其头部或尾部添加元素的时间都是恒定的。The speed of adding a new element with the LPUSH command to the head of a list with ten elements is the same as adding an element to the head of list with 10 million elements. 但缺点是，适用下标或者说索引检索元素会相对较慢。

Redis之所以使用LinkedList实现是因为对于一个数据库系统来说，快速的在一个很大的列表末尾插入元素的能力是很重要的，另一个优点是，Redis Lists can be taken at constant length in constant time. 如果要求对List中间元素的快速检索，可以使用Sorted set。

LPUSH和RPUSH分别用于在一个List的左侧（头部）和右侧（尾部）插入元素，这两个命令都支持可变数量参数。LRANGE命令用于检索List某一下表范围内的元素。例如：

```
127.0.0.1:6379> lpush mylist list:value:1
(integer) 1
127.0.0.1:6379> lpush mylist list:value:2
(integer) 2
127.0.0.1:6379> lpush mylist list:value:3
(integer) 3
127.0.0.1:6379> rpush mylist list:value:0
(integer) 4
127.0.0.1:6379> rpush mylist list:value:-1
(integer) 5
127.0.0.1:6379> rpush mylist list:value:-2
(integer) 6
127.0.0.1:6379> rpush mylist list:value:-3 list:value:-4 list:value:-5
(integer) 9
127.0.0.1:6379> lrange mylist 0 -1
1) "list:value:3"
2) "list:value:2"
3) "list:value:1"
4) "list:value:0"
5) "list:value:-1"
6) "list:value:-2"
7) "list:value:-3"
8) "list:value:-4"
9) "list:value:-5"
```

LRANGE的两个参数分别代表下标的起始和结束位置，均可以为负数，-1就表示最后一个元素，-2表示倒数第二个元素，等等。List最多可以存储2<sup>32</sup>-1个元素，相当于40多亿。

另外List的一个很重要的命令是LPOP/RPOP，用于删除并返回列表头部/尾部的第一个元素：

```
127.0.0.1:6379> lpush mylist list:value:1
(integer) 1
127.0.0.1:6379> rpush mylist list:value:-1
(integer) 2
127.0.0.1:6379> lrange mylist 0 -1
1) "list:value:1"
2) "list:value:-1"
127.0.0.1:6379> lpop mylist
"list:value:1"
127.0.0.1:6379> lrange mylist 0 -1
1) "list:value:-1"
127.0.0.1:6379> rpop mylist
"list:value:-1"
127.0.0.1:6379> lrange mylist 0 -1
(empty list or set)
```

以下是List的两个典型应用场景：

- Remeber the latest updates posted by users into a social network;
- Communication between processes, using a consumer-producer pattern where the producer pushes items into a list, and a consumer consumes those items and executed actions. Redis has special list commands to make this use case both more reliable and efficient.

例如在社交网络，如推特中，首页需要展示用户最近发的十张照片，我们可以这样实现：

- 用户每发一张照片，在就使用LPUSH将图片（或链接）保存在List中;
- 用户访问首页时，使用LRANGE 0 9 读取最近的10张图.

在很多情况下我们只需要保存最近的数据，Redis使我们能够像使用capped collection一样的方式使用List。通过LTRIM命令实现仅保存最近N个元素并丢弃老元素。LTRIM与LRANGE命令有相似之处，但不同于LRANGE，LTRIM会为List设定元素下标的边界，超出边界的元素会被移除，例如：

```
127.0.0.1:6379> rpush mylist list:value:1 list:value:2 list:value:3 list:value:4 list:value:5
(integer) 5
127.0.0.1:6379> lrange mylist 0 -1
1) "list:value:1"
2) "list:value:2"
3) "list:value:3"
4) "list:value:4"
5) "list:value:5"
127.0.0.1:6379> ltrim mylist 1 3
OK
127.0.0.1:6379> lrange mylist 0 -1
1) "list:value:2"
2) "list:value:3"
3) "list:value:4"
```

通过PUSH命令和TRIM命令的组合我们可以实现一个很有用的功能。例如可以在List中保存最新的1000条数据：

```
LPUSH mylist <elements>
LTRIM mylist 0 999
```

注意，LRANGE命令的时间复杂度为o(n)，但是从列表的头部或者尾部方向检索较少数量元素的操作花费的时间可以看做为常量。

列表天然具有一些可以用来实现队列的特性，一般可以作为实现内部进程通信系统：阻塞队列的基石。假设我们想通过一个进程向队列推送数据，然后用其他进程实际处理这些数据。这是一个很一般的生产者/消费者模型，可以简单实现为：

- 生产者通过LPUSH向列表中推送数据
- 消费者通过RPOP从队列中取数据消费

当队列为空时，RPOP返回NULL。在这种情况下消费者需要等待一段时间后重试，也就是轮询。在当前场景下这种处理方式有如下缺点：

1. 强制Redis服务器和客户端进行大量无效命令交互。当List为空时，我们可以认为所有轮询都是浪费的。
2. 由于消费端接收到NULL时需要等待，可以认为消费者对数据的处理出现了延迟。如果通过增加轮询频率减少这个延迟的话，相当于加重了问题1。

为了解决上述问题，Redis实现了RPOP和LPOP命令的阻塞版：BRPOP和BLPOP。当List为空时，调用这两个命令会阻塞直到有新数据存入List或者超过用户指定的超时时间。例如：

```
127.0.0.1:6379> brpop my:blocking:list 100
1) "my:blocking:list"
2) "blocking"
(66.71s)
```

我们通过brpop命令访问一个不存在的列表my:blocking:list，设置超时时间为100秒，执行此命令后会阻塞，然后我们通过另外一个客户端连接会话为其推送数据：

```
127.0.0.1:6379> lpush my:blocking:list blocking
(integer) 1
```

推送数据成功后阻塞的会话会立即返回上述的值，并会附带阻塞的时长。

超时时间的单位为秒，当超时时间设置为0时表示我们可以容忍客户端永远等待。BRPOP和BLPOP可以接收多个List参数，表示我们可以阻塞直到指定的List中任何一个返回了数据或者超过了超时时间。

关于B-POP还有一些要点：

1. 服务端按顺序处理客户端请求。当List有可用数据时，第一个开始等待的客户端会第一个得到返回值。
2. 由于B-POP命令支持指定多个List参数，所以返回值和普通POP的格式不一样，返回值为一个包含两个元素的数组，第一个元素是List的名字，第二个元素是返回值。
3. 超时之后返回NULL。

更多高端操作，可以参考使用RPOPLPUSH/BRPOPLPUSH命令，以实现更加安全的阻塞队列。

## Redis Hashes

Redis和Hash可以理解为编程语言中的Map，例如java.util.HashMap。可以简单轻便的存储面向对象编程语言中的对象，此外对于小对象，Redis通过单独的编码方式存储，确保其内存高效性。一些例子：

```
127.0.0.1:6379> hmset my:hash name Laibao age 27 address chongqing
OK
127.0.0.1:6379> hget my:hash name
"Laibao"
127.0.0.1:6379> hget my:hash age
"27"
127.0.0.1:6379> hmget my:hash name age
1) "Laibao"
2) "27"
127.0.0.1:6379> hgetall my:hash
1) "name"
2) "Laibao"
3) "age"
4) "27"
5) "address"
6) "chongqing"
```

## Redis Sets

Redis的Set存储字符串的集合，与我们数学意义上的集合比较吻合。使用SADD命令向集合添加元素，使用SISMEMBER命令检查集合中是否存在指定元素，支持交集/并集/差集等集合运算。使用SMEMBERS命令查询集合所有元素，但是并不保证任意两次请求返回值的顺序相同。例如：

```
127.0.0.1:6379> sadd set:1 a c d r g q
(integer) 6
127.0.0.1:6379> sadd set:2 b c o r p q
(integer) 6
127.0.0.1:6379> smembers set:2
1) "r"
2) "o"
3) "c"
4) "q"
5) "b"
6) "p"
127.0.0.1:6379> sismember set:1 a
(integer) 1
127.0.0.1:6379> sismember set:1 b
(integer) 0
127.0.0.1:6379> sunion set:1 set:2
1) "b"
2) "q"
3) "o"
4) "a"
5) "r"
6) "g"
7) "d"
8) "c"
9) "p"
127.0.0.1:6379> sinter set:1 set:2
1) "q"
2) "r"
3) "c"
127.0.0.1:6379> sdiff set:1 set:2
1) "g"
2) "d"
3) "a"
127.0.0.1:6379> 
```

Set擅长用来表示对象之间的关系，例如我们可以使用set实现标签功能。假设我么给新闻打标签，articleID=1000的新闻有1,2,5,77这几个标签：

```
127.0.0.1:6379> sadd news:1000:tags 1 2 5
(integer) 4
```

我们也可以反过来，对每个标签统计新闻：

```
127.0.0.1:6379> sadd tag:1:news 1000
(integer) 1
127.0.0.1:6379> sadd tag:2:news 1000
(integer) 1
127.0.0.1:6379> sadd tag:5:news 1000
(integer) 1
```

再比如，我们通过Redis实现扑克游戏，使用(C)lubs, (D)iamonds, (H)earts, (S)pades来表示四种花型，游戏开始我们有一副牌：

```
127.0.0.1:6379> sadd deck CA C2 C3 C4 C5 C6 C7 C8 C9 C10 CJ CQ CK DA D2 D3 D4 D5 D6 D7 D8 D9 D10 DJ DQ DK HA H2 H3 H4 H5 H6 H7 H8 H9 H10 HJ HQ HK SA S2 S3 S4 S5 S6 S7 S8 S9 S10 SJ SQ SK
(integer) 52
```

如果我们需要为每个玩家发牌，我们可以使用SPOP命令，SPOP从set中随机选择移除一个元素并返回。如果需要从set中随机选取数据但不移除，可以使用SRANDMEMBER命令。

## Redis Sorted set

Redis的Sorted set更像Set和Hash的合体，从set角度来讲，Sorted set的元素也不允许重复，但是与set不同的是Sorted set元素有序；从hash角度来讲，Sorted set中每个元素都与一个score值映射，这个score值用于和比较，但是sorted set元素不允许重复。以下是根据score的规则：

- 如果A.SCORE != B.SCORE，则A>B IF A.SCORE>B.SCORE。
- 如果A.SCORE == B.SCORE，则A和B通过字符顺序比较。

使用ZADD命令向Sorted set中添加元素，ZADD与SADD命令比较像，但是在key和元素值之前需要指定score的值。如果ZADD参数中的元素值已经存在，则会更新元素的score值。例如：

```
127.0.0.1:6379> zadd hackers 1991 Rambo 1990 John 1995 Alice 1998 Small 1998 Big
(integer) 5
```

Redis内部使用一种双端数据结构来实现Sorted set，这一数据结构包含一个skip list和hash table。由于元素在插入时就需要被，所以向Sorted set添加数据的时间复杂度为O(log(N))。但是在通过范围读取元素时，由于数据已经，所以Redis不需要再做任何额外工作。使用类似于LRANGE的ZRANGE命令来读取元素，使用ZREVRANGE反向读取元素：

```
127.0.0.1:6379> zrange hackers 0 -1
1) "John"
2) "Rambo"
3) "Alice"
4) "Big"
5) "Small"
127.0.0.1:6379> zrevrange hackers 0 -1
1) "Small"
2) "Big"
3) "Alice"
4) "Rambo"
5) "John"
```

我们看到元素的结果与上述规则是一致的。

Sorted set支持对和范围的强大操作，例如：

```sql
127.0.0.1:6379> zrangebyscore hackers -inf 1995
1) "John"
2) "Rambo"
3) "Alice"
127.0.0.1:6379> zremrangebyscore hackers -inf 1990
(integer) 1
127.0.0.1:6379> zrank hackers Small
(integer) 3
127.0.0.1:6379> zrevrank hackers Small
(integer) 0
```

## Redis Bitmaps

实际上Bitmap并不是Redis中单独的数据类型，而是定义在字符串类型上的面向位运算的操作。因为Redis字符串数据类型是二进制安全的，其最大长度为512MB，支持2<sup>32</sup>位。

位操作可以分为两种，一种是花费恒定时间的单个位操作，比如位赋值或读取运算，一种是针对多位的运算，不如在给定位范围内统计位的值的数量。

Bitmap的一个优势是它可以极大地节省内存空间。例如在一个使用自增ID表示用户ID的系统中，使用其他方法几乎不可能在仅仅512MB内存中存储4billion标签数据。

## HyperLogLogs

HyperLogLog是用来做基数（比如数据集 {1, 3, 5, 7, 5, 7, 8}， 那么这个数据集的基数集为 {1, 3, 5 ,7, 8}, 基数（不重复元素）为5）统计的算法，HyperLogLog 的优点是，在输入元素的数量或者体积非常非常大时，计算基数所需的空间总是固定 的、并且是很小的。在 Redis 里面，每个 HyperLogLog 键只需要花费最多 12 KB 内存，就可以计算接近 2<sup>64</sup> 个不同元素的基 数。这和计算基数时，元素越多耗费内存就越多的集合形成鲜明对比。

但是，因为 HyperLogLog 只会根据输入元素来计算基数，而不会储存输入元素本身，所以 HyperLogLog 不能像集合那样，返回输入的各个元素。

## Redis对key的自动创建和删除

所有任何类型，我们不需要显式的删除value为空的key，在使用例如LPUSH等修改value的命令之前也不需要先显式创建key，因为这些工作Redis都帮我们完成了。总的来讲我们可以将Redis帮我们维护key的默认行为总结为以下三个规则：

1. 如果我们向一个聚类数据类型中添加数据元素，如果目标key不存在，Redis在添加操作之前会为我们创建一个相同数据类型但是值空的value。
2. 如果我们从聚类数据类型中移除了元素之后，剩下的数据值为空，那么这个key会被自动销毁。
3. 使用只读命令或者从不存在的key中移除元素时，Redis的表现与存在一个value为空的key一样。



















