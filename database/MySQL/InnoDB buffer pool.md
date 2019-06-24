# InnoDB buffer pool

## The InnoDB Buffer Pool

InnoDB maintains a storage area called the buffer pool for **caching data and indexes in memory**. Knowing how the InnoDB buffer pool works, and taking advantage of it to keep frequently accessed data in memory, is an important aspect of MySQL tuning.

You can configure the various aspects of the InnoDB buffer pool to improve performance.

• Ideally, you set the size of the buffer pool to as **large** a value as practical, leaving enough memory for other processes on the server to run without excessive paging. The larger the buffer pool, the more InnoDB acts like an in-memory database, reading data from disk once and then accessing the data from memory during subsequent reads.

• With 64-bit systems with large memory sizes, you can split the buffer pool into **multiple parts**, to minimize contention for the memory structures among concurrent operations.

• You can keep frequently accessed data in memory despite sudden spikes of activity for operations
such as backups or reporting.

• You can control when and how InnoDB performs read-ahead requests to **prefetch** pages into the
buffer pool asynchronously, in anticipation that the pages will be needed soon.

• You can control when background **flushing** of dirty pages occurs and whether or not InnoDB dynamically adjusts the rate of flushing based on workload.

• You can fine-tune aspects of InnoDB buffer pool **flushing behavior** to improve performance. 

• You can configure how InnoDB preserves the current buffer pool state to avoid a lengthy warmup period after a server restart. You can also save the current buffer pool state while the server is running.

##### InnoDB Buffer Pool LRU Algorithm

InnoDB manages the buffer pool as a **list**, using a variation of the least recently used (LRU) algorithm. When room is needed to add a new page to the pool, InnoDB evicts the least recently used page and adds the new page to the middle of the list. This “midpoint insertion strategy” treats the list as two sublists:

• At the head, a sublist of “new” (or “young”) pages that were accessed recently.
• At the tail, a sublist of “old” pages that were accessed less recently.

This algorithm keeps pages that are heavily used by queries in the new sublist. The old sublist contains less-used pages; these pages are candidates for eviction.

The LRU algorithm operates as follows by default:

• 3/8 of the buffer pool is devoted to the old sublist.
• The midpoint of the list is the boundary where the tail of the new sublist meets the head of the old sublist.
• When InnoDB reads a page into the buffer pool, it initially inserts it at the midpoint (the head of the old sublist). A page can be read in because it is required for a user-specified operation such as an SQL query, or as part of a read-ahead operation performed automatically by InnoDB.
• Accessing a page in the old sublist makes it “young”, moving it to the head of the buffer pool (the head of the new sublist). If the page was read in because it was required, the first access occurs immediately and the page is made young. If the page was read in due to read-ahead, the first access does not occur immediately (and might not occur at all before the page is evicted).
• As the database operates, pages in the buffer pool that are not accessed “age” by moving toward the tail of the list. Pages in both the new and old sublists age as other pages are made new. Pages in the old sublist also age as pages are inserted at the midpoint. Eventually, a page that remains unused for long enough reaches the tail of the old sublist and is evicted.

By default, pages read by queries immediately move into the new sublist, meaning they stay in the buffer pool longer. A table scan (such as performed for a mysqldump operation, or a SELECT statement with no WHERE clause) can bring a large amount of data into the buffer pool and evict an equivalent amount of older data, even if the new data is never used again. Similarly, pages that are loaded by the read-ahead background thread and then accessed only once move to the head of the new list. These situations can push frequently used pages to the old sublist, where they become subject to eviction.

InnoDB Standard Monitor output contains several fields in the BUFFER POOL AND MEMORY section that pertain to operation of the buffer pool LRU algorithm.

## InnoDB Buffer Pool Configuration

Several configuration options affect different aspects of the InnoDB buffer pool.

### • **innodb_buffer_pool_size**

Specifies the size of the buffer pool. If the buffer pool is small and you have sufficient memory, making the buffer pool larger can improve performance by reducing the amount of disk I/O needed as queries access InnoDB tables. The innodb_buffer_pool_size option is dynamic, you can configure InnoDB buffer pool size offline (at startup) or online, while the server is running, without restarting the server. Behavior described in this section applies to both methods. 

When increasing or decreasing innodb_buffer_pool_size, the operation is performed **in chunks**. Chunk size is defined by the innodb_buffer_pool_chunk_size configuration option, which has a default of 128M. 

Buffer pool size must always be equal to or a multiple of innodb_buffer_pool_chunk_size × innodb_buffer_pool_instances. If you configure innodb_buffer_pool_size to a value that is not equal to or a multiple of innodb_buffer_pool_chunk_size × innodb_buffer_pool_instances, buffer pool size is automatically adjusted to a value that is equal to or a multiple of innodb_buffer_pool_chunk_size × innodb_buffer_pool_instances that is not less than the specified buffer pool size.

In the following example, innodb_buffer_pool_size is set to 8G, and innodb_buffer_pool_instances is set to 16. innodb_buffer_pool_chunk_size is 128M, which is the default value. 8G is a valid innodb_buffer_pool_size value because 8G is a multiple of innodb_buffer_pool_instances=16 × innodb_buffer_pool_chunk_size=128M, which is 2G.

```shell
shell> mysqld --innodb_buffer_pool_size=8G --innodb_buffer_pool_instances=16
```

```sql
mysql> SELECT @@innodb_buffer_pool_size/1024/1024/1024;
+------------------------------------------+
| @@innodb_buffer_pool_size/1024/1024/1024 |
+------------------------------------------+
|                           8.000000000000 |
+------------------------------------------+
```

In this example, innodb_buffer_pool_size is set to 9G, and innodb_buffer_pool_instances is set to 16. innodb_buffer_pool_chunk_size is 128M, which is the default value. In this case, 9G is not a multiple of innodb_buffer_pool_instances=16 × innodb_buffer_pool_chunk_size=128M, so innodb_buffer_pool_size is
adjusted to 10G, which is the next multiple of innodb_buffer_pool_chunk_size × 

```shell
shell> mysqld --innodb_buffer_pool_size=9G --innodb_buffer_pool_instances=16
```

```sql
mysql> SELECT @@innodb_buffer_pool_size/1024/1024/1024;
+------------------------------------------+
| @@innodb_buffer_pool_size/1024/1024/1024 |
+------------------------------------------+
|                          10.000000000000 |
+------------------------------------------+
```

### • innodb_buffer_pool_chunk_size

innodb_buffer_pool_chunk_size can be increased or decreased in 1MB (1048576 byte) units but can only be modified at startup, in a command line string or in a MySQL configuration file.

```shell
# Command line:
shell> mysqld --innodb_buffer_pool_chunk_size=134217728
```

```properties
# Configuration file:
[mysqld]
innodb_buffer_pool_chunk_size=134217728
```

The following conditions apply when altering innodb_buffer_pool_chunk_size:

• If the new innodb_buffer_pool_chunk_size value × innodb_buffer_pool_instances is larger than the current buffer pool size when the buffer pool is initialized, innodb_buffer_pool_chunk_size is truncated to innodb_buffer_pool_size / innodb_buffer_pool_instances.

For example, if the buffer pool is initialized with a size of 2GB (2147483648 bytes), 4 buffer pool instances, and a chunk size of 1GB (1073741824 bytes), chunk size is truncated to a value equal to
innodb_buffer_pool_size / innodb_buffer_pool_instances, as shown below:

```shell
shell> mysqld --innodb_buffer_pool_size=2147483648 --innodb_buffer_pool_instances=4
--innodb_buffer_pool_chunk_size=1073741824;
```

```sql
mysql> SELECT @@innodb_buffer_pool_size;
+---------------------------+
| @@innodb_buffer_pool_size |
+---------------------------+
| 				 2147483648 |
+---------------------------+
mysql> SELECT @@innodb_buffer_pool_instances;
+--------------------------------+
| @@innodb_buffer_pool_instances |
+--------------------------------+
| 							   4 |
+--------------------------------+
```

Chunk size was set to 1GB (1073741824 bytes) on startup but was truncated to innodb_buffer_pool_size / innodb_buffer_pool_instances:

```sql
mysql> SELECT @@innodb_buffer_pool_chunk_size;
+---------------------------------+
| @@innodb_buffer_pool_chunk_size |
+---------------------------------+
| 						536870912 |
+---------------------------------+
```

• Buffer pool size must always be equal to or a multiple of innodb_buffer_pool_chunk_size × innodb_buffer_pool_instances. If you alter innodb_buffer_pool_chunk_size, innodb_buffer_pool_size is automatically adjusted to a value that is equal to or a multiple of innodb_buffer_pool_chunk_size × innodb_buffer_pool_instances that is not less than current buffer pool size. The adjustment occurs when the buffer pool is initialized. This behavior is demonstrated in the following example:

The buffer pool has a default size of 128MB (134217728 bytes):

```sql
mysql> SELECT @@innodb_buffer_pool_size;
+---------------------------+
| @@innodb_buffer_pool_size |
+---------------------------+
| 				  134217728 |
+---------------------------+
```

The chunk size is also 128MB (134217728 bytes):

```sql
mysql> SELECT @@innodb_buffer_pool_chunk_size;
+---------------------------------+
| @@innodb_buffer_pool_chunk_size |
+---------------------------------+
| 						134217728 |
+---------------------------------+
```

There is a single buffer pool instance:

```sql
mysql> SELECT @@innodb_buffer_pool_instances;
+--------------------------------+
| @@innodb_buffer_pool_instances |
+--------------------------------+
| 							   1 |
+--------------------------------+
```

Chunk size is decreased by 1MB (1048576 bytes) at startup (134217728 - 1048576 = 133169152):

```shell
shell> mysqld --innodb_buffer_pool_chunk_size=133169152
```

```sql
mysql> SELECT @@innodb_buffer_pool_chunk_size;
+---------------------------------+
| @@innodb_buffer_pool_chunk_size |
+---------------------------------+
| 						133169152 |
+---------------------------------+
```

Buffer pool size increases from 134217728 to 266338304, Buffer pool size is automatically adjusted to a value that is equal to or a multiple of innodb_buffer_pool_chunk_size * innodb_buffer_pool_instances that is not less than current buffer pool size:

```sql
mysql> SELECT @@innodb_buffer_pool_size;
+---------------------------+
| @@innodb_buffer_pool_size |
+---------------------------+
| 				  266338304 |
+---------------------------+
```

This example demonstrates the same behavior but with multiple buffer pool instances:

The buffer pool has a default size of 2GB (2147483648 bytes):

```sql
mysql> SELECT @@innodb_buffer_pool_size;
+---------------------------+
| @@innodb_buffer_pool_size |
+---------------------------+
| 				 2147483648 |
+---------------------------+
```

The chunk size is .5 GB (536870912 bytes):

```sql
mysql> SELECT @@innodb_buffer_pool_chunk_size;
+---------------------------------+
| @@innodb_buffer_pool_chunk_size |
+---------------------------------+
| 						536870912 |
+---------------------------------+
```

There are 4 buffer pool instances:

```sql
mysql> SELECT @@innodb_buffer_pool_instances;
+--------------------------------+
| @@innodb_buffer_pool_instances |
+--------------------------------+
| 							   4 |
+--------------------------------+
```

Chunk size is decreased by 1MB (1048576 bytes) at startup (536870912 - 1048576 = 535822336):

```shell
shell> mysqld --innodb_buffer_pool_chunk_size=535822336
```

```sql
mysql> SELECT @@innodb_buffer_pool_chunk_size;
+---------------------------------+
| @@innodb_buffer_pool_chunk_size |
+---------------------------------+
| 						535822336 |
+---------------------------------+
```

Buffer pool size increases from 2147483648 to 4286578688. Buffer pool size is automatically adjusted to a value that is equal to or a multiple of innodb_buffer_pool_chunk_size × innodb_buffer_pool_instances that is not less than current buffer pool size of 2147483648:

```sql
mysql> SELECT @@innodb_buffer_pool_size;
+---------------------------+
| @@innodb_buffer_pool_size |
+---------------------------+
| 				 4286578688 |
+---------------------------+
```

Care should be taken when changing innodb_buffer_pool_chunk_size, as changing this value can increase the size of the buffer pool, as shown in the examples above. Before you change innodb_buffer_pool_chunk_size, calculate the effect on innodb_buffer_pool_size to ensure that the resulting buffer pool size is acceptable.

> **Note**
> To avoid potential performance issues, the number of chunks (innodb_buffer_pool_size / innodb_buffer_pool_chunk_size) **should not exceed 1000**.

**Configuring InnoDB Buffer Pool Size Online**

The innodb_buffer_pool_size configuration option can be set dynamically using a SET statement, allowing you to resize the buffer pool without restarting the server. For example:

```sql
mysql> SET GLOBAL innodb_buffer_pool_size=402653184;
```

Active transactions and operations performed through InnoDB APIs should be completed before resizing the buffer pool. When initiating a resizing operation, the operation does not start until all active transactions are completed. Once the resizing operation is in progress, new transactions and operations that require access to the buffer pool must wait until the resizing operation finishes. The exception to the rule is that concurrent access to the buffer pool is permitted while the buffer pool is defragmented and pages are withdrawn when buffer pool size is decreased. A drawback of allowing concurrent access is that it could result in a temporary shortage of available pages while pages are being withdrawn.

> **Note**
> Nested transactions could fail if initiated after the buffer pool resizing operation begins

**Monitoring Online Buffer Pool Resizing Progress**

The Innodb_buffer_pool_resize_status reports buffer pool resizing progress. For example:

```sql
mysql> SHOW STATUS WHERE Variable_name='InnoDB_buffer_pool_resize_status';
+----------------------------------+----------------------------------+
| 					 Variable_name |						    Value |
+----------------------------------+----------------------------------+
| Innodb_buffer_pool_resize_status | Resizing also other hash tables. |
+----------------------------------+----------------------------------+
```

Buffer pool resizing progress is also logged in the server error log. This example shows notes that are logged when increasing the size of the buffer pool:

> [Note] InnoDB: Resizing buffer pool from 134217728 to 4294967296. (unit=134217728)
> [Note] InnoDB: disabled adaptive hash index.
> [Note] InnoDB: buffer pool 0 : 31 chunks (253952 blocks) was added.
> [Note] InnoDB: buffer pool 0 : hash tables were resized.
> [Note] InnoDB: Resized hash tables at lock_sys, adaptive hash index, dictionary.
> [Note] InnoDB: completed to resize buffer pool from 134217728 to 4294967296.
> [Note] InnoDB: re-enabled adaptive hash index.

This example shows notes that are logged when decreasing the size of the buffer pool:

> [Note] InnoDB: Resizing buffer pool from 4294967296 to 134217728. (unit=134217728)
> [Note] InnoDB: disabled adaptive hash index.
> [Note] InnoDB: buffer pool 0 : start to withdraw the last 253952 blocks.
> [Note] InnoDB: buffer pool 0 : withdrew 253952 blocks from free list. tried to relocate 0 pages.
> (253952/253952)
> [Note] InnoDB: buffer pool 0 : withdrawn target 253952 blocks.
> [Note] InnoDB: buffer pool 0 : 31 chunks (253952 blocks) was freed.
> [Note] InnoDB: buffer pool 0 : hash tables were resized.
> [Note] InnoDB: Resized hash tables at lock_sys, adaptive hash index, dictionary.
> [Note] InnoDB: completed to resize buffer pool from 4294967296 to 134217728.
> [Note] InnoDB: re-enabled adaptive hash index.

**Online Buffer Pool Resizing Internals**

The resizing operation is performed by a background thread. When increasing the size of the buffer pool, the resizing operation:

• Adds pages in chunks (chunk size is defined by innodb_buffer_pool_chunk_size)
• Converts hash tables, lists, and pointers to use new addresses in memory
• Adds new pages to the free list

While these operations are in progress, other threads are blocked from accessing the buffer pool. When decreasing the size of the buffer pool, the resizing operation:

• Defragments the buffer pool and withdraws (frees) pages
• Removes pages in chunks (chunk size is defined by innodb_buffer_pool_chunk_size)
• Converts hash tables, lists, and pointers to use new addresses in memory

Of these operations, only defragmenting the buffer pool and withdrawing pages allow other threads to access to the buffer pool concurrently.

### • innodb_buffer_pool_instances

For systems with buffer pools in the multi-gigabyte range, dividing the buffer pool into separate instances can improve concurrency, by reducing contention as different threads read and write to cached pages. This feature is typically intended for systems with a buffer pool size in the multi-gigabyte range. Multiple buffer pool instances are configured using the **innodb_buffer_pool_instances** configuration option, and you might also adjust the innodb_buffer_pool_size value.

When the InnoDB buffer pool is large, many data requests can be satisfied by retrieving from memory. You might encounter bottlenecks from multiple threads trying to access the buffer pool at once. You can enable multiple buffer pools to minimize this contention. Each page that is stored in or read from the buffer pool is assigned to one of the buffer pools randomly, using a hashing function. Each buffer pool manages its own free lists, flush lists, LRUs, and all other data structures connected to a buffer pool, and is protected by its own buffer pool mutex.

To enable multiple buffer pool instances, set the innodb_buffer_pool_instances configuration option to a value greater than 1 (the default) up to 64 (the maximum). This option takes effect only when you set innodb_buffer_pool_size to a size of 1GB or more. The total size you specify is divided among all the buffer pools. For best efficiency, specify a combination of innodb_buffer_pool_instances and innodb_buffer_pool_size so that each buffer pool instance is at least 1GB.

### • innodb_old_blocks_pct && innodb_old_blocks_time - Making the Buffer Pool Scan Resistant

Divides the buffer pool into a user-specified number of separate regions, each with its own LRU list and related data structures, to reduce contention during concurrent memory read and write operations. This option only takes effect when you set innodb_buffer_pool_size to a value of 1GB or more. The total size you specify is divided among all the buffer pools. For best efficiency, specify a combination of innodb_buffer_pool_instances and innodb_buffer_pool_size so that each buffer pool instance is at least 1 gigabyte. 

Rather than using a strict LRU algorithm, InnoDB uses a technique to minimize the amount of data that is brought into the buffer pool and never accessed again. The goal is to make sure that frequently accessed (“hot”) pages remain in the buffer pool, even as read-ahead and full table scans bring in new blocks that might or might not be accessed afterward.

Newly read blocks are inserted into the middle of the LRU list. All newly read pages are inserted at a location that by default is 3/8 from the tail of the LRU list. The pages are moved to the front of the list (the most-recently used end) when they are accessed in the buffer pool for the first time. Thus, pages that are never accessed never make it to the front portion of the LRU list, and “age out” sooner than with a strict LRU approach. This arrangement divides the LRU list into two segments, where the pages downstream of the insertion point are considered “old” and are desirable victims for LRU eviction.

You can control the insertion point in the LRU list and choose whether InnoDB applies the same optimization to blocks brought into the buffer pool by table or index scans. The configuration parameter
innodb_old_blocks_pct controls the percentage of “old” blocks in the LRU list. The default value of
innodb_old_blocks_pct is 37, corresponding to the original fixed ratio of 3/8. The value range is 5 (new pages in the buffer pool age out very quickly) to 95 (only 5% of the buffer pool is reserved for hot
pages, making the algorithm close to the familiar LRU strategy).

The optimization that keeps the buffer pool from being churned by read-ahead can avoid similar problems due to table or index scans. In these scans, a data page is typically accessed a few times in quick succession and is never touched again. The configuration parameter innodb_old_blocks_time specifies the time window (in milliseconds) after the first access to a page during which it can be accessed without being moved to the front (most-recently used end) of the LRU list. The default value of innodb_old_blocks_time is 1000. Increasing this value makes more and more blocks likely to age out faster from the buffer pool.

Both **innodb_old_blocks_pct** and **innodb_old_blocks_time** are dynamic, global and can be specified in the MySQL option file (my.cnf or my.ini) or changed at runtime with the SET GLOBAL command. Changing the setting requires the SUPER privilege.

To help you gauge the effect of setting these parameters, the SHOW ENGINE INNODB STATUS command reports buffer pool statistics.

Because the effects of these parameters can vary widely based on your hardware configuration, your data, and the details of your workload, always benchmark to verify the effectiveness before changing these settings in any performance-critical or production environment.

In mixed workloads where most of the activity is OLTP type with periodic batch reporting queries which result in large scans, setting the value of innodb_old_blocks_time during the batch runs can help keep the working set of the normal workload in the buffer pool.

When scanning large tables that cannot fit entirely in the buffer pool, setting innodb_old_blocks_pct to a small value keeps the data that is only read once from consuming a significant portion of the buffer pool. For example, setting innodb_old_blocks_pct=5 restricts this data that is only read once to 5% of the buffer pool.

When scanning small tables that do fit into memory, there is less overhead for moving pages around within the buffer pool, so you can leave innodb_old_blocks_pct at its default value, or even higher, such as innodb_old_blocks_pct=50.

The effect of the innodb_old_blocks_time parameter is harder to predict than the innodb_old_blocks_pct parameter, is relatively small, and varies more with the workload. To arrive at an optimal value, conduct your own benchmarks if the performance improvement from adjusting innodb_old_blocks_pct is not sufficient.

### • innodb_read_ahead_threshold && innodb_random_read_ahead - Configuring InnoDB Buffer Pool Prefetching (Read-Ahead)

A read-ahead request is an I/O request to prefetch multiple pages in the buffer pool asynchronously, in anticipation that these pages will be needed soon. The requests bring in all the pages in one extent. InnoDB uses two read-ahead algorithms to improve I/O performance:

**Linear read-ahead** is a technique that predicts what pages might be needed soon based on pages in the buffer pool being accessed sequentially. You control when InnoDB performs a read-ahead operation by adjusting the number of sequential page accesses required to trigger an asynchronous read request, using the configuration parameter innodb_read_ahead_threshold. Before this parameter was added, InnoDB would only calculate whether to issue an asynchronous prefetch request for the entire next extent when it read in the last page of the current extent.

The configuration parameter **innodb_read_ahead_threshold** controls how sensitive InnoDB is in detecting patterns of sequential page access. If the number of pages read sequentially from an extent is greater than or equal to innodb_read_ahead_threshold, InnoDB initiates an asynchronous read-ahead operation of the entire following extent. innodb_read_ahead_threshold can be set to any value from 0-64. The default value is 56. The higher the value, the more strict the access pattern check. For example, if you set the value to 48, InnoDB triggers a linear read-ahead request only when 48 pages in the current extent have been accessed sequentially. If the value is 8, InnoDB triggers an asynchronous read-ahead even if as few as 8 pages in the extent are accessed sequentially. You can set the value of this parameter in the MySQL configuration file, or change it dynamically with the SET GLOBAL command, which requires the SUPER privilege.

**Random read-ahead** is a technique that predicts when pages might be needed soon based on pages already in the buffer pool, regardless of the order in which those pages were read. If 13 consecutive pages from the same extent are found in the buffer pool, InnoDB asynchronously issues a request to prefetch the remaining pages of the extent. To enable this feature, set the configuration variable **innodb_random_read_ahead** to ON.

The SHOW ENGINE INNODB STATUS command displays statistics to help you evaluate the effectiveness of the read-ahead algorithm. Statistics include counter information for the following global status variables:

• Innodb_buffer_pool_read_ahead
• Innodb_buffer_pool_read_ahead_evicted
• Innodb_buffer_pool_read_ahead_rnd

This information can be useful when fine-tuning the innodb_random_read_ahead setting.

### • innodb_adaptive_flushing - Configuring InnoDB Buffer Pool Flushing

InnoDB performs certain tasks in the background, including flushing of dirty pages (those pages that have been changed but are not yet written to the database files) from the buffer pool. InnoDB starts flushing buffer pool pages when the percentage of dirty pages in the buffer pool reaches the low water mark setting defined by **innodb_max_dirty_pages_pct_lwm**. This option is intended to control the ratio of dirty pages in the buffer pool and ideally prevent the percentage of dirty pages from reaching innodb_max_dirty_pages_pct. If the percentage of dirty pages in the buffer pool exceeds innodb_max_dirty_pages_pct, InnoDB begins to aggressively flush buffer pool pages. InnoDB uses an algorithm to estimate the required rate of flushing, based on the speed of redo log generation and the current rate of flushing. The intent is to smooth overall performance by ensuring that buffer flush activity keeps up with the need to keep the buffer pool “clean”. Automatically adjusting the rate of flushing can help to avoid sudden dips in throughput, when excessive buffer pool flushing limits the I/O capacity available for ordinary read and write activity.

InnoDB uses its log files in a circular fashion. Before reusing a portion of a log file, InnoDB flushes to disk all dirty buffer pool pages whose redo entries are contained in that portion of the log file, a process known as a sharp checkpoint. If a workload is write-intensive, it generates a lot of redo information, all written to the log file. If all available space in the log files is used up, a sharp checkpoint occurs, causing a temporary reduction in throughput. This situation can happen even if innodb_max_dirty_pages_pct is not reached.

InnoDB uses a heuristic-based algorithm to avoid such a scenario, by measuring the number of dirty pages in the buffer pool and the rate at which redo is being generated. Based on these numbers, InnoDB decides how many dirty pages to flush from the buffer pool each second. This self-adapting algorithm is able to deal with sudden changes in workload.

Internal benchmarking has shown that this algorithm not only maintains throughput over time, but can also improve overall throughput significantly.

Because adaptive flushing can significantly affect the I/O pattern of a workload, the **innodb_adaptive_flushing** configuration parameter lets you turn off this feature. The default value for innodb_adaptive_flushing is ON, enabling the adaptive flushing algorithm. You can set the value of this parameter in the MySQL option file (my.cnf or my.ini) or change it dynamically with the SET GLOBAL command, which requires the SUPER privilege.

### •  innodb_adaptive_flushing_lwm && innodb_flush_neighbors && innodb_flushing_avg_loops && innodb_lru_scan_depth && innodb_max_dirty_pages_pct && innodb_max_dirty_pages_pct_lwm - Fine-tuning InnoDB Buffer Pool Flushing

The configuration options innodb_flush_neighbors and innodb_lru_scan_depth let you finetune aspects of the flushing process for the InnoDB buffer pool.

• innodb_flush_neighbors

Specifies whether flushing a page from the buffer pool also flushes other dirty pages in the same extent. When the table data is stored on a traditional HDD storage device, flushing neighbor pages in one operation reduces I/O overhead (primarily for disk seek operations) compared to flushing individual pages at different times. For table data stored on SSD, seek time is not a significant factor and you can disable this setting to spread out write operations.

• innodb_lru_scan_depth

Specifies, per buffer pool instance, how far down the buffer pool LRU list the page cleaner thread scans looking for dirty pages to flush. This is a background operation performed once per second.

These options primarily help write-intensive workloads. With heavy DML activity, flushing can fall behind if it is not aggressive enough, resulting in excessive memory use in the buffer pool; or, disk writes due to flushing can saturate your I/O capacity if that mechanism is too aggressive. The ideal settings depend on your workload, data access patterns, and storage configuration (for example, whether data is stored on HDD or SSD devices).

For systems with constant heavy workloads, or workloads that fluctuate widely, several configuration options let you fine-tune the flushing behavior for InnoDB tables:

• innodb_adaptive_flushing_lwm
• innodb_max_dirty_pages_pct_lwm
• innodb_io_capacity_max
• innodb_flushing_avg_loops

These options feed into the formula used by the innodb_adaptive_flushing option. The innodb_adaptive_flushing, innodb_io_capacity and innodb_max_dirty_pages_pct options are limited or extended by the following options:

• innodb_adaptive_flushing_lwm
• innodb_io_capacity_max
• innodb_max_dirty_pages_pct_lwm

The InnoDB adaptive flushing mechanism is not appropriate in all cases. It gives the most benefit when the redo log is in danger of filling up. The innodb_adaptive_flushing_lwm option specifies a “low water mark” percentage of redo log capacity; when that threshold is crossed, InnoDB turns on adaptive flushing even if not specified by the innodb_adaptive_flushing option.

If flushing activity falls far behind, InnoDB can flush more aggressively than specified by innodb_io_capacity. innodb_io_capacity_max represents an upper limit on the I/O capacity used in such emergency situations, so that the spike in I/O does not consume all the capacity of the server.

InnoDB tries to flush data from the buffer pool so that the percentage of dirty pages does not exceed the value of innodb_max_dirty_pages_pct. The default value for innodb_max_dirty_pages_pct is 75.

> Note
> The innodb_max_dirty_pages_pct setting establishes a target for flushing activity. It does not affect the rate of flushing.

The innodb_max_dirty_pages_pct_lwm option specifies a “low water mark” value that represents the percentage of dirty pages where pre-flushing is enabled to control the dirty page ratio and ideally prevent the percentage of dirty pages from reaching innodb_max_dirty_pages_pct. A value of innodb_max_dirty_pages_pct_lwm=0 disables the “pre-flushing” behavior.

Most of the options referenced above are most applicable to servers that run write-heavy workloads for long periods of time and have little reduced load time to catch up with changes waiting to be written to disk.

innodb_flushing_avg_loops defines the number of iterations for which InnoDB keeps the previously calculated snapshot of the flushing state, which controls how quickly adaptive flushing responds to foreground load changes. Setting a high value for innodb_flushing_avg_loops means that InnoDB keeps the previously calculated snapshot longer, so adaptive flushing responds more slowly. A high value also reduces positive feedback between foreground and background work, but when setting a high value it is important to ensure that InnoDB redo log utilization does not reach 75% (the hardcoded limit at which async flushing starts) and that the innodb_max_dirty_pages_pct setting keeps the number of dirty pages to a level that is appropriate for the workload.

Systems with consistent workloads, a large innodb_log_file_size, and small spikes that do not reach 75% redo log space utilization should use a high innodb_flushing_avg_loops value to keep flushing as smooth as possible. For systems with extreme load spikes or log files that do not provide a lot of space, consider a smaller innodb_flushing_avg_loops value. A smaller value allows flushing to closely track the load and helps avoid reaching 75% redo log space utilization.

### • innodb_buffer_pool_filename && innodb_buffer_pool_dump_at_shutdown && innodb_buffer_pool_load_at_startup && innodb_buffer_pool_dump_now &&  innodb_buffer_pool_load_now && innodb_buffer_pool_dump_pct && innodb_buffer_pool_load_abort - Saving and Restoring the Buffer Pool State

To reduce the warmup period after restarting the server, InnoDB saves a percentage of the most recently used pages for each buffer pool at server shutdown and restores these pages at server startup. The percentage of recently used pages that is stored is defined by the innodb_buffer_pool_dump_pct configuration option.

After restarting a busy server, there is typically a warmup period with steadily increasing throughput, as disk pages that were in the buffer pool are brought back into memory (as the same data is queried, updated, and so on). The ability to restore the buffer pool at startup shortens the warmup period by reloading disk pages that were in the buffer pool before the restart rather than waiting for DML operations to access corresponding rows. Also, I/O requests can be performed in large batches, making the overall I/O faster. Page loading happens in the background, and does not delay database startup.

In addition to saving the buffer pool state at shutdown and restoring it at startup, you can save and restore the buffer pool state at any time, while the server is running. For example, you can save the state of the buffer pool after reaching a stable throughput under a steady workload. You could also restore the previous buffer pool state after running reports or maintenance jobs that bring data pages into the buffer pool that are only requited for those operations, or after running some other non-typical workload.

Even though a buffer pool can be many gigabytes in size, the buffer pool data that InnoDB saves to disk is tiny by comparison. Only tablespace IDs and page IDs necessary to locate the appropriate pages are saved to disk. This information is derived from the INNODB_BUFFER_PAGE_LRU INFORMATION_SCHEMA table. By default, tablespace ID and page ID data is saved in a file named ib_buffer_pool, which is saved to the InnoDB data directory. The file name and location can be modified using the **innodb_buffer_pool_filename** configuration parameter.

Because data is cached in and aged out of the buffer pool as it is with regular database operations, there is no problem if the disk pages are recently updated, or if a DML operation involves data that has not yet been loaded. The loading mechanism skips requested pages that no longer exist. The underlying mechanism involves a background thread that is dispatched to perform the dump and load operations.

Disk pages from compressed tables are loaded into the buffer pool in their compressed form. Pages are uncompressed as usual when page contents are accessed during DML operations. Because uncompressing pages is a CPU-intensive process, it is more efficient for concurrency to perform the operation in a connection thread rather than in the single thread that performs the buffer pool restore operation.

**Configuring the Dump Percentage for Buffer Pool Pages**

Before dumping pages from the buffer pool, you can configure the percentage of most-recently used buffer pool pages that you want to dump by setting the innodb_buffer_pool_dump_pct option. If you plan to dump buffer pool pages while the server is running, you can configure the option dynamically:

```sql
SET GLOBAL innodb_buffer_pool_dump_pct=40;
```

If you plan to dump buffer pool pages at server shutdown, set innodb_buffer_pool_dump_pct in your configuration file.

```properties
[mysqld]
innodb_buffer_pool_dump_pct=40
```

The innodb_buffer_pool_dump_pct default value was changed from 100 (dump all pages) to 25 (dump 25% of most-recently-used pages) in MySQL 5.7 when innodb_buffer_pool_dump_at_shutdown and innodb_buffer_pool_load_at_startup were enabled by default.

**Saving the Buffer Pool State at Shutdown and Restoring it at Startup**

To save the state of the buffer pool at server shutdown, issue the following statement prior to shutting down the server:

```sql
SET GLOBAL innodb_buffer_pool_dump_at_shutdown=ON;
```

innodb_buffer_pool_dump_at_shutdown is enabled by default.

To restore the buffer pool state at server startup, specify the --innodb_buffer_pool_load_at_startup option when starting the server:

```shell
mysqld --innodb_buffer_pool_load_at_startup=ON;
```

innodb_buffer_pool_load_at_startup is enabled by default.

**Saving and Restoring the Buffer Pool State Online**

To save the state of the buffer pool while MySQL server is running, issue the following statement:

```sql
SET GLOBAL innodb_buffer_pool_dump_now=ON;
```

To restore the buffer pool state while MySQL is running, issue the following statement:

```sql
SET GLOBAL innodb_buffer_pool_load_now=ON;
```

**Displaying Buffer Pool Dump Progress**

To display progress when saving the buffer pool state to disk, issue the following statement:

```sql
SHOW STATUS LIKE 'Innodb_buffer_pool_dump_status';
```

If the operation has not yet started, “not started” is returned. If the operation is complete, the completion time is printed (e.g. Finished at 110505 12:18:02). If the operation is in progress, status information is provided (e.g. Dumping buffer pool 5/7, page 237/2873).

**Displaying Buffer Pool Load Progress**

To display progress when loading the buffer pool, issue the following statement:

```sql
SHOW STATUS LIKE 'Innodb_buffer_pool_load_status';
```

If the operation has not yet started, “not started” is returned. If the operation is complete, the completion time is printed (e.g. Finished at 110505 12:23:24). If the operation is in progress, status information is provided (e.g. Loaded 123/22301 pages).

**Aborting a Buffer Pool Load Operation**

To abort a buffer pool load operation, issue the following statement:

```sql
SET GLOBAL innodb_buffer_pool_load_abort=ON;
```