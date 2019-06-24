## Buffering and Caching

MySQL使用几种策略在内存缓冲区中缓存信息以提高性能。

### InnoDB Buffer Pool Optimization

InnoDB维护一个称为缓冲池的存储区域，用于在内存中缓存数据和索引。了解InnoDB缓冲池如何工作，并利用它来将频繁访问的数据保存在内存中，是MySQL调优的一个重要方面。InnoDB的缓冲池使用LRU算法。

#### The InnoDB Buffer Pool

我们可以配置InnoDB缓冲池的各个方面以提高性能：

• 理想情况下，我们可以将缓冲池的大小设置为尽可能大的值，只为服务器上的其他进程留出足够的内存，而无需过多的分页即可。 缓冲池越大，InnoDB就越像内存数据库，从磁盘读取数据一次，然后在后续读取期间从内存中访问数据。

• 对于具有较大内存的64位系统，我们可以将缓冲池拆分为多个部分，以最大程度地减少并发操作中内存结构的争用。

• 尽管数据备份或生成报表等操作会使数据库负载激增，我们仍可以将频繁访问的数据保留在内存中。

• 我们可以控制InnoDB何时以及如何执行预读请求，在预计很快就会需要这些数据时以异步方式将数据预取到缓冲池中。 

• 我们可以控制何时进行脏数据的后台刷新以及InnoDB是否根据工作负载动态调整刷新率。

• 我们可以微调InnoDB缓冲池刷新行为的各个方面，以提高性能。 

• 我们可以配置InnoDB如何保存当前的缓冲池状态，以避免服务器重启后的长时间预热。我们还可以在服务器运行时保存当前缓冲池状态。

##### InnoDB Buffer Pool LRU Algorithm

InnoDB使用最近最少使用（LRU）算法的变体将缓冲池作为列表进行管理。当需要空间向池中添加新页面时，InnoDB会删除最近最少使用的页面并将新页面添加到列表中间。这种"midpoint insertion strategy"将列表视为两个子列表：

•头部 最近访问过的"新"("年轻")页面的子列表。
•尾部 最近访问的“旧”页面子列表。

此算法将查询大量使用的页面保留在"新"子列表中。"旧"子列表包含较少使用的页面；这些页面是清除的候选。

LRU算法默认操作如下：

• 缓冲池的3/8专用于"旧"子列表

• 列表的中点是"新"子列表的尾部与"旧"子列表的头部相交的边界

• 当InnoDB将页面读入缓冲池时，首先将其插入中点（旧子列表的头部）。读入页面的时机有可能是用户指定的操作（例如SQL查询）所需，或者是InnoDB自动执行的预读操作的一部分。

• 访问"旧"子列表中的页面使其"年轻"，此页面数据会被移动到缓冲池的头部（新子列表的头部）。如果因为处理请求而读入页面，则会立即执行第一次访问，并使页面变得"年轻"。如果由于预读而读入了页面，则第一次访问不会立即发生（并且在页面被逐出缓冲区之前可能根本不会发生）。

• 当数据库运行时，缓冲池中的页面因为使用频率低而逐渐移动到列表的尾部，形象的称之为"变老"。新旧子列表中的页面随着其他页面的变"年轻"而变"老"。旧子列表中的页面也会随着新页面插入中点而"变老"。最终，一个长时间未使用的页面到达旧子列表的尾部并被逐出。

默认情况下，查询读取的页面会立即移动到"新"子列表中，这意味着它们会在缓冲池中停留更长时间。表扫描（例如mysqldump操作，或者没有WHERE子句的SELECT语句）可以将大量数据带入缓冲池并逐出相同数量的旧数据，即使新数据从未再次使用过。类似地，由后台线程预读然后仅访问一次的页面会移动到新列表的头部。这些情况可以将经常使用的页面推到旧的子列表，在那里它们会被驱逐。

InnoDB标准监视器输出中BUFFER POOL和MEMORY部分中包含几个字段，这些字段与缓冲池LRU算法的操作有关。

##### InnoDB Buffer Pool Configuration Options

以下几个配置选项会影响InnoDB缓冲池的不同方面：

**• innodb_buffer_pool_size**

缓冲池的大小。在内存允许的情况下，增大缓冲池可以通过减少查询访问InnoDB表所需的磁盘I/O提高性能。innodb_buffer_pool_size选项是动态的，修改缓冲池大小无需重新启动服务器。

**• innodb_buffer_pool_chunk_size**

定义InnoDB缓冲池大小调整操作的块大小。

**• innodb_buffer_pool_instances**

将缓冲池划分为用户指定数量的单独区域，每个区域都有自己的LRU列表和相关数据结构，以减少并发读写操作期间的内存争用。此选项仅在将innodb_buffer_pool_size设置为大于1GB的值时才生效。为了获得最佳效率，需要配合使用innodb_buffer_pool_instances和innodb_buffer_pool_size，以便每个缓冲池实例至少为1gigabyte。

**• innodb_old_blocks_pct**

指定InnoDB用于"旧"子列表的内存占整个缓冲池的近似百分比。范围是5到95，默认值是37（即3/8）。

**• innodb_old_blocks_time**

指定旧子列表的页面在首次访问后必须保留的时间（以毫秒为单位），然后才能将其移动到新的子列表中。如果值为0，则插入旧子列表的页面都会在第一次访问时立即移动到新子列表。如果该值大于0，则页面将保留在旧子列表中，直到在第一次访问后等待多少毫秒。例如，值1000会导致页面在第一次访问后保留在旧子列表中1秒钟，然后才有资格移动到新子列表。

将innodb_old_blocks_time设置为大于0可防止一次性表扫描使用的仅用于此次扫描的页面填充新子列表。如果将innodb_old_blocks_time设置为大于处理页面的时间的值，则该页面保留在“旧”子列表中并且老化到列表的尾部以便快速逐出。这样，仅用于一次扫描的页面不会对新子列表中频繁使用的页面造成损害。

innodb_old_blocks_time可以在运行时设置，我们可以在执行表扫描和转储等操作时临时更改它：

```sql
SET GLOBAL innodb_old_blocks_time = 1000;
... perform queries that scan tables ...
SET GLOBAL innodb_old_blocks_time = 0;
```

如果我们的意图是通过填充表的内容来“预热”缓冲池，则此策略不适用。

**• innodb_read_ahead_threshold**

控制InnoDB用于将页面线性预读到缓冲池中的灵敏度。

**• innodb_random_read_ahead**

启用随机预读技术以将页面预取到缓冲池中。随机预读是一种技术，可以根据缓冲池中已有的页面预测很快就需要使用的页面，而不管这些页面的读取顺序如何。innodb_random_read_ahead默认是禁用的。

**• innodb_adaptive_flushing**

是否根据工作负载动态调整刷新缓冲池中脏数据页的速率。动态调整刷新率旨在避免突发的I/O活动。默认情况下启用此设置。

**• innodb_adaptive_flushing_lwm**

lwm(Low water markre) 表示启用自适应刷新时重做日志容量的百分比。(presenting percentage of redo log capacity at which adaptive flushing is enabled.)

**• innodb_flush_neighbors**

指定从缓冲池刷新页面是否也刷新相同范围内的其他脏数据页。

**• innodb_flushing_avg_loops**

InnoDB保留先前计算的刷新状态快照的迭代次数，控制自适应刷新对更改工作负载的响应速度。(Number of iterations for which InnoDB keeps the previously calculated snapshot of the flushing state, controlling how quickly adaptive flushing responds to changing workloads.)

**• innodb_lru_scan_depth**

影响缓冲池刷新操作的算法的参数。主要是为性能专家调整I/O密集型工作负载的场景下服务器的表现。它为每个缓冲池实例指定缓冲池LRU列表中page_cleaner线程扫描查找要刷新的脏数据页的深度。

**• innodb_max_dirty_pages_pct**

InnoDB尝试从缓冲池中刷新数据，以保持脏数据页的百分比不超过此值。默认值为75，值域0-99。

**• innodb_max_dirty_pages_pct_lwm**

启用预刷新时脏数据页的百分比的下限。 默认值0完全禁用预刷新行为。

**• innodb_buffer_pool_filename**

指定包含innodb_buffer_pool_dump_at_shutdown或innodb_buffer_pool_dump_now生成的表空间ID和页面ID列表的文件的名称。

**• innodb_buffer_pool_dump_at_shutdown**

指定在MySQL服务器关闭时是否保存缓冲池中缓存的页面，以便在下次重新启动时缩短预热过程。

**• innodb_buffer_pool_load_at_startup**

指定在MySQL服务器启动时，通过加载较早时保存的相同页面来自动预热缓冲池。通常与innodb_buffer_pool_dump_at_shutdown结合使用。

**• innodb_buffer_pool_dump_now**

立即保存缓冲池中缓存的数据页面。

**• innodb_buffer_pool_load_now**

通过加载一组数据页面立即预热缓冲池，而无需等待服务器重启。在基准测试期间用于将缓存恢复到已知状态，或者在MySQL服务器在生成报表或维护后恢复正常工作负载。通常与innodb_buffer_pool_dump_now一起使用。

**• innodb_buffer_pool_dump_pct**

指定每个缓冲池最近使用的页面的读取和转储百分比。范围是1到100。

**• innodb_buffer_pool_load_abort**

中断由innodb_buffer_pool_load_at_startup或innodb_buffer_pool_load_now出发的恢复缓冲池内容的过程。

#### Configuring InnoDB Buffer Pool Size

我们可以在服务器运行时离线或在线配置InnoDB缓冲池大小。下面描述的方法同时适用于这两种方式。当增加或减少innodb_buffer_pool_size时，操作以块为单位执行。块大小由innodb_buffer_pool_chunk_size配置选项定义，该选项默认值为128M。

缓冲池大小必须始终等于或者是innodb_buffer_pool_chunk_size * innodb_buffer_pool_instances的倍数。 如果将innodb_buffer_pool_size配置为不等于或者是innodb_buffer_pool_chunk_size * innodb_buffer_pool_instances的倍数，则缓冲池大小会自动调整为等于或等于innodb_buffer_pool_chunk_size * innodb_buffer_pool_instances的倍数但是不小于指定的缓冲池大小的数值。

##### Configuring InnoDB Buffer Pool Chunk Size

innodb_buffer_pool_chunk_size可以以1MB（1048576字节）为单位增加或减少，但只能在启动时通过命令行或MySQL配置文件修改。

命令行：

```sql
shell> mysqld --innodb_buffer_pool_chunk_size=134217728
```

配置文件：

> [mysqld]
> innodb_buffer_pool_chunk_size=134217728

> 为避免潜在的性能问题，块的数量（innodb_buffer_pool_size / innodb_buffer_pool_chunk_size）不应超过1000。

##### Configuring InnoDB Buffer Pool Size Online

可以在不重新启动服务器的情况下使用SET语句动态设置innodb_buffer_pool_size配置选项来调整缓冲池的大小。例如：

```sql
mysql> SET GLOBAL innodb_buffer_pool_size=402653184;
```

在调整缓冲池大小之前，必须先完成活动中的事务和通过InnoDB API执行的操作。开始调整大小操作时，在所有活动事务完成之前，操作不会启动。一旦调整大小操作开始进行，需要访问缓冲池的新事务和操作必须等到调整大小操作完成。例外是允许在缓冲池进行碎片整理或者减小缓冲池大小时对缓冲池进行并发访问。允许并发访问的一个缺点是，当数据页被从缓冲池中移除时，可能导致可用页暂时短缺。

##### Monitoring Online Buffer Pool Resizing Progress

Innodb_buffer_pool_resize_status变量报告了缓冲池大小调整的进度。 例如：

```sql
mysql>  SHOW STATUS WHERE Variable_name='InnoDB_buffer_pool_resize_status';
+----------------------------------+-------+
| Variable_name                    | Value |
+----------------------------------+-------+
| Innodb_buffer_pool_resize_status |       |
+----------------------------------+-------+
```

缓冲池大小调整进度也会被记录在服务器错误日志中。

##### Online Buffer Pool Resizing Internals

调整大小操作由后台线程执行。增加缓冲池的大小时，执行如下操作步骤：

• 向块中添加页面

• 转换hash table，list和pointers以使用内存中的新地址

• 将新页面添加到空闲列表

当这些操作正在进行时，其他线程将被阻止访问缓冲池。减小缓冲池的大小时，执行如下操作步骤：

• 对缓冲池进行碎片整理并撤消（释放）页面

• 删除块中的页面

• 转换hash table，list和pointers以使用内存中的新地址

在这些操作中，仅对缓冲池进行碎片整理并撤销页面时允许其他线程同时访问缓冲池。

### The MyISAM Key Cache

为了最小化磁盘I/O，MyISAM存储引擎借鉴了许多数据库管理系统使用的策略。它采用缓存机制将最常访问的数据块保存在内存中：

• 对于索引块，维护称为key cache（或key buffer）的特殊结构。该结构包含许多块缓冲区，其中放置了最常用的索引块。

• 对于数据块，MySQL不使用特殊缓存。相反，它依赖于本机操作系统文件系统缓存。

本节首先介绍MyISAM key cache的基本操作。然后讨论了可以提高key cache性能并更好地控制缓存操作的功能：

• 多个会话可以同时访问缓存。

• 可以设置多个key cache并将表索引分配给特定缓存。

使用key_buffer_size系统变量控制key cache的大小。如果将此变量设置为零，则不使用key cache。如果key_buffer_size值太小而无法分配最小数量(8)的块缓冲区，则不使用key cache。

当key cache不可操作时，仅使用操作系统提供的本机文件系统缓存来访问索引文件。（换句话说，使用与表数据块相同的策略访问表索引块）索引块是访问MyISAM索引文件的连续单元。通常，索引块的大小等于索引B树的节点大小。

key cache结构中的所有块缓冲区大小相同。该大小可以等于，大于或小于表索引块的大小。通常这两个值中的一个是另一个的倍数。当必须访问来自表索引块的数据时，服务器首先检查它是否在key cahce的某个块缓冲区中可用。如果是，则服务器访问key cache中的数据而不是磁盘上的数据。也就是说，它从缓存读取或写入其中而不是读取或写入磁盘。否则，服务器选择包含不同表索引块（或块）的缓存块，并将当前必需的表索引块数据副本替换其数据。只要新索引块位于缓存中，就可以访问索引数据。如果碰巧选择了要替换的块，则该块被认为是“脏的”。在这种情况下，在被替换之前，其内容被刷新到它来源的表索引。

通常服务器遵循LRU（最近最少使用）策略：当选择要替换的块时，它选择最近最少使用的索引块。为了更方便的定位这一索引块，key cache模块将所有使用的块维护在按使用时间的特殊列表（LRU链）中。访问块时，它是最近使用的块，位于列表的末尾。当需要替换块时，列表开头的块是最近最少使用的块，并成为第一个候选块被
驱逐。

### The MySQL Query Cache

> The query cache is deprecated as of MySQL 5.7.20, and is removed in MySQL 8.0.

#### How the Query Cache Operates

#### Query Cache SELECT Options

#### Query Cache Configuration

#### Query Cache Status and Maintenance

### Caching of Prepared Statements and Stored Programs

对于客户端可能在会话期间多次执行的某些语句，服务器会将语句转换为内部结构并缓存以供语句执行期间使用。缓存使服务器能够更有效地执行语句，因为它避免了在会话期间重复转换语句的开销。这些语句可以被转换和缓存：

• Prepared statements 包括在SQL级别（使用PREPARE语句）处理的语句和使用二进制客户端/服务器协议（使用mysql_stmt_prepare() C API函数处理的语句）。 max_prepared_stmt_count系统变量控制服务器缓存的语句总数（所有会话中语句总数）。

• Stored programs（存储过程和函数，触发器和事件）。在这种情况下，服务器转换并缓存整个程序体。 stored_program_cache系统变量指示服务器为每个会话缓存的大致存储程序数。服务器基于每个会话维护Prepared statements和Stored programs的高速缓存。

会话无法访问其他会话缓存的语句。会话结束时，服务器会丢弃为其缓存的所有语句。当服务器使用缓存的内部语句结构时，必须注意缓存不会过时。对于语句使用的对象，可能会发生元数据更改，从而导致当前对象定义与内部语句结构中表示的定义不匹配。 DDL语句（create, drop, alter, rename, or truncate tables, or that analyze, optimize, or repair tables）会使元数据发生更改。表内容更改（INSERT or UPDATE）不会更改元数据，SELECT语句也一样。假设客户预处理此声明：

```sql
PREPARE s1 FROM 'SELECT * FROM t1';
```

如果使用ALTER TABLE修改表中的列集，则预准备语句就会过期。如果服务器在下次客户端执行s1时未检测到此更改，则使用缓存好的语句将返回不正确的结果。

为了避免由预准备语句引用的表或视图的元数据更改导致的问题，服务器会检测这些更改并在下次执行时自动重新表示该语句。也就是说，服务器重新声明语句并重建内部结构。在从表定义缓存中刷新引用的表或视图之后，也会重新解析语句，或者隐式地为缓存中的新条目腾出空间，或者显式使用FLUSH TABLES进行重新分析。

同样，如果存储程序使用的对象发生更改，则服务器会重新编译程序中受影响的语句。服务器还检测表达式中对象的元数据更改。这些可能在特定于存储程序的语句中使用，例如DECLARE CURSOR或流控制语句，如IF，CASE和RETURN。为避免重新解析整个存储的程序，服务器仅在需要时重新编译程序中受影响的语句或表达式。例如：

• 假设更改了表或视图的元数据。对访问表或视图的程序中的SELECT * 进行重新分析，但不对不访问表或视图的SELECT * 进行重新分析。
• 当语句受到影响时，如果可能，服务器仅对其进行部分重新分析。例如这个CASE语句：

```sql
CASE case_expr
WHEN when_expr1 ...
WHEN when_expr2 ...
WHEN when_expr3 ...
...
END CASE
```

如果元数据更改仅影响WHEN when_expr3，则只会重新解析该表达式。case_expr和其他WHEN表达式不会被重新分析。重新解析使用对原始转换为内部表单有效的默认数据库和SQL模式。服务器尝试最多重新解析三次。如果所有尝试都失败，则会报错。重新解析是自动的，但是只要它发生，就会损失程序性能。对于prepared statements，Com_stmt_reprepare状态变量跟踪重新表示的数量。

## Optimizing Locking Operations

MySQL使用锁来管理对表内容的争用：

• 内部锁(Internal locking) 在MySQL服务器本身内执行，以管理多个线程对表内容的争用。这种类型的锁是内部的，因为它完全由服务器执行，不涉及其他程序。

• 外部锁(External locking) 发生在当服务器和其他程序锁定MyISAM表文件以在它们之间进行协调，已确定哪个程序何时可以访问这些表。

### Internal Locking Methods

#### Row-Level Locking

MySQL使用InnoDB表的行级锁定来支持多个会话的并发写访问，使其适用于多用户，高并发和OLTP应用程序。为了避免在单个InnoDB表上执行多个并发写操作时出现死锁，通过为每个预期要修改的语句组发出*SELECT ... FOR UPDATE*语句，在事务开始时获取必要的锁，即使写操作语句在事务的末尾也是如此。如果事务修改或锁定多个表，则在每个事务中以相同的顺序发送对应的语句。死锁并不会造成严重错误，但是会影响性能，因为InnoDB会自动检测死锁条件并回滚其中一个受影响的事务。在高并发系统上，当许多线程等待同一个锁时，死锁检测会导致速度减慢。有时，在发生死锁时，禁用死锁检测并依赖innodb_lock_wait_timeout设置进行事务回滚可能更有效。可以使用innodb_deadlock_detect配置选项禁用死锁检测。行级锁定的优点：

• 当不同的会话访问不同的行时，锁竞争会减少。

• 回滚的数据变化较少。

• 可以长时间锁定单行。

#### Table-Level Locking

MySQL中使用MyISAM, MEMORY 和 MERGE存储引擎的表使用表锁，在同一时刻仅允许一个会话对表进行写操作。表锁使得这些存储引擎更适用于存储只读，多读少写数据或者单用户应用程序。这些存储引擎通过始终在查询开始时立即请求所有需要的锁并始终以相同的顺序锁定表来避免死锁。代价是这种策略降低了并发性；想要修改表的其他会话必须等到当前数据更改语句执行完成。

表锁的优点：

• 所需内存相对较少（行锁需要为每行或每组行分配内存）
• 需要锁定表的大部分区域时速度快，因为只涉及一个锁。
• 如果经常对大部分数据执行GROUP BY操作，或者必须经常扫描整个表，则会很快。

MySQL为表授予写锁的逻辑如下：

1.如果表上没有锁，则在表上分配一个写锁。
2.否则，将锁定请求放入写锁等待队列。

MySQL为表授予读锁的逻辑如下：

1.如果表上没有写锁，则在表上分配一个写读锁。
2.否则，将锁定请求放入读锁等待队列。

表写锁的优先级高于表读锁。因此，当释放锁之后，锁首先对写锁队列中的请求可用，然后对读锁队列中的请求可用。这可以确保即使表的SELECT请求很多，对表的更新也不会"饥饿"。但是，如果表有许多更新，SELECT语句将一直等到更新操作全部完成。

我们可以通过检查Table_locks_immediate和Table_locks_waited状态变量来分析系统上的表锁争用，这些变量分别表示可以立即向请求分配表锁的请求次数和必须等待的次数：

```sql
mysql> SHOW STATUS LIKE 'Table%';
+-----------------------+---------+
| Variable_name 		| Value   |
+-----------------------+---------+
| Table_locks_immediate | 1151552 |
| Table_locks_waited 	| 15324   |
+-----------------------+---------+
```

performance_schema中的与锁有关的表也可以提供锁信息。

MyISAM存储引擎支持并发插入，以减少给定表的读和写之间的锁争用：如果MyISAM表在数据文件的中间没有空闲块，则始终在数据文件的末尾插入行。在这种情况下，我们可以自由地为没有锁的MyISAM表并发执行INSERT和SELECT语句。也就是说，我们可以在其他客户端读取数据的同时将行数据插入到MyISAM表中。从表中间删除或更新行会使表数据文件产生"空隙"。如果存在"空隙"，则并发插入会被禁用，但在所有"东西"都被新数据填充后会自动再次被启用。要控制此行为，可以使用concurrent_insert系统变量。如果使用LOCK TABLES显式获取表锁，则可以请求READ LOCAL锁而不是READ锁，以使其他会话在锁定表时可以执行并发插入。

To perform many INSERT and SELECT operations on a table t1 when concurrent inserts are not
possible, you can insert rows into a temporary table temp_t1 and update the real table with the rows
from the temporary table:

```sql
mysql> LOCK TABLES t1 WRITE, temp_t1 WRITE;
mysql> INSERT INTO t1 SELECT * FROM temp_t1;
mysql> DELETE FROM temp_t1;
mysql> UNLOCK TABLES;
```

#### Choosing the Type of Locking

通常，在以下情况下，表锁优于行级锁：

• 对该表的大多数语句都是读。
• 表的语句是读写的混合，其中写是单行的更新或删除，可以通过读取一个索引来获取：

```sql
UPDATE tbl_name SET column=value WHERE unique_key_col=key_value;
DELETE FROM tbl_name WHERE unique_key_col=key_value;
```

• SELECT配合并发INSERT语句和极少数UPDATE或DELETE语句。
• 整表扫描或GROUP BY操作，没有任何写。

对于更高级别的锁，我们可以通过支持不同类型的锁来更轻松地调整应用程序，因为高级锁开销小于行级锁。以下是行锁以外的选项：

• 版本号控制（例如MySQL中用于并发插入的版本控制），可以支持一写多度。这意味着数据库或表支持数据的不同视图，具体取决于读请求的开始时间。其他常见术语是“time travel,” “copy on write,” or “copy on demand.”
• Copy on demand在许多情况下优于行锁。但是，在最坏的情况下，它可以需要比使用普通锁更多的内存。
• 我们可以使用应用程序级锁，而不是使用行级锁，例如MySQL中的 GET_LOCK() 和 RELEASE_LOCK() 提供的锁。 这些是建议锁，因此它们仅适用于彼此协作的应用程序。

### Table Locking Issues

InnoDB表使用行锁，以便多个会话和应用程序可以同时对同一个表读和写，而不会彼此等待或产生不一致的数据。对于此存储引擎，避免使用LOCK TABLES语句，因为它非但不能提供任何额外保护，反而会降低并发性。自动行锁使这些表适用于具有最重要数据的最繁忙的数据库，同时还简化了应用程序逻辑，因为我们不需要显式锁定和解锁表。因此，InnoDB存储引擎是MySQL的默认设置。

MySQL对除InnoDB之外的所有存储引擎使用表锁（而不是页锁，行或列锁）。锁操作本身没有太多开销。但是因为在任何时候只有一个会话可以写入表，为了发挥其他存储引擎的最佳性能，建议将它们用于经常查询且很少插入或更新的表。

#### Performance Considerations Favoring InnoDB

在选择是使用InnoDB还是使用其他存储引擎创建表时，需要明确表锁的以下缺点：

• 表锁允许多个会话同时从表中读，但如果有会话要对表进行写操作，则必须首先获得独占访问权，这意味着它可能必须等待其他会话完成对表的访问。在更新期间，所有其他想要访问此特定表的会话必须等到更新完成。

• 表锁可能会引发如下问题：当一个会话在等待锁时，因为磁盘已满，但是会话继续执行需要可用磁盘空间。在这种情况下，所有想要访问问题表的会话也会处于等待状态，直到有更多磁盘空间可用。

• 需要花费很长时间SELECT语句会阻止其他会话在此期间更新表，使其他会话显得缓慢或无响应。当会话等待获得对表的独占访问以进行更新时，发出SELECT语句的其他会话将在其后排队，即使对于只读会话也会降低并发性。

#### Workarounds for Locking Performance Issues

以下各项描述了避免或减少表锁竞争的一些方法：

• 考虑将表切换到InnoDB存储引擎，在创建表过程中使用CREATE TABLE ... ENGINE=INNODB，对现有表使用ALTER TABLE ... ENGINE=INNODB。

• 优化SELECT语句的执行速度，使它们锁定时间更短。可能必须额外创建一些汇总表来执行此操作。

• 使用--low-priority-updates启动mysqld。对于仅使用表锁的存储引擎（例如MyISAM，MEMORY和MERGE），这使得所有写（修改）表的语句的优先级低于SELECT语句。在这种情况下，SELECT语句将在UPDATE语句之前执行，并且不会等待之前的SELECT完成。

• 要指定在特定连接中发出的所有更新都应以低优先级完成，将low_priority_updates服务器系统变量设置为1。

• 要为特定的INSERT，UPDATE或DELETE语句赋予较低的优先级，使用LOW_PRIORITY属性。

• 要为特定的SELECT语句赋予更高的优先级，使用HIGH_PRIORITY属性。

• 启动mysqld时为max_write_lock_count系统变量设置较低值，以强制MySQL临时提升在对表进行特定数量的插入后等待的SELECT语句的优先级。这允许在一定数量的WRITE锁之后进行READ锁。

• 如果INSERT与SELECT结合存在问题，考虑切换到支持并发SELECT和INSERT语句的MyISAM存储引擎。

• 如果混合SELECT和DELETE语句有问题，DELETE的LIMIT选项可能会有所帮助。

• 将SQL_BUFFER_RESULT与SELECT语句一起使用可以帮助缩短表锁的时间。

• 通过允许查询针对一个表中的列运行，将表内容拆分为单独的表可能会有所帮助，而更新仅限于不同表中的列。

• 可以更改mysys/thr_lock.c中的锁代码以使用单个队列。在这种情况下，写锁和读锁具有相同的优先级，这可能有助于某些应用程序。

### Concurrent Inserts

MyISAM存储引擎支持并发插入以减少给定表的读和写之间的锁争用：如果MyISAM表在数据文件中没有"空隙"（中间删除的行），则可以在执行INSERT语句向数据文件末尾添加行表的同时执行SELECT语句从表中读取行。如果有多个INSERT语句，它们将按顺序排队并与SELECT语句同时执行。并发INSERT的结果可能不会立即可见。

可以设置concurrent_insert系统变量以修改并发插入处理逻辑。默认情况下，变量设置为AUTO（或1），并发插入的处理方式如前所述。如果concurrent_insert设置为NEVER（或0），则禁用并发插入。如果变量设置为ALWAYS（或2），则即使对于有"空隙"的表，也允许在表末尾进行并发插入。

如果使用二进制日志，则并发插入将转换CREATE ... SELECT或INSERT ... SELECT语句为正常插入语句。这样做是为了确保我们可以在备份操作期间通过应用日志来重新创建表的精确副本。In addition, for those statements a read lock is placed on the selected-from table such that inserts into that table are blocked。结果是该表的并发插入也必须等待。

使用LOAD DATA INFILE时，如果对满足并发插入条件的MyISAM表（即不包含中间的空闲块）指定CONCURRENT，则其他会话可以在LOAD DATA执行时从表中检索数据。即使没有其他会话同时使用该表，使用CONCURRENT选项也会影响LOAD DATA的性能。如果指定HIGH_PRIORITY，则在使用该选项启动服务器时，它将覆盖--low-priority-updates选项的效果。它还会导致不使用并发插入。

对于LOCK TABLE，READ LOCAL锁和READ锁之间的区别在于READ LOCAL允许在保持锁定时执行非冲突的INSERT语句（并发插入）。但是，如果要在保持锁定时使用服务器外部的进程操作数据库，则无法使用此功能。

### Metadata Locking

### External Locking

## Optimizing the MySQL Server

### System Factors

### Optimizing Disk I/O

### Using Symbolic Links

### Optimizing Memory Use

### Optimizing Network Use
