### Optimizing SELECT Statements - 优化SELECT 语句

从表象上可以说SELECT语句承载了MySQL所有的查询操作，在SQL调优工作中也占据最高的优先级。对SELECT语句的优化同样适用于CREATE TABLE...AS SELECT, INSERT INTO...SELECT 和 DELETE 语句中的WHERE子句，但是后者由于在SELECT的基础上增加了写操作，需要额外考虑这些写操作的优化。

在优化SELECT语句时我们主要有这些方法：

• 优化一个很慢的SELECT ... WHERE语句的第一件事就是考虑是否可以加索引，索引可以极大提高SQL语句在执行时的求值、过滤和检索结果的性能。为避免浪费磁盘空间，可以在应用中常用的查询中常用的列上建立索引。索引对于使用连接和外键等功能引用不同表的查询尤其重要。我们可以使用EXPLAIN语句来确定SELECT语句中使用了哪些索引。

• 隔离并优化查询语句的每一部分：例如函数调用会导致额外的时间开销，不同的SQL结构会导致有时候会对结果集中的每一条数据执行一次函数或者对整个表中的每一条数据执行一次函数，明显后者会极大地降低查询性能。

• 尽量避免全表扫描，尤其是大表。

• 定期执行ANALYZE TABLE语句以提供最新的表的统计信息，供MySQL优化器制定合理有效地执行计划。

• 学习存储引擎对应的调参、配置、索引技术：最常用的InnoDB和MyISAM引擎都需要配合合理的配置和参数值来提供强大的性能。

• 对使用InnoDB引擎的表应用只读查询事务。

• 避免在SQL语句中使用MySQL优化器难以理解的语法，尤其是优化器会做自动转换的地方。

• 当问题并不是那么显而易见的时候，最好的方法是使用EXPLAIN分析SQL语句的执行细节，据此优化索引、WHERE子句、JOIN子句等。对于高阶玩家，EXPLAIN几乎是SQL调优的第一件事。

• 调整MySQL缓存的大小和缓存区属性。充分利用InnoDB的缓冲池，MyISAM的key缓存和MySQL的查询缓存

• 即使一个查询可以命中缓存，我们也可以进一步优化它以缩小缓存所需的内存空间。这样我们的应用可以应对更高的并发更大数据量的请求而不至于明显损失性能

• 优化锁机制，避免多个会话并发处理同一张表或同一行数据时的性能影响。

#### WHERE Clause Optimization - 优化WHERE子句

MySQL的SQL解析器和优化器会对我们写的SQL语句进行理解，并做一些自动的转换，我们可以在编写SQL语句的时候就考虑到这些因素以迎合MySQL，代价是牺牲SQL语句的可读性。MySQL在解析查询语句时会作如下优化：

• 去掉不必要的括号

```sql
	((a AND b) AND c OR (((a AND b) AND (c AND d))))
->  (a AND b AND c) OR (a AND b AND c AND d)
```

• 简化常量，避免不必要的常量计算

```sql
	(a<b AND b=c) AND a=5
->  b>5 AND b=c AND a=5

	(B>=5 AND B=5) OR (B=6 AND 5=5) OR (B=7 AND 5=6)
->  B=5 OR B=6
```

• 索引用到的常量表达式仅会求值一次

• MyISAM和MEMORY引擎中，单表的不带WHERE子句的COUNT(\*)会直接从表的统计信息里取值。单表的NOT NULL条件也一样。

• 尽早应用无效的常量表达式。MySQL可以快速检测没有必要或者不会返回任何结果的查询条件。

• 如果没有使用GROUP BY和聚类函数(COUNT(), MIN() 等)，HAVING和WHERE语句会被合并。

• For each table in a join, a simpler WHERE is constructed to get a fast WHERE evaluation for the table
and also to skip rows as soon as possible.

• 常量表的读取优先于其他任何表。常量表是指

- 空表或者只有一行记录的表

- WHERE子句中使用到表的PRIMARY KEY或者UNIQUE index，并且这些索引列与常量表达式比较值，并且索引列有非空约束。例如：

  ```sql
  SELECT * FROM t WHERE primary_key=1;
  SELECT * FROM t1,t2 WHERE t1.primary_key=1 AND t2.primary_key=t1.id;
  ```

• JOIN表的组合方式建立在尝试所有可能性的基础上。如果ORDER BY和GROUP BY子句中的列来自于同一个表，那么这个表会被优先读取。

• 如果ORDER BY子句和GROUP BY子句使用不同的列，或者ORDER BY和GROUP BY子句中包含了JOIN队列中第一个表之外其他表的字段，这种情况下会生成临时表。

• SELECT语句中指定了SQL_SMALL_RESULT关键字时，MySQL会直接在内存中生成临时表，不会使用索引。SQL_SMALL_RESULT必须和GROUP BY、DISTINCT或DISTINCTROW一起使用。

• MySQL优化器会扫描所有索引，从中选择最优的，如果使用这个最优索引效率会高于表扫描。以前，MySQL通过判断最优索引能否覆盖超过全表30%数据来决定使用此索引还是进行全表扫描，但是使用这一固定的比例的方式已经被废弃。现在这一衡量标准变得更加复杂，包括考虑表的大小、行数和I/O块的大小。

• 在一些情况下，MySQL甚至可以不去读取数据文件而直接从索引中读取行。如果索引中用到的所有列都是数值类型的，那么在处理查询的时候只需要读取索引树。

• 在输出每一行之前，与HAVING子句不匹配的行会被跳过

```sql
-- 一些效率比较高的查询语句
SELECT COUNT(*) FROM tbl_name;
SELECT MIN(key_part1), MAX(key_part1) FROM tbl_name;
SELECT MAX(key_part2) FROM tbl_name WHERE key_part1 = constant;
SELECT ... FROM tbl_name ORDER BY key_part1, key_part2,... LIMIT 10;
SELECT ... FROM tbl_name ORDER BY key_part1 DESC, key_part2 DESC, ... LIMIT 10;
-- 假设索引列是数值类型，MySQL处理下列查询时只需要用到索引树
SELECT key_part1, key_part2 FROM tbl_name WHERE key_part1 = val;
SELECT COUNT(*) FROM tbl_name WHERE key_part1 = val1 AND key_part2 = val2;
SELECT key_part2 FROM tbl_name GROUP BY key_part1;
-- 下列查询使用了索引的顺序，避免了另外的代价
SELECT ... FROM tbl_name ORDER BY key_part1, key_part2, ... ;
SELECT ... FROM tbl_name ORDER BY key_part1 DESC, key_part2 DESC, ... ;
```

#### Range Optimization - 范围查询优化

range访问方法使用单个索引来检索表数据中包含在一个或多个索引值区间内的数据子集。 它可用于单一索引或复合索引。

##### The Range Access Method for Single-Part Indexes - 单一索引的范围查询

对于单一索引，索引值区间可以方便地由WHERE子句中的相应条件表示。单一索引的范围条件定义如下：

• 对于BTREE和HASH索引，使用 =，<=>，IN()，IS NULL，IS NOT NULL操作符比较索引列和常量

• 另外，对于BTREE索引，使用>，<，>=，<=，BETWEEN，!=，<> 操作符比较索引列和常量，或者当LIKE条件的参数值是常量字符串并且不以通配符开头时

• 多个范围条件使用OR或AND连接时

```sql
-- 一些例子
SELECT * FROM t1 WHERE key_col > 1 AND key_col < 10;
SELECT * FROM t1 WHERE key_col = 1 OR key_col IN (15,18,20);
SELECT * FROM t1 WHERE key_col LIKE 'ab%' OR key_col BETWEEN 'bar' AND 'foo';
```

在常量的传播阶段(constant propagation phase)，MySQL优化器会将一些非常量转化为常量。

MySQL会尝试从每一个可能的索引中提取满足WHERE子句中的范围条件。在提取过程中，不能用于构建范围条件的条件会被丢弃，产生重叠范围的条件会被合并，产生空的范围的条件会被移除。

```sql
-- 例如下述SQL，key1是一个索引列，nonkey是一个没有索引的列
SELECT * FROM t1 
WHERE (key1 < 'abc' AND (key1 LIKE 'abcde%' OR key1 LIKE '%b')) 
OR (key1 < 'bar' AND nonkey = 4) 
OR (key1 < 'uux' AND key1 > 'z');
-- key1的条件提取顺序
-- 1. 从最原始的WHERE子句开始
(key1 < 'abc' AND (key1 LIKE 'abcde%' OR key1 LIKE '%b')) 
OR (key1 < 'bar' AND nonkey = 4) 
OR (key1 < 'uux' AND key1 > 'z')
-- 2. 移除无法应用索引的nonkey = 4和key1 LIKE '%b'条件，并替换为TRUE
(key1 < 'abc' AND (key1 LIKE 'abcde%' OR TRUE)) OR
(key1 < 'bar' AND TRUE) OR
(key1 < 'uux' AND key1 > 'z')
-- 3. 折叠恒为TRUE或FALSE的条件并替换为相应的TRUE或FALSE
-- (key1 LIKE 'abcde%' OR TRUE) = true
-- (key1 < 'uux' AND key1 > 'z') = false
(key1 < 'abc' AND TRUE) OR (key1 < 'bar' AND TRUE) OR (FALSE)
-- 再去除无用的TRUE或者FALSE
(key1 < 'abc') OR (key1 < 'bar')
-- 4. 折叠重合的条件范围
(key1 < 'bar')
```

我们可以创建实验表t1，通过EXPLAIN查看MySQL实际处理这条查询的细节：

```sql
create table t1(
    key1 varchar(10),
    nonkey bigint(10),
	key `idx_key1`(`key1`)
)engine = InnoDB default charset utf8;

mysql> explain SELECT * FROM t1
    -> WHERE (key1 < 'abc' AND (key1 LIKE 'abcde%' OR key1 LIKE '%b'))
    -> OR (key1 < 'bar' AND nonkey = 4)
    -> OR (key1 < 'uux' AND key1 > 'z')\G;
*************************** 1. row ***************************
           id: 1
  select_type: SIMPLE
        table: t1
   partitions: NULL
         type: range
possible_keys: idx_key1
          key: idx_key1
      key_len: 33
          ref: NULL
         rows: 1
     filtered: 100.00
        Extra: Using index condition; Using where

mysql> explain SELECT * FROM t1 WHERE key1 < 'bar'\G;
*************************** 1. row ***************************
           id: 1
  select_type: SIMPLE
        table: t1
   partitions: NULL
         type: range
possible_keys: idx_key1
          key: idx_key1
      key_len: 33
          ref: NULL
         rows: 1
     filtered: 100.00
        Extra: Using index condition
-- 相比原始语句，简化后的语句由于只解析了可以应用索引列range access的条件，所以Extra列中少了用于处理费索引列条件的using where
```

根据这个例子我们可以看到，MySQL的范围查询解析会得到一个WHERE条件的超集，然后进入下一步过滤。MySQL支持任意深度的嵌套AND/OR连接的范围查询，并且他们在WHERE子句中的顺序不影响最终结果。

MySQL does not support merging multiple ranges for the range access method for spatial indexes. To
work around this limitation, you can use a UNION with identical SELECT statements, except that you put
each spatial predicate in a different SELECT. - (spatial index:空间索引，GIS相关)

##### The Range Access Method for Multiple-Part Indexes - 复合索引的范围条件查询

复合索引的范围条件可以看做是单一索引条件的扩展，定义了索引列组成的元组的范围区间，例如对于索引key1(key_part1, key_part2, key_part3)，条件key_part1 = 1定义了这样的区间：

> (1, -∞, -∞) <= (key_part1, key_part2, key_part3) < (1, +∞, +∞)

相比之下，条件key_part3 = 'abc' 无法定义一个单一区间(因为不符合leftmost原则？)，所以无法被range access方法利用。

• For HASH indexes, each interval containing identical values can be used. This means that the interval
can be produced only for conditions in the following form:

对于HASH索引，每一个包含确定值的区间都是可用的。这意味着只有条件满足以下格式时才能为条件生成区间条件：

```sql
-- 在下面的例子中：const1, const2, … 都是常量；cmp是 =, <=>, IS NULL操作符中的一个；N个条件按顺序分别对应组成复合索引的N个列
WHERE key_part1 cmp const1
AND key_part2 cmp const2
AND ...
AND key_partN cmp constN;
-- 实例 如果key1由key_part1, key_part2, key_part3三列组成
key_part1 = 1 AND key_part2 IS NULL AND key_part3 = 'foo'
```

• 对于BTREE索引，用AND连接的多个条件可以用来确定一个区间，其中每个条件使用=，<=>，IS NULL，>，<，> =，<=，！=，<>，BETWEEN或LIKE'pattern'（其中'pattern'不以通配符开头）来比较索引的某一字段和常量值 。只要比较运算符为=，<=>或IS NULL，优化程序就会尝试使用更多的索引列来确定区间。 如果运算符是>，<，> =，<=，！=，<>，BETWEEN或LIKE，优化程序不再考虑之后的索引列。

```sql
-- 下述表达式中，key_part1使用 = 操作符，MySQL会使用这个条件并且进一步向后解析；key_part2使用了 >= 操作符，优化器会使用这个条件用于构建区间，但是不再向后解析；所以key_part3对于区间的构建是无用的
key_part1 = 'foo' AND key_part2 >= 10 AND key_part3 > 10
-- 最终确定区间
('foo', 10, -inf) <= (key_part1, key_part2, key_part3) < ('foo', +inf, +inf)
```

• 如果最终解析得到的多个区间使用OR连接，那么最终会生成这些区间的合集；如果是使用AND连接，那么最终得到的是他们的交集

```sql
-- 例如下面的语句
(key_part1 = 1 AND key_part2 < 2) OR (key_part1 > 5)
-- OR连接的两个条件分别可以解析得到两个区间
(1,-inf) < (key_part1,key_part2) < (1,2)
(5,-inf) < (key_part1,key_part2)
-- 最终生效的是他们的合集
```

##### Equality Range Optimization of Many-Valued Comparisons

```sql
-- 本标题是指如下的查询，假设col_name是索引的列
col_name IN(val1, ..., valN)
col_name = val1 OR ... OR col_name = valN
```

优化器采用如下的逻辑估算读取成本：

• 如果col_name上的索引是唯一索引，那么每一个条件的估算值为1

• 否则，优化器会对每一个条件值进行计数来估算这个值，估算方法有index dive和index statistics

使用index dive，优化器会对每个范围的两端进行一次index dive然后使用得到的行数作为估算。例如：col_name IN (10, 20, 30)包含三个范围，优化器对每个范围进行两次(col_name=10相当于10<=col_name<=10，或者col_name ∈[10, 10]，区间的两端都是10，优化器会对区间的两端分别进行一次index_dive)index dive获取行数的估算。这种方法可以获取精确地行数，但是随着参数列表的增加，耗费的时间也会边长。使用index statistics会快一些，但是精确度没有这么高。我们可以使用ANALYZE TABLE来更新表索引的统计信息，以供index statistics得到跟准确的统计数据。

MySQL提供了eq_range_index_dive_limit系统变量来配置当IN操作符的参数列表多长的时候切换这两种方法。当eq_range_index_dive_limit = N + 1时，IN操作符参数列表达到N时就会从index dive切换到index statistics，如果要求总是使用index dives，可以将eq_range_index_dive_limit设置为0。在MySQL5.6及以前的版本，这个值为10，在多数情况下这个值是偏小的，它会导致优化器转而使用index statistics方法去获取估算值，这个估算值有的时候会误差很大甚至完全错误，从而导致选择错误的执行计划。MySQL5.7版本之后这个值被调整到200.

即使在index dives可用的情况下，如果查询满足下列所有情况，也不会使用index dive：

• 在单一索引查询上使用FORCE INDEX。意思是如果我们强制使用索引，那么进行额外index dive的开销无法获得任何好处，所以MySQL优化器不会选择使用index dive。

• 索引不是unique索引，也不是FULLTEXT索引。

• 没有子查询

• 没有DISTINCT, GROUP BY或ORDER BY子句

这些跳过index dive的条件仅适用于单表查询。多表查询不会跳过index dive。

##### Limiting Memory Use for Range Optimization - 限制范围查询优化使用的内存

使用range_optimizer_max_mem_size系统变量来限制在范围查询优化时使用的内存阈值，0代表没有限制。如果优化器在确定范围查询使用的方法时使用的内存接近这个阈值，那么优化过程会被抛弃，改为使用一些其他不是最优的方法，包括全表扫描，并且我们会得到这样的警告：

> Warning 	3170 	Memory capacity of N bytes for
> 					'range_optimizer_max_mem_size' exceeded. Range
> 					optimization was not done for this query.

一般来说，如果有些查询的优化器遇到了这种情况，增大这个阈值可以帮助提高性能。

以下是处理范围表达式所需内存的估计方法：

```sql
-- For a simple query such as the following, where there is one candidate key for the 
-- range access method, each predicate combined with OR uses approximately 230 bytes:
SELECT COUNT(*) FROM t WHERE a=1 OR a=2 OR a=3 OR .. . a=N;
-- Similarly for a query such as the following, each predicate combined with AND uses 
-- approximately 125 bytes:
SELECT COUNT(*) FROM t WHERE a=1 AND b=1 AND c=1 ... N;
-- For a query with IN() predicates: Each literal value in an IN() list counts as a  
-- predicate combined with OR. If there are two IN() lists, the number of predicates 
-- combined with OR is the product of the number of literal values in each list.
-- Thus, the number of predicates combined with OR in the preceding case is M × N.
SELECT COUNT(*) FROM t WHERE a IN (1,2, ..., M) AND b IN (1,2, ..., N);
```

##### Range Optimization of Row Constructor Expressions

```sql
-- MySQL优化器现在支持这种语法：
SELECT ... FROM t1 WHERE 
( col_1, col_2 ) IN (( 'a', 'b' ), ( 'c', 'd' ));
-- 而以前我们必须这样写
SELECT ... FROM t1 WHERE 
( col_1 = 'a' AND col_2 = 'b' ) OR ( col_1 = 'c' AND col_2 = 'd' );
```

要使优化器支持这种格式的范围扫描，需要满足下列格式要求：

• 只允许使用IN()而非NOT IN()

• IN()左侧仅包含列的引用

• IN()右侧值必须为常量或在执行时可以被转化为常量

• IN()右侧至少多于一个参数

#### Index Merge Optimization - 索引合并优化

所谓Index Merge是指合并若干个范围查询条件的结果，如果一个查询语句的多个范围查询条件使用到了多个索引，在分别检索这些索引得出结果之后，对这些结果再进行集合操作。这个操作只支持单表，可以支持交集、并集或者并集的交集。 通过EXPLAIN分析使用了Index Merge的SQL语句，在输出的type列中会显示index_merge，key列会包含用到的索引的列表，key_len列包含了这些索引的长度。Index Merge使用了几种不同的算法，这些算法内容输出在EXPLAIN结果的Extra列中。比如：

> Using intersect(...)
> Using union(...)
> Using sort_union(...)

优化器会综合考虑各种因素来选取其中的算法。但是选取的开关受制于 optimizer_switch系统变量的index_merge, index_merge_intersection, index_merge_union, index_merge_sort_union的boolean类型的值。默认这些值全都为on，如果要使能指定算法，将index_merge的值设置为off，然后把需要使用的算法设置为on。

```sql
-- 我们建这样一张表，来看EXPLAIN结果中index merge的结果
CREATE TABLE `tmp_index_merge` (
  `id` INT(11) NOT NULL,
  `key1_part1` INT(11) NOT NULL,
  `key1_part2` INT(11) NOT NULL,
  `key2_part1` INT(11) NOT NULL,
  `key2_part2` INT(11) NOT NULL,
  `key2_part3` INT(11) NOT NULL,
  `key3_part1` INT(11) NOT NULL DEFAULT '4',
  KEY `index1` (`key1_part1`,`key1_part2`),
  KEY `index2` (`key2_part1`,`key2_part2`,`key2_part3`),
  KEY `index3` (`key3_part1`)
) ENGINE=INNODB DEFAULT CHARSET=uft8;

-- 1. key1_part1 = 2 OR key2_part1 = 4 分别使用到了两个索引，两个索引的检索结束后会执行index merge
EXPLAIN SELECT * FROM tmp_index_merge WHERE key1_part1 = 2 OR key2_part1 = 4\G;
*************************** 1. row ***************************
           id: 1
  select_type: SIMPLE
        table: tmp_index_merge
   partitions: NULL
         type: index_merge
possible_keys: ind1,ind2
          key: ind1,ind2
      key_len: 4,4
          ref: NULL
         rows: 9
     filtered: 100.00
        Extra: Using sort_union(ind1,ind2); Using where

-- 2. key2_part2 = 4造成了第二个索引失效，导致整个语句的全表扫描
EXPLAIN SELECT * FROM tmp_index_merge WHERE key1_part1 = 2 OR key2_part2 = 4\G;
*************************** 1. row ***************************
           id: 1
  select_type: SIMPLE
        table: tmp_index_merge
   partitions: NULL
         type: ALL
possible_keys: ind1
          key: NULL
      key_len: NULL
          ref: NULL
         rows: 48
     filtered: 22.86
        Extra: Using where
        
-- 3. (key1_part1 = 2 AND key1_part2 = 7)使用到了第一个索引，key2_part1 = 4命中了第二个索引，但是相较于1，由于使用到了index1的第二个字段，所以索引长度增加
EXPLAIN SELECT * FROM tmp_index_merge 
WHERE (key1_part1 = 2 AND key1_part2 = 7) OR key2_part1 = 4\G;
*************************** 1. row ***************************
           id: 1
  select_type: SIMPLE
        table: tmp_index_merge
   partitions: NULL
         type: index_merge
possible_keys: ind1,ind2
          key: ind1,ind2
      key_len: 8,4
          ref: NULL
         rows: 9
     filtered: 100.00
        Extra: Using sort_union(ind1,ind2); Using where

-- 4. 对于同一个索引的同一个字段的OR操作, MySQL会有其他的处理方式
EXPLAIN SELECT * FROM tmp_index_merge 
WHERE (key1_part1 = 2 OR  key1_part1 = 7) OR key2_part1 = 4\G;
*************************** 1. row ***************************
           id: 1
  select_type: SIMPLE
        table: tmp_index_merge
   partitions: NULL
         type: ALL
possible_keys: ind1,ind2
          key: NULL
      key_len: NULL
          ref: NULL
         rows: 48
     filtered: 37.03
        Extra: Using where
1 row in set, 1 warning (0.00 sec)
```

##### The Index Merge Intersection Access Algorithm

条件：WHERE子句中的查询条件被解析为若干个能应用到不同索引上的范围查询部分，并且由AND连接，这些部分需要满足如下的条件：

• 在一个由N个字段组成的复合索引上应用如下查询条件，其中key_part{n}组成了这个索引，并且索引在创建的时候也是这个顺序：

> key_part1 = const1 AND key_part2 = const2 ... AND key_partN = constN

• InnoDB引擎表中对主键应用范围条件查询	

```sql
-- 例如：
SELECT * FROM innodb_table WHERE primary_key < 10 AND key_col1 = 20;
SELECT * FROM tbl_name WHERE (key1_part1 = 1 AND key1_part2 = 2) AND key2 = 2;
```

应用Index Merge intersection算法会对用到的多个索引进行并发扫描并得到其交集。如果索引覆盖了查询的列，就不会去数据文件中读取列数据，这种情况在EXPLAIN结果中会通过Extra列的Using index值说明。例如：

```sql
SELECT COUNT(*) FROM t1 WHERE key1 = 1 AND key2 = 1;
```

如果使用的索引未涵盖查询中使用的所有列，则仅在满足所有使用的key的范围条件时才检索完整行。(啥意思)

查询条件中的范围条件如果是InnoDB表的主键，那么这个条件并不会用于检索完整行数据，而是用于过滤其他条件产生的结果。

##### The Index Merge Union Access Algorithm

这一算法的应用条件与上一个算法可以理解为完全相同，例如：

```sql
SELECT * FROM t1 WHERE key1 = 1 OR key2 = 2 OR key3 = 3;
SELECT * FROM innodb_table WHERE (key1 = 1 AND key2 = 2) 
OR (key3 = 'foo' AND key4 = 'bar') AND key5 = 5;
```

##### The Index Merge Sort-Union Access Algorithm

这一算法的应用条件是当WHERE子句被转换成若干由OR连接的范围条件查询，但是Index Merge union不是用的场景，两种算法的区别是，后者必须先获取rowID，并且在返回结果之前。例如：

```sql
SELECT * FROM tbl_name WHERE key_col1 < 10 OR key_col2 < 20;
SELECT * FROM tbl_name WHERE (key_col1 > 10 OR key_col2 = 20) AND nonkey_col = 30;
```

#### Engine Condition Pushdown Optimization - NDB专用

#### Index Condition Pushdown Optimization

Index Condition Pushdown (ICP)是MySQL对通过索引检索整行表数据的时机的优化。在没有ICP的时候，MySQL存储引擎通过遍历索引定位数据行在基础表中的位置，然后从存储引擎读取并返回给MySQL服务器，后者再对这些行应用WHERE条件。有了ICP之后，如果WHERE条件的某些部分只需要使用某个索引就能求值，MySQL服务器就会直接把这些条件“下推”给存储引擎处理，后者只需要返回满足条件的数据行。ICP的应用，减少了存储引擎读取数据表和MySQL服务器与存储引擎的交互。

ICP的应用受限于下列条件：

• 当需要访问完整的表行时，ICP应用于range，ref，eq_ref和ref_or_null方法

• ICP可以应用于InnoDB或者MyISAM引擎的数据表，包括分区的表

• 对于使用InnoDB存储引擎的表，ICP仅适用于二级索引。ICP的目标是减少对整行表数据的读取次数，从而减少I/O操作。对于InnoDB引擎下的聚类索引(clustered index)，完整的数据记录已经被读入InnoDB buffer，此时使用ICP并不能减少I/O操作

• InnoDB引擎支持在虚拟列上建立二级索引(secondary index)。但是ICP不支持这种场景

• 引用了子查询的条件无法被“下推”

• 引用存储函数的条件无法被“下推”，因为存储引擎无法调用存储函数

• 使用了触发器的查询条件不能被“下推”

> Clustered Index：
> 每一个InnoDB表都有一个特殊的索引，叫做clustered index，InnoDB选择clustered index原则如下：
>
> -如果表上定义了主键，则使用主键作为clustered index
> -如果没有定义主键，选择第一个非空的UNIQUE索引作为clustered index。所以，如果表只有一个非空的UNIQUE索引，那么InnoDB就把它当作主键了
> -如果即没有主键也没有合适的UNIQUE索引，InnoDB内部产生一个隐藏列，这个列包含了每一行的row ID, row ID随着新行的插入而单调增加。然后在这个隐藏列上建立索引作为clustered index
>
> Secondary Index：
> 除了Clustered Index之外的索引都是Secondary Index，每一个Secondary Index的记录中除了索引列的值之外，还包含row ID。通过二级索引查询首先查到是row ID值，然后InnoDB再根据查到的row ID值通过cluster index找到相应的数据块  

我们举例对比ICP带来的性能提升，假设我们的people表有一个索引`idx_zipcode_lastname_firstname`(zipcode, lastname, firstname)。对于下面的SQL语句：

```sql
SELECT * FROM people WHERE 
zipcode='95054' AND lastname LIKE '%etrunia%' AND address LIKE '%Main Street%';
```

**在没有ICP之前它是这样执行的**

1. 从索引idx_zipcode_lastname_firstname里面取出下一条满足zipcode='95054'的记录，然后利用索引中的row ID定位并读取整行数据
2. 然后对这个完整的行利用lastname LIKE '%etrunia%'进行判断看是否符合条件
3. 重复1-2

**有了ICP之后则是这样执行的**

1. 从索引idx_zipcode_lastname_firstname里面取出下一条满足zipcode='95054'的记录，然后利用这个索引的其他字段条件进行判断，如果条件成立，执行第2步，否则第3步
2. 在上一步中筛选出来符合条件的才会利用row ID读取完整行。
3. 从1开始重复这个过程

如果用到了ICP，EXPLAIN的Extra列会显示Using index condition结果：

```sql
mysql> explain SELECT * FROM people WHERE 
zipcode='95054' AND lastname LIKE '%etrunia%' AND address LIKE '%Main Street%'\G;
*************************** 1. row ***************************
           id: 1
  select_type: SIMPLE
        table: people
   partitions: NULL
         type: ref
possible_keys: idx_zipcode_lastname_firstname
          key: idx_zipcode_lastname_firstname
      key_len: 33
          ref: const
         rows: 1
     filtered: 100.00
        Extra: Using index condition; Using where
```

默认ICP是开启的，我么可以通过如下配置来改变这一预设

```sql
SET optimizer_switch = 'index_condition_pushdown=off';
SET optimizer_switch = 'index_condition_pushdown=on';
```

#### Nested-Loop Join Algorithms

MySQL执行表之间的JOIN操作时使用了Nested-Loop Join算法或它的一个变体Block Nested-Loop Join算法。

##### • Nested-Loop Join Algorithm

simple nested-loop join (NLJ) 算法从第一个表中每次读取一行，然后传入JOIN链的下一个表，如此递归和循环。例如我们有如下的JOIN条件

| Table | Join Type |
| ----- | --------- |
| t1    | range     |
| t2    | ref       |
| t3    | ALL       |

我们可以使用伪代码来说明NLJ算法的逻辑。很明显这种多级嵌套循环是会极大损失性能的。

```python
for each row in t1 matching range {
	for each row in t2 matching reference key {
		for each row in t3 { 
			if row satisfies join conditions {
            	send to client
            }
        }
    }
}
```

##### • Block Nested-Loop Join Algorithm

Block Nested-Loop (BNL) join算法通过缓冲外层循环中读取数据的行数来减少内层循环中对标数据的读取次数。例如，如果外层循环一次性从表中读取10行数据存入缓冲区，并将整个缓冲数据传入内层循环，那么内层循环中读取到的每一条数据都可以一次性和10条数据进行比较。这就将内部循环读取表的次数减少了一个数量级。

MySQL的JOIN缓冲区有下列特点：

• 当连接类型为ALL或index或range时，可以使用JOIN缓冲。缓冲区也适用于外连接

• 即使连接类型是ALL或index类型，第一个表如果不是常量表的时候也不会使用JOIN缓冲

• 并非外部表的整行数据都会被存入JOIN缓冲区，只有JOIN用到的列值会存入JOIN缓冲区，

• join_buffer_size系统变量决定了JOIN缓冲区的大小

• 每一次JOIN操作在满足条件时都会使用一个JOIN缓冲，所以在一个查询语句中有可能会用到多个JOIN缓冲区

• JOIN缓冲先于JOIN操作的执行被分配，查询完成后被释放

上述NLJ中的例子，应用BNL之后是这样的

```python
for each row in t1 matching range {
	for each row in t2 matching reference key { 
		store used columns from t1, t2 in join buffer
		if buffer is full {
			for each row in t3 { 
				for each t1, t2 combination in join buffer { 
					if row satisfies join conditions {
                        send to client empty join buffer
                    }
                }
            }
            empty join buffer
        }
    }
}    

if buffer is not empty {
	for each row in t3 { 
		for each t1, t2 combination in join buffer {
			if row satisfies join conditions, send to client
        }
    }
}
```

如果S是每一条t1, t2组合的数据大小，C是缓冲区中数据的数量，那么t3表被扫描的次数是

> (S * C)/join_buffer_size + 1

随着join_buffer_size的增长，t3被扫描的次数会减少，直到join_buffer_size大到足够容纳所有t1,t2数据组合，之后再增加这个值就不会有性能上的提升了。

#### Nested Join Optimization - 嵌套JOIN优化

与SQL标准相比，table_factor的语法可以看作是一种从只接收一个table_reference到接收一对括号内的table_reference列表的扩展。如果我们将table_reference项列表中的每个逗号视为等效于内连接，则这是一个保守的扩展。例如下列两个语句可以看做是等价的：

```sql
SELECT * FROM t1 LEFT JOIN (t2, t3, t4) ON (t2.a=t1.a AND t3.b=t1.b AND t4.c=t1.c)
SELECT * FROM t1 LEFT JOIN (t2 CROSS JOIN t3 CROSS JOIN t4) ON (t2.a=t1.a AND t3.b=t1.b AND t4.c=t1.c)
```

在MySQL语法中，CROSS JOIN与INNER JOIN是等价的，可以相互替换。在标准SQL中，他们不是等价的，INNER JOIN与ON子句一起使用；CROSS JOIN使用在其外的情况中。

一般情况下，如果JOIN表达式中只含有inner join操作时，括号可以被省略。我们来看如下两个语句：

```sql
t1 LEFT JOIN (t2 LEFT JOIN t3 ON t2.b=t3.b OR t2.b IS NULL) ON t1.a=t2.a
```

去掉括号并且把每组JOIN的条件分组之后，这个表达式可以变形为：

```sql
(t1 LEFT JOIN t2 ON t1.a=t2.a) LEFT JOIN t3 ON t2.b=t3.b OR t2.b IS NULL
```

但是这两个表达式的结果并不等价，假设其中涉及到的三个表有这样的数据：

• Table t1 contains rows (1), (2)
• Table t2 contains row (1,101)
• Table t3 contains row (101)

这两个语句的执行结果是这样的：

```sql
mysql> SELECT * FROM t1
 		LEFT JOIN (t2 LEFT JOIN t3 ON t2.b=t3.b OR t2.b IS NULL) ON t1.a=t2.a;
+------+------+------+------+
| a    | a    | b    | b    |
+------+------+------+------+
| 1    | 1    | 101  | 101  |
| 2    | NULL | NULL | NULL |
+------+------+------+------+
mysql> SELECT * FROM (t1 LEFT JOIN t2 ON t1.a=t2.a)
 		LEFT JOIN t3 ON t2.b=t3.b OR t2.b IS NULL;
+------+------+------+------+
| a    | a    | b    | b    |
+------+------+------+------+
| 1    | 1    | 101  | 101  |
| 2    | NULL | NULL | 101  |
+------+------+------+------+
```

下面的例子里同时用到了outer join和inner join操作

```sql
t1 LEFT JOIN (t2, t3) ON t1.a=t2.a
```

这个表达式可以变形为：

```sql
t1 LEFT JOIN t2 ON t1.a=t2.a, t3
```

在上述数据基础上执行这两个表达式可以得到

```sql
mysql> SELECT * FROM t1 LEFT JOIN (t2, t3) ON t1.a=t2.a;
+------+------+------+------+
| a    | a    | b    | b    |
+------+------+------+------+
| 1    | 1    | 101  | 101  |
| 2    | NULL | NULL | NULL |
+------+------+------+------+
mysql> SELECT * FROM t1 LEFT JOIN t2 ON t1.a=t2.a, t3;
+------+------+------+------+
| a    | a    | b    | b    |
+------+------+------+------+
| 1    | 1    | 101  | 101  |
| 2    | NULL | NULL | 101  |
+------+------+------+------+
```

因此，如果我们在outer join操作中省略括号，我们可能会改变结果集。

更准确的说，**我们不能省略left outer join操作的右操作数和right join操作的左操作数中的括号**。换句话说，**我们不能忽略outer join操作中的inner table表达式中的括号**。其他操作数(outer table)表达式中的括号可以省略。

下面的表达式

```sql
(t1,t2) LEFT JOIN t3 ON P(t2.b,t3.b)
```

对于任意表t1,t2,t3和基于t2.b和t3.b属性的任意条件P是等价的：

```sql
t1, t2 LEFT JOIN t3 ON P(t2.b,t3.b)
```

当连接表达式中的连接操作的执行顺序不是从左到右时，我们讨论 的就是嵌套连接。

```sql
SELECT * FROM t1 LEFT JOIN (t2 LEFT JOIN t3 ON t2.b=t3.b) ON t1.a=t2.a 
WHERE t1.a > 1
SELECT * FROM t1 LEFT JOIN (t2, t3) ON t1.a=t2.a 
WHERE (t2.b=t3.b OR t2.b IS NULL) AND t1.a > 1
-- 这两个语句分别包含嵌套连接条件：
t2 LEFT JOIN t3 ON t2.b=t3.b
t2, t3
```

在第一个查询中，括号可以省略，因为连接表达式的语法结构说明连接操作按照相同顺序执行。对于第二个查询，括号不能省略，尽管这里的连接表达式可以在没有括号的情况下得到明确解释。在我们的扩展语法中，第二个查询的（t2，t3）中的括号是必需的，虽然理论上可以在没有它们的情况下解析查询，我们仍然会为查询提供明确的语法结构，因为LEFT JOIN和ON扮演了表达式（t2，t3）的左右括号的角色。

以上例子说明：

• 对于仅涉及内连接的连接表达式，可以删除括号并从左到右计算。 实际上，可以按任何顺序计算

• 通常，对于外连接或与内连接混合的外连接，情况并非如此。 删除括号可能会改变结果

使用嵌套外连接的查询以与具有内连接的查询相同的管道方式执行。更确切地说，利用了nested-loop join算法的变体。假设有一个涉及到三个表T1,T2,T3的连接操作查询：

```sql
SELECT * FROM T1 INNER JOIN T2 ON P1(T1,T2) INNER JOIN T3 ON P2(T2,T3)
WHERE P(T1,T2,T3)
```

 nested-loop join算法这样执行上述查询

```python
for each row t1 in T1 {
	for each row t2 in T2 such that P1(t1,t2) {
		for each row t3 in T3 such that P2(t2,t3) {
 			if P(t1,t2,t3) {
 				t:=t1||t2||t3; 
                OUTPUT t;
            }
        }
    }
}
```

现在思考这个嵌套外连接的查询

```sql
SELECT * FROM T1 LEFT JOIN (T2 LEFT JOIN T3 ON P2(T2,T3)) ON P1(T1,T2) 
WHERE P(T1,T2,T3)
```

通过修改语句格式使得NLJ这样执行这个查询

```python
for each row t1 in T1 
	BOOL f1:=FALSE;
	for each row t2 in T2 such that P1(t1,t2) 
		BOOL f2:=FALSE;
		for each row t3 in T3 such that P2(t2,t3) 
		if P(t1,t2,t3) 
			t:=t1||t2||t3; 
            OUTPUT t;
 	f2=TRUE;
 	f1=TRUE;
 
if (!f2) 
 	if P(t1,t2,NULL) 
 		t:=t1||t2||NULL; 
        OUTPUT t;
	f1=TRUE;
 
if (!f1) 
	if P(t1,NULL,NULL) 
 		t:=t1||NULL||NULL; 
        OUTPUT t; 
```

通常，对于outer join操作中第一个内部表的任何嵌套循环，引入一个在循环之前置为false并在循环之后检查的标志。 当对于来自外部表的当前行，找到与表示内部操作数的表的匹配时，该标志被置为true。 如果在循环周期结束时标志仍处于false，则表示未找到外部表的当前行的匹配项。 在这种情况下，行由内部表的列的NULL值补充。 结果行将传递给输出的最终检查或下一个嵌套循环，但前提是该行满足所有嵌入外连接的连接条件。

在上面的示例中，嵌入了由以下表达式表示的外连接表:

```sql
(T2 LEFT JOIN T3 ON P2(T2,T3))
```

对于具有inner join的查询，优化程序可以选择不同的嵌套循环顺序，例如

```python
FOR each row t3 in T3 
	FOR each row t2 in T2 such that P2(t2,t3) 
		FOR each row t1 in T1 such that P1(t1,t2) 
			IF P(t1,t2,t3) 
				t:=t1||t2||t3; 
                OUTPUT t;
```

对于具有外连接的查询，优化器只能选择这样一个顺序，其中外部表的循环先于内部表的循环。因此，对于具有外连接的查询，只能有一个嵌套顺序。对于以下查询，优化程序将对两个不同的嵌套求值。在两个嵌套中，T1必须在外部循环中处理，因为它在外连接中使用。T2和T3用于内连接，所以必须在内部循环中处理该连接。但是，因为连接是内连接，T2和T3可以按任何顺序处理。

```sql
SELECT * T1 LEFT JOIN (T2,T3) ON P1(T1,T2) AND P2(T1,T3) WHERE P(T1,T2,T3)
```

```python
-- 先求值T2再求值T3
FOR each row t1 in T1 {
	BOOL f1:=FALSE;
	FOR each row t2 in T2 such that P1(t1,t2) {
		FOR each row t3 in T3 such that P2(t1,t3) {
			IF P(t1,t2,t3) {
				t:=t1||t2||t3; OUTPUT t;
 			}
 			f1:=TRUE
 		}
 	}
	IF (!f1) {
 		IF P(t1,NULL,NULL) {
 			t:=t1||NULL||NULL; 
        	OUTPUT t;
 		}
 	}
}
-- 先求值T3再求值T2
FOR each row t1 in T1 {
	BOOL f1:=FALSE;
	FOR each row t3 in T3 such that P2(t1,t3) {
		FOR each row t2 in T2 such that P1(t1,t2) {
 			IF P(t1,t2,t3) {
				t:=t1||t2||t3; 
                OUTPUT t;
 			}
 			f1:=TRUE
 		}
 	}
	IF (!f1) {
		IF P(t1,NULL,NULL) {
 			t:=t1||NULL||NULL; 
        	OUTPUT t;
 		}
	}
}
```

在讨论内部联接的嵌套循环算法时，我们省略了一些对查询执行性能的影响可能很大的细节。我们没有提到所谓的"push down"条件。 假设我们的WHERE条件P（T1，T2，T3）可以用连接公式表示：

```sql
P(T1,T2,T2) = C1(T1) AND C2(T2) AND C3(T3).
```

实际上MySQL使用如下的nested-loop算法

```python
FOR each row t1 in T1 such that C1(t1) {
	FOR each row t2 in T2 such that P1(t1,t2) AND C2(t2) {
		FOR each row t3 in T3 such that P2(t2,t3) AND C3(t3) {
			IF P(t1,t2,t3) {
				t:=t1||t2||t3; 
                OUTPUT t;
 			}
 		}
 	}
}
```

我们注意到到每个链接条件C1（T1），C2（T2），C3（T3）都被推出最内层循环到最外层循环求值。 如果C1（T1）是一个非常严格的条件，这种情况下pushdown可能会大大减少表T1传递到内部循环的行数，查询的执行时间可能会大大改善。

对于具有外连接的查询，仅在发现外部表中的当前行在内部表中具有匹配项之后才检查WHERE条件。 因此，从内部嵌套循环中将条件推到外层循环检查的优化方式不能直接应用于具有外部联接的查询。 还是上一个连接，使用guarded pushed-down conditions nested-loop的算法是这样的：

```python
FOR each row t1 in T1 such that C1(t1) {
	BOOL f1:=FALSE;
	FOR each row t2 in T2 such that P1(t1,t2) AND (f1?C2(t2):TRUE) {
		BOOL f2:=FALSE;
		FOR each row t3 in T3 such that P2(t2,t3) AND (f1&&f2?C3(t3):TRUE) {
			IF (f1&&f2?TRUE:(C2(t2) AND C3(t3))) {
				t:=t1||t2||t3; 
                OUTPUT t;
			}
			f2=TRUE;
			f1=TRUE;
		}
		IF (!f2) {
			IF (f1?TRUE:C2(t2) && P(t1,t2,NULL)) {
				t:=t1||t2||NULL; 
            	OUTPUT t;
			}
			f1=TRUE;
		}
	}
	IF (!f1 && P(t1,NULL,NULL)) {
		t:=t1||NULL||NULL; 
    	OUTPUT t;
	}
}
```

#### Left Join and Right Join Optimization - 左/右连接优化

MySQL是这样实现**A LEFT JOIN B join_condition**这种查询的：

• 表B被设置为依赖于表A和A所依赖的所有表

• 表A设置为依赖于LEFT JOIN条件中使用的所有表（B除外）

• LEFT JOIN条件用于决定如何从表B中检索行。（换句话说，不使用WHERE子句中的任何条件。）

• 执行所有标准JOIN操作的优化，例外是一个表的读取始终在所依赖的所有表读取完成之后。 如果存在循环依赖关系，则会发生错误

• 执行所有标准WHERE优化

• 如果A中有一行与WHERE子句匹配，但B中没有与ON条件匹配的行，则会生成一个额外的B行，并将所有列设置为NULL

• If you use LEFT JOIN to find rows that do not exist in some table and you have the following test:
col_name IS NULL in the WHERE part, where col_name is a column that is declared as NOT
NULL, MySQL stops searching for more rows (for a particular key combination) after it has found one
row that matches the LEFT JOIN condition.

RIGHT JOIN实现类似于LEFT JOIN的实现，只需要把表格角色颠倒过来。右连接可以转换为等效的左连接。连接优化器通过计算确定连接表的读取顺序。 由LEFT JOIN或STRAIGHT_JOIN指定的表读取顺序有助于连接优化器更快地完成这一工作，因为要检查的表排列更少。 这意味着如果执行以下查询，MySQL会对b执行全表扫描，因为LEFT JOIN强制在d之前读取b：

```sql
SELECT * FROM a 
 JOIN b 
 LEFT JOIN c ON (c.key = a.key)
 LEFT JOIN d ON (d.key = a.key)
 WHERE b.key = d.key;
```

这个例子中的解决方式是对换FROM子句中a和b的顺序

```sql
SELECT * FROM b 
 JOIN a 
 LEFT JOIN c ON (c.key=a.key)
 LEFT JOIN d ON (d.key=a.key)
 WHERE b.key=d.key;
```

对于LEFT JOIN，如果生成的NULL行的WHERE条件始终为false，则LEFT JOIN将被转换为普通连接。例如在下列的查询中，如果t2.column为NULL，则WHERE子句的值恒为false：

```sql
SELECT * FROM t1 LEFT JOIN t2 ON (column1) WHERE t2.column2 = 5;
```

那么，我们可以安全地将上述查询语句转化成：

```sql
SELECT * FROM t1, t2 WHERE t2.column2 = 5 AND t1.column1=t2.column1;
```

现在优化器可以在表t1之前使用表t2，如果这样做能得到更优的查询计划。

#### Outer Join Simplification - 外连接简化

在许多情况下，查询语句FROM子句中的表达式会被简化。

在解析阶段，具有右外连接操作的查询将转换为等效的仅包含左连接操作的查询，例如：

```sql
-- 下面的右连接会被转化为等价的左连接
(T1, ...) RIGHT JOIN (T2, ...) ON P(T1, ..., T2, ...)
(T2, ...) LEFT JOIN (T1, ...) ON P(T1, ..., T2, ...)
```

形如T1 INNER JOIN T2 ON P(T1,T2)的内连接表达式会被替换成T1,T2, P(T1,T2)格式的列表连接到WHERE条件或者内嵌的JOIN条件中。

当优化器评估外连接操作的计划时，它仅考虑对于每个外连接操作需要在读取内部表之前读取外部表。 优化器的选择是受限的，因为只有这样的计划才能使用nested-loop算法执行外连接。

我们来看这样的一个查询，R(T2)可以显著地减少T2表中符合条件的数据：

```sql
SELECT * T1 LEFT JOIN T2 ON P1(T1,T2) WHERE P(T1,T2) AND R(T2)
```

如果查询是按照语句字面顺序的那样执行的，那么优化器别无选择，只能在读取限制较多的表T2之前访问限制较少的表T1，这可能会导致执行计划的效率非常低。

相反，如果WHERE条件为null-rejected，MySQL会将查询转换为没有外连接操作的查询。(也就是说，它将外连接转换为内连接)。对于外连接操作，如果对于为操作生成的任何NULL补充行，它的计算结果为FALSE或UNKNOWN，则表示该条件为null-rejected。

```sql
-- 下列条件都是null-rejected，因为对于NULL补全的行，这些条件恒为false
T2.B IS NOT NULL
T2.B > 3
T2.C <= T1.C
T2.B < 2 OR T2.C > 1
-- 下列是非null-rejected条件的例子
T2.B IS NULL
T1.B < 3 OR T2.B IS NOT NULL
T1.B < 3 OR T2.B > 3
```

判断外连接条件是否是null-rejected的普遍规则也很简单：

• 形如A IS NOT NULL，其中A是任意一个内部表的属性

• 包含对内部表的引用，当其中一个参数为NULL时，该内部表的计算结果为UNKNOWN

• It is a conjunction containing a null-rejected condition as a conjunct

• It is a disjunction of null-rejected conditions

同一个连接条件可以在一个外连接操作中是null-rejected，但在另外一个外连接操作中不是null-rejected。在这二个查询中，WHERE条件在第一个外连接中不是null-rejected，但是在第二个外连接中就是null-rejected：

```sql
SELECT * FROM T1 
LEFT JOIN T2 ON T2.A = T1.A
LEFT JOIN T3 ON T3.B = T1.B
WHERE T3.C > 0
```

如果查询中的外连接操作的WHERE条件为null-rejected，则外连作将被替换为内连接。所以上面的例子可以被转换为如下的格式：

```sql
SELECT * FROM T1 
LEFT JOIN T2 ON T2.A=T1.A
INNER JOIN T3 ON T3.B=T1.B
WHERE T3.C > 0
```

对于原始查询，优化程序仅评估与单表访问顺序T1，T2，T3兼容的计划。 对于重写的查询，它还考虑访问顺序T3，T1，T2。

外连接的转换为内连接之后的结果还可以进一步被转换或者优化，上面的例子可以进一步改写成：

```sql
SELECT * FROM (T1 LEFT JOIN T2 ON T2.A=T1.A), T3
WHERE T3.C > 0 AND T3.B=T2.B
```

结果中剩下的外连接也可以由内连接替换，因为条件T3.B = T2.B是null-rejected。所以结果中完全不包含外连接：

```sql
SELECT * FROM (T1 INNER JOIN T2 ON T2.A=T1.A), T3
WHERE T3.C > 0 AND T3.B=T2.B
```

有时，优化器可以成功替换但无法转换内嵌的外连接操作。如下查询可以这样转换：

```sql
SELECT * FROM T1 
LEFT JOIN (T2 LEFT JOIN T3 ON T3.B=T2.B) ON T2.A=T1.A
WHERE T3.C > 0
---->
SELECT * FROM T1 
LEFT JOIN (T2 INNER JOIN T3 ON T3.B=T2.B) ON T2.A=T1.A
```

转换结果只能重写为仍包含嵌入外连接操作的形式：

```sql
SELECT * FROM T1 
LEFT JOIN (T2,T3) ON (T2.A=T1.A AND T3.B=T2.B)
WHERE T3.C > 0
```

在查询中转换内嵌外连接操作都必须考虑内嵌外连接和WHERE条件的连接条件。 在下面的查询中，内嵌外连接的WHERE条件不是null-rejected，但嵌入外连接T2.A = T1.A和T3.C = T1.C的连接条件为null-rejected，所以可以这样变形：

```sql
SELECT * FROM T1 
LEFT JOIN (T2 LEFT JOIN T3 ON T3.B=T2.B) ON T2.A=T1.A AND T3.C=T1.C
WHERE T3.D > 0 OR T1.D > 0
---->
SELECT * FROM T1 
LEFT JOIN (T2, T3) ON T2.A=T1.A AND T3.C=T1.C AND T3.B=T2.B
WHERE T3.D > 0 OR T1.D > 0
```

#### Multi-Range Read Optimization

在二级索引上使用范围扫描读取行可能会导致在表很大且未存储在存储引擎的缓存中时对基表进行许多随机磁盘访问。 通过DiskSweep Multi-Range Read (MRR)优化，MySQL尝试通过首先扫描索引并收集相关行的key来减少随机磁盘访问次数。然后对key进行，最后使用主键的顺序从基表中检索行。 MRR的动机是减少随机磁盘访问的次数，实现对基表数据的更顺序化扫描。MRR有如下好处：

• MRR使MySQL能够基于索引元组按顺序而不是随机顺序访问数据行。 MySQL服务器获取一组满足查询条件的索引元组，根据数据的row ID顺序对它们进行，并使用的元组按顺序检索数据行。 这使数据访问更高效。

• MRR enables batch processing of requests for key access for operations that require access to data
rows through index tuples, such as range index scans and equi-joins that use an index for the join
attribute. MRR迭代一系列索引范围以获得合格的索引元组。 这些结果的累积到一定数量，就用他们去访问相应的数据行。 在开始读取数据行之前不必获取所有索引元组。

InnoDB引擎支持在虚拟列上建立二级索引。但是MRR不支持这种场景。MRR优化在以下场景中是有利的：

场景A：MRR可用于InnoDB和MyISAM表，用于索引范围扫描和等连接操作

1. 一部分索引元组在缓冲区中累积
2. 缓冲区中的索引元组按其row ID
3. 根据的索引元组序列访问数据行

如果用到了MRR，EXPLAIN的输出结果中，Extra列会显示Using MRR。

如果不需要访问完整的表行来生成查询结果，InnoDB和MyISAM不使用MRR。 如果结果可以完全基于索引元组中的信息生成，MRR无法带来任何性能提升。

假设表t上有一个索引idx_1(key_part1, key_part2)，下面的查询可以使用MRR：

```sql
SELECT * FROM t
 WHERE key_part1 >= 1000 AND key_part1 < 2000
 AND key_part2 = 10000;
```

索引数据由（key_part1，key_part2）值的元组组成，首先由key_part1，然后由key_part2。如果不使用MRR，索引扫描将覆盖key_part1范围从1000到2000的所有索引元组，而不管这些元组中的key_part2值如何，从这一意义上讲，扫描执行了多余的工作。使用MRR后，扫描被分为多个范围，每个范围使用key_part1的单个值(1000,1001，...，1999)。 这些扫描中的每一个单元都只需要寻找key_part2 = 10000的元组。如果索引包含许多key_part2不等于10000的元组，则MRR会使读取的索引元组数量减少很多。要使用区间表示法来表达这一点，非MRR扫描必须检查索引范围[{1000,10000}，{2000，MIN_INT}]，其中可能包括除key_part2 = 10000之外的许多元组。MRR扫描检查多个单点区间([{1000,10000}]，...，[{1999,10000}])，仅包含key_part2 = 10000的元组。

两个optimizer_switch系统变量提供了使用MRR优化的接口。mrr flag控制是否启用MRR。 如果启用了mrr(on)，则mrr_cost_based flag控制优化器是否尝试在使用和不使用MRR(on)之间进行基于成本的选择，或者尽可能使用MRR(off)。 默认情况下，mrr=om且mrr_cost_based=on。对于MRR，存储引擎使用read_rnd_buffer_size系统变量的值作为其可为缓冲区分配的内存大小的准则。引擎最多使用read_rnd_buffer_size字节，并确定一次传递中要处理的范围数。

Block Nested-Loop and Batched Key Access Joins

#### Block Nested-Loop and Batched Key Access Joins -- TODO

##### Join Buffer Management for Block Nested-Loop and Batched Key Access Algorithms

##### Block Nested-Loop Algorithm for Outer Joins and Semi-Joins

##### Batched Key Access Joins

#### IS NULL Optimization

MySQL可以使用与col_name=constant_value相同的优化手段来优化col_name is null语句。例如在col_name is null查询条件中可以使用索引和范围扫描。

```sql
SELECT * FROM tbl_name WHERE key_col IS NULL;
SELECT * FROM tbl_name WHERE key_col <=> NULL;
SELECT * FROM tbl_name WHERE key_col=const1 OR key_col=const2 OR key_col IS NULL;
```

如果WHERE子句包含声明为NOT NULL的列的col_name IS NULL条件，则表达式将被优化掉。在列可能产生NULL的情况下不会发生这种优化；例如来自LEFT JOIN右侧的表。

MySQL还可以优化col_name = expr OR col_name IS NULL这样的条件组合，这是一种在子查询中很常见的形式。使用此优化时，EXPLAIN显示ref_or_null，ref_or_null的工作原理是首先对引用到的索引键执行读操作，然后单独搜索具有NULL值的行。此优化可以为任何索引的组成部分处理一个IS NULL条件。假设在表t2的列a和b上建有索引，以下是优化查询的一些示例：

```sql
SELECT * FROM t1 WHERE t1.a=expr OR t1.a IS NULL;
SELECT * FROM t1, t2 WHERE t1.a=t2.a OR t2.a IS NULL;
SELECT * FROM t1, t2 WHERE (t1.a=t2.a OR t2.a IS NULL) AND t2.b=t1.b;
SELECT * FROM t1, t2 WHERE t1.a=t2.a AND (t2.b=t1.b OR t2.b IS NULL);
SELECT * FROM t1, t2 WHERE (t1.a=t2.a AND t2.a IS NULL AND ...)
 OR (t1.a=t2.a AND t2.a IS NULL AND ...);
```

此优化只支持一个IS NULL条件。 在以下查询中，MySQL仅对表达式(t1.a = t2.a AND t2.a IS NULL)应用优化，(t1.b=t2.b AND t2.b IS NULL)条件无法享受这一优化：

```sql
SELECT * FROM t1, t2
 WHERE (t1.a=t2.a AND t2.a IS NULL)
 OR (t1.b=t2.b AND t2.b IS NULL);
```

#### ORDER BY Optimization

##### • Use of Indexes to Satisfy ORDER BY

在某些情况下，MySQL可以直接使用索引来满足ORDER BY子句，而无需进行额外的。即使ORDER BY与索引不完全匹配，也可以使用索引，只要索引的所有未使用部分和所有额外的ORDER BY列都是WHERE子句中的常量。以下是使用索引来解析ORDER BY部分的查询：

```sql
SELECT * FROM t1 ORDER BY key_part1, key_part2;
SELECT * FROM t1 WHERE key_part1 = constant ORDER BY key_part2;
SELECT * FROM t1 ORDER BY key_part1 DESC, key_part2 DESC;
SELECT * FROM t1 WHERE key_part1 = 1 ORDER BY key_part1 DESC, key_part2 DESC;
SELECT * FROM t1 WHERE key_part1 > constant ORDER BY key_part1 ASC;
SELECT * FROM t1 WHERE key_part1 < constant ORDER BY key_part1 DESC;
SELECT * FROM t1 WHERE key_part1 = constant1 AND key_part2 > constant2 ORDER BY key_part2;
```

在某些情况下，MySQL不能使用索引来解析ORDER BY，尽管它仍然可以使用索引来查找与WHERE子句匹配的行。 例如：

```sql
-- 在不同的索引上使用ORDER BY
SELECT * FROM t1 ORDER BY key1, key2;
-- 在索引的非连续部分上使用ORDER BY
SELECT * FROM t1 WHERE key2=constant ORDER BY key_part1, key_part3;
-- 混合了ASC和DESC
SELECT * FROM t1 ORDER BY key_part1 DESC, key_part2 ASC;
-- 用于获取行数据的索引与ORDER BY中使用的索引不同
SELECT * FROM t1 WHERE key2=constant ORDER BY key1;
-- ORDER BY表达式包含索引列名以外的列名
SELECT * FROM t1 ORDER BY ABS(key);
SELECT * FROM t1 ORDER BY -key;
-- 查询连接了许多表，ORDER BY中的列并非全部来自用于检索行的第一个非常量表。
-- 使用不同的ORDER BY和GROUP BY表达式
-- 索引只包含ORDER BY子句中使用的列的一部分，在这种情况下，索引不能用于完全解析顺序。例如，如果仅索引CHAR（20）列的前10个字节，则索引无法区分超过第10个字节的值，这时需要filesort
-- 索引不按顺序存储行。例如MEMORY表中的HASH索引。
```

中能否使用索引可能受列别名的影响。假设列t1.a上建立了索引，在下面语句中，选择列表中列的名称是a，指向t1.a，对ORDER BY中的a的引用也是如此，因此可以使用t1.a上的索引：

```sql
SELECT a FROM t1 ORDER BY a;
```

在此语句中，选择列表中列的名称也是a，但它是别名。 它指的是ABS(a)，对ORDER BY中的a的引用也是如此，因此不能使用t1.a上的索引：

```sql
SELECT ABS(a) AS a FROM t1 ORDER BY a;
```

在以下语句中，ORDER BY引用的名称不是选择列表中列的名称。 但是在t1中有一个名为a的列，因此ORDER BY引用t1.a并且可以使用t1.a上的索引：

```sql
SELECT ABS(a) AS b FROM t1 ORDER BY a;
```

默认情况下，MySQL会对所有GROUP BY col1，col2，...查询进行，和在查询中显式指定了ORDER BY col1，col2，.... 一样。MySQL优化器会将ORDER BY语句优化掉而不付出任何代价。

如果查询包含GROUP BY但我们希望避免对结果进行的开销，则可以通过指定ORDER BY NULL来禁止。 

```sql
SELECT a, COUNT(*) FROM bar GROUP BY a ORDER BY NULL;
```

优化器仍然需要来实现分组操作。 ORDER BY NULL禁止对最终结果进行，而不是禁止分组操作需要的预先。

##### • Optimization Using filesort

MySQL有多种文件算法，用于和检索结果。原始的filesort算法仅使用ORDER BY子句中用到列。修改后的算法不仅使用ORDER BY列，还使用查询中用到的所有列。还有一种用于小型结果集的算法，该算法使用缓冲区作为优先级队列在内存中进行，而不使用合并文件。

##### • The Original filesort Algorithm

原始的文件算法步骤如下：

1. 根据key或全表扫描读取所有行，跳过与WHERE子句不匹配的行
2. 对于每一行，在缓冲区中存储由关键字的值和row ID组成的元组数据
3. 如果缓冲区可以容纳所有元组数据，则不需要创建临时文件。否则，当缓冲区占满时，在内存中运行快速并将其写入临时文件并保存指向已块的指针
4. 重复上述步骤，直到读取完所有行
5. 对最多MERGEBUFF (7)块数据进行合并并存入另外一个临时文件。重复这一过程直到第一个文件中的所有块都在第二个文件中
6. 重复以下步骤，直到剩下的文件块少于MERGEBUFF2(15)
7. 在后一次的合并操作中，只将row ID写入结果文件
8. 通过结果文件中好的row ID读取行数据。要优化这一步，增大row ID数据块的大小，对它们进行，然后按这个顺序将行数据读入行缓冲区。行缓冲区大小是read_rnd_buffer_size系统变量值。

上述过程在sql/records.cc源代码文件中。这种方法的一个问题是它需要两次读取行数据：一次是在WHERE子句求值期间，一次是在对值对进行之后。即使第一次是连续访问行（例如，如果进行了表扫描），第二次还是随机访问行数据。（元组数据是按照的键而非row ID）

##### • The Modified filesort Algorithm

修改后的文件算法为了避免两次读取行数据进行了优化：元组中存储了key值和查询引用的列，而不是row ID。具体步骤为：

1. 读取符合WHERE子句条件的行数据
2. 对于每一行，在缓冲区中存储一个由key值和查询引用的列数据组成的元组
3. 当缓冲区变满时，按内存中的key值对元组进行，并将其写入临时文件
4. 合并临时文件后，按顺序直接从的元组中读取查询所需的列，不需要第二次访问表数据

修改后的文件算法使用的元组数据比原始算法使用的元组长，所以缓冲区能容纳的元组数据量更少。因此会导致额外的I/O，可能会使修改后的方法更慢，而不是更快。为避免这一点，仅当元组中额外列的总大小不超过max_length_for_sort_data系统变量的值时，优化程序才选用修改后的算法。将此变量的值设置得过高会导致高磁盘活动和低CPU活动的组合。

修改后的文件算法包括一个额外的优化，旨在使缓冲区能容纳更多元组数据：对于CHAR或VARCHAR类型的列，或任何可为空的固定大小数据类型，值都是被打包的。例如，在没有打包的情况下，包含仅3个字符的VARCHAR(255)列值在缓冲区中占用255个字符。 使用打包时，该值仅需要3个字符加上两个字节的长度指示符。 NULL值只需要一个位掩码。这改进了缓冲区的内存中和基于磁盘的临时文件合并的性能。

在边缘情况下，打包可能是不利的：如果可压缩字符串接近最大列长度或NULL值很少，则长度指示符所需的空间会减少缓冲区能容纳的记录数，并且内存和磁盘中的速度较慢。

##### • The In-Memory filesort Algorithm

对于以下形式的查询（和子查询），优化器可以使用filesort有效地在内存中处理ORDER BY操作而不需要在磁盘上生成合并文件：

```sql
SELECT ... FROM single_table ... ORDER BY non_index_column [DESC] LIMIT [M,]N;
```

如果N行(如果指定了M，则M + N行)待数据足够小，缓冲区可以完全容纳，则服务器可以避免使用合并文件，直接将缓冲区视为优先级队列在内存中执行：

1. Scan the table, inserting the select list columns from each selected row in sorted order in the
   queue. If the queue is full, bump out the last row in the sort order.
2. Return the first N rows from the queue. (If M was specified, skip the first M rows and return the next
   N rows.)

如果没有上述优化方法，服务器将使用合并文件执行此操作：

1. Scan the table, repeating these steps through the end of the table:
   • Select rows until the sort buffer is filled.
   • Write the first N rows in the buffer (M+N rows if M was specified) to a merge file.
2. Sort the merge file and return the first N rows. (If M was specified, skip the first M rows and return the
   next N rows.)

表扫描操作的成本对于内存队列和合并文件方法是相同的，因此优化器根据其他成本在方法之间进行选择：

• 队列方法涉及更多CPU资源，用于按顺序将行插入队列。

• merge-file方法需要写入和读取文件的I/O成本以及对其进行的CPU成本。

优化程序会根据特定的M，N值和行大小这些因素考虑使用哪一种方法。

##### • Comparison of filesort Algorithms - 比较

假设表t1有四个VARCHAR列a，b，c和d，并且优化器使用filesort算法处理下述查询：

```sql
SELECT * FROM t1 ORDER BY a, b;
```

查询按a和b，但返回所有列，因此查询引用的列为a，b，c和d。根据优化程序选择的filesort算法，查询执行细节如下：
对于原始filesort算法，缓冲区元组存储以下内容：

```
(fixed size a value, fixed size b value, row ID into t1)
优化器对固定大小的元组值进行。后，优化器按顺序读取元组并使用每个元组中的row ID从t1表中读取行以获取SELECT列表列值

```

对于不打包的修改的filesort算法，缓冲区元组存储以下内容：

```sql
(fixed size a value, fixed size b value, a value, b value, c value, d value)
优化器对固定大小的元组值进行。后，优化器按顺序读取元组，并使用元组中a，b，c和d的值来获取SELECT列表列值，而无需再次读取表t1
```

对于打包的修改的filesort算法，缓冲区元组存储以下内容：

```sql
(fixed size a value, fixed size b value, 
 a length, packed a value, b length, packed b value, 
 c length, packed c value, d length, packed d value)
如果a，b，c或d中的任何一列为NULL，则除了位掩码之外，它们在缓冲区中不占用空间。优化器对固定大小元组值进行。后，优化器按顺序读取元组，并使用a，b，c和d的值来获取选择列表列值，而无需再次读取t1
```

##### • Influencing ORDER BY Optimization - 如何影响ORDER BY优化

对于未使用filesort的慢ORDER BY查询，可以尝试降低max_length_for_sort_data到一个合理的值来触发filesort。要提高ORDER BY语句的执行速度，可以检查是否可以让MySQL使直接用索引顺序而不是额外的增加工作。 如果无法做到这一点，可以尝试以下策略：

• 增加sort_buffer_size变量值。理想情况下，该值应足够大到容纳将整个结果集放入缓冲区（以避免写入磁盘和合并传递），但至少该值必须足够大以容纳十五个元组。

考虑到存储在缓冲区中的列值的大小受max_sort_length系统变量值的影响。例如，如果元组存储长字符串列的值并且我们增加了max_sort_length的值，则缓冲区元组的大小也会增加，相应的可能需要增加sort_buffer_size的值。 对于作为字符串表达式（例如调用字符串值函数的那些）计算的列值，filesort算法无法分辨表达式值的最大长度，因此它必须为每个元组分配max_sort_length个字节。

要监控the number of merge passes，可以检查Sort_merge_passes状态变量。

• 增大read_rnd_buffer_size变量值

• 将列的长度值设置在合理范围以节约存储和内存空间。例如，如果值不超过16个字符，则CHAR(16)优于CHAR(200)

• 将tmpdir系统变量更改为指向具有大量可用空间的专用文件系统。变量值可以指定多个路径的列表，MySQL服务器以轮询方式使用这几个路径；使用此方法可以将负载分散到多个目录中。 在Unix上用冒号字符(:)分隔路径，在Windows上用分号字符(;)分隔路径。路径应指定位于不同物理磁盘上的文件系统中的目录，而不是同一磁盘上的不同分区。

##### • ORDER BY Execution Plan Information Available

使用EXPLAIN SELECT ... ORDER BY，我们可以检查MySQL是否可以使用索引来解析查询。如果在Extra列中看到Using filesort，则不能使用索引。filesort使用固定长度的行存储格式，类似于MEMORY存储引擎使用固定长度存储可变长度类型(例如VARCHAR)。如果使用了了filesort，则EXPLAIN输出包括在Extra列显示using filesort，此时优化器会跟踪输出filesort_summary块，例如：

```json
"filesort_summary": {
 "rows": 100,
 "examined_rows": 100,
 "number_of_tmp_files": 0,
 "sort_buffer_size": 25192,
 "sort_mode": "<sort_key, packed_additional_fields>"
}
```

sort_mode值提供有关所使用的filesort算法和缓冲区中元组内容的信息。

• <sort_key, rowid>：使用原始filesort算法。缓冲区元组是包含原始表行的key值和行ID的对。元组按key值，row ID用于从表中读取行
• <sort_key, additional_fields>：使用修改的filesort算法。 缓冲区元组包含key值和查询引用的列。元组按key值，列值直接从元组中读取
• <sort_key, packed_additional_fields>：使用修改的filesort算法。 缓冲区元组包含查询引用的key值和打包列。元组按key值，列值直接从元组中读取

#### GROUP BY Optimization

处理GROUP BY子句的最常用方法是扫描整个表并创建一个新的临时表，其中每个GROUP中的所有行都是连续的，然后使用此临时表来查找组并应用聚合函数（如果有）。在某些情况下，MySQL能够使用索引来避免创建临时表。

GROUP BY子句命中索引的最重要的前提条件是所有GROUP BY列引用同一索引的属性，并且索引按顺序存储其key（例如，使用BTREE索引而不是HASH索引）。是否能够通过扫描索引代替临时表的使用还取决于查询中使用索引的哪些部分，为这些部分指定的条件以及所选的聚合函数。

有两种方法可以通过扫描索引来执行GROUP BY查询。 在第一种方法中，分组操作与所有范围条件（如果有的话）一起应用。 第二种方法首先执行范围扫描，然后对生成的元组进行分组。

在MySQL中，GROUP BY用到，因此MySQL服务也可以将ORDER BY的优化手段应用于GROUP BY。

##### Loose Index Scan

处理GROUP BY的最有效方法是使用索引直接检索分组列。在这种方法中，MySQL利用了在一些索引类型中，索引key是有序存储的这一特性（例如，BTREE）。此特性允许在索引中使用查找组，而无需考虑索引中满足所有WHERE条件的所有键。由于此访问方法仅需要考虑索引中的一小部分键，因此称为松散索引扫描。当没有WHERE子句时，松散索引扫描读取的索引key与GROUP BY用到的key数量一样多，这可能比索引的全部key的数量小得多。如果WHERE子句包含范围条件，则松散索引扫描会查找满足范围条件的每个组的第一个key，并继续读取尽可能少的key。上述情况需要满足以下条件：

• 单表查询

• GROUP BY仅使用构成索引的最左前缀而不包含其他列的列。(如果查询使用DISTINCT子句而不是GROUP BY，则所有DISTINCT引用列都用于构成索引最左前缀)例如，如果表t1在（c1，c2，c3）列上有索引，则如果查询使用GROUP BY c1，c2，则松散索引扫描适用。如果查询使用GROUP BY c2，c3（列不是最左前缀）或GROUP BY c1，c2，c4（c4不在索引中），则不适用

• SELECT列表中如果用到了聚合函数，唯一支持的是MIN()和MAX()，并且它们都引用同一列。该列必须在索引中，并且必须紧跟GROUP BY

• 查询中引用的索引列，除了MIN()或MAX()函数的参数之外任何其他部分必须是常量(即，它们必须以与常量相等的方式引用)

• 对于索引中的列，必须索引完整列值，而不仅仅是前缀。 例如，对于c1 VARCHAR(20)，INDEX(c1(10))，索引不能用于松散索引扫描

如果查询适用松散索引扫描，则EXPLAIN输出在Extra列中显示Using group for groupby。假设在表t1(c1，c2，c3，c4)上存在索引idx(c1，c2，c3)。 松散索引扫描访问方法可用于以下查询：

```sql
SELECT c1, c2 FROM t1 GROUP BY c1, c2;
SELECT DISTINCT c1, c2 FROM t1;
SELECT c1, MIN(c2) FROM t1 GROUP BY c1;
SELECT c1, c2 FROM t1 WHERE c1 < const GROUP BY c1, c2;
SELECT MAX(c3), MIN(c3), c1, c2 FROM t1 WHERE c2 > const GROUP BY c1, c2;
SELECT c2 FROM t1 WHERE c1 < const GROUP BY c1, c2;
SELECT c1, c2 FROM t1 WHERE c3 = const GROUP BY c1, c2;
```

此快速选择方法无法应用于以下查询：

```sql
-- 使用到了MIN()和MAX()之外的聚类函数：
SELECT c1, SUM(c2) FROM t1 GROUP BY c1;
-- GROUP BY子句中的列不构成索引的最左前缀：
SELECT c1, c2 FROM t1 GROUP BY c2, c3;
-- 查询引用了GROUP BY部分用到的索引key之后其他索引key，并且不满足与常量相等的条件:
SELECT c1, c3 FROM t1 GROUP BY c1, c2;
-- 若果上述查询加上WHERE c3 = const条件，松散索引扫描就是可用的
```

除了已经支持的MIN()和MAX()引用之外，松散索引扫描可以应用于SELECT列表中的其他形式的聚合函数引用：

• 支持AVG(DISTINCT)，SUM(DISTINCT)和COUNT(DISTINCT)。 AVG(DISTINCT)和SUM(DISTINCT)只有一个参数。COUNT(DISTINCT)可以有多个列参数

• 查询中必须没有GROUP BY或DISTINCT子句。

• 前面描述的松散扫描限制仍然适用

```sql
-- 上述表的例子中，如下语句也可以使用松散索引扫描
SELECT COUNT(DISTINCT c1), SUM(DISTINCT c1) FROM t1;
SELECT COUNT(DISTINCT c1, c2), COUNT(DISTINCT c2, c1) FROM t1;
```

##### Tight Index Scan

紧密索引扫描可以是完整索引扫描或范围索引扫描，具体取决于查询条件。当不满足松散索引扫描的条件时，仍可以避免为GROUP BY查询创建临时表。如果WHERE子句中存在范围条件，则此方法仅读取满足这些条件的键。 否则，它执行索引扫描。因为此方法读取WHERE子句定义的每个范围中的所有key，或者如果没有范围条件则扫描整个索引，我们将其称为紧密索引扫描。 使用紧密索引扫描时，仅在查找到满足范围条件的所有key之后才执行分组操作。

For this method to work, it is sufficient that there is a constant equality condition for all columns in
a query referring to parts of the key coming before or in between parts of the GROUP BY key。来自等式条件的常量填充搜索关键字中的任何"gap"，以便可以形成索引的完整前缀。然后，这些索引前缀可用于索引查找。如果我们需要对GROUP BY结果进行，并且可以形成作为索引前缀的搜索键，MySQL还可以避免额外的操作，因为在有序索引中使用前缀进行搜索已经按顺序检索了所有键。

假设在表t1(c1，c2，c3，c4)上存在索引idx(c1，c2，c3)。 以下查询不适用于前面描述的松散索引扫描访问方法，但仍可使用紧密索引扫描访问方法。

```sql
-- GROUP BY中存在间隙，但它被条件c2 ='a'填充
SELECT c1, c2, c3 FROM t1 WHERE c2 = 'a' GROUP BY c1, c3;
-- GROUP BY不以索引的第一部分开头，但有一个条件为该部分提供常量条件
SELECT c1, c2, c3 FROM t1 WHERE c1 = 'a' GROUP BY c2, c3;
```

#### DISTINCT Optimization

在很多情况下，DISTINCT与ORDER BY组合的查询需要一个临时表。

在大多数情况下，DISTINCT子句可以视为GROUP BY的特例。 例如，以下两个查询是等效的：

```sql
SELECT DISTINCT c1, c2, c3 FROM t1 WHERE c1 > const;
SELECT c1, c2, c3 FROM t1 WHERE c1 > const GROUP BY c1, c2, c3;
```

由于这种等效性，适用于GROUP BY查询的优化也可以应用于具有DISTINCT子句的查询。

当LIMIT row_count与DISTINCT组合时，MySQL会在找到row_count数量的唯一行后立即停止。如果SELECT列表只用到查询中指定的所有表中的部分列，MySQL会在找到第一个匹配项后立即停止扫描所有未使用的表。 在下面的例子中，假设在扫描t2表之前扫描了t1表（可以用EXPLAIN检查），对于t1中的任何特定行，当MySQL找到t2中满足连接条件的第一行数据时，MySQL会停止从t2继续读取：

```sql
SELECT DISTINCT t1.a FROM t1, t2 where t1.a=t2.a;
```

LIMIT Query Optimization

#### LIMIT Query Optimization

如果只需要结果集中指定数量的行，请在查询中使用LIMIT子句，而不是获取整个结果集并丢弃额外数据。MySQL有时会优化具有LIMIT row_count子句且没有HAVING子句的查询：

• 如果我们使用LIMIT选择几行数据，MySQL在某些情况下会使用索引，而通常情况下它更喜欢进行全表扫描

• 如果将LIMIT row_count与ORDER BY结合使用，MySQL会在找到结果的前row_count行后立即停止，而不是对整个结果进行。如果使用索引，则速度非常快。如果必须使用filesort，在找到前row_count之前，会读取与没有LIMIT子句的查询匹配的所有行，并对其中的大部分或全部进行。在找到LIMIT条件指定的行数据之后，MySQL不会对结果集的任何剩余部分进行。此方法的一种表现形式是，带有和不带LIMIT的ORDER BY查询可能会以不同的顺序返回行，如本节后面所述。

• 如果将LIMIT row_count与DISTINCT结合使用，MySQL会在找到row_count数量的唯一行后立即停止

• 在某些情况下，GROUP BY的解析顺序是这样的：通过按顺序读取索引（或对索引进行），然后计算摘要直到索引值改变。在这种情况下，LIMIT row_count不会计算任何不必要的GROUP BY值

• 一旦MySQL向客户端发送了所需的行数数量的行数据，它就会中止查询，除非使用了SQL_CALC_FOUND_ROWS。 在这种情况下，可以使用SELECT FOUND_ROWS()检索行数

• LIMIT 0快速返回空集，这对于检查查询语句的有效性非常有用。它还可用于在使用了MySQL API的应用程序中获取结果列类型和结果集元数据。使用mysql客户端程序，可以使用--column-type-info选项显示结果列类型

• 如果服务器使用临时表来解析查询，则它使用LIMIT row_count子句来计算需要多少空间

• 如果ORDER BY字句无法使用索引，但是存在LIMIT子句，则优化器可能使用内存中的filesort操作对内存中的行进行而够避免使用合并文件

如果ORDER BY列中的多个行具有相同的值，则服务器可以按任何顺序自由返回这些行，并且可能会根据整体执行计划的不同而不同。 换句话说，这些行的顺序相对于无序列是不确定的。

影响执行计划的一个因素是LIMIT，因此使用和不使用LIMIT的ORDER BY查询可能会返回不同顺序的行。
我们来看这一查询，该查询按category列，但对id和rating列不确定：

```sql
SELECT * FROM ratings ORDER BY category;
+----+----------+--------+
| id | category | rating |
+----+----------+--------+
| 1  | 1        | 4.5    |
| 5  | 1        | 3.2    |
| 3  | 2        | 3.7    |
| 4  | 2 	    | 3.5    |
| 6  | 2        | 3.5    |
| 2  | 3        | 5.0    |
| 7  | 3        | 2.7    |
+----+----------+--------+
```

增加LIMIT子句可能会影响每个category值中的行顺序。 例如，这是一个有效的查询结果集：

```sql
SELECT * FROM ratings ORDER BY category LIMIT 5;
+----+----------+--------+
| id | category | rating |
+----+----------+--------+
| 1  | 1        | 4.5    |
| 5  | 1        | 3.2    |
| 4  | 2        | 3.5    |
| 3  | 2        | 3.7    |
| 6  | 2        | 3.5    |
+----+----------+--------+
```

在上述两种情况下，行数据都是按ORDER BY列的，这完全符合SQL标准。如果需要确保使用和不使用LIMIT子句得到的结果顺序保持一样，请在ORDER BY子句中添加其他列以使顺序具有确定性。例如，如果id值是唯一的，则可以通过如下使得给定category值的行以id顺序显示：

```sql
SELECT * FROM ratings ORDER BY category, id;
+----+----------+--------+
| id | category | rating |
+----+----------+--------+
| 1  | 1        | 4.5    |
| 5  | 1        | 3.2    |
| 3  | 2        | 3.7    |
| 4  | 2        | 3.5    |
| 6  | 2        | 3.5    |
| 2  | 3        | 5.0    |
| 7  | 3        | 2.7    |
+----+----------+--------+
SELECT * FROM ratings ORDER BY category, id LIMIT 5;
+----+----------+--------+
| id | category | rating |
+----+----------+--------+
| 1  | 1        | 4.5    |
| 5  | 1        | 3.2    |
| 3  | 2        | 3.7    |
| 4  | 2        | 3.5    |
| 6  | 2        | 3.5    |
+----+----------+--------+
```

Function Call Optimization

#### Function Call Optimization

MySQL函数在内部分为确定性函数和非确定性函数。如果给定参数的固定值，函数可以为不同的调用返回不同的结果，那么函数是非确定性的。非确定性函数的示例：RAND()，UUID()。对于非确定性函数，WHERE子句中的每行（从一个表中选择时）或每一种行组合（从多表连接中选择时）都会对函数求值。

MySQL还根据参数类型是表的列值还是常量值确定何时对函数求值。对于将表列作为参数的确定性函数，只要该列更改值，就必须重新求值。

非确定性函数可能会影响查询性能，例如，某些优化可能不可用，或者可能需要更多锁定。以下例子讨论使用RAND()，但也适用于其他非确定性函数：

假设表t的定义语句为：

```sql
CREATE TABLE t (id INT NOT NULL PRIMARY KEY, col_a VARCHAR(100));
```

考虑如下两个查询：

```sql
SELECT * FROM t WHERE id = POW(1,2);
SELECT * FROM t WHERE id = FLOOR(1 + RAND() * 49);
```

由于WHERE子句中使用与主键的相等性比较条件，两个查询似乎都使用主键查找，但仅对第一个查询是正确的。第一个查询会得到最多一行结果，因为带有常量参数的POW()是一个常量值，使用此值查找索引。第二个查询包含一个使用非确定性函数RAND()的表达式，该函数在查询中不是常量，但实际上对于表t的每一行都有一个新值。因此，查询读取表的每一行，并输出主键与函数产生的随机值匹配的所有行。可能是零行，一行或多行，具体取决于id列值和RAND()函数得到的值。

非确定性函数的影响不仅限于SELECT语句。 此UPDATE语句使用非确定性函数来选择要修改的行：

```sql
UPDATE t SET col_a = some_expr WHERE id = FLOOR(1 + RAND() * 49);
```

我们可以推测这个语句的本意是最多更新主键与表达式匹配的单个行。但是，它可能会更新零行，一行或多行，具体取决于id列值和RAND()序列中的值。

以上描述的MySQL行为对性能和复制有影响：

• 由于非确定性函数不会生成常量值，因此优化程序无法选择可能适用的策略，例如索引。结果可能是表扫描

• InnoDB可能会升级到范围锁，而不是对一个匹配的行进行单行锁定

• 不确定的更新对于复制是不安全的

上述困难源于RAND()函数对表的每一行进行一次求值。要避免函数的多次求值，可以使用以下技术：

• 将包含非确定性函数的表达式移动到单独的语句中，将值保存在变量中。在原始语句中，将表达式替换为对变量的引用，此时优化程序可将其视为常量值

```sql
SET @keyval = FLOOR(1 + RAND() * 49);
UPDATE t SET col_a = some_expr WHERE id = @keyval;
```

• 将随机值存储在派生表中的变量里。在WHERE子句条件解析之前，此技术会对变量进行一次性的赋值

```sql
SET optimizer_switch = 'derived_merge=off';
UPDATE t, (SELECT @keyval := FLOOR(1 + RAND() * 49)) AS dt
SET col_a = some_expr WHERE id = @keyval;
```

如果优化器可以使用partial_key来减少所选的结果集的行数，则RAND()执行的次数会减少，这会减少非确定性函数对优化的影响。

#### Row Constructor Expression Optimization

行构造函数允许同时比较多个值。 例如，这两个语句在语义上是等价的，优化器以相同的方式处理两个表达式：

```sql
SELECT * FROM t1 WHERE (column1,column2) = (1,1);
SELECT * FROM t1 WHERE column1 = 1 AND column2 = 1;
```

如果行构造函数列未覆盖索引的前缀，则优化程序不太可能使用可用索引。 例如下表，其中有一个主键（c1，c2，c3）：

```sql
CREATE TABLE t1 (
 c1 INT, c2 INT, c3 INT, c4 CHAR(100),
 PRIMARY KEY(c1,c2,c3)
);
```

在此查询中，WHERE子句使用到了索引中的所有列。 但是，行构造函数本身没有覆盖索引前缀，结果是优化程序仅使用c1索引数据（key_len = 4，c1的大小）：

```sql
EXPLAIN SELECT * FROM t1 WHERE c1=1 AND (c2,c3) > (1,1)\G;
*************************** 1. row ***************************
           id: 1
  select_type: SIMPLE
        table: t1
   partitions: NULL
         type: ref
possible_keys: PRIMARY
          key: PRIMARY
      key_len: 4
          ref: const
         rows: 1
     filtered: 100.00
        Extra: Using where
```

在这种情况下，使用等效的非构造函数表达式重写行构造函数表达式可能会得到更完整的索引使用。对于上述查询，行构造函数和等效的非构造函数表达式是：

```sql
(c2,c3) > (1,1)
c2 > 1 OR ((c2 = 1) AND (c3 > 1))
```

使用上述方法重写查询以使用非构造函数表达式会让优化程序能够用到索引中的所有三列 (key_len=12)：

```sql
EXPLAIN SELECT * FROM t1 WHERE c1 = 1 AND (c2 > 1 OR ((c2 = 1) AND (c3 > 1)))\G;
*************************** 1. row ***************************
           id: 1
  select_type: SIMPLE
        table: t1
   partitions: NULL
         type: range
possible_keys: PRIMARY
          key: PRIMARY
      key_len: 12
          ref: NULL
         rows: 2
     filtered: 100.00
        Extra: Using where
```

因此，为了获得更好的结果，请避免将行构造函数与AND/OR表达式混合。使用其中一个。在某些情况下，优化器可以将范围访问方法应用于具有行构造函数参数的IN()表达式。

#### Avoiding Full Table Scans

当MySQL使用全表扫描来解析查询时，EXPLAIN的输出在type列中显示ALL。这通常为以下条件：

• 该表非常小，执行索引查找比全表扫描更麻烦。这对于行少于10行且行长度较短的表来说很常见。

• ON或WHERE子句中的条件在索引列中没有可用的查询条件

• WHERE条件将索引列与常量值进行比较，并且MySQL已经计算出（基于索引树）常量覆盖了表的很大一部分，此时全表扫描会更快

• SQL中使用的其他列条件分辨度很低（许多行与键值匹配）。在这种情况下，MySQL假设通过使用索引会需要执行许多key查找，此时表扫描会更快

对于小表，表扫描通常是合理的，性能影响可以忽略不计。对于大表，请尝试以下技术以避免优化程序错误地选择表扫描：

• 使用ANALYZE TABLE tbl_name更新扫描表的索引存出结构 

• 使用FORCE INDEX扫描表告诉MySQL，与使用给定索引相比，表扫描非常昂贵

```sql
SELECT * FROM t1, t2 FORCE INDEX (index_for_column) WHERE t1.col_name=t2.col_name;
```

• 使用**--max-seeking-for-key = 1000**选项启动mysqld或使用**SET max_seeks_for_key = 1000**告诉优化器，索引扫描最多1,000个 

### Optimizing Subqueries, Derived Tables, and View References - 优化子查询，派生表和视图引用

MySQL查询优化器有不同的策略可用于解析子查询。

对于IN（或 = ANY）子查询，优化程序有这些选项：

• 半连接

• 实体化

• EXISTS策略

对于NOT IN（或<> ALL）子查询，优化器有这些选择：

• 实体化

• EXISTS策略

对于派生表（FROM子句中的子查询）和视图引用，优化程序具有这些选项：

• 将派生表合并到外部查询块中

• 将派生表实现为内部临时表

#### Optimizing Subqueries, Derived Tables, and View References with Semi-Join Transformations

优化器使用半连接(https://www.linuxidc.com/Linux/2015-05/117523.htm)策略来改进子查询执行。

对于两个表之间的内连接，对应一个表中的每一条匹配数据，连接从另一个表返回一行。但对于一些问题，我们唯一关心的是有没有匹配连接条件的数据，而不是匹配数据的数量。 假设有一个名为class和roster的表分别列出每个班级的课程表和每个班级注册的学生。下列实验需要的表结构和数据如下：

```sql
create table class(
    class_num bigint(10) unsigned NOT NULL PRIMARY KEY,
    class_name varchar(10) 
)engine=InnoDB default charset utf8;

insert into class(class_num, class_name) 
values(1, "classname1"), (2, "classname2");

create table roster(
    roster_num bigint(10) unsigned NOT NULL,
    class_num bigint(10) unsigned NOT NULL,
    roster_name varchar(10)
)engine=InnoDB default charset utf8;

insert into roster(roster_num, class_num, roster_name)
values(1, 1, "rostname1"), (1, 2, "rostname1"), (2, 2, "rostname2");
```

要查询实际注册学生的班级，可以使用这样的连接：

```sql
mysql> SELECT class.class_num, class.class_name
    -> FROM class INNER JOIN roster
    -> WHERE class.class_num = roster.class_num;
+-----------+------------+
| class_num | class_name |
+-----------+------------+
|         1 | classname1 |
|         2 | classname2 |
|         2 | classname2 |
+-----------+------------+
```

但是，对应roster表每个注册的学生，class表会列出班级数据一次。 对于上述查询，这是不必要的重复信息。假设class_num是class表中的主键，可以使用SELECT DISTINCT进行去重，但是还是需要先查询到所有匹配的行，再对结果集进行去重，效率很低。

```sql
mysql> SELECT distinct(class.class_num), class.class_name
    -> FROM class INNER JOIN roster
    -> WHERE class.class_num = roster.class_num;
+-----------+------------+
| class_num | class_name |
+-----------+------------+
|         1 | classname1 |
|         2 | classname2 |
+-----------+------------+
```

使用子查询可以获得相同的去重效果：

```sql
mysql> SELECT class_num, class_name
    -> FROM class
    -> WHERE class_num IN (SELECT class_num FROM roster);
+-----------+------------+
| class_num | class_name |
+-----------+------------+
|         1 | classname1 |
|         2 | classname2 |
+-----------+------------+
```

在这里，优化器可以识别IN子句要求子查询只从roster表中返回每个class_num的一个实例。在这种情况下，查询可以使用半连接；也就是说，一个操作只返回class中每行的一个实例，该实例与roster中的行匹配。

上述三个语句的EXPLAIN结果如下：

```sql
mysql> explain SELECT class.class_num, class.class_name
    -> FROM class INNER JOIN roster
    -> WHERE class.class_num = roster.class_num\G;
*************************** 1. row ***************************
           id: 1
  select_type: SIMPLE
        table: class
   partitions: NULL
         type: ALL
possible_keys: PRIMARY
          key: NULL
      key_len: NULL
          ref: NULL
         rows: 2
     filtered: 100.00
        Extra: NULL
*************************** 2. row ***************************
           id: 1
  select_type: SIMPLE
        table: roster
   partitions: NULL
         type: ALL
possible_keys: NULL
          key: NULL
      key_len: NULL
          ref: NULL
         rows: 3
     filtered: 33.33
        Extra: Using where; Using join buffer (Block Nested Loop)

mysql> explain SELECT distinct(class.class_num), class.class_name
    -> FROM class INNER JOIN roster
    -> WHERE class.class_num = roster.class_num\G;
*************************** 1. row ***************************
           id: 1
  select_type: SIMPLE
        table: class
   partitions: NULL
         type: ALL
possible_keys: PRIMARY
          key: NULL
      key_len: NULL
          ref: NULL
         rows: 2
     filtered: 100.00
        Extra: Using temporary
*************************** 2. row ***************************
           id: 1
  select_type: SIMPLE
        table: roster
   partitions: NULL
         type: ALL
possible_keys: NULL
          key: NULL
      key_len: NULL
          ref: NULL
         rows: 3
     filtered: 33.33
        Extra: Using where; Distinct; Using join buffer (Block Nested Loop)

mysql> explain SELECT class_num, class_name
    -> FROM class
    -> WHERE class_num IN (SELECT class_num FROM roster)\G;
*************************** 1. row ***************************
           id: 1
  select_type: SIMPLE
        table: class
   partitions: NULL
         type: ALL
possible_keys: PRIMARY
          key: NULL
      key_len: NULL
          ref: NULL
         rows: 2
     filtered: 100.00
        Extra: NULL
*************************** 2. row ***************************
           id: 1
  select_type: SIMPLE
        table: roster
   partitions: NULL
         type: ALL
possible_keys: NULL
          key: NULL
      key_len: NULL
          ref: NULL
         rows: 3
     filtered: 33.33
        Extra: Using where; FirstMatch(class); Using join buffer (Block Nested Loop)
```

第二个语句仅仅是在第一个语句的执行计划基础上增加了distinct去重，性能上并没有任何改善，反而增加了distinct的成本，第三个语句用到了class表的FirstMatch，即仅返回第一条符合连接条件的数据。

SQL规范中，外部查询允许外连接和内连接语法，表引用可以是基表，派生表或视图引用。

在MySQL中，子查询必须满足这些条件才能作为半连接处理：

• 必须是出现在WHERE或ON子句顶层的IN（或= ANY）子查询，可能是AND表达式中的条件。例如：

```sql
SELECT ...
FROM ot1, ...
WHERE (oe1, ...) IN (SELECT ie1, ... FROM it1, ... WHERE ...);
```

• 必须是没有UNION结构的单个SELECT

• 它不能包含GROUP BY或HAVING子句。

• 它不能被隐式分组（必须不包含聚合函数）

• 不能含有带LIMIT的ORDER BY

• 不得在外部查询中使用STRAIGHT_JOIN连接类型

• 不得出现STRAIGHT_JOIN修饰符

• 外表和内表的总数量必须小于连接中允许的最大表数

子查询可以是相关的或不相关的。 除非使用ORDER BY，否则允许使用DISTINCT和LIMIT。如果子查询符合上述条件，MySQL会将其转换为半连接，并根据这些策略进行基于成本的选择：

• 将子查询转换为连接，或使用表pullout技术将查询作为子查询表和外部表之间的内连接运行。表pullout是将表从子查询“拉出”到外部查询

• Duplicate Weedout：像处理表连接一样处理半连接，并使用临时表删除重复的记录

• FirstMatch：扫描内部表数据时，如果有多条数据满足外部表连接条件，会返回第一条而不是全部数据行。这种“快捷方式”扫描并消除了不必要的行的产生。

• LooseScan：使用索引扫描子查询表，该索引允许从每个子查询的值组中选择单个值

• 将子查询实现为带索引的临时表用于执行连接操作，其中索引用于删除重复项。临时表的索引也可能稍后在执行临时表与外部表连接时用于查找；如果无法使用临时表的索引，则全表扫描临时表

上述每一个优化策略都可以使用optimizer_switch系统变量启用或禁用：

• semijoin标志控制是否使用半连接

• 如果启用了semijoin，则firstmatch，loosescan，duplicateweedout和materialization标志可以进一步更细粒度地控制启用的半连接策略

• 如果禁用了duplicateweedout半连接策略，则除非禁用了所有其他适用策略，否则不会使用该策略

• 如果禁用了duplicateweedout，则优化程序有时可能会生成远非最佳的查询计划。This occurs due to heuristic pruning during greedy search, which can be avoided by setting optimizer_prune_level=0

优化程序最大限度地减少了对视图和派生表（FROM子句中的子查询）处理的差异。这会影响到可以转换为半连接的使用STRAIGHT_JOIN修饰符的查询和具有IN子查询的视图。以下例子说明了这一点，因为语句处理的更改会导致转换方式发生更改，从而导致执行策略不同：

```sql
CREATE VIEW v AS SELECT * FROM t1 WHERE a IN (SELECT b FROM t2);
SELECT STRAIGHT_JOIN * FROM t3 JOIN v ON t3.x = v.a;
```

优化器首先检查视图并将IN子查询转换为半连接，然后检查是否可以将视图合并到外部查询中。由于外部查询中的STRAIGHT_JOIN修饰符会阻止半连接，优化程序会拒绝合并，从而导致使用实现表进行派生表求值。

EXPLAIN输出中对使用半连接策略的说明如下：

• Semi-joined tables show up in the outer select. For extended EXPLAIN output, the text displayed by a following SHOW WARNINGS shows the rewritten query, which displays the semi-join structure. From this you can get an idea about which tables were pulled out of the semi-join. If a subquery was converted to a semi-join, you will see that the subquery predicate is gone and its tables and WHERE clause were merged into the outer query join list and WHERE clause

• Temporary table use for Duplicate Weedout is indicated by Start temporary and End temporary in the Extra column. Tables that were not pulled out and are in the range of EXPLAIN output rows covered by Start temporary and End temporary have their rowid in the temporary table

• FirstMatch(tbl_name) in the Extra column indicates join shortcutting

• LooseScan(m..n) in the Extra column indicates use of the LooseScan strategy. m and n are key
part numbers

• Temporary table use for materialization is indicated by rows with a select_type value of
MATERIALIZED and rows with a table value of

Optimizing Subqueries with Materialization

#### Optimizing Subqueries with Materialization

优化程序使用实体化来实现更有效的子查询处理。实体化通过生成子查询结果作为临时表（通常在内存中）来加速查询执行。MySQL第一次需要子查询结果时，会将结果实体化为临时表。在任何后续需要结果的时候，MySQL再次引用临时表。优化器可以使用哈希索引对表进行索引，以使查找快速且廉价。索引是唯一的，它消除了重复并使表更小。

子查询表的实体化在可能的情况下使用内存中的临时表，如果表变得太大，则降级存储到磁盘上。

如果未使用实体化，则优化器有时会将非相关子查询重写为相关子查询。例如，以下IN子查询是不相关的（where_condition仅涉及来自t2而不是t1的列），优化器可能会将其重写为使用EXISTS的相关子查询：

```sql
SELECT * FROM t1
WHERE t1.a IN (SELECT t2.b FROM t2 WHERE where_condition);

SELECT * FROM t1
WHERE EXISTS (SELECT t2.b FROM t2 WHERE where_condition AND t1.a=t2.b);
```

Subquery materialization using a temporary table avoids such rewrites and makes it possible to execute the subquery only once rather than once per row of the outer query.

要在MySQL中使用的子查询实体化，必须启用optimizer_switch系统变量的materialization标志。启用materialization标志后，实体化将在满足下述情况的出现在任何位置（在选择列表中，WHERE，ON，GROUP BY，HAVING或ORDER BY）的子查询条件生效：

• 当条件具有这种形式，并且外部表达式oe_i或内部表达式ie_i都不能为空时(N >=1)

```sql
(oe_1, oe_2, ..., oe_N) [NOT] IN (SELECT ie_1, i_2, ..., ie_N ...)
```

• 当条件具有这种形式，并且只存在单个外部表达式oe和内部表达式ie时。此时表达式列可以为空

```sql
oe [NOT] IN (SELECT ie ...)
```

• 谓词是IN或NOT IN，UNKNOWN（NULL）的结果与FALSE的结果具有相同的含义时

以下示例说明了对UNKNOWN和FALSE相等条件求值时如何影响是否可以使用实体化处理子查询。假设where_condition仅涉及来自t2而不是t1的列，因此子查询是不相关的。

此查询取决于实体化

```sql
SELECT * FROM t1
WHERE t1.a IN (SELECT t2.b FROM t2 WHERE where_condition);
```

这里，IN谓词是返回UNKNOWN还是FALSE并不重要。因为无论返回何值，来自t1的行都不包括在查询结果中。

以下查询是不使用实体化子查询结果的示例，其中t2.b是可空列：

```sql
SELECT * FROM t1
WHERE (t1.a, t1.b) NOT IN (SELECT t2.a,t2.b FROM t2 WHERE where_condition);
```

以下限制适用于实体化子查询结果这一优化方式的使用：

• 子查询和外部查询表达式的类型必须匹配。例如，如果两个表达式都是integer或者decimal，则优化器可能能够使用实体化，但如果一个表达式是integer而另一个是decimal则不能

• 子查询表达式不能是BLOB类型

EXPLAIN结果会通过以下方面说明指示优化程序是否使用实体化子查询。与不使用实体化的子查询相比，select_type可能会从DEPENDENT SUBQUERY更改为SUBQUERY。这表明，对于每个外部查询的行将执行一次的子查询，使用子查询实体化只需执行一次。此外，对于扩展的EXPLAIN输出，SHOW WARNINGS显示的内容会包括materialize和materialized-subquery。

#### Optimizing Derived Tables and View References

优化器可以使用两种策略处理派生表（FROM子句中的子查询）引用，对于视图引用也采用相同的处理策略：

• Merge the derived table into the outer query block - 将衍生表合并到外部查询

• Materialize the derived table to an internal temporary table - 将衍生表实体化为内部的临时表

```sql
-- 例1:
SELECT * FROM (SELECT * FROM t1) AS derived_t1;
-- 将衍生表合并到外部查询之后类似于
SELECT * FROM t1;

-- 例2:
SELECT * FROM t1 
JOIN (SELECT t2.f1 FROM t2) AS derived_t2 ON t1.f2=derived_t2.f1
WHERE t1.f1 > 0;
-- 将衍生表合并到外部查询之后类似于
SELECT t1.*, t2.f1 FROM t1 
JOIN t2 ON t1.f2=t2.f1
WHERE t1.f1 > 0;
```

优化器以相同的方式处理派生表和视图引用：它尽可能避免不必要的实体化，这样可以将条件从外部查询推送到派生表，并生成更高效的执行计划。如果将子查询合并到外部查询会导致外部查询块引用的表总数超过61个，则优化程序会选择实体化。

如果这些条件都为真，则优化程序将派生表或视图引用中的ORDER BY子句传播到外部查询块，否则优化器会忽略ORDER BY条件：

• The outer query is not grouped or aggregated.
• The outer query does not specify DISTINCT, HAVING, or ORDER BY.
• The outer query has this derived table or view reference as the only source in the FROM clause.

以下方法可用于影响优化程序是否尝试将派生表和视图引用合并到外部查询块中：

• The derived_merge flag of the optimizer_switch system variable can be used, assuming that no other rule prevents merging. By default, the flag is enabled to permit merging. Disabling the flag prevents merging and avoids ER_UPDATE_TABLE_USED errors.

The derived_merge flag also applies to views that contain no ALGORITHM clause. Thus, if an ER_UPDATE_TABLE_USED error occurs for a view reference that uses an expression equivalent to the subquery, adding ALGORITHM=TEMPTABLE to the view definition prevents merging and takes precedence over the derived_merge value.

• It is possible to disable merging by using in the subquery any constructs that prevent merging, although these are not as explicit in their effect on materialization. Constructs that prevent merging are the same for derived tables and view references:

```
• Aggregate functions (SUM(), MIN(), MAX(), COUNT(), and so forth)

• DISTINCT

• GROUP BY

• HAVING

• LIMIT

• UNION or UNION ALL

• Subqueries in the select list

• Assignments to user variables

• Refererences only to literal values (in this case, there is no underlying table)

```

如果优化器选择实体化策略而不是合并派生表到外部查询，它将按如下方式处理查询：

• The optimizer postpones derived table materialization until its contents are needed during query
execution. This improves performance because delaying materialization may result in not having to
do it at all. Consider a query that joins the result of a derived table to another table: If the optimizer
processes that other table first and finds that it returns no rows, the join need not be carried out
further and the optimizer can completely skip materializing the derived table.

• During query execution, the optimizer may add an index to a derived table to speed up row retrieval
from it.

我们来看以下EXPLAIN语句，子查询出现在SELECT查询的FROM子句中：

```sql
EXPLAIN SELECT * FROM (SELECT * FROM t1) AS derived_t1;
```

优化程序会将子查询的实体化推迟到SELECT执行期间，并且仅当SELECT列表中需要子查询结果时才会真正执行实体化。在上述例子中，子查询并不会执行（因为它出现在EXPLAIN语句中）。即使对于需要被执行的子查询，实体化的延迟也可能使优化器完全避免实体化子查询。在以下查询语句中，该查询将FROM子句中子查询的结果连接到另一个表：

```sql
SELECT * FROM t1 
JOIN (SELECT t2.f1 FROM t2) AS derived_t2 ON t1.f2=derived_t2.f1
WHERE t1.f1 > 0;
```

如果优化器首先处理t1并且WHERE子句产生空结果集，则连接必须为空并且子查询不需要实体化。

对于派生表确实需要实体化的情况，优化器可以向实现表添加索引以加速对其的访问。如果索引允许对表进行ref访问，则可以大大减少查询执行期间读取的数据量。比如以下查询：

```sql
SELECT * FROM t1 
JOIN (SELECT DISTINCT f1 FROM t2) AS derived_t2 ON t1.f1=derived_t2.f1;
```

优化器会在derived_t2.f1列上建立索引，如果索引允许对派生表的ref访问并且可以获得代价最低的执行计划。添加索引后，优化程序可以将实体化的派生表视为与具有索引的常规表相同，并且同样可以从建立的索引上获得优化空间。与在没有索引的派生表上执行查询的成本相比，索引创建的开销可以忽略不计。如果ref访问会导致比其他访问方法更高的成本，则优化器不会创建任何索引，这也不会损失任何性能。对于优化器的trace输出，合并的派生表或视图引用不会显示为节点。只有其基础表出现在执行计划的顶部。

#### Optimizing Subqueries with the EXISTS Strategy

对于下述子查询的比较条件：

```sql
outer_expr IN (SELECT inner_expr FROM ... WHERE subquery_where)
```

MySQL按照“from outside to inside”的顺序解析查询。也就是说，它首先获取外部表达式outer_expr的值，然后运行子查询并捕获它生成的行。一个非常有用的优化是“通知”子查询，我们唯一感兴趣的行是内部表达式inner_expr等于outer_expr的行。这是通过将适当的相等条件下推到子查询的WHERE子句中以使其更具限制性来实现的。所以上述插叙条件可以转化为：

```sql
EXISTS (SELECT 1 FROM ... WHERE subquery_where AND outer_expr=inner_expr)
```

转换后，MySQL可以使用下推的相等条件来限制它必须求值的子查询的行数。推广到一般，对外部查询的N个值，子查询返回N条数据的情况也适用于这样的转换。

```sql
-- If oe_i and ie_i represent corresponding outer and inner expression values, this subquery comparison:
(oe_1, ..., oe_N) IN (SELECT ie_1, ..., ie_N FROM ... WHERE subquery_where)
---->
EXISTS (SELECT 1 FROM ... WHERE subquery_where
AND oe_1 = ie_1
AND ...
AND oe_N = ie_N)
```

刚刚描述的转换有其局限性。仅当我们可以忽略连接条件对应列可能的NULL值时它才有效。也就是说，只要这两个条件都成立，“下推”策略就会起作用：

• outer_expr和inner_expr列不能为NULL

• 无需区分子查询结果中的NULL与FALSE。如果子查询是WHERE子句中OR或AND表达式的一部分，则MySQL默认不需要区分。另外一种情况是

```sql
... WHERE outer_expr IN (subquery)
```

在这种情况下，无论子查询返回NULL还是FALSE，WHERE子句都不成立。

当这些条件中的任何一个或两个都不成立时，优化就更复杂了。

假设已知outer_expr列不允许NULL值，但子查询不存在满足outer_expr = inner_expr的行。那么outer_expr IN（SELECT ...）的执行结果如下：

• NULL, if the SELECT produces any row where inner_expr is NULL
• FALSE, if the SELECT produces only non-NULL values or produces nothing

在这种情况下，使用outer_expr = inner_expr查找行的方法不再完全有效，我们还需要查找inner_expr为NULL的行。粗略地说，子查询可以转换为这样的东西：

```sql
EXISTS (SELECT 1 FROM ... WHERE subquery_where AND 
        (outer_expr=inner_expr OR inner_expr IS NULL))
```

需要对额外的IS NULL条件做处理是MySQL具有ref_or_null访问方法的原因。unique_subquery和index_subquery特定于子查询的访问方法也具有类似上述“or NULL”情况的变体。额外的OR ... IS NULL条件使得查询执行稍微复杂一些（并且子查询中的一些优化变得不适用），但通常这是可以容忍的。

当outer_expr可以为NULL时，情况会更糟。 根据SQL规范中将NULL解释为“未知值”，NULL IN(SELECT inner_expr ...)应该这样求值

• NULL, if the SELECT produces any rows
• FALSE, if the SELECT produces no rows

为了正确求值，有必要能够检查SELECT子查询是否能够返回数据，因此无法将outer_expr = inner_expr下推到子查询中。这是一个问题，许多实际使用的子查询在“下推”不适用的情况下会变得非常慢。所以在实质上，必须有不同的方法来根据outer_expr的值确定处理子查询的方式。

优化器选择遵守SQL规范而不是执行速度，因此它考虑了outer_expr可能为NULL的可能性：

• 如果outer_expr为NULL，则要计算以下表达式，必须先执行SELECT以确定它是否能返回任何行：

```sql
NULL IN (SELECT inner_expr FROM ... WHERE subquery_where)
```

在这种情况下有必要执行原始SELECT，并且前面提到的任何下推条件均不满足。

• 另一方面，当outer_expr不为NULL时，这种比较绝对必要:

```sql
outer_expr IN (SELECT inner_expr FROM ... WHERE subquery_where)
```

转换为使用下推条件的表达式(如果没有这种转换，子查询将会很慢)

```sql
EXISTS (SELECT 1 FROM ... WHERE subquery_where AND outer_expr=inner_expr)
```

为了解决是否能够将条件下推子查询的困境，条件被包装在“trigger”函数中。因此有下述的转换：

```sql
outer_expr IN (SELECT inner_expr FROM ... WHERE subquery_where)
---->
EXISTS (SELECT 1 FROM ... WHERE subquery_where AND trigcond(outer_expr=inner_expr))
```

更一般地，如果子查询比较基于几对外部和内部表达式，则转换具体是这样的：

```sql
(oe_1, ..., oe_N) IN (SELECT ie_1, ..., ie_N FROM ... WHERE subquery_where)
---->
EXISTS (SELECT 1 FROM ... WHERE subquery_where
AND trigcond(oe_1=ie_1)
AND ...
AND trigcond(oe_N=ie_N))
```

每个trigcond（X）是一个特殊函数，其值为以下值：

• X when the “linked” outer expression oe_i is not NULL
• TRUE when the “linked” outer expression oe_i is NULL

包含在trigcond()函数中的等式不是查询优化器考虑的一级条件，他们假设任何trigcond(X)是未知函数并先忽略它们。在这些情况下优化器才可以使用trigcond()函数：

• 引用优化：trigcond(X = Y[OR Y IS NULL])可用于构造ref，eq_ref或ref_or_null表访问方法

• 基于索引查找的子查询执行引擎：trigcond(X=Y)可用于构造unique_subquery或index_subquery表访问方法

• 表条件生成器：如果子查询是多个表的连接，则会尽快检查triggered condition

当优化程序使用triggered condition创建某种基于索引查找的访问方法时，它必须具有针对triggered condition为false时的情况的回退策略。此回退策略始终相同：执行全表扫描。在EXPLAIN输出中，这种回退显示为Extra列中的Full scan on NULL key：

```sql
mysql> EXPLAIN SELECT t1.col1,
 t1.col1 IN (SELECT t2.key1 FROM t2 WHERE t2.col2=t1.col2) FROM t1\G
*************************** 1. row ***************************
 id: 1
 select_type: PRIMARY
 table: t1
 ...
*************************** 2. row ***************************
 id: 2
 select_type: DEPENDENT SUBQUERY
 table: t2
 type: index_subquery
possible_keys: key1
 key: key1
 key_len: 5
 ref: func
 rows: 2
 Extra: Using where; Full scan on NULL key

-- If you run EXPLAIN followed by SHOW WARNINGS, you can see the triggered condition:
*************************** 1. row ***************************
 Level: Note
 Code: 1003
Message: select `test`.`t1`.`col1` AS `col1`,
 <in_optimizer>(`test`.`t1`.`col1`,
 <exists>(<index_lookup>(<cache>(`test`.`t1`.`col1`) in t2
 on key1 checking NULL
 where (`test`.`t2`.`col2` = `test`.`t1`.`col2`) having
 trigcond(<is_not_null_test>(`test`.`t2`.`key1`))))) AS
 `t1.col1 IN (select t2.key1 from t2 where t2.col2=t1.col2)`
 from `test`.`t1`
```

triggered condition的使用对性能具有一些影响。使用了triggered condition之后，NULL IN(SELECT ...)表达式可能会引起全表扫描，而之前没有。这是为了得到正确结果而付出的代价（triggered condition策略的目标是符合SQL规范，而不是速度）。

对于多表子查询，执行NULL IN(SELECT ...)会特别慢，因为连接优化器不会针对外部表达式为NULL的情况进行优化。它假定表达式左侧为NULL的情况非常罕见，即使有统计信息表明不是这样。另一方面，如果外部表达式可能为NULL但实际上从不存在，则不存在性能损失。

为了查询优化器更好地执行我们的查询语句，考虑如下建议：

• 如果确实有非空约束，那么将列声明为NOT NULL。这样可以简化列的条件求值，也有助于优化器的其他方面

• 如果不需要将子查询中NULL与FALSE结果区分开来，则可以通过如下方法轻松避免缓慢的执行计划：

```sql
outer_expr IN (SELECT inner_expr FROM ...)
-- NULL IN(SELECT ...)情况永远不会被执行，因为只要能确定表达式结果，MySQL就会停止对剩余AND部分求值
(outer_expr IS NOT NULL) AND (outer_expr IN (SELECT inner_expr FROM ...))
-- 或者
EXISTS (SELECT inner_expr FROM ... WHERE inner_expr=outer_expr)
```

optimizer_switch系统变量的subquery_materialization_cost_based标志提供控制实体化子查询和IN-to-EXISTS子查询转换之间的选择。

## Special Thanks

https://blog.csdn.net/yunhua_lee/article/details/12064477