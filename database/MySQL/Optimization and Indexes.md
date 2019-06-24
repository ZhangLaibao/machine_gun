## Optimization and Indexes 

提高SELECT操作性能的最佳方法是在查询中用到的一个或多个列上创建索引。索引的作用类似于表的行指针，允许查询快速确定哪些行与WHERE子句中的条件匹配，并检索这些行的其他列值。MySQL所有数据类型上都可以创建索引。

尽管为查询中每个可能使用的列创建索引很有诱惑力，但不必要的索引会浪费空间并让MySQL在选择要使用的索引上浪费时间。索引还会增加INSERT，UPDATE和DELETE的成本，因为必须更新每个索引。所以我们必须找到适当的平衡，以使用最佳索引集实现快速查询。

### How MySQL Uses Indexes - MySQL是怎样使用索引的

索引用于快速查找具有特定列值的行。如果没有索引，MySQL必须从第一行开始，然后读取整个表以查找相关行数据。表越大，性能代价就越高。如果表中有查询列的索引，MySQL可以快速确定要在数据文件中寻找的位置，而无需查看所有数据。这比按顺序读取每一行要快得多。

大多数MySQL索引（PRIMARY KEY，UNIQUE，INDEX和FULLTEXT）都使用B-tree索引。例外：空间数据类型的索引使用R-tree；MEMORY表也支持哈希索引；InnoDB对FULLTEXT索引使用反转列表(inverted lists)。

通常MySQL使用索引进行这些操作：

• 快速查找与WHERE子句匹配的行

• To eliminate rows from consideration. 如果需要在多个索引之间选择，MySQL通常使用能使结果集行数最小（最具选择性）的索引。

• 如果表具有复合索引，则优化程序可以使用索引的任何最左前缀来查找行。

• 在执行连接时从其他表中检索行。如果连接字段的数据类型和大小相同，MySQL可以更有效地使用索引。在此上下文中，如果将VARCHAR和CHAR类型的列声明为相同大小，则认为它们是相同的。例如，VARCHAR(10)和CHAR(10)的大小相同，但VARCHAR(10)和CHAR(15)不是。对于非二进制字符串列之间的比较，两列应使用相同的字符集。例如，将utf8列与latin1列进行比较时会导致索引失效。不相似列（例如，将字符串列与时间或数字列进行比较）如果不能在没有转换的情况下直接比较值，会排导致索引失效。对于给定值，例如整形列中的值1，它可能在等于条件下比较字符串列中的任何数量的值，例如“1”，“1”，“00001”或“01 .e1”，但是这排除了对字符串列的任何索引的使用。

• 查找特定索引列key_col的MIN()或MAX()值。这是由预处理器优化的，该预处理器会检查是否在索引中的key_col之前出现的所有索引列部分上使用WHERE key_part_N = 常量。在这种情况下，MySQL对每个MIN()或MAX()表达式执行单个key查找，并用常量替换它。如果所有表达式都被替换为常量，则查询立即返回。例如：

```sql
SELECT MIN(key_part2),MAX(key_part2) FROM tbl_name WHERE key_part1=10;
```

• 基于对可用索引的最左前缀列的（例如，ORDER BY key_part1，key_part2）或分组。如果所有索引列都是按照DESC，则按相反顺序读取索引。

• 在某些情况下，查询可以被优化到在不查询数据文件的的情况下仅通过索引获取查询数据。（为查询提供所有必要结果的索引称为覆盖索引）如果查询仅使用表中某些索引包含的列，则可以从索引树中检索所选值以获得更快的速度。例如：

```sql
SELECT key_part3 FROM tbl_name WHERE key_part1=1
```

对于小表或处理大表的大多数或所有行的报表查询，索引并没有优势。当查询需要访问大多数行时，顺序读取比通过索引更快。顺序读取可以最大限度地减少磁盘查找，即使查询不需要所有行也是如此。

### Primary Key Optimization - 优化主键

表的主键表示我们在最重要的查询中使用的一列或几列。主键具有对应的索引，用以实现高性能的快速查询。查询性能受益于NOT NULL优化，因为主键不能包含任何NULL值。对于InnoDB存储引擎，表数据在磁盘上的物理组织方式的目的就是根据主键进行超快速查找和。

如果我们的表数据量很大并且很重要，但没有明显的可以用作主键的列或列集，则可以创建一个单独的列，其中包含自动增长值以用作主键。当我们使用外键连接表时，这些唯一ID可用作指向其他表中相应行的指针。

### Foreign Key Optimization - 优化外键

如果一个表有很多列，并且我们会查询许多不同的列组合，那么一种很有效的处理方式是将使用频率较低的数据列拆分到各自有少数几列的单独表里，并通过复制主表主键的数值类型ID将它们与主表相关联。这样，每个小表都可以有一个主键用来快速查找其数据，并且我们可以使用连接操作仅查询所需的列集。取决于数据的分布方式，查询可能会执行较少的I/O并占用较少的缓存，因为相关列在磁盘上打包存储在一起。（为了最大限度地提高性能，查询会尽可能少的从磁盘中读取数据块；对于这种只有很少几列的表来说，每个数据块中可以容纳更多行数据，所以需要更少的I/O次数）

### Column Indexes - 单一索引

最常见的索引类型只包含单个列，在数据结构中存储该列的值的副本，使我们能快速查找具有相应列值的行。B-tree这种数据结构可以使索引快速查找特定值，一组值或值的范围，对应于WHERE子句中的=，>，≤，BETWEEN，IN等运算符。

每个表的最大索引数和最大索引长度由其存储引擎规定。所有存储引擎每个表至少支持16个索引，总索引长度至少为256个字节。大多数存储引擎都支持更高的索引数和索引长度。

#### Index Prefixes - 前缀索引

对于字符串类型的列，使用索引规范中的col_name(N)语法，可以创建仅使用列的前N个字符的索引。仅索引列值的前缀可以使索引文件更小。索引BLOB或TEXT列时，必须为索引指定前缀长度。例如：

```sql
CREATE TABLE test (blob_col BLOB, INDEX(blob_col(10)));
```

前缀最长可达1000个字节（InnoDB表为767个字节，除非我们另外指定了innodb_large_prefix系统变量的值）。

> 前缀长度限制以字节为单位，而CREATE TABLE，ALTER TABLE和CREATE INDEX语句中的前缀长度被解释为非二进制字符串类型（CHAR，VARCHAR，TEXT）的字符数和二进制字符串类型的字节数（BINARY， VARBINARY，BLOB）。在为使用多字节字符集的非二进制字符串列指定前缀长度时，需要考虑这一点。

#### FULLTEXT Indexes - 全文索引

FULLTEXT索引用于全文搜索。只有InnoDB和MyISAM存储引擎支持FULLTEXT索引，并且仅支持CHAR，VARCHAR和TEXT类型的列。全文索引始终建立在整个列数据上，并且不支持列前缀索引。

针对单个InnoDB表的某些类型的FULLTEXT查询会有一些优化的手段。这些优化手段对具有这些特征的查询由其有效：

• 仅返回document ID或者document ID和搜索排名的FULLTEXT查询

• FULLTEXT查询按得分的降序对匹配的行进行，并应用LIMIT子句来获取前N个匹配的行。要应用此优化，SQL语句中不能有WHERE子句，并且只能含有单个ORDER BY子句并按降序排列

• FULLTEXT查询仅检索与搜索词匹配的行的COUNT()值，而没有其他WHERE子句。将WHERE子句编码为WHERE MATCH(text) AGAINST('other_text')，不带任何> 0比较运算符

对于包含全文表达式的查询，MySQL在执行查询的优化阶段对这些表达式求值。优化器不只是查看全文表达式并进行估计，它实际上是在制定执行计划的过程中对它们进行求值。对于全文查询，EXPLAIN通常比没有进行表达式求值的非全文查询慢，这是上述行为的一个例证。

由于优化期间匹配数据，全文查询的EXPLAIN可能会在Extra列中显示“Select tables optimized away”。 在这种情况下，在以后的执行期间不需要进行表访问。

#### Spatial Indexes - 空间索引

MySQL支持在空间数据类型上创建索引。MyISAM和InnoDB支持空间类型的R-tree索引。其他存储引擎使用B-tree来索引空间数据类型（ARCHIVE除外，它不支持空间数据类型索引）。

#### Indexes in the MEMORY Storage Engine - MEMORY存储引擎下的索引

MEMORY存储引擎默认使用HASH索引，但也支持BTREE索引。

### Multiple-Column Indexes - 复合索引

MySQL可以创建复合索引（即多列上的索引）。索引最多可包含16列。对于某些数据类型，我们可以仅索引列的前缀。MySQL可以对检索索引中所有列的查询使用多列索引，或者只检索第一列，前两列，前三列等的查询。如果在索引定义中以正确的顺序指定列，则单个复合索引可以加速同一表上的多种查询。复合索引可以视为数组，数组的元素是通过连接索引列的值创建的值。

> 作为复合索引的替代方法，我们可以基于复合索引列的信息引入额外的“hash”列。如果此列长度很短，hash算法足够合理，基于此列建立单一索引，可能比在许多列上创建的“宽”索引的查询速度更快。在MySQL中，这个额外"hash"列的适应非简单：
>
> ```sql
> SELECT * FROM tbl_name WHERE hash_col=MD5(CONCAT(val1,val2)) 
> AND col1=val1 AND col2=val2;
> ```

假设有一个表定义如下：

```sql
CREATE TABLE test (
 id INT NOT NULL,
 last_name CHAR(30) NOT NULL,
 first_name CHAR(30) NOT NULL,
 PRIMARY KEY (id),
 INDEX name (last_name,first_name)
);
```

在last_name和first_name列上创建了名为name的索引。该索引可用于查询last_name和first_name指定已知范围内的值的组合。它也可以用于仅指定last_name值的查询，因为该列是索引的最左前缀。name索引可以应用于以下查询：

```sql
SELECT * FROM test WHERE last_name='Widenius';
SELECT * FROM test WHERE last_name='Widenius' AND first_name='Michael';
SELECT * FROM test WHERE last_name='Widenius' 
AND (first_name='Michael' OR first_name='Monty');
SELECT * FROM test WHERE last_name='Widenius'
AND first_name >='M' AND first_name < 'N';
```

但是，name索引不能用于以下形式的查询：

```sql
SELECT * FROM test WHERE first_name='Michael';
SELECT * FROM test WHERE last_name='Widenius' OR first_name='Michael';
```

上面的正例和反例就是复合索引命中的最左前缀原则(leftmost prefix)。

假设我们发出以下SELECT语句：

```sql
SELECT * FROM tbl_name WHERE col1=val1 AND col2=val2;
```

如果col1和col2上存在复合索引，则可以使用复合索引直接查询到符合条件的行。如果col1和col2上存在单独的单一索引，则优化程序会尝试使用Index Merge优化，或尝试通过确定哪个索引可以排除更多行来确定更具有选择性的索引。

### Verifying Index Usage - 检查是否确实使用了索引

我们需要使用EXPLAIN语句检查每一个查询是否确实使用到了我们在表中创建的索引。

### InnoDB and MyISAM Index Statistics Collection - InnoDB and MyISAM引擎下索引统计数据的收集

存储引擎收集有关表的统计信息以供优化程序使用。表统计信息基于value group，其中value group是具有相同key前缀值的一组行。对于优化器来说，一个重要的统计数据是平均value group大小。

MySQL在以下场景中使用平均value group大小：

• 估计每次ref访问必须读取多少行

• 估计部分连接将产生多少行数据；也就是此格式的操作将产生的行数：

```sql
(...) JOIN tbl_name ON tbl_name.key = expr
```

随着索引的平均value group大小增加，索引在以上两个场景中的用处会减少，因为每个查找的平均行数增加：为了使索引有利于优化目的，最好每个索引值都能指向表中的一小部分行数据。当给定的索引值指向大量行时，索引就变得没那么有用，MySQL不太可能使用它。

平均value group大小与表基数相关，表基数是值组的数量。SHOW INDEX语句显示基于N/S的基数值，其中N是表中的行数，S是平均value group大小。该比值得出表中的近似value group数。

对于基于<=>比较运算符的连接，NULL不会与任何其他值区别对待：NULL <=> NULL，就像任何其他N的N <=> N一样。但是，对于基于=运算符的连接，NULL与非NULL值不同：当expr1或expr2（或两者）为NULL时，expr1 = expr2不为true。这会影响对tbl_name.key = expr形式进行比较的ref访问：如果expr的当前值为NULL，MySQL将不会访问该表，因为比较结果不会为true。

对于=比较，从优化角度出发，表中有多少NULL值无关紧要，重要的是非NULL值组的平均大小。但是，MySQL目前不支持收集或使用该值。

对于InnoDB和MyISAM表，我们可以分别通过innodb_stats_method和myisam_stats_method系统变量控制表统计信息的收集行为。这些变量有三个可能的值，它们的不同之处如下：

• 当变量设置为nulls_equal时，所有NULL值都被视为相同（即它们都形成一个值组）。如果NULL值组大小远远高于非NULL值组平均大小，则此方法会使平均值组大小增大。这使得在优化器看来，索引对于查找非NULL值的连接查询的效果要比实际差一些。因此，nulls_equal方法可能会导致优化器错误的不使用索引进行ref访问。

• 当变量设置为nulls_unequal时，NULL值被视为互不相等。相反，每个NULL值形成一个大小为1的单独值组。如果表中有许多NULL值，则此方法会使平均值组大小降低。如果非NULL值组平均大小很大，则将每个NULL值计为大小为1的组会导致优化程序高估索引在查找非NULL值的联接中的作用。因此，当可能存在其他更好的方法时，nulls_unequal方法可能会导致优化器将此索引用于ref查找。

• 当变量设置为nulls_ignored时，将忽略NULL值

如果我们的工程倾向于使用许多<=>而不是=进行连接，则NULL值在比较中并不特殊，所有NULL值可以认为是相等的。 在这种情况下，nulls_equal是最适当的统计方法。

innodb_stats_method系统变量具有全局值；myisam_stats_method系统变量同时具有全局值和会话值。设置全局值会影响相应存储引擎中表的统计信息收集。设置会话值仅影响当前客户端连接的统计信息收集。这意味着我们可以通过设置myisam_stats_method的会话值来强制使用给定方法重新生成表的统计信息，而不会影响其他客户端。

要重新生成MyISAM表统计信息，可以使用以下任一方法：

• 执行myisamchk --stats_method=method_name --analyze

• 更改表以使其统计信息过期（例如，插入行然后将其删除），然后设置myisam_stats_method并发出ANALYZE TABLE语句

以下是关于使用innodb_stats_method和myisam_stats_method的一些警告：

• 如上所述，我们可以强制显式收集表统计信息。但是，MySQL也可能会自动收集统计信息。例如，如果在对表执行语句的过程中，其中一些语句修改了表，MySQL可能会收集统计信息。（例如，对于批量插入或删除，或某些ALTER TABLE语句，可能会出现这种情况）如果发生这种情况，则使用innodb_stats_method或myisam_stats_method在收集时刻具有的值来收集统计信息。因此，如果使用一种方法收集统计信息，但在稍后自动收集表的统计信息时将系统变量设置为另一种方法，则将使用另一种方法。

• 无法得知使用了哪种方法为给定表生成统计信息。

• 这些变量仅适用于InnoDB和MyISAM表。其他存储引擎只有一种收集表统计信息的方法。通常它更接近于nulls_equal方法。

### Comparison of B-Tree and Hash Indexes

了解B-tree和Hash数据结构有助于预测不同查询在使用这些数据结构的不同存储引擎上的执行情况，特别是对于允许我们选择B-tree或Hash索引的MEMORY存储引擎。

#### B-Tree Index Characteristics - B-Tree索引特征

B树索引可用于使用=，>，> =，<，<=或BETWEEN运算符的表达式中的列值比较。如果LIKE的参数是不以通配符开头的常量字符串，则索引也可用于LIKE比较。例如，以下SELECT语句使用索引：

```sql
-- 只考虑带有'Patrick'<= key_col <'Patrick'的行
SELECT * FROM tbl_name WHERE key_col LIKE 'Patrick%';
-- 仅考虑具有'Pat'<= key_col <'Pau'的行
SELECT * FROM tbl_name WHERE key_col LIKE 'Pat%_ck%';
```

以下SELECT语句不使用索引：

```sql
-- LIKE参数值以通配符开头
SELECT * FROM tbl_name WHERE key_col LIKE '%Patrick%';
-- LIKE参数值不是常量
SELECT * FROM tbl_name WHERE key_col LIKE other_col;
```

如果我们使用col_name LIKE '％string％' 且string长度超过三个字符，MySQL使用Turbo Boyer-Moore算法初始化字符串的模式，然后使用此模式更快地执行搜索。

如果col_name列上有索引，则col_name IS NULL的搜索会使用索引。

任何未覆盖WHERE子句中所有AND条件的索引都不用于优化查询。换句话说，为了能够使用索引，必须在所有AND组中使用索引的前缀。

以下WHERE子句使用索引：

```sql
... WHERE index_part1=1 AND index_part2=2 AND other_column=3
 /* index = 1 OR index = 2 */
... WHERE index=1 OR A=10 AND index=2
 /* optimized like "index_part1='hello'" */
... WHERE index_part1='hello' AND index_part3=5
 /* Can use index on index1 but not on index2 or index3 */
... WHERE index1=1 AND index2=2 OR index1=3 AND index3=3;
```

以下WHERE子句不能使用索引：

```sql
 /* index_part1 is not used */
... WHERE index_part2=1 AND index_part3=2
 /* Index is not used in both parts of the WHERE clause */
... WHERE index=1 OR A=10
 /* No index spans all rows */
... WHERE index_part1=1 OR index_part2=10
```

有时候即使有合适的索引，MySQL也不使用索引。其中一种情况是，优化器估计使用索引将需要MySQL访问表中的很大一部分行。在这种情况下，表扫描可能会快得多，因为它需要较少的磁盘寻址。但是，如果这些的查询使用LIMIT来仅检索某些行，那么MySQL无论如何都会使用索引，因为它可以更快地找到在结果中返回的那几行。

#### Hash Index Characteristics - Hash索引特征

散列索引与刚才讨论B-Tree索引有些不同的特征：

• 仅用于使用=或<=>运算符的相等性比较（但速度非常快），不用于比较运算符，例如<找到一格范围内的系列值。依赖于这种类型的单值查找的系统被称为“key-value store”；要将MySQL用于此类应用程序，请尽可能使用Hash索引

• 优化程序无法使用Hash索引来加速ORDER BY操作。（此类型的索引不能用于按顺序搜索下一个键值对）

• MySQL无法确定两个值之间大约有多少行（范围优化器使用它来决定使用哪个索引）。如果将MyISAM或InnoDB表更改为哈希索引的MEMORY表，这一特点可能会影响某些查询

• 只有整个索引值可用于搜索行。（使用B树索引，键的任何最左前缀都可用于查找行）

### Use of Index Extensions - 索引扩展

InnoDB通过将主键列附加到二级索引后面来自动扩展每个二级索引。例如：

```sql
CREATE TABLE t1 (
 i1 INT NOT NULL DEFAULT 0,
 i2 INT NOT NULL DEFAULT 0,
 d DATE DEFAULT NULL,
 PRIMARY KEY (i1, i2),
 INDEX k_d (d)
) ENGINE = InnoDB;
```

此表在列(i1, i2)上定义主键。 它还在列(d)上定义了二级索引k_d，但内部InnoDB扩展了该索引并将其视为列(d, i1, i2)。在确定是否使用和如何使用该索引时，优化程序会考虑在二级索引上扩展的主键列。这可以带来更高效的查询执行计划和更好的性能。优化器可以使用扩展的二级索引进行ref，range和index_merge索引访问，进行松散索引扫描，进行连接和优化，以及进行MIN()/MAX()优化。

以下示例显示优化程序是否扩展二级索引如何影响执行计划。 假设t1填充了这些行：

```sql
INSERT INTO t1 VALUES
(1, 1, '1998-01-01'), (1, 2, '1999-01-01'), (1, 3, '2000-01-01'), 
(1, 4, '2001-01-01'), (1, 5, '2002-01-01'), (2, 1, '1998-01-01'), 
(2, 2, '1999-01-01'), (2, 3, '2000-01-01'), (2, 4, '2001-01-01'), 
(2, 5, '2002-01-01'), (3, 1, '1998-01-01'), (3, 2, '1999-01-01'),
(3, 3, '2000-01-01'), (3, 4, '2001-01-01'), (3, 5, '2002-01-01'), 
(4, 1, '1998-01-01'), (4, 2, '1999-01-01'), (4, 3, '2000-01-01'), 
(4, 4, '2001-01-01'), (4, 5, '2002-01-01'), (5, 1, '1998-01-01'), 
(5, 2, '1999-01-01'), (5, 3, '2000-01-01'), (5, 4, '2001-01-01'),
(5, 5, '2002-01-01');
```

对于这个查询：

```sql
EXPLAIN SELECT COUNT(*) FROM t1 WHERE i1 = 3 AND d = '2000-01-01';
```

在这种情况下，优化器不能使用主键，因为主键基于列(i1, i2)，查询不引用i2。相反，优化器可以使用(d)上的二级索引k_d，执行计划取决于是否使用了扩展索引。当优化器不考虑索引扩展时，它将索引k_d视为仅(d)。 查询的EXPLAIN产生此结果：

```sql
mysql> EXPLAIN SELECT COUNT(*) FROM t1 WHERE i1 = 3 AND d = '2000-01-01'\G;
*************************** 1. row ***************************
 			id: 1
   select_type: SIMPLE
         table: t1
          type: ref
 possible_keys: PRIMARY,k_d
           key: k_d
       key_len: 4
           ref: const
          rows: 5
         Extra: Using where; Using index
```

当优化器考虑索引扩展时，它将k_d视为(d, i1, i2)。 在这种情况下，它可以使用索引最左前缀(d，i1)来生成更好的执行计划：

```sql
mysql> EXPLAIN SELECT COUNT(*) FROM t1 WHERE i1 = 3 AND d = '2000-01-01'\G;
*************************** 1. row ***************************
			id: 1
   select_type: SIMPLE
         table: t1
          type: ref
 possible_keys: PRIMARY,k_d
           key: k_d
 	   key_len: 8
 		   ref: const,const
 		  rows: 1
 	     Extra: Using index
```

在这两种情况下，key表示优化器将使用二级索引k_d，但EXPLAIN输出显示使用扩展索引可以提供这些改进：

• key_len从4个字节变为8个字节，表示索引查找使用列d和i1，而不仅仅是d

• ref值从const变为const, const因为索引查找使用两个字段，而不是一个

• rows结果从5减少到1，表明InnoDB可以检查更少的行以产生结果

• Extra值从Using where; Using index到Using index 。这意味着只需要使用索引读取行，而无需查询数据行中的列

使用SHOW STATUS也可以看到使用扩展索引后优化器行为的差异(FLUSH TABLES和FLUSH STATUS用来刷新表缓存并清除状态计数器)：

```sql
FLUSH TABLE t1;
FLUSH STATUS;
SELECT COUNT(*) FROM t1 WHERE i1 = 3 AND d = '2000-01-01';
SHOW STATUS LIKE 'handler_read%'
```

没有索引扩展，SHOW STATUS会产生以下结果：

```sql
+-----------------------+-------+
| Variable_name         | Value |
+-----------------------+-------+
| Handler_read_first    | 0     |
| Handler_read_key      | 1     |
| Handler_read_last     | 0     |
| Handler_read_next     | 5     |
| Handler_read_prev     | 0     |
| Handler_read_rnd      | 0     |
| Handler_read_rnd_next | 0     |
+-----------------------+-------+
```

使用索引扩展之后SHOW STATUS会生成此结果。Handler_read_next值从5减少到1，表明扩展索引的效率更高：

```sql
+-----------------------+-------+
| Variable_name         | Value |
+-----------------------+-------+
| Handler_read_first    | 0     |
| Handler_read_key      | 1     |
| Handler_read_last     | 0     |
| Handler_read_next     | 1     |
| Handler_read_prev     | 0     |
| Handler_read_rnd      | 0     |
| Handler_read_rnd_next | 0     |
+-----------------------+-------+
```

optimizer_switch系统变量的use_index_extensions标志允许在使用InnoDB表的二级索引时控制优化程序是否将主键列考虑在内。默认情况下，use_index_extensions是启用的。要检查禁用索引扩展是否会提高性能，可以使用以下语句：

```sql
SET optimizer_switch = 'use_index_extensions=off';
```

优化程序对索引扩展的使用受索引列数量(16)和最大索引长度(3072字节)的限制。

### Optimizer Use of Generated Column Indexes

MySQL支持生成列的索引。 例如：

```sql
CREATE TABLE t1 (f1 INT, gc INT AS (f1 + 1) STORED, INDEX (gc));
```

生成列gc由表达式f1 + 1定义。我们在该列上建立了索引，优化器可以在创建执行计划期间考虑该索引。在以下查询中，WHERE子句引用gc，优化器会考虑该列上的索引是否产生更高效的计划：

```sql
SELECT * FROM t1 WHERE gc > 9;
```

优化器可以使用生成列上的索引来确定执行计划，即使在没有按名称直接引用生成列列的情况下，如果WHERE，ORDER BY或GROUP BY子句使用与定义某个生成列的表达式匹配的表达式，这种情况下也可以使用生成列上的索引。例如以下查询不直接引用gc，但使用与定义gc列使用的表达式匹配的表达式：

```sql
SELECT * FROM t1 WHERE f1 + 1 > 9;
```

优化器识别出表达式f1 + 1与定义gc的表达式匹配，并且gc被索引，因此它在制定执行计划期间会考虑该索引。使用EXPLAIN可以得到这样的结果：

```sql
mysql>  EXPLAIN SELECT * FROM t3 WHERE f1 + 1 > 9\G;
*************************** 1. row ***************************
           id: 1
  select_type: SIMPLE
        table: t3
   partitions: NULL
         type: range
possible_keys: gc
          key: gc
      key_len: 5
          ref: NULL
         rows: 1
     filtered: 100.00
        Extra: Using index condition
```

实际上，优化器已将表达式f1 + 1替换为与表达式匹配的生成列的名称gc。在SHOW WARNINGS显示的扩展EXPLAIN信息中可以明显的看出这一重写：

```sql
mysql> SHOW WARNINGS\G;
*************************** 1. row ***************************
  Level: Note
   Code: 1003
Message: /* select#1 */ select `optimization_examples`.`t3`.`f1` AS `f1`,`optimization_examples`.`t3`.`gc` AS `gc` from `optimization_examples`.`t3` where (`optimization_examples`.`t3`.`gc` > 9)
```

以下限制和条件适用于优化程序使用生成列的索引：

• 要使查询表达式与定义生成列的表达式匹配，表达式必须相同且必须具有相同的结果类型。例如，如果定义生成列表达式为f1 + 1，则如果查询使用1 + f1，或者将f1 + 1（整数表达式）与字符串进行比较，则优化程序将无法识别匹配项。

• 优化适用于这些运算符：=，<，<=，>，> =，BETWEEN和IN()。对于除BETWEEN和IN()之外的运算符，任一操作数都可以由匹配的生成列替换。对于BETWEEN和IN()，只有第一个参数可以被匹配的生成列替换，而其他参数必须具有相同的结果类型。对于涉及JSON值的比较，尚不支持BETWEEN和IN()。

• 定义生成列的表达式至少包含函数调用或包含运算符运算表达式。表达式不能仅包含对另一列的简单引用。例如，gc INT AS(f1) STORED仅包含列引用，因此不考虑gc上的索引。

• 对于将字符串与带索引的生成列进行比较，如果列值来自于返回带引号的字符串的JSON函数，在列定义中需要JSON_UNQUOTE()以从函数返回值中删除额外的引号。（为了直接将字符串与函数结果进行比较，JSON比较器会删除引号，但索引查找时不会发生这种情况）例如：

```sql
-- 不要这样写
doc_name TEXT AS (JSON_EXTRACT(jdoc, '$.name')) STORED
-- 要这样写
doc_name TEXT AS (JSON_UNQUOTE(JSON_EXTRACT(jdoc, '$.name'))) STORED
```

使用后一种定义写法，优化器可以检测这两种格式比较的匹配：

```sql
... WHERE JSON_EXTRACT(jdoc, '$.name') = 'some_string' ...
... WHERE JSON_UNQUOTE(JSON_EXTRACT(jdoc, '$.name')) = 'some_string' ...
```

如果列定义中没有JSON_UNQUOTE()，则优化程序仅检测第一个比较中的匹配项。

• 如果优化程序未能选择想要的索引，则可以使用索引提示强制优化程序进行不同的选择。
