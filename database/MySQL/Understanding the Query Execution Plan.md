## Understanding the Query Execution Plan

根据表，列，索引和WHERE子句中的条件的详细信息，MySQL优化器会考虑许多技术来有效地执行SQL SELECT语句中涉及的查找。例如可以在不读取所有行的情况下执行对大表的查询；可以在不比较每个行组合的情况下执行涉及多个表的连接。优化程序选择的执行最有效查询的操作集称为“查询执行计划”，也称为EXPLAIN计划。我们的目标是识别EXPLAIN计划中表明查询已经过优化的方面，并了解SQL语法和索引技术，以便在看到执行计划在一些低效操作时改进计划。

### Optimizing Queries with EXPLAIN

EXPLAIN语句的结果提供的信息说明了MySQL如何执行语句：

• EXPLAIN适用于SELECT，DELETE，INSERT，REPLACE和UPDATE语句

• 当EXPLAIN与可解释的语句一起使用时，MySQL会显示优化器中有关语句执行计划的信息。也就是说，MySQL解释了它将如何处理语句，包括有关表如何连接以及以何种顺序连接的信息，等

• 当EXPLAIN与FOR CONNECTION connection_id一起使用时，它将显示在指定连接中执行的语句的执行计划

• 对于SELECT语句，EXPLAIN会生成额外的执行计划信息

• EXPLAIN对于检查涉及分区表的查询很有用

• FORMAT选项可用于选择结果输出格式。 TRADITIONAL(默认)以表格格式显示输出；JSON以JSON格式显示信息

在EXPLAIN的帮助下，我们可以看到应该向表中哪些列添加索引，以便通过使用索引查找行来更快地执行语句。我们还可以使用EXPLAIN来检查优化程序是否以最佳顺序连接表。要提示优化器使用与SELECT语句中命名表的顺序相对应的连接顺序，需要使用SELECT STRAIGHT_JOIN而不是仅仅是SELECT语句。但是，STRAIGHT_JOIN可能会使索引失效，因为它会禁用半连接转换。

优化器跟踪日志有时可以提供与EXPLAIN的信息互补的信息。但是，优化程序跟踪日志格式和内容可能会在不同版本之间发生变化。

如果在我们认为应该使用索引但是优化器并没有用到，这时可以运行ANALYZE TABLE以更新可能影响优化程序所做选择的表统计信息，例如索引的基数。 

>
> EXPLAIN还可用于获取有关表中列的信息。 EXPLAIN tbl_name与DESCRIBE tbl_name和SHOW COLUMNS FROM tbl_name同义。

### EXPLAIN Output Format

EXPLAIN为SELECT语句中使用到的每个表返回一行信息。在输出结果中，表的顺序是按照MySQL在处理语句时读取它们的顺序列出的。MySQL使用嵌套循环连接方法解析所有连接。这意味着MySQL从第一个表中读取一行，然后在第二个表，第三个表中找到匹配的行，依此类推。处理完所有表后，MySQL会通过表列表输出所选列和回溯，直到找到有更多匹配行的表。然后从该表中读取下一行，并继续下一个表。

EXPLAIN输出包括分区信息。此外，对于SELECT语句，EXPLAIN还会生成扩展信息，可以使用EXPLAIN后的SHOW WARNINGS查看。

> MySQL Workbench具有Visual Explain功能，可以直观地显示EXPLAIN输出。

#### **EXPLAIN Output Columns** - EXPLAIN输出结果的列

|    Column     |   JSON Name   |                    Meaning                     |
| :-----------: | :-----------: | :--------------------------------------------: |
|      id       |   select_id   |             The SELECT identifier              |
|  select_type  |     none      |                The SELECT type                 |
|     table     |  table_name   |          The table for the output row          |
|  partitions   |  partitions   |            The matching partitions             |
|     type      |  access_type  |                 The join type                  |
| possible_keys | possible_keys |         The possible indexes to choose         |
|      key      |      key      |           The index actually chosen            |
|    key_len    |  key_length   |          The length of the chosen key          |
|      ref      |      ref      |       The columns compared to the index        |
|     rows      |     rows      |        Estimate of rows to be examined         |
|   filtered    |   filtered    | Percentage of rows filtered by table condition |
|     Extra     |     None      |             Additional information             |

**• id (JSON name: select_id)**

SELECT标识符，查询中SELECT的序号。如果行引用其他行的UNION结果，则该值可以为NULL。在这种情况下，此列的值，表示该行指的是id值为M和N的行的UNION

**• select_type (JSON name: none)**

SELECT的类型，可以是下表中展示的任何类型。JSON格式的EXPLAIN将SELECT类型展示为query_block的属性，除非它是SIMPLE或PRIMARY。

| select_type Value    | JSON Name                  | Meaning                                                      |
| -------------------- | -------------------------- | ------------------------------------------------------------ |
| SIMPLE               | None                       | Simple SELECT (not using UNION or subqueries)                |
| PRIMARY              | None                       | Outermost SELECT                                             |
| UNION                | None                       | Second or later SELECT statement in a UNION                  |
| DEPENDENT UNION      | dependent (true)           | Second or later SELECT statement in a UNION, dependent on outer query |
| UNION RESULT         | union_result               | Result of a UNION                                            |
| SUBQUERY             | None                       | First SELECT in subquery                                     |
| DEPENDENT SUBQUERY   | dependent (true)           | First SELECT in subquery, dependent on outer query           |
| DERIVED              | None                       | Derived table SELECT (subquery in FROM clause)               |
| MATERIALIZED         | materialized_from_subquery | Materialized subquery                                        |
| UNCACHEABLE SUBQUERY | cacheable (false)          | A subquery for which the result cannot be cached and must be re-evaluated for each row of the
outer query |
| UNCACHEABLE UNION    | cacheable (false)          | The second or later select in a UNION that belongs to an uncacheable subquery (see
UNCACHEABLE SUBQUERY) |

DEPENDENT通常表示使用相关子查询。DEPENDENT SUBQUERY与UNCACHEABLE SUBQUERY不同。对于DEPENDENT SUBQUERY，子查询仅针对来自其外部上下文的变量的每组不同值重新求值一次。对于UNCACHEABLE SUBQUERY，将为外部上下文的每一行重新对子查询求值。

子查询的可缓存性与查询缓存中查询结果的缓存不同。子查询缓存发生在查询执行期间，而查询缓存仅在查询执行完成后用于存储结果。

使用EXPLAIN指定FORMAT = JSON时，输出结果中没有直接等同于select_type的单个属性；其结果汇总在query_block属性中。SIMPLE或PRIMARY在JSON格式中没有等价属性。

非SELECT语句的select_type值显示受影响表的语句类型。例如，对于DELETE语句，select_type是DELETE。

**• table (JSON name: table_name)**

输出行引用的表的名称。也可以是以下值之一：

	• 该行指的是id值为M和N的行的UNION

	• 该行引用id值为N的行的派生表结果。表可能来自FROM子句中的子查询

	• 该行指向id值为N的行的子查询实体化的结果

**• partitions (JSON name: partitions)**

数据记录来自与查询匹配的哪一个分区。对于非分区表，该值为NULL

**• type (JSON name: access_type)**

连接类型

**• possible_keys (JSON name: possible_keys)**

possible_keys列指示MySQL可以用来查找此表中的行数据的备选索引。注意，此列完全独立于EXPLAIN输出中显示的表的顺序。这意味着possible_keys中的某些索引在结果中展示的表顺序前提下可能无法使用。如果此列为NULL（或在JSON格式的输出中为undefined），则表示没有相关索引可以使用。在这种情况下，我们可以审视WHERE子句以检查它是否引用某些了可以创建合理索引的列来提高查询性能。如果是，创建适当的索引并再次使用EXPLAIN检查查询。要查看表有哪些索引，请使用SHOW INDEX FROM tbl_name。

**• key (JSON name: key)**

key列显示MySQL确实决定使用的索引。如果MySQL决定使用possible_keys其中一个索引来查找行，那么该索引将被列为key值。key列值可能是在possible_keys值中不存在的内容。

对于InnoDB，即使在查询中使用了二级索引列之外还使用了主键列，此二级索引也是可用的，因为InnoDB在每个二级索引后追加主键值。如果key为NULL，则说明MySQL找不到用于更有效地执行查询的索引。

要强制MySQL使用或忽略possible_keys列中列出的索引，在查询中使用FORCE INDEX，USE INDEX或IGNORE INDEX语法。对于MyISAM表，运行ANALYZE TABLE可帮助优化器选择更好的索引，myisamchk --analyze也是如此。

**• key_len (JSON name: key_length)**

key_len列指示MySQL决定使用的索引的长度。key_len的值使我们可以判断MySQL实际使用了索的哪些部分。
如果key列显示NULL，则len_len列也会显示NULL。索引的存储格式决定了，NULL列的索引长度比NOT NULL列的索引长度大1。

**• ref (JSON name: ref)**

ref列显示从表中选择行时使用哪些列或常量与key列中指定的索引进行比较。如果值为func，则使用的值是某个函数的结果。要查看是哪个函数，在EXPLAIN之后使用SHOW WARNINGS来查看扩展的EXPLAIN输出。该函数实际上可能是算术运算符等运算符。

**• rows (JSON name: rows)**

rows列表示MySQL认为执行查询必须要扫描的行数。对于InnoDB表，此数字是估计值，可能并不总是准确的。

**• filtered (JSON name: filtered)**

filtered列表示按表条件筛选的表占总行数的估计百分比。也就是说，rows列表示了估计要扫描的总行数，rows × filtered / 100表示将与之前的表连接的行数。

**• Extra (JSON name: none)**

此列包含有关MySQL如何解析查询的其他信息。没有与Extra列对应的单个JSON属性；但是，此列中可能出现的值将作为JSON属性展示，或者作为message属性的文本展示。

#### EXPLAIN Join Types

EXPLAIN输出结果的type列描述了表的连接方式。在JSON格式的输出中，这些结果显示为access_type属性的值。以下列表按照从最佳到最差的顺序介绍了连接类型：

**• system**

该表只有一行（=系统表）。 这是const连接类型的特例。

**• const**

该表最多只有一个匹配行，并在执行查询的最开始被读取。因为只有一行，所以优化器的其余部分可以将此行中列的值视为常量。const表非常快，因为它们只读一次。当我们将PRIMARY KEY或UNIQUE索引的所有部分都与常量值进行比较时，将使用const。例如在以下查询中，tbl_name可用作const表：

```sql
SELECT * FROM tbl_name WHERE primary_key=1;
SELECT * FROM tbl_name WHERE primary_key_part1=1 AND primary_key_part2=2;
```

**• eq_ref**

对于与前面表中每个行的组合，从该表中只读取一行。除了system和const类型之外，这是最好的连接类型。当连接使用索引的所有组成部分并且索引是PRIMARY KEY或UNIQUE NOT NULL索引时会使用eq_ref。eq_ref可用于使用=运算符进行比较的索引列。比较值可以是常量，也可以是使用在此表之前读取的表中的列的表达式。例如在下面的例子中，MySQL可以使用eq_ref处理ref_table表：

```sql
SELECT * FROM ref_table,other_table WHERE ref_table.key_column=other_table.column;
SELECT * FROM ref_table,other_table WHERE ref_table.key_column_part1=other_table.column
AND ref_table.key_column_part2=1;
```

**• ref**

对于与前面表中每个行的组合，将从此表中读取具有匹配索引值的所有行。 如果连接仅使用索引的最左前缀或者索引不是PRIMARY KEY或UNIQUE索引（换句话说，如果连接不能基于索引值匹配单行），则使用ref。 如果使用的索引值只匹配几行，这是一个很好的连接类型。ref可用于使用=或<=>运算符进行比较的索引列。 在以下示例中，MySQL可以使用ref连接方式来处理ref_table：

```sql
SELECT * FROM ref_table WHERE key_column=expr;
SELECT * FROM ref_table,other_table WHERE ref_table.key_column=other_table.column;
SELECT * FROM ref_table,other_table WHERE ref_table.key_column_part1=other_table.column
AND ref_table.key_column_part2=1;
```

**• fulltext**

使用FULLTEXT索引执行连接。

**• ref_or_null**

这种连接类型与ref类似，但说明MySQL会对包含NULL值的行进行额外搜索。此连接类型优化最常用于解析子查询。在以下示例中，MySQL可以使用ref_or_null连接来处理ref_table：

```sql
SELECT * FROM ref_table
WHERE key_column=expr OR key_column IS NULL;
```

**• index_merge**

此连接类型表示使用了索引合并优化。在这种情况下，输出行中的key列包含使用的索引列表，key_len包含索引可以的最长部分的长度。

**• unique_subquery**

此类型替换以下形式的某些IN子查询的eq_ref：

```sql
value IN (SELECT primary_key FROM single_table WHERE some_expr)
```

unique_subquery只是一个索引查找函数，它可以完全替换子查询以提高效率。

**• index_subquery**

此连接类型类似于unique_subquery，也是取代了IN子查询，但它适用于以下形式的子查询中的非唯一索引：

```sql
value IN (SELECT key_column FROM single_table WHERE some_expr)
```

**• range**

使用索引检索给定范围内的行。输出行中的key列指示使用哪个索引。key_len包含使用的最长索引部分。对于此类型，ref列为NULL。

使用=，<>，>，> =，<，<=，IS NULL，<=>，BETWEEN或IN() 运算符中的任何一个将索引列与常量进行比较时，可以使用range：

```sql
SELECT * FROM tbl_name WHERE key_column = 10;
SELECT * FROM tbl_name WHERE key_column BETWEEN 10 and 20;
SELECT * FROM tbl_name WHERE key_column IN (10,20,30);
SELECT * FROM tbl_name WHERE key_part1 = 10 AND key_part2 IN (10,20,30);
```

**• index**

除了扫描索引树之外，index连接类型与ALL相同。这种情况有两种：

	• 如果索引是查询的覆盖索引，并能够满足表中所需的所有数据，则仅扫描索引树。在这种情况下，Extra列显示Using index。仅索引扫描通常比ALL快，因为索引的大小通常小于表数据 

	• 使用索引中的读取执行全表扫描，以按索引顺序查找数据行。Extra列中不会显示Using index

当查询仅使用属于单个索引的列时，MySQL可以使用此连接类型。

**• ALL**

对与前一个表中的每个行组合进行全表扫描。如果表是第一个没有标记为const的表，这通常是不好的，并且在所有其他情况下通常都非常糟糕。通常，我们可以通过添加索引来避免ALL，这些索引根据前一个表中的常量值或列值从表中搜索行。

#### EXPLAIN Extra Information

EXPLAIN输出的Extra列包含有关MySQL如何解析查询的一些额外信息。下面列出了此列中可能出现的值。每个项目还有在Extra值中对应的JSON格式的输出。其中一些有特定的属性与之对应，其他显示为message属性的文本。

如果要将我们的查询语句优化到最快，在Extra语句中寻找Using filesort和Using temporary，或者在JSON格式的EXPLAIN输出中寻找using_filesort和using_temporary_table属性等于true的：

**• Child of 'table' pushed join@1 (JSON: message text)**

Applies only in NDB Cluster

**• const row not found (JSON property: const_row_not_found)**

对于诸如SELECT ... FROM tbl_name之类的查询，该表为空。

**• Deleting all rows (JSON property: message)**

对于DELETE，某些存储引擎（如MyISAM）支持一种以简单快捷的方式删除所有表行的方法。如果引擎使用此优化，则会在Extra列中显示此值。

**• Distinct (JSON property: distinct)**

MySQL正在寻找DISTINNCT值，因此它在找到第一个匹配行后停止为当前行组合搜索更多行。

**• FirstMatch(tbl_name) (JSON property: first_match)**

在tbl_name上使用了半连接FirstMatch连接快捷方式。

**• Full scan on NULL key (JSON property: message)**

当优化器无法使用indexlookup访问方法时，子查询优化作会使用这种情况做为回退策略。

**• Impossible HAVING (JSON property: message)**

HAVING子句始终为false，无法返回任何行。

**• Impossible WHERE (JSON property: message)**

WHERE子句始终为false，无法返回任何行。

**• Impossible WHERE noticed after reading const tables (JSON property: message)**

MySQL已经读取了所有const（和系统）表，并确定WHERE子句始终为false。

**• LooseScan(m..n) (JSON property: message)**

使用半连接LooseScan策略。 m和n是索引组成部分编号。

**• No matching min/max row (JSON property: message)**

没有行满足类似SELECT MIN(...) FROM ... WHERE的条件。

**• no matching row in const table (JSON property: message)**

对于具有连接的查询，有一个空表或有个表其数据没有一条能满足唯一索引条件。

**• No matching rows after partition pruning (JSON property: message)**

对于DELETE或UPDATE，优化器在分区修剪后未发现任何要删除或更新的内容。它与SELECT语句的Impossible WHERE的含义相似。

**• No tables used (JSON property: message)**

该查询没有FROM子句，或者使用FROM DUAL子句。对于INSERT或REPLACE语句，EXPLAIN在没有SELECT部分时会显示该值。例如，它出现在EXPLAIN INSERT INTO VALUES(10)中，因为它等同于EXPLAIN INSERT INTO t SELECT 10 FROM DUAL.。

**• Not exists (JSON property: message)**

MySQL能够对查询执行LEFT JOIN优化，并且在找到与LEFT JOIN条件匹配的行之后，不会再检查此表中针对上一行组合的更多行。以下是可以通过上述方式优化的查询类型的示例：

```sql
SELECT * FROM t1 LEFT JOIN t2 ON t1.id=t2.id WHERE t2.id IS NULL;
```

假设t2.id列被定义为NOT NULL。在这种情况下，MySQL扫描t1并使用t1.id的值在t2中查找行。如果MySQL在t2中找到匹配的行，则它知道t2.id永远不能为NULL，并且不会扫描t2中具有相同id值的其余行。换句话说，对于t1中的每一行，MySQL需要在t2中只进行一次查找，而不管t2中实际匹配多少行。

**• Plan isn't ready yet (JSON property: none)**

当优化器尚未完成为在指定连接中执行的语句创建执行计划时，EXPLAIN FOR CONNECTION会显示此值。如果执行计划输出包含多行，则其中的任何一行或全部都可以显示此Extra值，具体取决于优化程序在确定完整执行计划时的进度。

**• Range checked for each record (index map: N) (JSON property: message)**

MySQL没有找到合适的索引可以使用，但发现在前面的表的列值已知后可能能使用某些索引。对于与上一个表中的每个行组合，MySQL检查是否可以使用range或index_merge访问方法来检索行。这样虽然不是很快，但比执行没有索引的连接更快。索引的编号从1开始，顺序与表SHOW INDEX结果的顺序相同。索引映射值N是表示候选索引位掩码值。例如，值0x19（二进制11001）表示将考虑索引1,4和5。

**• Scanned N databases (JSON property: message)**

这表示在处理INFORMATION_SCHEMA表的查询时服务器执行的目录扫描数。N的值可以是0,1或ALL。

**• Select tables optimized away (JSON property: message)**

如果优化器确定了1）应该返回最多一行数据，2）为了得到该行数据，必须读取确定的行集。当要读取的行在优化阶段期间可以被读取时（例如，通过读取索引行），在查询执行期间不需要读取任何表。

第一个条件当查询被隐式分组时（包含聚合函数但没有GROUP BY子句）被满足，第二个条件当每个使用的索引执行一行查找时被满足。用到的索引数决定了要读取的行数。例如以下隐式分组查询：

```sql
SELECT MIN(c1), MIN(c2) FROM t1;
```

假设可以通过读取一个索引行来查询MIN(c1)，并且可以通过从不同索引读取来查询MIN(c2)。也就是说，对于每个列c1和c2，都存在索引，上述列是索引的第一个列。在这种情况下通过读取两个确定行来生成并返回一行。如果要读取的行不是确定性的，则不会产生此Extra值。例如这个查询：

```sql
SELECT MIN(c2) FROM t1 WHERE c1 <= 10;
```

假设(c1, c2)是覆盖索引，使用此索引，必须扫描c1 <= 10的所有行以查找MIN(c2)。相比之下，对于以下查询：

```sql
SELECT MIN(c2) FROM t1 WHERE c1 = 10;
```

在这种情况下，第一个索引行上的条件c1 = 10的结果中包含MIN(c2)。要生成返回的行只读取一行数据。

对于维护每个表的精确行数的存储引擎（例如MyISAM，但不包括InnoDB），对于没有WHERE子句或WHERE子句始终为true并且没有GROUP BY子句的COUNT(*)查询，可能会出显示Extra值。（这是隐式分组查询的一种实例，其中存储引擎会影响是否可以读取确定数量的行）

**• Skip_open_table, Open_frm_only, Open_full_table (JSON property: message)**

这些值表示适用于INFORMATION_SCHEMA表的查询的文件打开优化。

**• Start temporary, End temporary (JSON property: message)**

这表示有用于半连接Duplicate Weedout策略的临时表。

**• unique row not found (JSON property: message)**

对于诸如SELECT ... FROM tbl_name之类的查询，没有行满足表上UNIQUE索引或PRIMARY KEY的条件。

**• Using filesort (JSON property: using_filesort)**

MySQL必须执行额外的工作以找出如何按顺序检索行。文件中存储的是通过根据连接类型遍历所有行得到的key和指向与WHERE子句匹配的行数据的指针。然后对key进行，并按顺序检索行。

**• Using index (JSON property: using_index)**

表示列信息可以仅使用索引树中的信息检索而不必另外寻找读取实际行数据。当查询仅使用属于单个索引的列时，可以使用此策略。

对于具有用户自定义的聚簇索引的InnoDB表，即使Extra列中不显示Using index，也可以使用该索引。例如如果type是index并且key是PRIMARY，则会出现这种情况。

**• Using index condition (JSON property: using_index_condition)**

通过访问索引元组，并对其应用查询条件已确定是否需要读取完整的表行数据。以这种方式，优化器会利用索引数据用于推迟("push down")读取表行数据的时机。

**• Using index for group-by (JSON property: using_index_for_group_by)**

与Using index表访问方法类似，Using index for group-by表示MySQL找到了一个索引，可用于检索GROUP BY或DISTINCT查询的所有列，而无需对实际表进行任何额外的磁盘访问。此外，索引以最有效的方式使用，因此对于每个group，只需读取少数索引条目。

**• Using join buffer (Block Nested Loop), Using join buffer (Batched Key Access) (JSON property: using_join_buffer)**

将联接中先读取的表分成几部分读入连接缓冲区，然后从缓冲区中取出行来与当前表执行连接。

(Block Nested Loop)表示使用块嵌套循环算法， (Batched Key Access) 表示使用批量密钥访问算法。也就是说，来自EXPLAIN输出前一行的表中的key将被存入缓冲区，匹配的行将从由出现Using join buffer的行所表示的表中批量提取。

**• Using MRR (JSON property: message)**

使用Multi-Range Read优化策略读取表。

**• Using sort_union(...), Using union(...), Using intersect(...) (JSON property:message)**

这些表示index_merge连接类型合并索引扫描时用到的特定算法。

**• Using temporary (JSON property: using_temporary_table)**

表示要解析查询，MySQL需要创建一个临时表来保存结果。如果查询包含的GROUP BY和ORDER BY子句中使用不同的列规则，则通常会发生这种情况。

**• Using where (JSON property: attached_condition)**

WHERE子句用于过滤出哪些行与下一个表进行连接匹配求值或发送到客户端。除非我们特意打算从表中获取或检查所有行，否则如果Extra值不是Using where并且表连接类型为ALL或index，则查询可能是有问题的。

**• Using where with pushed condition (JSON property: message)**

This item applies to NDB tables only.

**• Zero limit (JSON property: message)**

查询中使用了LIMIT 0子句，无法查询到任何行。

#### EXPLAIN Output Interpretation

通过获取EXPLAIN输出的rows列中的值的乘积，可以很好地指示连接的好坏程度。这个结果可以大致告诉我们要执行查询MySQL必须检索多少行。如果使用max_join_size系统变量限制查询，则此结果还用于确定要执行哪些多表SELECT语句以及要抛弃哪些。

以下示例显示了如何根据EXPLAIN提供的信息逐步优化多表连接：

```sql
EXPLAIN SELECT tt.TicketNumber, tt.TimeIn, tt.ProjectReference, 
	tt.EstimatedShipDate, tt.ActualShipDate, tt.ClientID, tt.ServiceCodes, 
	tt.RepetitiveID, tt.CurrentProcess, tt.CurrentDPPerson, tt.RecordVolume,
    tt.DPPrinted, et.COUNTRY, et_1.COUNTRY, do.CUSTNAME
 FROM tt, et, et AS et_1, do
 WHERE tt.SubmitTime IS NULL
 	AND tt.ActualPC = et.EMPLOYID
 	AND tt.AssignedPC = et_1.EMPLOYID
 	AND tt.ClientID = do.CUSTNMBR;
```

假设各相关表的列定义如下：

• 参与比较的列声明如下：

| Table | Column     | Data Type |
| ----- | ---------- | --------- |
| tt    | ActualPC   | CHAR(10)  |
| tt    | AssignedPC | CHAR(10)  |
| tt    | ClientID   | CHAR(10)  |
| et    | EMPLOYID   | CHAR(15)  |
| do    | CUSTNMBR   | CHAR(15)  |

• 表具有以下索引：

| Table | Index                  |
| ----- | ---------------------- |
| tt    | ActualPC               |
| tt    | AssignedPC             |
| tt    | ClientID               |
| et    | EMPLOYID (primary key) |
| do    | CUSTNMBR (primary key) |

• tt.ActualPC值分布不均匀

最初，在执行任何优化之前，EXPLAIN语句会生成以下信息：

```sql
table 	type 	possible_keys 	key 	key_len 	ref 	rows 	Extra
et 		ALL 	PRIMARY 		NULL 	NULL 		NULL 	74
do 		ALL 	PRIMARY 		NULL 	NULL 		NULL 	2135
et_1 	ALL 	PRIMARY 		NULL 	NULL 		NULL 	74
tt 		ALL 	AssignedPC, 	NULL 	NULL 		NULL 	3872
 				ClientID,
 				ActualPC
 				Range checked for each record (index map: 0x23)
```

因为每个表的type都是ALL，所以此输出表明MySQL正在生成所有表的笛卡尔积；也就是说，每一种行的组合。这需要相当长的时间，因为必须检查的组合数为每个表中行数的乘积。对于上述的情况，该乘积为74×2135×74×3872 = 45,268,558,720行。想象一下如果表数据更大时需要多长时间。

这里的一个问题是，如果声明连接条件列的类型和大小相同，MySQL可以更有效地使用列上的索引。在此例中，如果将VARCHAR和CHAR声明为相同大小，则认为它们是相同的。tt.ActualPC声明为CHAR(10)，et.EMPLOYID声明为CHAR(15)，因此存在长度不匹配。我们需要改变列的定义使其数据类型和大小一致：

```sql
 ALTER TABLE tt MODIFY ActualPC VARCHAR(15);
```

现在tt.ActualPC和et.EMPLOYID都是VARCHAR(15)。 再次执行EXPLAIN语句会得到此结果：

```sql
table 	type 	possible_keys 	key 	key_len 	ref 		rows 	Extra
tt 		ALL 	AssignedPC, 	NULL 	NULL 		NULL 		3872 	Using where
 				ClientID, 
 				ActualPC
do 		ALL 	PRIMARY 		NULL 	NULL 		NULL 		2135 	Range checked 													    for each record (index map: 0x1)
et_1 	ALL 	PRIMARY 		NULL 	NULL 		NULL 		74		Range checked 														for each record (index map: 0x1)
et 		eq_ref 	PRIMARY 		PRIMARY 15 			tt.ActualPC 1
```

虽然并不完美，但这要比最初的版本好得多：行值的乘积减少了74倍。执行这一计划大约需要几秒钟。

同样可以进行第二次列定义的修改以消除tt.AssignedPC = et_1.EMPLOYID和tt.ClientID = do.CUSTNMBR比较的列长度不匹配：

```sql
 ALTER TABLE tt MODIFY AssignedPC VARCHAR(15), MODIFY ClientID VARCHAR(15);
```

在这次修改之后，EXPLAIN产生如下所示的输出：

```sql
table 	type 	possible_keys 	key 		key_len ref 			rows 	Extra
et 		ALL 	PRIMARY 		NULL 		NULL 	NULL 			74
tt 		ref 	AssignedPC, 	ActualPC 	15 		et.EMPLOYID 	52 		Using where
 				ClientID, 
 				ActualPC
et_1 	eq_ref 	PRIMARY 		PRIMARY 	15 		tt.AssignedPC 	1
do 		eq_ref 	PRIMARY 		PRIMARY 	15 		tt.ClientID 	1
```

此时，查询几乎是尽最大可能的被优化了。剩下的问题是，默认情况下，MySQL假定tt.ActualPC列中的值是均匀分布的，而tt表则不是这种情况。幸运的是，我们可以告诉MySQL分析索引值的分布：

```sql
ANALYZE TABLE tt;
```

有了这一附加的索引信息，连接是完美的，EXPLAIN产生这个结果：

```sql
table 	type 	possible_keys 	key 		key_len ref 			rows Extra
tt 		ALL 	AssignedPC 		NULL 		NULL 	NULL 			3872 Using where
 				ClientID, 
 				ActualPC
et 		eq_ref 	PRIMARY 		PRIMARY 	15 		tt.ActualPC 	1
et_1 	eq_ref 	PRIMARY 		PRIMARY 	15 		tt.AssignedPC 	1
do 		eq_ref 	PRIMARY 		PRIMARY 	15 		tt.ClientID 	1
```

EXPLAIN输出中的rows列是来自MySQL连接优化器的有根据的猜测。通过将rows列的乘积与查询返回的实际行数进行比较，检查数字是否与事实接近。如果数字完全不同，则可以通过在SELECT语句中使用STRAIGHT_JOIN并尝试在FROM子句中以不同顺序列出表来获得更好的性能。

### Extended EXPLAIN Output Format

对于SELECT语句，EXPLAIN语句生成Extra或者Extended信息，这些信息不是EXPLAIN输出的一部分，但可以通过在EXPLAIN之后发出SHOW WARNINGS语句来查看。SHOW WARNINGS输出中的Message值显示优化器如何限定SELECT语句中的表名和列名，SELECT应用重写和优化规则后的样子，以及可能有关优化过程的其他说明。可以在EXPLAIN之后使用SHOW WARNINGS语句显示的扩展信息仅针对SELECT语句，对DELETE，INSERT，REPLACE和UPDATE语句展示空结果。

由于SHOW WARNINGS显示的语句可能包含特殊标记以提供有关查询重写或优化程序操作的信息，因此该语句不一定是有效的SQL，也不打算被执行。输出还可能包含具有Message值的行，这些行提供有关优化程序所执行操作的其他非SQL解释性说明。这些特殊标记列举如下：

**• <auto_key>**
为临时表临时生成的索引key

**• \<cache\>(expr)**
表达式（例如标量子查询）执行一次，结果值保存在内存中供以后使用。对于由多个值组成的结果，可能会创建一个临时表

**• \<exists\>(query fragment)**
子查询谓词转换为EXISTS谓词，子查询被转换，以便它可以与EXISTS谓词一起使用

**• <in_optimizer>(query fragment)**
这是一个内部优化器对象，对于用户没有意义

**• <inde_lookup>(query fragment)**
对某个查询片段使用索引查找处理以查找符合条件的行

**• \<if\>(condition, expr1, expr2)**
如果条件为真，则计算为expr1，否则为expr2

**• <is_not_null_test>(expr)**
用于验证表达式不计算为NULL的测试

**• \<materialize\>(query fragment)**
使用了子查询实体化

**• materialized-subquery.col_name**
对内部临时表中列col_name的引用，该列用来保存子查询的求值结果

**• <primary_index_lookup>(query fragment)**
使用主键查找处理查询片段以查找符合条件的行

**• <ref_null_helper>(expr)**
这是一个内部优化器对象，对于用户没有意义

**• /* select#N */ select_stmt**
SELECT与非扩展EXPLAIN输出中具有id值N的行相关联

**• outer_tables semi join (inner_tables)**
半连接操作。 inner_tables显示未"pull out"的表

**• <temporary table\>**
这表示为缓存中间结果而创建的内部临时表

### Obtaining Execution Plan Information for a Named Connection

获取在指定连接中执行的可解释语句的执行计划，使用如下格式：

```sql
EXPLAIN [options] FOR CONNECTION connection_id;
```

获取connection_id，使用：

```sql
 SELECT CONNECTION_ID();
```

### Estimating Query Performance

在大多数情况下，我们可以通过计算磁盘搜索次数来估计查询性能。对于小表，一般可以在一次磁盘查找中找到行数据（因为索引可能已缓存）。对于更大的表，使用B-tree索引，我们估计需要这么多次磁盘寻址来找到一行数据：

> log（row_count）/ log（index_block_length / 3 * 2 /（index_length + data_pointer_length））+ 1。

在MySQL中，索引块通常为1,024字节，数据指针通常为4字节。对于一个有500,000行数据的表，索引key长度为3个字节（MEDIUMINT的大小），通过公式可以计算得出需要4次寻道。

> log（500,000）/ log（1024/3 * 2 /（3 + 4））+ 1 = 4

该索引需要大约500,000 * 7 * 3/2 = 5.2MB存储空间（假设典型的索引缓冲区填充比例为2/3），但是可能在内存中有很多此索引的缓存数据，所以也许只需要一两次调用读取数据以查找行。但是，对于写操作，我们需要四次搜索请求来查找放置新索引值的位置，通常两次寻址以更新索引并写入行数据。

前面的讨论并不意味着我们的应用程序性能会以logN的速度慢慢变慢。只要操作系统或MySQL服务器缓存了所有内容，事情就会变得微不足道。但是在数据太大而无法缓存之后，性能开始明细变慢，直到我们的应用程序只受磁盘搜索的约束（它增加了log N）。要避免这种情况，需要增加数据增长时的key cache大小。对于MyISAM表，key cache大小由key_buffer_size系统变量控制。 