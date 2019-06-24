# Redis Data Persistence

Redis 提供了两种持久化方式：RDB（默认） 和AOF 

## **RDB**

RDB是Redis DataBase的缩写，功能核心是两个函数：rdbSave（生成RDB文件保存到磁盘）和rdbLoad（从磁盘文件加载到内存）

### RDB的时机

默认情况下，redis使用RDB方式对当前内存中的数据进行快照(SNAPSHOT)进行持久化操作，生成.rdb文件并保存在磁盘上。在默认配置文件中我们可以看到如下配置，SNAPSHOTTING块配置定义了RDB操作的基本行为：

```javascript
################################ SNAPSHOTTING  ################################
#
# Save the DB on disk:
#   save <seconds> <changes>
#
#   Will save the DB if both the given number of seconds and the given
#   number of write operations against the DB occurred.
#
#   In the example below the behaviour will be to save:
#   after 900 sec (15 min) if at least 1 key changed
#   after 300 sec (5 min) if at least 10 keys changed
#   after 60 sec if at least 10000 keys changed
#
#   Note: you can disable saving completely by commenting out all "save" lines.
#   It is also possible to remove all the previously configured save
#   points by adding a save directive with a single empty string argument
#   like in the following example:
#   save ""
save 900 1
save 300 10
save 60 10000

# By default Redis will stop accepting writes if RDB snapshots are enabled
# (at least one save point) and the latest background save failed.
# This will make the user aware (in a hard way) that data is not persisting
# on disk properly, otherwise chances are that no one will notice and some
# disaster will happen.
#
# If the background saving process will start working again Redis will
# automatically allow writes again.
#
# However if you have setup your proper monitoring of the Redis server
# and persistence, you may want to disable this feature so that Redis will
# continue to work as usual even if there are problems with disk,
# permissions, and so forth.
stop-writes-on-bgsave-error yes

# Compress string objects using LZF when dump .rdb databases?
# For default that's set to 'yes' as it's almost always a win.
# If you want to save some CPU in the saving child set it to 'no' but
# the dataset will likely be bigger if you have compressible values or keys.
rdbcompression yes

# Since version 5 of RDB a CRC64 checksum is placed at the end of the file.
# This makes the format more resistant to corruption but there is a performance
# hit to pay (around 10%) when saving and loading RDB files, so you can disable it
# for maximum performances.
#
# RDB files created with checksum disabled have a checksum of zero that will
# tell the loading code to skip the check.
rdbchecksum yes

# The filename where to dump the DB
dbfilename dump.rdb

# The working directory.The DB will be written inside this directory, 
# with the filename specified above using the 'dbfilename' configuration directive.
#
# The Append Only File will also be created inside this directory.
dir ./
```

此外，用户可以在需要的场景下通过手动执行SAVE/BGSAVE或者FLUSHALL命令保存redis的数据快照，比如当我们需要重启、迁移、备份Redis时。

- **SAVE命令：**当执行SAVE命令时，Redis同步进行快照操作，期间会阻塞所有来自客户端的请求，所以当数据库数据较多时，应该避免使用该命令；
- **BGSAVE命令：** BGSAVE命令与SAVE命令的区别在于该命令的快照操作是在后台异步进行的，进行快照操作的同时还能处理来自客户端的请求。执行BGSAVE命令后Redis会马上返回OK表示开始进行快照操作，如果想知道快照操作是否已经完成，可以使用LASTSAVE命令返回最近一次成功执行快照的时间，返回结果是一个Unix时间戳。
- **FLUSHALL命令：**当执行FLUSHALL命令时，Redis会清除数据库中的所有数据。需要注意的是：**不论清空数据库的过程是否触发了自动快照的条件，只要自动快照条件不为空，Redis就会执行一次快照操作，当没有定义自动快照条件时，执行FLUSHALL命令不会进行快照操作。**

最后，在设置了主从模式的场景下，Redis会在复制初始化时进行自动快照。

### RDB的原理

Redis快照执行的过程如下：

（1）Redis使用Linux系统的fork()函数复制一份当前进程（父进程）的副本（子进程）；
（2）父进程继续处理来自客户端的请求，子进程开始将内存中的数据写入硬盘中的临时文件；
（3）当子进程写完所有的数据后，用该临时文件替换旧的RDB文件，至此，一次快照操作完成。

需要注意的是，在执行fork的时候操作系统（类Unix操作系统）会使用写时复制（copy-on-write）策略，即调用fork()函数的一刻，父进程和子进程共享同一块内存数据，当父进程需要修改其中的某片数据（如执行写命令）时，操作系统会将该片数据复制一份以保证子进程不受影响，所以RDB文件存储的是执行fork操作那一刻的内存数据。所以RDB方式理论上是会存在丢数据的情况的(fork之后修改的的那些没有写进RDB文件)。另外，当系统停止，或者redis被kill掉，最后写入Redis的数据就会丢失。

通过上述的介绍可以知道，快照进行时是不会修改RDB文件的，只有完成的时候才会用临时文件替换老的RDB文件，所以就保证任何时候RDB文件的都是完整的。这使得我们可以通过定时备份RDB文件来实现Redis数据的备份。RDB文件是经过压缩处理的二进制文件，所以占用的空间会小于内存中数据的大小，更有利于传输。

Redis启动时会自动读取RDB快照文件，将数据从硬盘载入到内存，根据数量的不同，这个过程持续的时间也不尽相同，通常来讲，一个记录1000万个字符串类型键，大小为1GB的快照文件载入到内存需要20-30秒的时间。

## **AOF:**

AOF是Append-Only File的缩写。默认情况下AOF是关闭的，关于AOF的默认配置如下：

```javascript
############################## APPEND ONLY MODE ###############################
# By default Redis asynchronously dumps the dataset on disk. This mode is
# good enough in many applications, but an issue with the Redis process or
# a power outage may result into a few minutes of writes lost (depending on
# the configured save points).
#
# The Append Only File is an alternative persistence mode that provides
# much better durability. For instance using the default data fsync policy
# (see later in the config file) Redis can lose just one second of writes in a
# dramatic event like a server power outage, or a single write if something
# wrong with the Redis process itself happens, but the operating system is
# still running correctly.
#
# AOF and RDB persistence can be enabled at the same time without problems.
# If the AOF is enabled on startup Redis will load the AOF, that is the file
# with the better durability guarantees.
# (http://redis.io/topics/persistence)
appendonly no

# The name of the append only file (default: "appendonly.aof")
appendfilename "appendonly.aof"

# The fsync() call tells the Operating System to actually write data on disk
# instead of waiting for more data in the output buffer. Some OS will really flush
# data on disk, some other OS will just try to do it ASAP.
#
# Redis supports three different modes:
#
# no: don't fsync, just let the OS flush the data when it wants. Faster.
# 当设置appendfsync为no的时候，Redis不会主动调用fsync去将AOF日志内容同步到磁盘，这一操作完全
# 依赖于操作系统。对大多数Linux操作系统，是每30秒进行一次fsync，将缓冲区中的数据写到磁盘上。

# always: fsync after every write to the append only log. Slow, Safest.
# 当设置appendfsync为always时，每一次写操作都会调用一次fsync，这时数据是最安全的，
# 当然，由于每次都会执行fsync，所以其性能也会受到影响

# everysec: fsync only one time every second. Compromise.
# 当设置appendfsync为everysec的时候，Redis会默认每隔一秒进行一次fsync调用，将缓冲区中的数据写到
# 磁盘。但是当这一次的fsync调用时长超过1秒时。Redis会采取延迟fsync的策略，在下次调用fsync之前再等
# 一秒钟。也就是在两秒后再进行fsync，这一次的fsync就不管会执行多长时间都会进行。这时候由于在fsync
# 时文件描述符会被阻塞，所以当前的写操作就会阻塞。所以，在绝大多数情况下，Redis会每一秒进行一次fsync。
# 只有在最坏的情况下，两秒钟会进行一次fsync操作。这一操作在大多数数据库系统中被称为group commit，
# 就是组合多次写操作的数据，一次性将日志写到磁盘
#
# The default is "everysec", as that's usually the right compromise between
# speed and data safety.If unsure, use "everysec".

# appendfsync always
appendfsync everysec
# appendfsync no

# When the AOF fsync policy is set to always or everysec, and a background
# saving process (a background save or AOF log background rewriting) is
# performing a lot of I/O against the disk, in some Linux configurations
# Redis may block too long on the fsync() call. Note that there is no fix for
# this currently, as even performing fsync in a different thread will block
# our synchronous write(2) call.
#
# In order to mitigate this problem it's possible to use the following option
# that will prevent fsync() from being called in the main process while a
# BGSAVE or BGREWRITEAOF is in progress.
#
# This means that while another child is saving, the durability of Redis is
# the same as "appendfsync none". In practical terms, this means that it is
# possible to lose up to 30 seconds of log in the worst scenario (with the
# default Linux settings).
#
# If you have latency problems turn this to "yes". Otherwise leave it as
# "no" that is the safest pick from the point of view of durability.
no-appendfsync-on-rewrite no

# Automatic rewrite of the append only file.
# Redis is able to automatically rewrite the log file implicitly calling
# BGREWRITEAOF when the AOF log size grows by the specified percentage.
#
# This is how it works: Redis remembers the size of the AOF file after the
# latest rewrite (if no rewrite has happened since the restart, the size of
# the AOF at startup is used).
#
# This base size is compared to the current size. If the current size is
# bigger than the specified percentage, the rewrite is triggered. Also
# you need to specify a minimal size for the AOF file to be rewritten, this
# is useful to avoid rewriting the AOF file even if the percentage increase
# is reached but it is still pretty small.
#
# Specify a percentage of zero in order to disable the automatic AOF
# rewrite feature.
# 目前的AOF文件的大小超过上一次重写时的AOF文件的百分之多少时再次进行重写，
# 如果之前没有重写过，则以启动时AOF文件大小为依据。
auto-aof-rewrite-percentage 100
# 当AOF文件的大小大于64MB时才进行重写，因为如果AOF文件本来就很小时，
# 有一些无效的命令也是允许的。
auto-aof-rewrite-min-size 64mb

# An AOF file may be found to be truncated at the end during the Redis
# startup process, when the AOF data gets loaded back into memory.
# This may happen when the system where Redis is running
# crashes, especially when an ext4 filesystem is mounted without the
# data=ordered option (however this can't happen when Redis itself
# crashes or aborts but the operating system still works correctly).
#
# Redis can either exit with an error when this happens, or load as much
# data as possible (the default now) and start if the AOF file is found
# to be truncated at the end. The following option controls this behavior.
#
# If aof-load-truncated is set to yes, a truncated AOF file is loaded and
# the Redis server starts emitting a log to inform the user of the event.
# Otherwise if the option is set to no, the server aborts with an error
# and refuses to start. When the option is set to no, the user requires
# to fix the AOF file using the "redis-check-aof" utility before to restart
# the server.
#
# Note that if the AOF file will be found to be corrupted in the middle
# the server will still exit with an error. This option only applies when
# Redis will try to read more data from the AOF file but not enough bytes
# will be found.
aof-load-truncated yes

# When rewriting the AOF file, Redis is able to use an RDB preamble in the
# AOF file for faster rewrites and recoveries. When this option is turned
# on the rewritten AOF file is composed of two different stanzas:
#
#   [RDB file][AOF tail]
#
# When loading Redis recognizes that the AOF file starts with the "REDIS"
# string and loads the prefixed RDB file, and continues loading the AOF
# tail.
#
# This is currently turned off by default in order to avoid the surprise
# of a format change, but will at some point be used as the default.
aof-use-rdb-preamble no
```

通过以上配置我们可看到，默认情况下AOF文件和RDB文件是在同一目录下的，事实上RDB和AOF可以同时开启。默认AOF文件名appendonly.aof，它是一个可识别的纯文本文件。实际上AOF文件就是将我们所有的命令操作记录下来，所以这个文件很容易就能长到很大。并且在很多场景下，我们没有必要记录每一条命令操作（例如对key1的连续多次incre操作）所以Redis帮我们压缩了一些中间操作，这一过程被称为AOF的**重写**。我们可以手动执行BGREWRITEAOF命令主动让Redis重写AOF文件。

## **比较**：

**RDB优势**

1. 采用RDB备份数据，整个Redis数据库将会被打包进一个文件，这对于文件备份而言是很有利的。比如，我们可能打算每个小时归档一次最近24小时的数据，同时还要每天归档一次最近30天的数据。通过这样的备份策略，一旦系统出现灾难性故障，我们可以非常容易的进行恢复。
2. 对于灾难恢复而言，RDB是非常不错的选择。因为我们可以非常轻松的将一个单独的文件压缩后再转移到其它存储介质上。
3. 性能最大化。对于Redis的服务进程而言，在开始持久化时，它唯一需要做的只是fork出子进程，之后再由子进程完成这些持久化的工作，这样就可以极大的避免服务进程执行IO操作了。
4. 相比于AOF机制，如果数据集很大，RDB的启动效率会更高。

**RDB劣势**

1. 如果我们需要保证数据的高可用性，即最大限度的避免数据丢失，那么RDB将不是一个很好的选择。因为系统一旦在定时持久化之前出现宕机现象，此前没有来得及写入磁盘的数据都将丢失。
2. 由于RDB是通过fork子进程来协助完成数据持久化工作的，因此，如果当数据集较大时，可能会导致整个服务器停止服务几百毫秒，甚至是1秒钟。

**AOF优势** 

1. 该机制可以带来更高的数据安全性。Redis中提供了3中同步策略，即每秒同步、每次修改同步和不同步。事实上，每秒同步也是异步完成的，其效率也是非常高的，所差的是一旦系统出现宕机现象，那么这一秒钟之内修改的数据将会丢失。而每次修改同步，我们可以将其视为同步持久化，即每次发生的数据变化都会被立即记录到磁盘中。可以预见，这种方式在效率上是最低的。至于无同步，无需多言。

2. 由于该机制对日志文件的写入操作采用的是append模式，因此在写入过程中即使出现宕机现象，也不会破坏日志文件中已经存在的内容。如果我们本次操作的中途出现了系统崩溃问题，那么在Redis下一次启动之前，我们可以通过redis-check-aof工具来帮助我们解决数据一致性的问题，命令格式如：

   > redis-check-aof --fix \<filename\>

3. 如果日志过大，Redis可以自动启用rewrite机制。即Redis以append模式不断的将修改数据写入到老的磁盘文件中，同时Redis还会创建一个新的文件用于记录此期间有哪些修改命令被执行。因此在进行rewrite切换时可以更好的保证数据安全性。

4. AOF包含一个格式清晰、易于理解的日志文件用于记录所有的修改操作。事实上，我们也可以通过该文件完成数据的重建。

**AOF劣势**

1.  对于相同数量的数据集而言，AOF文件通常要大于RDB文件。RDB 在恢复大数据集时的速度比 AOF 的恢复速度要快。
2.  根据同步策略的不同，AOF在运行效率上往往会慢于RDB。总之，每秒同步策略的效率是比较高的，同步禁用策略的效率和RDB一样高效。

**配合使用**

- 如果只配置AOF，重启时加载AOF文件恢复数据；
- 如果同时配置了RBD和AOF，启动是只加载AOF文件恢复数据；
- 如果只配置RBD，启动是加载dump文件恢复数据。

## 鸣谢

https://www.cnblogs.com/xiaoxi/p/7065328.html