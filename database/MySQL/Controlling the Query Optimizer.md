## Controlling the Query Optimizer

MySQL通过提供系统变量支持用户影响查询计划的评估方式，可切换优化，优化器和索引提示以及优化器成本模型。

### Controlling Query Plan Evaluation

查询优化器的任务是找到执行SQL查询的最佳计划。因为“好”和“坏”计划之间的性能可能相差几个数量级（即秒与小时甚至几天），所以大多数查询优化器（包括MySQL的查询优化器）执行或多或少的详尽搜索以从所有可能的查询计划中评估获得最佳计划。

对于连接查询，MySQL优化器需要考虑的可用计划数量随查询中引用的表的数量呈指数增长。对于少量表（通常小于7到10），这不是问题。但是，当提交较大的查询时，在查询优化中花费的时间可能很容易成为服务器性能的主要瓶颈。

更灵活的查询优化方法使用户能够控制优化程序在搜索最佳查询执行计划时的详尽程度。一般的想法是，优化器考虑的计划越少，编译查询所花费的时间就越少。另一方面，由于优化器会跳过某些计划，因此可能会漏掉最佳计划。

可以使用两个系统变量来控制优化程序考虑的计划数量的行为：

• **optimizer_prune_level**变量告诉优化器基于对每个表访问的行数的估计跳过某些计划。我们的经验表明，这种“有根据的猜测”很少会错过最佳计划，并且可能会大大减少查询编译时间。这就是默认情况下此选项打开（optimizer_prune_level = 1）的原因。但是，如果您认为优化程序错过了更好的查询计划，则可以关闭此选项（optimizer_prune_level = 0），但存在查询编译可能需要更长时间的风险。请注意，即使使用此启发式算法，优化程序仍会探索大致按照指数增长的计划。

• **optimizer_search_depth**变量指示优化程序对每个不完整计划的需要探索的深度。较小的optimizer_search_depth值可以使查询编译时间缩小几个数量级。例如，如果optimizer_search_depth接近查询中的表数，则使用12，13或更多表的查询可能很容易使编译时间飙升至数小时甚至数天。同时，如果使用optimizer_search_depth等于3或4，优化器可以在不到一分钟的时间内为同一查询进行编译。如果我们不确定optimizer_search_depth的合理值是什么，则可以将此变量设置为0以告知优化器自动确定该值。

### Optimizer Hints

控制优化器策略的一种方法是设置optimizer_switch系统变量。对此变量的更改会影响所有后续查询的执行；为了使一个查询与另一个查询不同，有必要在每个查询之前更改optimizer_switch。

另一种控制优化器的方法是使用优化器提示，可以在各个语句中指定。由于优化提示适用于每个语句，因此相较于使用optimizer_switch实现，它们可以更细粒度地控制语句执行计划。例如，我们可以为语句中的一个表启用优化，并禁用其他表的优化。语句中的提示优先于optimizer_switch系统变量。

```sql
SELECT /*+ NO_RANGE_OPTIMIZATION(t3 PRIMARY, f2_idx) */ f1
 FROM t3 WHERE f1 > 30 AND f1 < 33;
SELECT /*+ BKA(t1) NO_BKA(t2) */ * FROM t1 INNER JOIN t2 WHERE ...;
SELECT /*+ NO_ICP(t1, t2) */ * FROM t1 INNER JOIN t2 WHERE ...;
SELECT /*+ SEMIJOIN(FIRSTMATCH, LOOSESCAN) */ * FROM t1 ...;
EXPLAIN SELECT /*+ NO_ICP(t1) */ * FROM t1 WHERE ...;
```

#### Optimizer Hint Overview

优化提示适用于不同的范围级别：

• 全局：提示会影响整个语句
• 查询块：提示会影响语句中的特定查询块
• 表级：提示会影响查询块中的特定表
• 索引级别：提示会影响表中的特定索引

下表总结了可用的优化提示，它们影响的优化程序策略以及它们适用的范围。

| Hint Name             | Description                                              | Applicable Scopes  |
| --------------------- | -------------------------------------------------------- | ------------------ |
| BKA, NO_BKA           | Affects Batched Key Access join processing               | Query block, table |
| BNL, NO_BNL           | Affects Block Nested-Loop join processing                | Query block, table |
| MAX_EXECUTION_TIME    | Limits statement execution time                          | Global             |
| MRR, NO_MRR           | Affects Multi-Range Read optimization                    | Table, index       |
| NO_ICP                | Affects Index Condition Pushdown optimization            | Table, index       |
| NO_RANGE_OPTIMIZATION | Affects range optimization                               | Table, index       |
| QB_NAME               | Assigns name to query block                              | Query block        |
| SEMIJOIN, NO_SEMIJOIN | Affects semi-join strategies                             | Query block        |
| SUBQUERY              | Affects materialization, IN-to-EXISTS subquery stratgies | Query block        |

禁用某种优化会阻止优化程序使用它。启用某种并不意味着优化器必须使用它，而是优化器在合适的情况下可以自由选用某一策略。

#### Optimizer Hint Syntax

优化器提示使用/ * ... * / C语言风格注释语法的变体，在/ *注释开始序列后面加上+字符。例如：

```sql
-- +字符后允许空格
/*+ BKA(t1) */
/*+ BNL(t1, t2) */
/*+ NO_RANGE_OPTIMIZATION(t4 PRIMARY) */
/*+ QB_NAME(qb2) */
```

解析器在SELECT，UPDATE，INSERT，REPLACE和DELETE语句的初始关键字后识别优化提示注释。在这些情况下允许提示：

• 在SELECT或者写语句的开头：

```sql
SELECT /*+ ... */ ...
INSERT /*+ ... */ ...
REPLACE /*+ ... */ ...
UPDATE /*+ ... */ ...
DELETE /*+ ... */ ...
```

• 在查询语句块的开头

```sql
(SELECT /*+ ... */ ... )
(SELECT ... ) UNION (SELECT /*+ ... */ ... )
(SELECT /*+ ... */ ... ) UNION (SELECT /*+ ... */ ... )
UPDATE ... WHERE x IN (SELECT /*+ ... */ ...)
INSERT ... SELECT /*+ ... */ ...
```

• 在EXPLAIN开头的支持提示的语句中

```sql
EXPLAIN SELECT /*+ ... */ ...
EXPLAIN UPDATE ... WHERE x IN (SELECT /*+ ... */ ...)
```

这意味着我们可以使用EXPLAIN来查看优化提示如何影响执行计划。在EXPLAIN之后立即使用SHOW WARNINGS可以查看提示是如何被优化器处理的。由扩展EXPLAIN之后的SHOW WARNINGS结果显示优化器使用了哪些提示。忽略的提示不会显示。

一个提示注释块中可能包含多个提示，但查询块不能包含多个提示注释块。例如：

```sql
-- OK
SELECT /*+ BNL(t1) BKA(t2) */ ...
-- 不OK
SELECT /*+ BNL(t1) */ /* BKA(t2) */ ...
```

当提示注释块中包含多个提示时，存在重复和冲突的可能性。一般情况下遵循以下准则。对于特定提示类型，可能会应用其他规则，详见每个提示类型的文档。

• 重复提示：对于诸如 / * + MRR（idx1）MRR（idx1）* / 的提示，MySQL使用第一个提示并发出有关重复提示的警告。

• 冲突提示：对于诸如/ * + MRR（idx1）NO_MRR（idx1）* /的提示，MySQL使用第一个提示并发出有关第二个冲突提示的警告。

查询块名称是标识符，并遵循有关标识符名称有效性以及引用它们的一般规则。提示名称，查询块名称和策略名称不区分大小写。对表和索引名称的引用遵循通常的标识符大小写区分规则。

##### Table-Level Optimizer Hints

表级优化提示会影响Block Nested-Loop（BNL）和批量密钥访问Batch Key Access（BKA）联接处理算法的使用。这些提示类型适用于特定表或查询块中的所有表。语法如下：

```sql
hint_name([@query_block_name] [tbl_name [, tbl_name] ...])
hint_name([tbl_name@query_block_name [, tbl_name@query_block_name] ...])
```

例子如下：

```sql
SELECT /*+ NO_BKA(t1, t2) */ t1.* FROM t1 INNER JOIN t2 INNER JOIN t3;
SELECT /*+ NO_BNL() BKA(t1) */ t1.* FROM t1 INNER JOIN t2 INNER JOIN t3;
```

上述语法中使用这些术语：

• **hint_name**：允许使用下列提示名称：

	• BKA, NO_BKA：对特定表使用或禁用BKA
	• BNL, NO_BNL：对特定表使用或禁用BNL

> 要使用BNL或BKA提示为外连接的任何内部表启用联接缓冲，必须先为外连接的所有内部表启用联接缓冲。

• **tbl_name**：SQL语句中使用的表的名称。提示可以用于它命名的所有表。如果优化提示不指定表，则它会被应用到它所在的查询块的所有表。如果表具有别名，则提示必须引用别名，而不是表名。提示中的表名称不能使用schema名称。

• **query_block_name**：提示适用的查询块。如果提示不包含前导@query_block_name语法，则提示将应用于它所在的查询块。对于tbl_name @query_block_name语法，提示应用于命名查询块中的命名表。

表级提示适用于从先前表接收记录的表，而不是发送这些记录的表。例如这个语句：

```sql
SELECT /*+ BNL(t2) */ FROM t1, t2;
```

如果优化器选择首先处理t1，则在开始从t2读取之前，通过缓冲来自t1的行数据，将块BNL连接应用于t2。如果优化器首先选择处理t2，则提示不起任何作用，因为t2是发送数据的表。

##### Index-Level Optimizer Hints

索引级提示会影响优化程序对特定表或索引的索引处理策略。这些提示类型会影响Index Condition Pushdown（ICP），Multi-Range Read（MRR）和range optimizations的使用。语法如下：

```sql
hint_name([@query_block_name] tbl_name [index_name [, index_name] ...])
hint_name(tbl_name@query_block_name [index_name [, index_name] ...])
```

上述语法中使用这些术语：

• **hint_name**：允许使用下列提示名称：

	• MRR, NO_MRR：对特定表或索引使用或禁用MRR。MRR提示仅适用于InnoDB和MyISAM表
	• NO_ICP：对特定表或索引禁用MRR。默认ICP是启用的，索引没有用于启用ICP的提示
	• NO_RANGE_OPTIMIZATION：对特定表或索引禁用index range access。这一提示同时会禁用Index Merge    和Loose Index Scan。默认情况下range access是启用的，所以也没有启用的提示。当范围的数量很多并且范围优化需要许多资源时，该提示可能是有用的。

• **tbl_name**：提示应用的表

• **index_name**：指定表中的一个索引的名称。提示适用于它命名的所有索引。如果提示不指定索引，则它适用于表中的所有索引。要引用主键，请使用名称PRIMARY。要查看表的索引名称，请使用SHOW INDEX。

• **query_block_name**： 提示适用的查询块。如果提示不包含前导@query_block_name语法，则提示将应用于它所在的查询块。对于tbl_name @query_block_name语法，提示应用于命名查询块中的命名表。

例子如下：

```sql
SELECT /*+ MRR(t1) */ * FROM t1 WHERE f2 <= 3 AND 3 <= f3;
SELECT /*+ NO_RANGE_OPTIMIZATION(t3 PRIMARY, f2_idx) */ f1
 FROM t3 WHERE f1 > 30 AND f1 < 33;
INSERT INTO t3(f1, f2, f3) (SELECT /*+ NO_ICP(t2) */ t2.f1, t2.f2, t2.f3 FROM t1,t2
 WHERE t1.f1=t2.f1 AND t2.f2 BETWEEN t1.f1
 AND t1.f2 AND t2.f2 + 1 >= t1.f1 + 1);
```

#### Subquery Optimizer Hints

子查询优化提示会影响是否使用半连接转换以及允许使用哪种半连接策略，并且在不使用半连接时，是否使用子查询实体化或IN-to-EXISTS转换。影响半连接策略的提示语法：

```sql
hint_name([@query_block_name] [strategy [, strategy] ...])
```

上述语法中使用这些术语：

• **hint_name**：允许使用下列提示名称：

​	• SEMIJOIN, NO_SEMIJOIN：启用或禁用指定的semi-join策略

• **strategy**：启用或禁用的semi-join策略名称。

支持这些semi-join：DUPSWEEDOUT, FIRSTMATCH, LOOSESCAN, MATERIALIZATION。

对于SEMIJOIN提示，如果没有指定策略名称，则使用optimizer_switch系统变量启用的策略，如果可能，使用半连接。如果指定策略不适用于该语句，则使用DUPSWEEDOUT。

对于NO_SEMIJOIN提示，如果没有指定策略名称，则使用半连接。如果指定的策略排除了该语句的所有适用策略，则使用DUPSWEEDOUT。

如果一个子查询嵌套在另一个子查询中并且两者都被合并为外部查询的半连接，则忽略最内层查询的任何半连接策略规范。SEMIJOIN和NO_SEMIJOIN提示仍可用于启用或禁用此类嵌套子查询的半连接转换。

如果禁用DUPSWEEDOUT，则优化程序有时可能会生成远非最佳的查询计划。This occurs due to heuristic pruning during greedy search，这可以通过设置optimizer_prune_level = 0来避免。

例如：

```sql
SELECT /*+ NO_SEMIJOIN(@subq1 FIRSTMATCH, LOOSESCAN) */ * FROM t2
 WHERE t2.a IN (SELECT /*+ QB_NAME(subq1) */ a FROM t3);
SELECT /*+ SEMIJOIN(@subq1 MATERIALIZATION, DUPSWEEDOUT) */ * FROM t2
 WHERE t2.a IN (SELECT /*+ QB_NAME(subq1) */ a FROM t3);
```

影响是否使用子查询实体化或IN-to-EXISTS转换的提示语法：

```sql
SUBQUERY([@query_block_name] strategy)
```

提示名称恒为SUBQUERY。对于SUBQUERY提示，支持这些策略值：INTOEXISTS, MATERIALIZATION。例如：

```sql
SELECT id, a IN (SELECT /*+ SUBQUERY(MATERIALIZATION) */ a FROM t1) FROM t2;
SELECT * FROM t2 WHERE t2.a IN (SELECT /*+ SUBQUERY(INTOEXISTS) */ a FROM t1);
```

对于半连接和SUBQUERY提示，前导@query_block_name指定提示适用的查询块。如果提示不包含前导@query_block_name，则提示将应用于它所在的查询块。

如果包含多个子查询提示，则使用第一个。如果还有其他类型的提示，则会发出警告。其他类型的提示会被忽略。

#### Statement Execution Time Optimizer Hints

MAX_EXECUTION_TIME提示仅支持SELECT语句。它将服务器终止执行语句之前允许执行语句的时间限制为N（毫秒）：

```sql
MAX_EXECUTION_TIME(N)
```

例如：

```sql
SELECT /*+ MAX_EXECUTION_TIME(1000) */ * FROM t1 INNER JOIN t2 WHERE ...
```

MAX_EXECUTION_TIME(N)提示将语句执行超时设置为N毫秒。如果此选项不存在或N为0，则应用max_execution_time系统变量建立的语句超时时间。

MAX_EXECUTION_TIME提示适用规则如下：

• 对于具有多个SELECT关键字的语句，例如UNION或带子查询的语句，MAX_EXECUTION_TIME适用于整个语句，并且必须出现在第一个SELECT之后。

• 它只适用于只读SELECT语句。非只读的语句是那些调用存储函数的语句，该函数会带来数据修改副作用。

• 它不适用于存储程序中的SELECT语句，将被忽略。

#### Optimizer Hints for Naming Query Blocks

表级，索引级和子查询优化器提示语法允许对特定查询块命名并作为其参数。要创建名称，使用QB_NAME提示，该提示为它所在的查询块指定名称：

```sql
QB_NAME(name)
```

可以使用QB_NAME提示以明确的方式确定其他提示适用的查询块。它们还允许在单个提示注释中指定非查询块名称，以便更容易理解复杂语句。例如下列语句：

```sql
SELECT ...
 FROM (SELECT ...
 FROM (SELECT ... FROM ...)) ...
```

使用QB_NAME提示为语句中的查询块指定名称：

```sql
SELECT /*+ QB_NAME(qb1) */ ...
 FROM (SELECT /*+ QB_NAME(qb2) */ ...
 FROM (SELECT /*+ QB_NAME(qb3) */ ... FROM ...)) ...
```

然后其他提示可以使用这些名称来引用相应的查询块：

```sql
SELECT /*+ QB_NAME(qb1) MRR(@qb1 t1) BKA(@qb2) NO_MRR(@qb3t1 idx1, id2) */ ...
 FROM (SELECT /*+ QB_NAME(qb2) */ ...
 FROM (SELECT /*+ QB_NAME(qb3) */ ... FROM ...)) ...
```

由此产生的效果如下：

• MRR(@qb1 t1) 适用于查询块qb1中的表t1。
• BKA(@qb2) 适用于查询块qb2。
• NO_MRR(@qb3 t1 idx1, id2) 适用于查询块qb3中表t1中的索引idx1和idx2。

查询块名称也是标识符，遵循有关名称有效性以及如何引用它们的通用规则。例如，必须引用包含空格的查询块名称，可以使用反引号来完成：

```sql
SELECT /*+ BKA(@`my hint name`) */ ...
 FROM (SELECT /*+ QB_NAME(`my hint name`) */ ...) ...
```

如果启用了ANSI_QUOTES SQL模式，则还可以在双引号内引用查询块名称：

 ```sql
SELECT /*+ BKA(@"my hint name") */ ...
 FROM (SELECT /*+ QB_NAME("my hint name") */ ...) ...
 ```

### Switchable Optimizations 

optimizer_switch系统变量可以控制优化器行为。它的值是一组flag，每个flag的值都为on或off，用于指示是启用还是禁用相应的优化器行为。此变量具有全局值和会话值，可以在运行时更改，也可以在服务器启动时设置全局默认值。

```sql
mysql>  SELECT @@optimizer_switch\G;
*************************** 1. row ***************************
@@optimizer_switch: 
    index_merge=on,
    index_merge_union=on,
    index_merge_sort_union=on,
    index_merge_intersection=on,
    engine_condition_pushdown=on,
    index_condition_pushdown=on,
    mrr=on,
    mrr_cost_based=on,
    block_nested_loop=on,
    batched_key_access=off,
    materialization=on,
    semijoin=on,
    loosescan=on,
    firstmatch=on,
    duplicateweedout=on,
    subquery_materialization_cost_based=on,
    use_index_extensions=on,
    condition_fanout_filter=on,
    derived_merge=on
```

要更改optimizer_switch的值，指定一个由逗号分隔的一个或多个命令列表组成的值：

```sql
SET [GLOBAL|SESSION] optimizer_switch='command[,command]...';
```

每个command值都应遵循下表中显示的格式：

| Command Syntax   | Meaning                                         |
| ---------------- | ----------------------------------------------- |
| default          | Reset every optimization to its default value   |
| opt_name=default | Set the named optimization to its default value |
| opt_name=off     | Disable the named optimization                  |
| opt_name=on      | Enable the named optimization                   |

尽管默认命令首先执行（如果存在），但值中命令的顺序无关紧要。不允许在值中多次指定同一个opt_name，这会导致报错。值中的任何错误都会导致赋值失败并抛出错误，optimizer_switch的值不变。

下表列出了按优化策略分组的允许的opt_name标志名称。

<table>
<tr>
    <th>Optimization</th>
    <th>Flag Name</th>
    <th>Meaning</th>
    <th>Default</th>
</tr>
<tr>
    <td>Batched Key Access</td>
    <td>batched_key_access</td>
    <td>Controls use of BKA join algorithm</td>
    <td>OFF</td>
</tr>
<tr>
    <td>Block Nested-Loop</td>
    <td>block_nested_loop</td>
    <td>Controls use of BNL join algorithm</td>
    <td>ON</td>
</tr>    
<tr>
    <td>Condition Filtering</td>
    <td>condition_fanout_filter</td>
    <td>Controls use of condition filtering</td>
    <td>ON</td>
</tr>
<tr>
    <td>Engine Condition Pushdown</td>
    <td>engine_condition_pushdown</td>
    <td>Controls engine condition pushdown</td>
    <td>ON</td>
</tr>
<tr>
    <td>Index Condition Pushdown</td>
    <td>Index_condition_pushdown</td>
    <td>Controls Index condition pushdown</td>
    <td>ON</td>
</tr>
<tr>
    <td>Index Extensions</td>
    <td>use_index_extensions</td>
    <td>Controls use of index extensions</td>
    <td>ON</td>
</tr>
<tr>
    <td rowspan="4">Index Merge</td>
    <td>index_merge</td>
    <td>Controls all Index Merge optimizations</td>
    <td>ON</td>
</tr>
<tr>
    <td>index_merge_intersection</td>
    <td>Controls the Index Merge Intersection Access optimization</td>
    <td>ON</td>
</tr>
<tr>
    <td>index_merge_sort_union</td>
    <td>Controls the Index Merge SortUnion Access optimization</td>
    <td>ON</td>
</tr>
<tr>
    <td>index_merge_union</td>
    <td>Controls the Index Merge Union Access optimization</td>
    <td>ON</td>
</tr>
<tr>
    <td rowspan="2">Multi-Range Read</td>
    <td>mrr</td>
    <td>Controls the Multi-Range Read strategy</td>
    <td>ON</td>
</tr>
<tr>
    <td>mrr_cost_based</td>
    <td>Controls use of cost-based MRR if mrr=on</td>
    <td>ON</td>
</tr>
<tr>
    <td rowspan="4">Semi-join</td>
    <td>semijoin</td>
    <td>Controls all semi-join strategies</td>
    <td>ON</td>
</tr>
<tr>
    <td>firstmatch</td>
    <td>Controls the semi-join FirstMatch strategy</td>
    <td>ON</td>
</tr>
<tr>
    <td>loosescan</td>
    <td>Controls the semi-join LooseScan strategy (not to be confused with LooseScan for GROUP BY)</td>
    <td>ON</td>
</tr>
<tr>
    <td>duplicateweedout</td>
    <td>Controls the semi-join Duplicate Weedout strategy</td>
    <td>ON</td>
</tr>
<tr>
    <td rowspan="2">Subquery materialization</td>
    <td>materialization</td>
    <td>Controls materialization (including semi-join materialization)</td>
    <td>ON</td>
</tr>
<tr>
    <td>subquery_materialization_cost_based</td>
    <td>Used cost-based materialization choice</td>
    <td>ON</td>
</tr>
<tr>
    <td>Derived table merging</td>
    <td>derived_merge</td>
    <td>Controls merging of derived tables and views into outer query block</td>
    <td>ON</td>
</tr>
</table>

要使batched_key_access在设置为on时生效，mrr标志也必须设置为on。目前，MRR的成本估算过于悲观，因此，mrr_cost_based也必须设置为off时才能使用BKA。

semijoin，firstmatch，loosescan，duplicateweedout和materialization标志可以控制semi-join和subquery materialization策略。semijoin标志控制是否使用semi-join。如果将其设置为on，则firstmatch和loosescan标志可以更好地控制允许的semi-join策略。materialization标志控制是否使用subquery meterialization。如果semi-join和materialization都已启用，则semi-join也会在适用的情况下使用materialization。这些标志默认为on。

如果禁用了duplicateweedout半连接策略，则除非禁用所有其他适用策略，否则不会使用该策略。

subquery_materialization_cost_based标志可以控制优化器在subquery materialization和IN-to-EXISTS子查询之间的选择。如果标志为on（默认值），则优化程序在subquery materialization和IN-to-EXISTS子查询转换之间执行基于成本的选择（如果可以使用任一方法）。如果该标志处于off状态，优化程序选择IN - > EXISTS子查询转换优先于subquery materialization。

derived_merge标志控制优化器在没有其他规则阻止合并时是否尝试将派生表和视图引用合并到外部查询块中； 例如，视图的ALGORITHM指令优先于derived_merge设置。默认情况下，该标志处于on状态以启用合并。

### Index Hints

索引提示为优化程序提供有关如何在查询处理期间选择索引的信息。索引提示与优化程序提示不同，可分别单独使用，也可以一起使用。

索引提示在表名后面指定。语法如下所示：

```sql
tbl_name [[AS] alias] [index_hint_list]

index_hint_list:	
	index_hint [index_hint] ...
	
index_hint:
	USE {INDEX|KEY}
		[FOR {JOIN|ORDER BY|GROUP BY}] ([index_list])
 	| IGNORE {INDEX|KEY}
 		[FOR {JOIN|ORDER BY|GROUP BY}] (index_list)
 	| FORCE {INDEX|KEY}
 		[FOR {JOIN|ORDER BY|GROUP BY}] (index_list)
 		
index_list:
	index_name [, index_name] ...
```

USE INDEX(index_list) 提示告诉MySQL只使用其中一个指定的索引来查找表中的行。替代语法IGNORE INDEX(index_list) 告诉MySQL不使用某些特定的索引。如果EXPLAIN显示MySQL正在使用possible indexes列表中的错误索引，则可以使用这些提示语法。

FORCE INDEX提示的作用类似于USE INDEX(index_list)，不同的是仅当无法使用命名索引查找表中的行时，才使用表扫描。

提示的参数是索引名称，而不是列名。要引用主键，使用名称PRIMARY。index_name值不必是完整的索引名称。 它可以是索引名称的明确前缀。如果前缀不明确，则会报错。例如：

```sql
SELECT * FROM table1 USE INDEX (col1_index, col2_index)
 WHERE col1=1 AND col2=2 AND col3=3;
SELECT * FROM table1 IGNORE INDEX (col3_index)
 WHERE col1=1 AND col2=2 AND col3=3;
```

索引提示的语法具有以下特征：

•
•您可以通过向提示添加FOR子句来指定索引提示的范围。
这为查询处理的各个阶段的执行计划的优化器选择提供了更细粒度的控制。
要仅影响MySQL决定如何在表中查找行以及如何处理联接时使用的索引，请使用FOR JOIN。
要影响对行进行或分组的索引用法，请使用FOR ORDER BY或FOR GROUP BY。
•您
SELECT * FROM t1 USE INDEX（i1）IGNORE INDEX for ORDER BY（i2）ORDER by a;

SELECT * FROM t1 USE INDEX（i1）USE INDEX（i1，i1）;

SELECT * FROM t1 USE INDEX FOR JOIN（i1）FORCE INDEX FOR JOIN（i2）;

• 省略USE INDEX的index_list参数在语法上有效，这意味着“不使用索引”。省略FORCE INDEX或IGNORE INDEX的index_list参数是语法错误。

• 可以通过向提示添加FOR子句来指定索引提示的范围。这为优化器在查询处理的各个阶段的执行计划选择提供了更细粒度的控制。要仅影响MySQL决定如何在表中查找行以及如何处理联接时使用的索引，使用FOR JOIN。要影响或分组的索引用法，使用FOR ORDER BY或FOR GROUP BY。

• 可以指定多个索引提示：

```sql
SELECT * FROM t1 USE INDEX (i1) IGNORE INDEX FOR ORDER BY (i2) ORDER BY a;
```

在几个提示中命名相同的索引（即使在相同的提示中）不是错误：

```sql
SELECT * FROM t1 USE INDEX (i1) USE INDEX (i1,i1);
```

但是，将USE INDEX和FORCE INDEX混合到同一个表中是错误的：

```sql
SELECT * FROM t1 USE INDEX FOR JOIN (i1) FORCE INDEX FOR JOIN (i2);
```

如果索引提示不包含FOR子句，则提示的范围将应用于语句的所有部分。例如，这个提示：

```sql
IGNORE INDEX(i1)
```

相当于这种提示组合：

```sql
IGNORE INDEX FOR JOIN (i1)
IGNORE INDEX FOR ORDER BY (i1)
IGNORE INDEX FOR GROUP BY (i1)
```

处理索引提示时，它们会被按类型（USE，FORCE，IGNORE）和范围（FOR JOIN，FOR ORDER BY，FOR GROUP BY）汇总在单个列表中。例如以下两个语句是等价的：

```sql
SELECT * FROM t1 USE INDEX () IGNORE INDEX (i2) USE INDEX (i1) USE INDEX (i2);
SELECT * FROM t1 USE INDEX (i1,i2) IGNORE INDEX (i2);
```

然后按以下顺序为每个范围应用索引提示：

1. {USE|FORCE} INDEX如果存在则应用，如果不存在，则使用优化程序确定的索引集。
2. 将IGNORE INDEX应用于上一步的结果。例如，以下两个查询是等效的：

```sql
SELECT * FROM t1 USE INDEX (i1) IGNORE INDEX (i2) USE INDEX (i2);
SELECT * FROM t1 USE INDEX (i1);
```

对于FULLTEXT搜索，索引提示的工作方式如下：

• 对于自然语言模式搜索，将以静默方式忽略索引提示。例如，忽略IGNORE INDEX(i1)并且不会抛出警告，仍然使用索引。

• 对于布尔模式搜索，将自动忽略带有FOR ORDER BY或FOR GROUP BY的索引提示。使用FOR JOIN或没有FOR修饰符的索引提示优先考虑。In contrast to how hints apply for non-FULLTEXT searches, the hint is used for all phases of query execution (finding rows and retrieval, grouping, and ordering). This is true even if the hint is given for a non-FULLTEXT index.

例如，以下两个查询是等效的：

```sql
SELECT * FROM t 
	USE INDEX (index1)
	IGNORE INDEX (index1) FOR ORDER BY
 	IGNORE INDEX (index1) FOR GROUP BY
 	WHERE ... IN BOOLEAN MODE ... ;
SELECT * FROM t
 	USE INDEX (index1)
 	WHERE ... IN BOOLEAN MODE ... ;
```

### The Optimizer Cost Model

为了生成执行计划，优化程序使用基于对查询执行期间发生的各种操作的总成本进行估计的成本模型。优化器有一组可编译的默认“成本常量”，可用于制定有关执行计划的决策。

优化器还具有成本估算数据库以便在执行计划构建期间使用。这些估计值存储在mysql系统数据库的server_cost和engine_cost表中，并且可以随时配置。这些表的目的是使我们可以轻松调整优化程序在尝试获取查询执行计划时使用的成本估算。

#### Cost Model General Operation

可配置的优化器成本模型的工作方式如下：

• 服务器在启动时将成本模型表读入内存，并在运行时使用内存中的值。表中指定的任何非NULL成本估算优先于相应的已编译默认成本常量。任何NULL估计都指示优化器使用编译的默认值。

• 在运行时，服务器可能会重新读取成本表。当动态加载存储引擎或执行FLUSH OPTIMIZER_COSTS语句时会发生这种情况。

• 成本表使MySQL管理员可以通过更改表中的条目轻松调整成本估算。通过将条目的开销值设置为NULL，也可以轻松恢复为默认值。优化程序使用内存中的成本值，因此对表更改之后应使用FLUSH OPTIMIZER_COSTS使刷新生效。

• 内存成本估算数据基于会话开始时的设置，并在整个会话生命周期中生效，直到结束。特别地，如果服务器重新读取成本表，则更改的估计值仅适用于随后启动的会话。现有会话不受影响。

• 成本表特定于给定的服务器实例。服务器不会将成本表更改复制到复制从属服务器。

#### The Cost Model Database

优化器成本模型数据库由mysql系统数据库中的两个表组成，其中包含查询执行期间发生的操作的成本估算信息：

• **engine_cost**：特定存储引擎特定操作的优化程序成本估算

```sql
mysql> select * from engine_cost\G;
*************************** 1. row ***************************
engine_name: default
device_type: 0
  cost_name: io_block_read_cost
 cost_value: NULL
last_update: 2018-09-14 17:10:01
    comment: NULL
*************************** 2. row ***************************
engine_name: default
device_type: 0
  cost_name: memory_block_read_cost
 cost_value: NULL
last_update: 2018-09-14 17:10:01
    comment: NULL
```

• *engine_name*

此成本估算适用的存储引擎的名称，不区分大小写。如果该值为default，则它适用于没有自己的命名条目的所有存储引擎。如果服务器在读取此表时无法识别引擎名称，则会向错误日志写入警告。

• *device_type*

The device type to which this cost estimate applies. The column is intended for specifying different
cost estimates for different storage device types, such as hard disk drives versus solid state drives.
Currently, this information is not used and 0 is the only permitted value.

engine_cost表的主键是包含（cost_name，engine_name，device_type）列的元组。服务器可以识别engine_cost表的下列cost_name值：

• *io_block_read_cost (default 1.0)*

从磁盘读取索引或数据块的成本。与读取较少磁盘块的查询计划相比，增加此值会导致读取许多磁盘块的查询计划成本更加昂贵。

*• memory_block_read_cost (default 1.0)*

类似io_block_read_cost，表示从内存数据库缓冲区读取索引或数据块的开销。

如果io_block_read_cost和memory_block_read_cost值不同，则同一查询的两次运行可能使用了不同的执行计划。假设内存访问的成本低于磁盘访问的成本。在这种情况下，在数据读入缓冲池之前和之后，我们可能会得到不同的计划。 

• **server_cost**：一般服务器操作的优化程序成本估算

```sql
mysql> select * from server_cost\G;
*************************** 1. row ***************************
  cost_name: disk_temptable_create_cost
 cost_value: NULL
last_update: 2018-09-14 17:10:01
    comment: NULL
*************************** 2. row ***************************
  cost_name: disk_temptable_row_cost
 cost_value: NULL
last_update: 2018-09-14 17:10:01
    comment: NULL
*************************** 3. row ***************************
  cost_name: key_compare_cost
 cost_value: NULL
last_update: 2018-09-14 17:10:01
    comment: NULL
*************************** 4. row ***************************
  cost_name: memory_temptable_create_cost
 cost_value: NULL
last_update: 2018-09-14 17:10:01
    comment: NULL
*************************** 5. row ***************************
  cost_name: memory_temptable_row_cost
 cost_value: NULL
last_update: 2018-09-14 17:10:01
    comment: NULL
*************************** 6. row ***************************
  cost_name: row_evaluate_cost
 cost_value: NULL
last_update: 2018-09-14 17:10:01
    comment: NULL
```

• *cost_name*（主键）

成本模型中使用的成本估算的名称，不区分大小写。如果服务器在读取此表时无法识别成本名称，则会向错误日志写入警告。

• *cost_value*

成本估算值。如果该值为非NULL，则服务器将其用作成本。否则，它使用默认估计值（已编译的值）。DBA可以通过更新此列来更改成本估算。如果服务器在读取此表时发现成本值无效（非正数），则会向错误日志写入警告。

要覆盖默认成本估算值，只需要将此列设置为非NULL值。要恢复为默认值，将值重新设置为NULL。然后执行FLUSH OPTIMIZER_COSTS告诉服务器重新读取成本表。

cost_value支持下列值：

*• disk_temptable_create_cost (default 40.0), disk_temptable_row_cost (default 1.0)*

在基于磁盘的存储引擎中创建内部临时表的成本估算。增加这些值会增加使用内部临时表的成本估算，并使优化程序更喜欢使用不需要创建临时表的查询计划。与相应内存参数(memory_temptable_create_cost,
memory_temptable_row_cost)的默认值相比，这些磁盘参数的默认值越大，反映了处理基于磁盘的表的成本更高。

*• key_compare_cost (default 0.1)*

比较数据记录key的成本。增大此值会导致涉及多次比较key的查询计划变得成本更加高。例如，与使用索引避免进行的查询计划相比，执行文件的查询计划变得相对成本更高。

*• memory_temptable_create_cost (default 2.0), memory_temptable_row_cost (default 0.2)*

使用MEMORY存储引擎在内存中创建的临时表的成本估算。增大这些值会增加使用内部临时表的成本估算，并使优化程序更喜欢使用不带有临时表的查询计划。

*• row_evaluate_cost (default 0.2)*

对数据记录条件求值的成本。与处理较少行的查询计划相比，增大此值会导致处理许多行的查询计划变得更加成本高昂。例如，与读取较少行的范围扫描相比，全表扫描变得相对成本更高。

#### Making Changes to the Cost Model Database

对于希望更改成本模型参数默认值的DBA，可以尝试将值加倍或减半并测量效果。对io_block_read_cost和memory_block_read_cost参数的更改最有可能产生有价值的结果。这些参数值使数据访问方法的成本模型能够考虑从不同来源读取信息的成本；也就是说，从磁盘读取信息与读取内存缓冲区中的信息的成本。

例如，在所有其他条件相同的情况下，将io_block_read_cost设置为大于memory_block_read_cost的值会使优化器相对于必须从磁盘读取数据的查询计划更倾向于读取已经保存在内存中的信息的查询计划。

修改这些值的例子：

```sql
UPDATE mysql.engine_cost SET cost_value = 2.0
 WHERE cost_name = 'io_block_read_cost';
FLUSH OPTIMIZER_COSTS;

INSERT INTO mysql.engine_cost
 VALUES ('InnoDB', 0, 'io_block_read_cost', 3.0,
 CURRENT_TIMESTAMP, 'Using a slower disk for InnoDB');
FLUSH OPTIMIZER_COSTS;
```