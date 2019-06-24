### Optimizing INFORMATION_SCHEMA Queries

监控数据库的应用程序可能会频繁使用INFORMATION_SCHEMA数据库的表。对INFORMATION_SCHEMA表的一些类型查询也可以被优化以获得更高的执行速度。目标是最小化文件操作（例如，扫描目录或打开表文件）以收集组成这些动态表的信息。

##### Try to use constant lookup values for database and table names in the WHERE clause - 在WHERE子句中对数据库和表名使用常量查找值

我们可以按照以下方法利用此原则：

• 要查找数据库或表，使用可以求值为常量的表达式，例如字面值、返回常量的函数或标量子查询

• 避免使用非常量数据库名称查找值（或没有查找值）的查询，因为它们需要扫描数据目录以查找匹配的数据库目录名称

• 在数据库中，避免使用非常量表名查找值（或没有查找值）的查询，因为它们需要扫描数据库目录以查找匹配的表文件

此原则适用于下表中显示的INFORMATION_SCHEMA表，其中显示了供服务器使用以避免目录扫描的常量查找值。例如，如果查询TABLES数据，则在WHERE子句中使用TABLE_SCHEMA的常量查找值可以避免数据目录扫描。

| Table                   | Column to specify to avoid<br/>data directory scan | Column to specify to avoid<br/>database directory scan |
| ----------------------- | -------------------------------------------------- | ------------------------------------------------------ |
| COLUMNS                 | TABLE_SCHEMA                                       | TABLE_NAME                                             |
| KEY_COLUMN_USAGE        | TABLE_SCHEMA                                       | TABLE_NAME                                             |
| PARTITIONS              | TABLE_SCHEMA                                       | TABLE_NAME                                             |
| REFERENTIAL_CONSTRAINTS | CONSTRAINT_SCHEMA                                  | TABLE_NAME                                             |
| STATISTICS              | TABLE_SCHEMA                                       | TABLE_NAME                                             |
| TABLES                  | TABLE_SCHEMA                                       | TABLE_NAME                                             |
| TABLE_CONSTRAINTS       | TABLE_SCHEMA                                       | TABLE_NAME                                             |
| TRIGGERS                | EVENT_OBJECT_SCHEMA                                | EVENT_OBJECT_TABLE                                     |
| VIEWS                   | TABLE_SCHEMA                                       | TABLE_NAME                                             |

将数据库名称限制为特定常量的查询的好处是只需要对指定的数据库目录进行检索，在下面的例子中，使用数据库名称字面量test使服务器只检查数据库test目录，而不管可能有多少其他数据库：

```sql
SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_SCHEMA = 'test';
```

相比之下，以下查询效率较低，因为它需要扫描数据目录以确定哪些数据库名称与模式'test％'匹配：

```sql
SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_SCHEMA LIKE 'test%';
```

对于将表名称限制为特定常量的查询，只需对相应数据库目录中的相应名称的表进行检索，例如下面的查询，使用表名称字面量t1使服务器只检查t1表的文件，而不管test数据库中可能有多少个表：

```sql
SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_SCHEMA = 'test' AND TABLE_NAME = 't1';
```

相比之下，以下查询效率较低，因为它需要扫描test数据库文件目录以确定哪些表名称与模式't％'匹配：

```sql
SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_SCHEMA = 'test' AND TABLE_NAME LIKE 't%';
```

以下查询需要扫描整个数据库目录以确定模式“test％”的匹配数据库名称，并且对于每个匹配的数据库，它需要扫描数据库目录以确定模式“t％”的匹配表名称：

```sql
SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_SCHEMA = 'test%' AND TABLE_NAME LIKE 't%';
```

##### Write queries that minimize the number of table files that must be opened - 以最小化必须打开的表文件的数量

对于使用某些INFORMATION_SCHEMA表的查询，MySQL优化器使用多种优化手段来最小化必须打开的表文件的数量。例如：

```sql
SELECT TABLE_NAME, ENGINE FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_SCHEMA = 'test';
```

在这种情况下，在服务器扫描数据库目录以确定数据库(TABLE_SCHEMA = 'test')中表的名称之后，这些表名称就是可用的，不需要再进行文件系统查找。因此，TABLE_NAME列不需要打开任何文件。可以通过打开表的.frm文件来确定ENGINE列值，而无需打开其他表文件，如.MYD或.MYI文件。（某些值（例如，MyISAM表的INDEX_LENGTH）也需要打开.MYD或.MYI文件）。

```sql
mysql> explain SELECT TABLE_NAME, ENGINE FROM INFORMATION_SCHEMA.TABLES 
	   WHERE TABLE_SCHEMA = 'optimization_examples'\G;
*************************** 1. row ***************************
           id: 1
  select_type: SIMPLE
        table: TABLES
   partitions: NULL
         type: ALL
possible_keys: NULL
          key: TABLE_SCHEMA
      key_len: NULL
          ref: NULL
         rows: NULL
     filtered: NULL
        Extra: Using where; Open_frm_only; Scanned 1 database
```

以下表示文件打开优化类型：

• SKIP_OPEN_TABLE：不需要打开表文件。 通过扫描数据库目录，所需的查询信息已经可用

• OPEN_FRM_ONLY：只需打开表的.frm文件

• OPEN_TRIGGER_ONLY：只需打开表的.TRG文件

• OPEN_FULL_TABLE：未优化的，.frm, .MYD, 和.MYI文件都必须被打开

##### Use EXPLAIN to determine whether the server can use INFORMATION_SCHEMA optimizations for a query - 使用EXPLAIN查看是否可以应用INFORMATION_SCHEMA 优化

这尤其适用于从多个数据库中搜索信息的INFORMATION_SCHEMA查询，因为这种查询可能需要很长时间并影响性能。EXPLAIN输出中的Extra值表明MySQL服务器可以应用前面描述的哪些方法来优化INFORMATION_SCHEMA查询。

```sql
-- 使用字符串常量作为数据库或表名称查找值可使服务器避免目录扫描。对于VIEWS.TABLE_NAME的引用，只需要打开.frm文件。
mysql>  EXPLAIN SELECT TABLE_NAME FROM INFORMATION_SCHEMA.VIEWS 
		WHERE TABLE_SCHEMA = 'test' AND TABLE_NAME = 'v1'\G;
*************************** 1. row ***************************
           id: 1
  select_type: SIMPLE
        table: VIEWS
   partitions: NULL
         type: ALL
possible_keys: NULL
          key: TABLE_SCHEMA,TABLE_NAME
      key_len: NULL
          ref: NULL
         rows: NULL
     filtered: NULL
        Extra: Using where; Open_frm_only; Scanned 0 databases
```

```sql
-- 由于没有提供查找值（没有WHERE子句），因此服务器必须扫描整个数据目录和每个数据库目录。对于扫描到的每个表，查询TABLE_NAME和ROW_FORMAT。TABLE_NAME不需要打开其他表文件（SKIP_OPEN_TABLE优化适用）。ROW_FORMAT需要打开所有表文件（OPEN_FULL_TABLE适用）
mysql>  EXPLAIN SELECT TABLE_NAME, ROW_FORMAT FROM INFORMATION_SCHEMA.TABLES\G;
*************************** 1. row ***************************
           id: 1
  select_type: SIMPLE
        table: TABLES
   partitions: NULL
         type: ALL
possible_keys: NULL
          key: NULL
      key_len: NULL
          ref: NULL
         rows: NULL
     filtered: NULL
        Extra: Open_full_table; Scanned all databases
```

```sql
-- 未提供表名查找值，因此服务器必须扫描test数据库目录。对于TABLE_NAME和TABLE_TYPE列，分别应用SKIP_OPEN_TABLE和OPEN_FRM_ONLY优化
mysql>  EXPLAIN SELECT TABLE_NAME, TABLE_TYPE FROM INFORMATION_SCHEMA.TABLES 
		WHERE TABLE_SCHEMA = 'test'\G;
*************************** 1. row ***************************
           id: 1
  select_type: SIMPLE
        table: TABLES
   partitions: NULL
         type: ALL
possible_keys: NULL
          key: TABLE_SCHEMA
      key_len: NULL
          ref: NULL
         rows: NULL
     filtered: NULL
        Extra: Using where; Open_frm_only; Scanned 1 database
```

```sql
-- 对于第一个EXPLAIN输出行：常量数据库和表查找值使服务器可以避免对TABLES值进行目录扫描。对TABLES.TABLE_NAME的引用不需要其他表文件。对于第二个EXPLAIN输出行：所有COLUMNS表值都是OPEN_FRM_ONLY查找，因此COLUMNS.TABLE_NAME需要打开.frm文件
mysql>  EXPLAIN SELECT B.TABLE_NAME FROM 
		INFORMATION_SCHEMA.TABLES AS A, INFORMATION_SCHEMA.COLUMNS AS B 
		WHERE A.TABLE_SCHEMA = 'test' 
		AND A.TABLE_NAME = 't1' 
		AND B.TABLE_NAME = A.TABLE_NAME\G
*************************** 1. row ***************************
           id: 1
  select_type: SIMPLE
        table: A
   partitions: NULL
         type: ALL
possible_keys: NULL
          key: TABLE_SCHEMA,TABLE_NAME
      key_len: NULL
          ref: NULL
         rows: NULL
     filtered: NULL
        Extra: Using where; Skip_open_table; Scanned 0 databases
*************************** 2. row ***************************
           id: 1
  select_type: SIMPLE
        table: B
   partitions: NULL
         type: ALL
possible_keys: NULL
          key: NULL
      key_len: NULL
          ref: NULL
         rows: NULL
     filtered: NULL
        Extra: Using where; Open_frm_only; Scanned all databases; Using join buffer (Block Nested Loop)
```

```sql
-- 在这种情况下，不应用任何优化，因为COLLATIONS是优化不可用的INFORMATION_SCHEMA表之一
mysql>  EXPLAIN SELECT * FROM INFORMATION_SCHEMA.COLLATIONS\G;
*************************** 1. row ***************************
           id: 1
  select_type: SIMPLE
        table: COLLATIONS
   partitions: NULL
         type: ALL
possible_keys: NULL
          key: NULL
      key_len: NULL
          ref: NULL
         rows: NULL
     filtered: NULL
        Extra: NULL
```

### Optimizing Data Change Statements

传统的OLTP应用程序和现代Web应用程序通常执行许多小的数据写操作，此时并发性至关重要。数据分析和报表应用程序通常会同时运行影响多行的数据写操作，其中主要考虑因素是大量数据的写入并使索引保持最新的I/O开销。对于插入和更新大量数据，有时我们会使用其他SQL语句或外部命令来模仿INSERT，UPDATE和DELETE语句的效果。

#### Optimizing INSERT Statements

要优化插入速度，我们需要将多个单行插入语句组合到一个批量插入操作中。理想情况下，只需要获取单个数据库连接，一次发送多条新行的数据，并延迟所有索引更新和一致性检查到最后。插入行所需的时间由以下因素决定，其中数字表示大致比例（没有考虑打开表的初始开销，每个并发运行的查询都会执行一次打开表的操作）：

• Connecting: (3)

• Sending query to server: (2)

• Parsing query: (2)

• Inserting row: (1 × size of row)

• Inserting indexes: (1 × number of indexes)（假设BTREE索引，表的大小对插入速度的影响是logN）

• Closing: (1)

我们可以使用以下方法来加速INSERT语句：

• 如果要同时从同一客户端插入多行，使用包含多个VALUES列表的INSERT语句一次插入多行。与使用单独的单行INSERT语句相比，这要快得多（在某些情况下要快很多倍）。如果要将数据添加到非空表，则可以调整bulk_insert_buffer_size变量以使数据插入更快。

• 从文本文件加载表时，请使用LOAD DATA INFILE。这通常比使用INSERT语句快20倍。
• 利用列的默认值。仅在要插入的值与默认值不同时才显式插入值。这减少了MySQL必须执行的解析并提高了插入速度。

• See Section 8.5.5, “Bulk Data Loading for InnoDB Tables” for tips specific to InnoDB tables.

• See Section 8.6.2, “Bulk Data Loading for MyISAM Tables” for tips specific to MyISAM tables.

#### Optimizing UPDATE Statements

MySQL优化器会对UPDATE语句像SELECT查询一样进行优化，但是要附加额外的写开销。写入速度取决于要更新的数据量和更新的索引数。未更改的索引不会更新。获得快速UPDATE的另一种方法是延迟单个更新，然后在以后连续执行许多更新。如果锁定表，一次执行多个更新比多次执行单个更新要快得多。对于使用动态行格式的MyISAM表，UPDATE语句将行更新为为更长的总长度可能会拆分该行。如果经常这样做，经常使用OPTIMIZE TABLE非常重要。

#### Optimizing DELETE Statements

删除MyISAM表中单行所需的时间与索引数完全成比例。要更快地删除行数据，可以通过增加key_buffer_size系统变量来增加key缓存的大小。要从MyISAM表中删除所有行，TRUNCATE TABLE tbl_name比DELETE FROM tbl_name更快。TRUNCATE操作不是事务安全的；在活动事务或活动表锁定过程中尝试TRUNCATE时会发生错误。

### Optimizing Database Privileges

权限设置越复杂，所有SQL语句的开销就越大。简化GRANT语句授予的权限使MySQL能够在客户端执行语句时减少权限检查开销。例如，如果不授予任何表级或列级权限，则服务器无需检查tables_priv和columns_priv表的内容。同样，如果对任何帐户都没有资源限制，则服务器不必执行资源计数。所以如果MySQL服务器语句处理负载非常高，可以考虑使用简化的授权结构来减少权限检查开销。

### Other Optimization Tips

本节列出了一些用于提高查询处理速度的其他技巧：

• 如果您的应用程序发出多个数据库请求来执行相关更新，则将语句组合到存储例程中可以帮助提高性能。同样，如果您的应用程序需要基于多个列值或大量数据计算得到单个结果，则将计算组合到UDF（用户定义的函数）可以帮助提高性能。然后，由此产生的快速数据库操作可供其他查询、应用程序甚至用不同编程语言编写的代码重用。

• 要解决ARCHIVE表发生的任何压缩问题，请使用OPTIMIZE TABLE。

• 如果可能，将报表分类为“实时”或“统计”，其中统计报告所需的数据仅从定期从实时数据生成的汇总表中创建。

• 如果您的数据不符合rows-and-columns结构（即关系型数据库表结构），则可以将数据打包并存储到BLOB列中。在这种情况下，我们必须在应用程序中提供打包和解包信息的代码，但这可能会节省I/O操作以读取和写入相关值集。

• 在Web服务器中，将图像和其他二进制内容存储为文件，将路径名存储在数据库中而不是文件本身。大多数Web服务器在缓存文件方面比数据库更好更快。（尽管在这种情况下我们必须自己处理备份和存储问题）

• 如果我们需要非常高的速度，请查看更底层的MySQL接口。例如，通过直接访问MySQL InnoDB或MyISAM存储引擎，与使用SQL接口相比，可以大幅提高速度。

• Replication可以为某些操作提供性能优势。 您可以在副本服务器之间分发客户端检索以分担负载。为避免在进行备份时影响主服务器的速度，可以使用从服务器进行备份。

