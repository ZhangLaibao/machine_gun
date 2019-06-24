## Optimizing for InnoDB Tables

InnoDB是MySQL中的默认存储引擎。在可靠性和并发性都非常重要的生产环境下，InnoDB是应用最广泛的存储引擎。

### Optimizing Storage Layout for InnoDB Tables - 优化存储布局

• 一旦数据量达到稳定大小，或者表增加了几十或几百兆字节数据，此时需要考虑使用OPTIMIZE TABLE语句重新组织表并任何浪费的空间。重新组织的表在执行全表扫描时需要较少的磁盘I/O。这是一种简单实用的技术，可以在其他技术（如改进索引使用或调整应用程序代码）收效甚微时提高性能。

OPTIMIZE TABLE会复制表的数据部分并重建索引。其改进来自于改进了索引中数据的压缩，减少了表空间和磁盘上的碎片。改进的效果取决于每个表中的数据。我们可能会发现OPTIMIZE TABLE有时候可能会得到明显的性能提升，但有时候又收效甚微。或者在下次优化表之前，性能提升会随着时间的推移而降低。如果表很大或者正在重建的索引与缓冲池大小不匹配，则此操作可能会很慢。向表中添加大量数据后的第一次运行通常比以后的运行慢得多。

• 在InnoDB中，具有长PRIMARY KEY（无论主键是具有冗长值的单个列或者由多个列组成的冗长复合值）的表会浪费大量磁盘空间。在指向同一行的所有二级索引记录中，行的主键值都是冗余的。如果主键为值很长，则创建AUTO_INCREMENT列作为主键，或者索引冗长的VARCHAR列的前缀而不是整列。

• 使用VARCHAR数据类型而不是CHAR来存储可变长度字符串或具有许多NULL值的列。即使字符串较短或其值为NULL时，CHAR(N)列也始终使用N个字符来存储数据。较小的表更适合缓冲池并减少磁盘I/O。当使用COMPACT行格式（默认的InnoDB行格式）和可变长度字符集（如utf8或sjis）时，CHAR(N)列占用可变的空间量，但仍至少占用N个字节。

• 对于大表或包含大量重复文本或数字数据的表，考虑使用COMPRESSED行格式。这样在将数据放入缓冲池或执行全表扫描需时要较少的磁盘I/O。在做出决定之前，可以使用COMPRESSED与COMPACT行格式测量可以实现的压缩量。

### Optimizing InnoDB Transaction Management - 优化事务管理

要优化InnoDB事务处理，需要在事务功能的性能开销和服务器的工作负载之间找到理想的平衡点。例如，如果应用程序每秒提交数千次，则可能会遇到性能问题，如果仅每2-3小时提交一次，则会遇到另外的性能问题。

• 默认的MySQL设置AUTOCOMMIT = 1可能会对高负载的数据库服务器带来性能瓶颈。在可行的情况下，通过发出SET AUTOCOMMIT = 0或START TRANSACTION语句，然后在进行所有更改后发出COMMIT语句，将几个相关的数据更改操作包装到单个事务中。

如果在一个事务中对数据库进行了修改，InnoDB必须在每次事务提交时将日志刷新到磁盘。如果每次更改后都提交（默认的autocommit），存储设备的I/O吞吐量会接近每秒潜在操作的数量。

• 对于仅包含单个SELECT语句的事务，启用AUTOCOMMIT可帮助InnoDB识别只读事务并对其进行优化。

• 插入，更新或删除大量行后，避免执行回滚。如果一个大的事务正在降低服务器性能，那么回滚它会使问题变得更糟，相较于执行原始数据更改操作可能需要几倍的时间。杀死数据库进程没有用，因为在服务器启动时会再次启动回滚。尽量减少此问题发生的可能性：

	• 增加缓冲池的大小，以便可以缓存所有数据更改，而不是立即写入磁盘。
	
	• 设置innodb_change_buffering = all，以在缓存插入之外还缓冲更新和删除操作。
	
	• 在大量数据更改操作期间定期发出COMMIT语句，将单个删除或更新改写为对较少行进行操作的多个语句

要摆脱失控的回滚，可以增加缓冲池大小以使回滚变为CPU限制并快速运行，或者终止服务器并使用innodb_force_recovery = 3重新启动。

使用默认设置innodb_change_buffering = all的情况下，此问题很少发生，它允许将更新和删除操作缓存在内存中，从而使它们在第一时间更快地执行，并且如果需要还可以更快地回滚。确保在处理具有许多插入，更新或删除的长时间运行事务的服务器上使用此参数设置。

• 如果应用程序能容忍在发生崩溃的情况下丢失一些最新提交的事务，则可以将innodb_flush_log_at_trx_commit参数设置为0。InnoDB尝试每秒刷新一次日志，尽管不保证此刷新频率。此外，将innodb_support_xa的值设置为0，这将减少因磁盘数据和二进制日志同步而导致的磁盘刷新次数。

> innodb_support_xa已弃用，将在以后的版本中删除。从MySQL 5.7.10开始，默认始终启用InnoDB对XA事务中两阶段提交的支持，并且不再允许禁用innodb_support_xa。

• 修改或删除行时，不会立即或甚至在事务提交后立即删除行数据和关联的撤消日志。 旧数据将保留到先前或同时启动的事务完成，以便这些事务可以访问已修改或已删除行的先前状态。因此，长时间运行的事务可以阻止InnoDB清除由不同事务更改的数据。

• 在长时间运行的事务中修改或删除行时，使用READ COMMITTED和REPEATABLE READ隔离级别的其他事务必须执行额外的工作，以在读取相同行时重建旧数据。

• 当长时间运行的事务修改表时，从其他事务对该表的查询不使用覆盖索引技术。通常可以从二级索引检索所有结果列的查询，而不是从表数据中查找适当的值。但如果发现二级索引页的PAGE_MAX_TRX_ID太新，或者二级索引中的记录被标记为删除，则InnoDB可能需要使用聚簇索引查找记录。

### Optimizing InnoDB Read-Only Transactions - 优化只读事务

InnoDB可以避免与已知为只读的事务设置事务ID（TRX_ID字段）相关的开销。只有可能执行写操作或锁定读取的事务（例如SELECT ... FOR UPDATE）才需要事务ID。消除不必要的事务ID会减少每次查询或数据更改语句构造读取视图时所读取的内部数据结构的大小。InnoDB在下列情况下检测到只读事务:

• 事务以START TRANSACTION READ ONLY语句开始。在这种情况下，尝试更改数据库（对于InnoDB，MyISAM或其他类型的表）会导致错误，并且事务将继续保持只读状态：

> ERROR 1792 (25006): Cannot execute statement in a READ ONLY transaction.

此时我们仍然可以在只读事务中更改特定于会话的临时表，或者向它们发出锁定请求，因为这些更改和锁对任何其他事务都不可见。

• 自动提交设置已打开，因此可以保证事务只有单个语句，并且构成事务的单个语句是“非锁定”SELECT语句。也就是说，SELECT不使用FOR UPDATE或LOCK IN SHARED MODE子句。

• 事务在没有READ ONLY选项的情况下启动，但是尚未执行显式锁定行的更新或语句。在需要更新或显式锁定之前，事务将保持只读模式。因此，对于读密集型应用程序（如报表生成应用），我们可以通过在START TRANSACTION READ ONLY和COMMIT中对它们进行分组来调整InnoDB查询顺序，或者在运行SELECT语句之前打开自动提交设置，或者简单地避免任何散布在查询中的数据更改语句。

> 符合自动提交，非锁定和只读（AC-NLRO）的事务不在某些内部InnoDB数据结构之内，因此未在SHOW ENGINE INNODB STATUS输出中列出。

### Optimizing InnoDB Redo Logging - 优化事务日志

考虑以下有关优化事务日志的准则：

• 增大事务日志文件变，甚至与缓冲池一样大。当InnoDB将事务日志文件写满时，它必须将检查点中缓冲池的修改内容写入磁盘。小的重做日志文件会导致多次不必要的磁盘写入。虽然特别大的重做日志文件会导致冗长的恢复时间，但现在恢复速度更快，我们可以放心地使用大型重做日志文件。使用innodb_log_file_size和innodb_log_files_in_group配置选项配置重做日志文件的大小和数量。

• 增加日志缓冲区的大小。大的日志缓冲区可以在事务提交之前运行大型事务，而无需将日志写入磁盘。因此，如果有更新，插入或删除许多行的事务，则使用更大的日志缓冲区可以节省磁盘I/O。使用innodb_log_buffer_size配置选项配置日志缓冲区大小。

### Bulk Data Loading for InnoDB Tables - 批量数据载入

• 批量数据导入InnoDB时，关闭自动提交，因为自动提交会为每个插入执行磁盘日志刷新。要在导入操作期间禁用自动提交，使用SET autocommit和COMMIT语句包裹SQL语句：

```sql
SET autocommit=0;
... SQL import statements ...
COMMIT;
```

mysqldump选项--opt创建快速导入InnoDB表的转储文件，在没有使用SSET autocommit和COMMIT语句包裹SQL语句时也能达到相同的效果。

• 如果导入的表二级索引具有UNIQUE约束，则可以通过在导入会话期间临时关闭唯一性检查来加快导入速度：

```sql
SET unique_checks=0;
... SQL import statements ...
SET unique_checks=1;
```

对于大表，这节省了大量磁盘I/O，因为InnoDB可以使用其更改缓冲区批量写入二级索引记录。当然此时需要我们确保数据不包含重复键。

• 如果导入的表二级索引具有FOREIGN KEY约束，则可以通过在导入会话期间临时关闭外键检查来加快导入速度：

```sql
SET foreign_key_checks=0;
... SQL import statements ...
SET foreign_key_checks=1;
```

对于大表，这节省了大量磁盘I/O。

• 如果需要插入多行，请使用多行INSERT语法来减少客户端和服务器之间的通信开销：

```sql
INSERT INTO yourtable VALUES (1,2), (5,5), ...;
```

此方法适用于插入任何表，而不仅仅是InnoDB表。

• 在向带有自增列的表进行批量插入时，请将innodb_autoinc_lock_mode设置为2而不是默认值1。

• 执行批量插入时，以PRIMARY KEY顺序插入行会更快。InnoDB表使用聚簇索引，这使得以PRIMARY KEY的顺序使用数据相对较快。以PRIMARY KEY顺序执行批量插入对于不完全适合缓冲池的表尤为重要。

• 要在将数据加载到InnoDB FULLTEXT索引时获得最佳性能，遵循以下步骤：

1. 在表创建时定义列FTS_DOC_ID，类型为BIGINT UNSIGNED NOT NULL，在此列上创建名为FTS_DOC_ID_INDEX的唯一索引。 例如 ：

  ```sql
  CREATE TABLE t1 (
  	FTS_DOC_ID BIGINT unsigned NOT NULL AUTO_INCREMENT,
  	title varchar(255) NOT NULL DEFAULT '',
  	text mediumtext NOT NULL,
  	PRIMARY KEY (`FTS_DOC_ID`)
  ) ENGINE=InnoDB DEFAULT CHARSET=latin1;
  CREATE UNIQUE INDEX FTS_DOC_ID_INDEX on t1(FTS_DOC_ID);
  ```

2. 将数据加载到表中。

3. 加载数据完毕后创建FULLTEXT索引。

  > 在建表语句中添加FTS_DOC_ID列时，需要确保在更新FULLTEXT索引列时更新FTS_DOC_ID列，因为FTS_DOC_ID必须随每个INSERT或UPDATE单调增加。如果我们选择不在创建表时添加FTS_DOC_ID并让InnoDB为我们管理DOC ID，InnoDB将使用下一个CREATE FULLTEXT INDEX调用将FTS_DOC_ID添加为隐藏列。但是，这种方法需要进行表重建，这会影响性能。

### Optimizing InnoDB Queries - 优化查询

要优化InnoDB表的查询，需要我们在每个表上创建一组适当的索引。InnoDB索引的创建遵循以下准则：

• 因为每个InnoDB表都有一个主键（无论我们是否显式声明需要一个主键），所以最好显式为每个表指定一组主键列，这些列用于最重要和时间要求最高的查询。

• 不要在主键中指定太多或太长的列，因为这些列值在每个二级索引中都是重复的。当索引包含不必要的数据时，读取此数据的I/O和用于缓存它的内存会降低服务器的性能和可扩展性。

• 不要单独为每列创建二级索引，因为每个查询都只能使用一个索引。很少求值的列或只有少量不同值的列的索引可能对任何查询都没有帮助。如果对同一个表有很多查询，测试不同的列组合，尝试创建少量复合索引而不是大量单一索引。如果索引包含结果集所需的所有列（称为覆盖索引），则查询可能会完全避免读取表数据。

• 如果索引列不能包含任何NULL值，在创建表时最好将其声明为NOT NULL。当知道每个列是否包含NULL值时，优化器可以更好地确定哪个索引可以最有效地用于查询。

• 优化InnoDB表的单查询事务（只读事务）

### Optimizing InnoDB DDL Operations - 优化DDL

• 对于表和索引的DDL操作（CREATE，ALTER和DROP语句），InnoDB表最重要的方面是在MySQL 5.5及更高版本中创建和删除二级索引比在早期版本中快得多。

• “Fast index creation”使得在某些情况下更快地在将数据加载到表中之前删除索引，然后在加载数据之后重新创建索引。

• 使用TRUNCATE TABLE清空表，而不是DELETE FROM tbl_name。外键约束可以使TRUNCATE语句的性能跟常规DELETE语句一样，在这种情况下，像DROP TABLE和CREATE TABLE这样的命令组合可能是最快的。

• 因为主键是每个InnoDB表的存储布局的组成部分，并且更改主键的定义涉及重新组织整个表，所以始终将主键设置为CREATE TABLE语句的一部分，并提前计划以免执行之后的ALTER或DROP主键操作。

### Optimizing InnoDB Disk I/O - 优化磁盘I/O

如果我们遵循数据库设计和SQL调优技术的最佳实践，但由于磁盘I/O负担很重，数据库仍然很慢，那么可以考虑这些磁盘I/O优化。如果Unix 'top' 工具或Windows任务管理器显示的工作负载中CPU使用率低于70％，则我们服务器的工作负载可能是磁盘限制的。

**• Increase buffer pool size - 增大缓冲池大小**

当表数据缓存在InnoDB缓冲池中时，查询操作可以询重复访问它，而无需任何磁盘I/O。使用innodb_buffer_pool_size选项指定缓冲池的大小。此内存区域非常重要，通常建议将innodb_buffer_pool_size配置为系统内存的50％到75％。

**• Adjust the flush method - 调整刷新方法**

在某些版本的GNU/Linux和Unix中，调用Unix的fsync()函数（默认情况下InnoDB使用）和类似方法将文件刷新到磁盘的速度非常慢。如果数据库写入性能存在问题，可以将innodb_flush_method参数设置为O_DSYNC进行基准测试。

**• Use a noop or deadline I/O scheduler with native AIO on Linux - 在Linux上使用noop或deadline I/O调度器调度本地AIO**

InnoDB使用Linux上的异步I/O子系统（本机AIO）来执行数据文件页的预读和写请求。此行为由innodb_use_native_aio配置选项控制，该选项默认启用。对于本机AIO，I/O调度程序的类型对I/O性能的影响更大。 通常，建议使用noop和deadline I/O调度程序。通过执行基准测试可以确定哪个I/O调度程序能为我们的工作负载和环境提供最佳性能。

**• Use direct I/O on Solaris 10 for x86_64 architecture**

**• Use raw storage for data and log files with Solaris 2.6 or later**

**• Use additional storage devices**

**• Consider non-rotational storage**

非转动存储设备通常能为随机I/O操作提供更好的性能；转动存储设备更适用于顺序I/O操作。在转动和非转动存储设备之间分发数据和日志文件时，考虑主要在每个文件上执行的I/O操作的类型。

**• Increase I/O capacity to avoid backlogs**

如果由于InnoDB checkpoint操作导致吞吐量周期性下降，可以考虑增加innodb_io_capacity配置选项的值。较高的值会带来更频繁的刷新，从而避免积压工作可能导致的吞吐量下降。

**• Lower I/O capacity if flushing does not fall behind**

如果系统InnoDB刷新操作没有落后，可以考虑降低innodb_io_capacity配置选项的值。通常，我们将此选项值保持尽可能低，但不要太低以至于导致上述的吞吐量周期性下降。

**• Store system tablespace files on Fusion-io devices**

**• Disable logging of compressed pages**

### Optimizing InnoDB Configuration Variables - 优化配置变量

对于具有轻量、可预测负载的服务器和始终满负荷运行的服务器，或者遇到高活动高峰的服务器来说，他们各自有不同的最佳配置。由于InnoDB存储引擎会自动执行许多优化工作，因此大部分性能调整任务就包括监控以确保数据库性能良好，并在性能下降时更改配置选项。

我们可以执行的主要配置步骤包括：

• 使InnoDB在支持的系统上使用高性能内存分配器。

• 控制InnoDB缓存的写操作类型，以避免频繁的小磁盘写入。因为默认是缓冲所有类型的写操作，所以只有在需要减少缓冲数据量时才更改此设置。

• 使用innodb_adaptive_hash_index选项打开和关闭自适应哈希索引功能。我们可以在一些不平常的活动期间更改此设置，使用完后将其恢复为原始设置。

• 如果线程上下文切换成为瓶颈，则对InnoDB处理的并发线程数设置限制。

• 控制InnoDB通过其预读操作执行的数据预读取量。当系统有空闲的I/O容量时，更多预读可以提高查询性能。
过多的预读会导致负载很重的系统性能周期性下降。

• 如果默认配置值未能充分利用我们具有的高端I/O子系统，则可以增加读取或写入操作的后台线程数。

• 控制InnoDB在后台执行的I/O数量。如果我们观察到性能周期性下降，则可以减小此设置。

• 控制确定InnoDB何时执行某些类型的后台写入的算法。该算法适用于某些类型的工作负载，但不适用于其他工作负载，因此如果我们观察到性能周期性下降，可以关闭此设置。

• 利用多核处理器及其高速缓存配置，最大限度地减少上下文切换的延迟。

• 防止表扫描等一次性操作干扰存储在InnoDB缓冲区高速缓存中的频繁访问的数据。

• 将日志文件调整为对可靠性和崩溃恢复合理的大小。InnoDB日志文件通常保持较小，以避免崩溃后启动时间过长。MySQL 5.5中引入的优化加速了崩溃恢复过程的某些步骤。特别是，由于改进了内存管理算法，扫描重做日志和应用重做日志的速度更快。如果我们认为将日志文件缩小以避免过长的启动时间，则现在可以考虑增加日志文件大小以减少基于重做日志恢复数据而发生的I/O.

• 配置InnoDB缓冲池的实例大小和数量，对于具有几千兆字节缓冲池的系统尤其重要。 

• 增加最大并发事务数，从而显著提高繁忙数据库的可伸缩性。

• 将清除操作（一种垃圾收集）移动到后台线程中。要有效地测试此设置的效果，需要首先调整其他与I/O相关的配置和与线程相关的配置设置。

• 减少InnoDB在并发线程之间进行的切换次数，以便繁忙服务器上的SQL操作不会排队并堵塞。为innodb_thread_concurrency选项设置一个值，对于高性能的现代系统，最多约为32。增加innodb_concurrency_tickets选项的值，通常为5000左右。 这些选项的组合设置了InnoDB在任何时候处理的线程数量的上限，并允许每个线程在被换出之前执行大量工作，这样等待线程的数量保持很低并且操作可以在没有过多上下文切换的情况下完成。

### Optimizing InnoDB for Systems with Many Tables - 优化多表

如果已配置非持久优化器统计信息(non-persistent optimizer statistics)（非默认配置，默认情况下，优化程序统计信息将持久保存到磁盘，并由innodb_stats_persistent配置选项启用），则InnoDB会在启动后第一次访问该表时计算表的索引基数值，而不是在表中存储此类值。对于将数据分区为多个表的系统，此步骤可能会花费大量时间。由于此开销仅发生在初始表打开操作，要“预热”表以供以后使用，可以在启动后立即通过发出SELECT 1 FROM tbl_name LIMIT 1等语句来访问表。

## Optimizing for MyISAM Tables

MyISAM存储引擎在多读少写或低并发操作时表现最佳，因为其表锁限制了执行同步更新的能力。在MySQL中，InnoDB是默认的存储引擎，而不是MyISAM（MySQL的存储引擎分为官方提供的存储引擎和第三方提供的存储引擎，InnoDB虽然不是官方提供的存储引擎，但由于其卓越的性能和市场欢迎程度，所以M有SQL采用其作为默认存储引擎）。

MyISAM存储引擎表文件由.myd和.myi组成，前者用于存放数据文件，后者用于存放索引文件。运行时MySQL只缓存索引文件，数据文件的缓存由操作系统本身完成，这一点与其他使用LRU算法缓存数据的大部分数据库不同。

### Optimizing MyISAM Queries

有关加快MyISAM表查询速度的一些通用技巧：

• 为了帮助MySQL更好地优化查询，在向表里载入数据后使用ANALYZE TABLE或运行myisamchk --analyze以更新每个索引部分的值，以指示具有相同索引值的平均行数（对于唯一索引，值始终为1）。当我们基于非常量表达式连接两个表时，MySQL使用这个平均行数来决定选择哪个索引。我们还可以使用SHOW INDEX FROM tbl_name检查表分析的结果并检查基数值，使用myisamchk --description --verbose显示索引分布信息。

• 要根据索引对索引和数据进行，使用myisamchk --sort-index --sortrecords = 1（假设要对索引1进行）。如果表有一个唯一索引，在根据索引按顺序读取所有行时，这是一种更快速查询的好方法。第一次以这种方式对大表进行时，可能需要很长时间。

• 尽量避免在频繁更新的MyISAM表上执行复杂SELECT查询，以避免由于读写之间的竞争而导致的表锁定问题。

• MyISAM支持并发插入：如果表在数据文件的中间没有空闲块，则可以在其他线程从表中读取的同时将新行插入其中。如果确实需要支持并发插入，需要我们以避免删除行的方式使用该表。另一种可能性是在从表中删除大量行后运行OPTIMIZE TABLE对表进行碎片整理。通过设置concurrent_insert变量可以更改此行为。我们可以强制添加新行时追加到数据文件末尾（因此允许并发插入），即使在已删除行的表中也是如此。

• 对于经常更改的MyISAM表，避免使用所有可变长度列（VARCHAR，BLOB和TEXT）。只要表包含一个可变长度列，则表使用动态行格式。

• 因为单行变大而将表拆分成不同的表通常没有用。在访问行数据时，最大的性能损失是找到行的第一个字节所需的磁盘搜索。定位数据后，大多数现代磁盘可以足够快地读取整行数据，以满足大多数应用程序的要求。拆分表可以产生明显性能差异的唯一情况是，对于使用动态行格式的MyISAM表，我们可以用这种方式将其更改为固定行大小的格式，或者如果我们需要经常扫描表但大部分列数据是用不到的。

• 如果我们通常按照expr1，expr2，... 顺序检索表中的行数据。那么在修改表结构时使用ALTER TABLE ... ORDER BY expr1，expr2，...顺序。当对表进行大量更改时使用此选项，可以获得更高的性能。

• 如果我们的应用程序经常需要根据来自大量行的信息计算结果，例如计数，则最好引入新表并实时更新计数器。
以下表的更新非常快：

```sql
UPDATE tbl_name SET count_col=count_col+1 WHERE key_col=constant;
```

对于只支持表级锁（具有单个写多个读）的MyISAM引擎，这非常重要。这也为大多数数据库系统提供了更好的性能，因为在这种情况下行锁定管理器做的事情较少。

• 定期使用OPTIMIZE TABLE优化使用动态格式的MyISAM表碎片。

• 创建MyISAM表时使用DELAY_KEY_WRITE = 1选项会使索引更新更快，因为在表关闭之前它们不会被刷新到磁盘。缺点是如果在这样的表打开时某些原因杀死了服务器，我们必须通过--myisam-recover-options选项启动服务器，或者在重新启动服务器之前运行myisamchk来确保表数据没有发生丢失的。（但是，即使在这种情况下，使用DELAY_KEY_WRITE也不会丢失任何数据，因为始终可以从数据行生成key信息）

• 字符串在MyISAM索引中自动压缩前缀和末尾空白字符。

 • 我们可以通过在应用程序中缓存查询或结果，然后一次性执行多条插入或更新来提高性能。在此操作期间锁定表可确保仅在所有更新完成后仅刷新索引缓存一次。我们也可以利用MySQL的查询缓存来实现类似的结果。

### Bulk Data Loading for MyISAM Tables

• 对于MyISAM表，如果对没有数据文件中间的行的删除操作，则可以使用并发插入在SELECT语句运行的同时添加行。

• 通过一些额外的工作，当表有多个索引时，遵循以下步骤可以使MyISAM表的LOAD DATA INFILE运行得更快：

1. 执行FLUSH TABLES语句或mysqladmin flush-tables命令。
2. 使用myisamchk --keys-used=0 -rq /path/to/db/tbl_name删除表的所有索引。
3. 使用LOAD DATA INFILE将数据插入表中。这不会更新任何索引，因此非常快。
4. 如果将来该表将来用作只读表，可以使用myisampack进行压缩。
5. 使用myisamchk -rq /path/to/db/tbl_name创建之前的索引。这会在将数据写入磁盘之前在内存中创建索引树，比在LOAD DATA INFILE期间单条更新索引要快得多，因为它避免了大量磁盘寻址工作。生成的索引树也是完美的平衡树。
6. 执行FLUSH TABLES语句或mysqladmin flush-tables命令。

如果插入数据的MyISAM表为空，则LOAD DATA INFILE会自动执行上述优化。自动优化和显式使用上述过程的主要区别在于，我们可以让myisamchk为索引创建分配更多的临时内存，而不是在执行LOAD DATA INFILE语句时服务器为索引重建分配的默认大小内存。

我们还可以使用以下语句而不是myisamchk禁用或启用MyISAM表的非唯一索引。如果使用这些语句，则可以跳过FLUSH TABLES操作：

```sql
ALTER TABLE tbl_name DISABLE KEYS;
ALTER TABLE tbl_name ENABLE KEYS;
```

• 要加快使用非事务表的多个INSERT语句执行的速度，最好锁定表：

```sql
LOCK TABLES a WRITE;
INSERT INTO a VALUES (1,23),(2,34),(4,33);
INSERT INTO a VALUES (8,26),(6,29);
...
UNLOCK TABLES;
```

这有利于提高性能，因为在完成所有INSERT语句之后，索引缓冲区仅刷新到磁盘一次。通常，索引缓冲区刷新到磁盘的次数与INSERT语句一样多的。如果可以使用单个INSERT插入所有行，则不需要显式锁定语句。

锁还可以降低多连接测试的总时间，尽管单个连接的最长等待时间可能会因为等待锁定而上升。假设五个客户端尝试同时执行插入，如下所示：

• 连接1执行1000次插入
• 连接2,3和4进行1次插入
• 连接5执行1000次插入

如果不使用锁，则连接2,3和4在1和5之前执行完毕。如果使用锁，则连接2,3和4可能需要等待1和5完成，但总时间会节约大约40％。INSERT，UPDATE和DELETE操作在MySQL中非常快，但是我们仍然可以通过对大约每五次连续INSERT或UPDATE添加锁定来获得更好的整体性能。如果我们需要做很多连续的插入，可以先使用一个LOCK TABLES开始，然后偶尔用一个UNLOCK TABLES（每1000行左右）来允许其他线程访问表。这仍然会带来不错的性能提升。导入数据的INSERT仍然比LOAD DATA INFILE慢得多，即使使用刚刚概述的策略也是如此。

• 要提高MyISAM表的性能，不管对于LOAD DATA INFILE还是INSERT，通过增加key_buffer_size系统变量来增大key cache。

### Optimizing REPAIR TABLE Statements - OMITED

## Optimizing for MEMORY Tables

MEMORY存储引擎原名HEAP存储引擎，由于它将所有的表数据存储在内存中，所以其速度非常快，但是在使用上会有一些限制，例如只支持表锁，并发性能较差，不支持TEXT和BLOB数据类型，并且存储可变长度数据类型字段时是按照定长数据类型处理的。此外如果数据库重启或者崩溃，那么MEMORY引擎中的数据会全部丢失。MEMORY引擎默认使用HASH索引。

MEMORY存储引擎常用于经常访问的非关键数据（例如存储临时数据的临时表或者数据仓库中的纬度表），并且这些数据是只读的或很少更新。在实际工作负载下针对对应的InnoDB或MyISAM表对进行基准测试，以确定我们的应用程序使用MEMORY引擎带来的性能提升值得付出丢失数据的风险，或者在应用程序启动时从基于磁盘的表复制数据的开销。

为了发挥出MEMORY表的最佳性能，我们需要测试针对每个表的查询类型，并指定每个索引的类型（B树索引或哈希索引）（在CREATE INDEX语句中，使用USING BTREE或USING HASH子句指定索引类型） 对于通过>或BETWEEN等运算符进行大于或小于比较的查询，B-Tree索引很快。HASH索引仅适用于通过=运算符查找单个值的查询，或通过IN运算符查找一组特定值的查询。 

## Storage Engines Feature Summary

| Feature                                     | MyISAM          | Memory              | InnoDB          | Archive | NDB             |
| ------------------------------------------- | --------------- | ------------------- | --------------- | ------- | --------------- |
| Storage limits                              | 256TB           | RAM                 | 64TB            | None    | 384EB           |
| Transactions                                | No              | No                  | Yes             | No      | Yes             |
| Locking granularity                         | Table           | Table               | Row             | Row     | Row             |
| MVCC(多版本并发控制)                        | No              | No                  | Yes             | No      | No              |
| Geospatial data type support                | Yes             | No                  | Yes<sup>a</sup> | Yes     | Yes             |
| B-tree indexes                              | Yes             | Yes                 | Yes             | No      | No              |
| T-tree indexes                              | No              | No                  | No              | No      | Yes             |
| Hash indexes                                | No              | Yes                 | No<sup>b</sup>  | No      | Yes             |
| Full-text search indexes                    | Yes             | No                  | Yes<sup>c</sup> | No      | No              |
| Clustered indexes                           | No              | No                  | Yes             | No      | No              |
| Data caches                                 | No              | N/A                 | Yes             | No      | Yes             |
| Compressed data                             | Yes<sup>d</sup> | No                  | Yes<sup>e</sup> | Yes     | No              |
| Encrypted data<sup>f</sup>                  | Yes             | Yes                 | Yes             | Yes     | Yes             |
| Cluster database support                    | No              | No                  | No              | No      | Yes             |
| Replication support<sup>g</sup>             | Yes             | Limited<sup>h</sup> | Yes             | Yes     | Yes             |
| Foreign key support                         | No              | No                  | Yes             | No      | Yes<sup>i</sup> |
| Backup / point-in-time recovery<sup>j</sup> | Yes             | Yes                 | Yes             | Yes     | Yes             |
| Query cache support                         | Yes             | Yes                 | Yes             | Yes     | Yes             |
| Update statistics for data
dictionary        | Yes             | Yes                 | Yes             | Yes     | Yes             |

a:InnoDB support for geospatial indexing is available in MySQL 5.7.5 and later.
b:InnoDB utilizes hash indexes internally for its Adaptive Hash Index feature.
c:InnoDB support for FULLTEXT indexes is available in MySQL 5.6.4 and later.
d:Compressed MyISAM tables are supported only when using the compressed row format. Tables using the compressed row format with MyISAM are read only.
e:Compressed InnoDB tables require the InnoDB Barracuda file format.
f:Implemented in the server (via encryption functions). Data-at-rest tablespace encryption is available in MySQL 5.7 and later.
g:Implemented in the server, rather than in the storage engine.
h:See the discussion later in this section.
i:Support for foreign keys is available in MySQL Cluster NDB 7.3 and later.
j:Implemented in the server, rather than in the storage engine.