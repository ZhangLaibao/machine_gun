# Introduction to InnoDB

InnoDB is a general-purpose storage engine that **balances high reliability and high performance**. In MySQL 5.7, InnoDB is the default MySQL storage engine. Unless you have configured a different default storage engine, issuing a CREATE TABLE statement without an ENGINE= clause creates an InnoDB table.

**Key Advantages of InnoDB**

• Its DML operations follow the ACID model, with transactions featuring commit, rollback, and crashrecovery capabilities to protect user data.

• Row-level locking and Oracle-style consistent reads increase multi-user concurrency and performance.

• InnoDB tables arrange your data on disk to optimize queries based on primary keys. Each InnoDB table has a primary key index called the clustered index that organizes the data to minimize I/O for primary key lookups.

• To maintain data integrity, InnoDB supports FOREIGN KEY constraints. With foreign keys, inserts, updates, and deletes are checked to ensure they do not result in inconsistencies across different tables.

InnoDB Storage Engine Features

| Storage limits                             | 64TB            | Transactions                    | Yes             | Locking granularity                   | Row             |
| ------------------------------------------ | --------------- | ------------------------------- | --------------- | ------------------------------------- | --------------- |
| MVCC                                       | Yes             | Geospatial data type support    | Yes             | Geospatial indexing support           | Yes<sup>a</sup> |
| B-tree indexes                             | Yes             | T-tree indexes                  | No              | Hash indexes                          | No<sup>b</sup>  |
| Full-text search indexes                   | Yes<sup>c</sup> | Clustered indexes               | Yes             | Data caches                           | Yes             |
| Index caches                               | Yes             | Compressed data                 | Yes<sup>d</sup> | Encrypted data<sup>e</sup>            | Yes             |
| Cluster database support                   | No              | Replication support<sup>f</sup> | Yes             | Foreign key support                   | Yes             |
| Backup / point-intime recovery<sup>g</sup> | Yes             | Query cache support             | Yes             | Update statistics for data dictionary | Yes             |

a:InnoDB support for geospatial indexing is available in MySQL 5.7.5 and later.
b:InnoDB utilizes hash indexes internally for its Adaptive Hash Index feature.
c:InnoDB support for FULLTEXT indexes is available in MySQL 5.6.4 and later.
d:Compressed InnoDB tables require the InnoDB Barracuda file format.
e:Implemented in the server (via encryption functions). Data-at-rest tablespace encryption is available in MySQL 5.7 and later.
f:Implemented in the server, rather than in the storage engine.
g:Implemented in the server, rather than in the storage engine.

## Benefits of Using InnoDB Tables - 优势

If you use MyISAM tables but are not committed to them for technical reasons, you may find InnoDB tables beneficial for the following reasons:

• If your server crashes because of a hardware or software issue, regardless of what was happening in the database at the time, you don't need to do anything special after restarting the database. InnoDB **crash recovery** automatically finalizes any changes that were committed before the time of the crash, and undoes any changes that were in process but not committed. Just restart and continue where you left off.

• The InnoDB storage engine maintains its own **buffer pool** that caches table and index data in main memory as data is accessed. Frequently used data is processed directly from memory. This cache applies to many types of information and speeds up processing. On dedicated database servers, up to 80% of physical memory is often assigned to the InnoDB buffer pool.

• If you split up related data into different tables, you can set up **foreign keys** that enforce referential integrity. Update or delete data, and the related data in other tables is updated or deleted automatically. Try to insert data into a secondary table without corresponding data in the primary table, and the bad data gets kicked out automatically.

• If data becomes corrupted on disk or in memory, a **checksum mechanism** alerts you to the bogus data before you use it.

• When you design your database with appropriate primary key columns for each table, operations involving those columns are **automatically optimized**. It is very fast to reference the primary key columns in WHERE clauses, ORDER BY clauses, GROUP BY clauses, and join operations.

• Inserts, updates, and deletes are optimized by an automatic mechanism called **change buffering**. InnoDB not only allows concurrent read and write access to the same table, it caches changed data to streamline disk I/O.

• Performance benefits are not limited to giant tables with long-running queries. When the same rows are accessed over and over from a table, a feature called the **Adaptive Hash Index** takes over to make these lookups even faster, as if they came out of a hash table.

• You can **compress** tables and associated indexes.

• You can create and drop indexes with much less impact on performance and availability.

• Truncating a file-per-table tablespace is very fast, and can free up disk space for the operating system to reuse, rather than freeing up space within the system tablespace that only InnoDB could reuse.

• The storage layout for table data is more efficient for **BLOB and long text fields**, with the DYNAMIC
row format.

• You can monitor the internal workings of the storage engine by querying **INFORMATION_SCHEMA** tables.

• You can monitor the performance details of the storage engine by querying **Performance Schema** tables.

• You can freely **mix** InnoDB tables with tables from other MySQL storage engines, even within the same statement. For example, you can use a join operation to combine data from InnoDB and MEMORY tables in a single query.

• InnoDB has been designed for **CPU efficiency** and maximum performance when processing large data volumes.

• InnoDB tables can **handle large quantities of data**, even on operating systems where file size is limited to 2GB.

## Best Practices for InnoDB Tables

• Specifying a **primary key** for every table using the most frequently queried column or columns, or an auto-increment value if there is no obvious primary key.

• Using joins wherever data is pulled from multiple tables based on identical ID values from those tables. For **fast join performance**, define foreign keys on the join columns, and declare those columns with the same data type in each table. Adding foreign keys ensures that referenced columns are indexed, which can improve performance. Foreign keys also propagate deletes or updates to all affected tables, and prevent insertion of data in a child table if the corresponding IDs are not present in the parent table.

• **Turning off autocommit**. Committing hundreds of times a second puts a cap on performance (limited
by the write speed of your storage device).

• Grouping sets of related DML operations into **transactions**, by bracketing them with START TRANSACTION and COMMIT statements. While you don't want to commit too often, you also don't want to issue huge batches of INSERT, UPDATE, or DELETE statements that run for hours without committing.

• Not using **LOCK TABLES** statements. InnoDB can handle multiple sessions all reading and writing to the same table at once, without sacrificing reliability or high performance. To get exclusive write access to a set of rows, use the SELECT ... FOR UPDATE syntax to lock just the rows you intend to update.

• Enabling the **innodb_file_per_table** option to put the data and indexes for individual tables into separate files, instead of in a single giant system tablespace. This setting is required to use some of the other features, such as table compression and fast truncation.

• Evaluating whether your data and access patterns benefit from the InnoDB table **compression** feature (ROW_FORMAT=COMPRESSED) on the CREATE TABLE statement. You can compress InnoDB tables without sacrificing read/write capability.

• Running your server with the option **--sql_mode=NO_ENGINE_SUBSTITUTION** to prevent tables being created with a different storage engine if there is an issue with the engine specified in the ENGINE= clause of CREATE TABLE.

## Testing and Benchmarking with InnoDB

If InnoDB is not your default storage engine, you can determine if your database server or applications work correctly with InnoDB by restarting the server with --default-storage-engine=InnoDB defined on the command line or with default-storage-engine=innodb defined in the [mysqld] section of the my.cnf configuration file.

Since changing the default storage engine only affects new tables as they are created, run all your application installation and setup steps to confirm that everything installs properly. Then exercise all the application features to make sure all the data loading, editing, and querying features work. If a table relies on some MyISAM-specific feature, you'll receive an error; add the ENGINE=MyISAM clause to the CREATE TABLE statement to avoid the error.

If you did not make a deliberate decision about the storage engine, and you just want to preview how certain tables work when they're created under InnoDB, issue the command ALTER TABLE table_name ENGINE=InnoDB; for each table. Or, to run test queries and other statements without disturbing the original table, make a copy like so:

```sql
CREATE TABLE InnoDB_Table (...) ENGINE=InnoDB AS SELECT * FROM MyISAM_Table;
```

To get a true idea of the performance with a full application under a realistic workload, install the latest MySQL server and run benchmarks.

Test the full application lifecycle, from installation, through heavy usage, and server restart. Kill the server process while the database is busy to simulate a power failure, and verify that the data is recovered successfully when you restart the server.

Test any replication configurations, especially if you use different MySQL versions and options on the
master and the slaves.

# InnoDB and the ACID Model

The ACID model is a set of database design principles that emphasize aspects of reliability that are important for business data and mission-critical applications. MySQL includes components such as the InnoDB storage engine that adhere closely to the ACID model, so that data is not corrupted and results are not distorted by exceptional conditions such as software crashes and hardware malfunctions. When you rely on ACID-compliant features, you do not need to reinvent the wheel of consistency checking and crash recovery mechanisms. In cases where you have additional software safeguards, ultra-reliable hardware, or an application that can tolerate a small amount of data loss or inconsistency, you can adjust MySQL settings to trade some of the ACID reliability for greater performance or throughput.

**Atomicity**

The atomicity aspect of the ACID model mainly involves InnoDB transactions. Related MySQL features include:

• Autocommit setting.

• COMMIT statement.

• ROLLBACK statement.

• Operational data from the INFORMATION_SCHEMA tables.

**Consistency**

The consistency aspect of the ACID model mainly involves internal InnoDB processing to protect data from crashes. Related MySQL features include:

• InnoDB doublewrite buffer.

• InnoDB crash recovery.

**Isolation**

The isolation aspect of the ACID model mainly involves InnoDB transactions, in particular the isolation level that applies to each transaction. Related MySQL features include:

• Autocommit setting.

• SET ISOLATION LEVEL statement.

• The low-level details of InnoDB locking. During performance tuning, you see these details through INFORMATION_SCHEMA tables.

**Durability**

The durability aspect of the ACID model involves MySQL software features interacting with your particular hardware configuration. Because of the many possibilities depending on the capabilities of your CPU, network, and storage devices, this aspect is the most complicated to provide concrete guidelines for. (And those guidelines might take the form of buy “new hardware”.) Related MySQL features include:

• InnoDB doublewrite buffer, turned on and off by the innodb_doublewrite configuration option.

• Configuration option innodb_flush_log_at_trx_commit.

• Configuration option sync_binlog.

• Configuration option innodb_file_per_table.

• Write buffer in a storage device, such as a disk drive, SSD, or RAID array.

• Battery-backed cache in a storage device.

• The operating system used to run MySQL, in particular its support for the fsync() system call.

• Uninterruptible power supply (UPS) protecting the electrical power to all computer servers and
storage devices that run MySQL servers and store MySQL data.

• Your backup strategy, such as frequency and types of backups, and backup retention periods.

• For distributed or hosted data applications, the particular characteristics of the data centers where
the hardware for the MySQL servers is located, and network connections between the data centers.

# InnoDB Multi-Versioning

InnoDB is a **multi-versioned** storage engine: it keeps information about old versions of changed rows, to support transactional features such as concurrency and rollback. This information is stored in the tablespace in a data structure called a **rollback segment** (after an analogous data structure in Oracle). InnoDB uses the information in the rollback segment to perform the undo operations needed in a transaction rollback. It also uses the information to build earlier versions of a row for a consistent read. Internally, InnoDB adds three fields to each row stored in the database. A **6-byte DB_TRX_ID** field indicates the transaction identifier for the last transaction that inserted or updated the row. Also, a deletion is treated internally as an update where a special bit in the row is set to mark it as deleted. Each row also contains a **7-byte DB_ROLL_PTR** field called the roll pointer. The roll pointer points to an undo log record written to the rollback segment. If the row was updated, the undo log record contains the information necessary to rebuild the content of the row before it was updated. A **6-byte DB_ROW_ID** field contains a row ID that increases monotonically as new rows are inserted. If InnoDB generates a clustered index automatically, the index contains row ID values. Otherwise, the DB_ROW_ID column does not appear in any index.

Undo logs in the rollback segment are divided into insert and update undo logs. Insert undo logs are needed only in transaction rollback and can be discarded as soon as the transaction commits. Update undo logs are used also in consistent reads, but they can be discarded only after there is no transaction present for which InnoDB has assigned a snapshot that in a consistent read could need the information in the update undo log to build an earlier version of a database row.

**Commit your transactions regularly**, including those transactions that issue only consistent reads. Otherwise, InnoDB cannot discard data from the update undo logs, and the rollback segment may grow too big, filling up your tablespace.

The physical size of an undo log record in the rollback segment is typically smaller than the corresponding inserted or updated row. You can use this information to calculate the space needed for your rollback segment.

In the InnoDB multi-versioning scheme, a row is not physically removed from the database immediately when you delete it with an SQL statement. InnoDB only physically removes the corresponding row and its index records when it discards the update undo log record written for the deletion. This removal operation is called a **purge**, and it is quite fast, usually taking the same order of time as the SQL statement that did the deletion.

If you insert and delete rows in smallish batches at about the same rate in the table, the purge thread can start to lag behind and the table can grow bigger and bigger because of all the “dead” rows, making everything disk-bound and very slow. In such a case, throttle new row operations, and allocate more resources to the purge thread by tuning the **innodb_max_purge_lag** system variable. 

**Multi-Versioning and Secondary Indexes**

InnoDB multiversion concurrency control (**MVCC**) treats secondary indexes differently than clustered indexes. Records in a clustered index are updated in-place, and their hidden system columns point undo log entries from which earlier versions of records can be reconstructed. Unlike clustered index records, secondary index records do not contain hidden system columns nor are they updated in-place. When a secondary index column is updated, old secondary index records are delete-marked, new records are inserted, and delete-marked records are eventually purged. When a secondary index record is delete-marked or the secondary index page is updated by a newer transaction, InnoDB looks up the database record in the clustered index. In the clustered index, the record's DB_TRX_ID is checked, and the correct version of the record is retrieved from the undo log if the record was modified after the reading transaction was initiated.

If a secondary index record is marked for deletion or the secondary index page is updated by a newer transaction, the covering index technique is not used. Instead of returning values from the index structure, InnoDB looks up the record in the clustered index.

However, if the index condition pushdown (ICP) optimization is enabled, and parts of the WHERE condition can be evaluated using only fields from the index, the MySQL server still pushes this part of the WHERE condition down to the storage engine where it is evaluated using the index. If no matching records are found, the clustered index lookup is avoided. If matching records are found, even among delete-marked records, InnoDB looks up the record in the clustered index.

# InnoDB Architecture

## Buffer Pool

The buffer pool is an area in main memory where InnoDB caches table and index data as data is accessed. The buffer pool allows frequently used data to be processed directly from memory, which speeds up processing. On dedicated database servers, up to 80% of physical memory is often assigned to the InnoDB buffer pool.

For efficiency of high-volume read operations, the buffer pool is divided into pages that can potentially hold multiple rows. For efficiency of cache management, the buffer pool is implemented as a linked list of pages; data that is rarely used is aged out of the cache, using a variation of the LRU algorithm.

## Change Buffer

> insert buffer - The former name of the change buffer. In MySQL 5.5, support was added for buffering changes to secondary index pages for DELETE and UPDATE operations. Previously, only changes resulting from INSERT operations were buffered. The preferred term is now change buffer.

The change buffer is a special data structure that caches changes to secondary index pages when affected pages are not in the buffer pool. The buffered changes, which may result from INSERT, UPDATE, or DELETE operations (DML), are merged later when the pages are loaded into the buffer pool by other read operations.

Unlike clustered indexes, secondary indexes are usually nonunique, and inserts into secondary indexes happen in a relatively random order. Similarly, deletes and updates may affect secondary index pages that are not adjacently located in an index tree. Merging cached changes at a later time, when affected pages are read into the buffer pool by other operations, avoids substantial random access I/O that would be required to read-in secondary index pages from disk.

Periodically, the purge operation that runs when the system is mostly idle, or during a slow shutdown, writes the updated index pages to disk. The purge operation can write disk blocks for a series of index
values more efficiently than if each value were written to disk immediately.

Change buffer merging may take several hours when there are numerous secondary indexes to update and many affected rows. During this time, disk I/O is increased, which can cause a significant slowdown for disk-bound queries. Change buffer merging may also continue to occur after a transaction is committed. In fact, change buffer merging may continue to occur after a server shutdown and restart.

In memory, the change buffer occupies part of the InnoDB buffer pool. On disk, the change buffer is part of the system tablespace, so that index changes remain buffered across database restarts. The type of data cached in the change buffer is governed by the **innodb_change_buffering** configuration option. You can also configure the maximum change buffer size.

**Monitoring the Change Buffer**

The following options are available for change buffer monitoring:

• **InnoDB Standard Monitor** output includes status information for the change buffer. To view monitor
data, issue the SHOW ENGINE INNODB STATUS command.

```sql
mysql> SHOW ENGINE INNODB STATUS\G
```

Change buffer status information is located under the **INSERT BUFFER AND ADAPTIVE HASH INDEX** heading and appears similar to the following:

> Ibuf: size 1, free list len 0, seg size 2, 0 merges
> merged operations:
>     insert 0, delete mark 0, delete 0
> discarded operations:
>     insert 0, delete mark 0, delete 0
> Hash table size 4425293, used cells 32, node heap has 1 buffer(s)
> 13577.57 hash searches/s, 202.47 non-hash searches/s

• The **INFORMATION_SCHEMA.INNODB_METRICS** table provides most of the data points found in InnoDB Standard Monitor output, plus other data points. To view change buffer metrics and a description of each, issue the following query:

```sql
mysql> SELECT NAME, COMMENT FROM INFORMATION_SCHEMA.INNODB_METRICS WHERE NAME LIKE '%ibuf%'\G
```

• The **INFORMATION_SCHEMA.INNODB_BUFFER_PAGE** table provides metadata about each page in the buffer pool, including change buffer index and change buffer bitmap pages. Change buffer pages are identified by PAGE_TYPE. IBUF_INDEX is the page type for change buffer index pages, and IBUF_BITMAP is the page type for change buffer bitmap pages.

> **Warning**
> Querying the INNODB_BUFFER_PAGE table can introduce significant performance overhead. To avoid impacting performance, reproduce the issue you want to investigate on a test instance and run your queries on the test instance.

For example, you can query the INNODB_BUFFER_PAGE table to determine the approximate number of IBUF_INDEX and IBUF_BITMAP pages as a percentage of total buffer pool pages.

```sql
mysql> SELECT (SELECT COUNT(*) FROM INFORMATION_SCHEMA.INNODB_BUFFER_PAGE
    -> WHERE PAGE_TYPE LIKE 'IBUF%') AS change_buffer_pages,
    -> (SELECT COUNT(*) FROM INFORMATION_SCHEMA.INNODB_BUFFER_PAGE) AS total_pages,
    -> (SELECT ((change_buffer_pages/total_pages)*100));
+---------------------+-------------+--------------------------------------------------+
| change_buffer_pages | total_pages | (SELECT ((change_buffer_pages/total_pages)*100)) |
+---------------------+-------------+--------------------------------------------------+
|                  24 |         512 |                                           4.6875 |
+---------------------+-------------+--------------------------------------------------+
```

• Performance Schema provides **change buffer mutex wait instrumentation** for advanced performance
monitoring. To view change buffer instrumentation, issue the following query:

```sql
mysql> SELECT * FROM performance_schema.setup_instruments 
	-> WHERE NAME LIKE '%wait/synch/mutex/innodb/ibuf%';
+-------------------------------------------------------+---------+-------+
| NAME                                                  | ENABLED | TIMED |
+-------------------------------------------------------+---------+-------+
| wait/synch/mutex/innodb/ibuf_bitmap_mutex             | NO      | NO    |
| wait/synch/mutex/innodb/ibuf_mutex                    | NO      | NO    |
| wait/synch/mutex/innodb/ibuf_pessimistic_insert_mutex | NO      | NO    |
+-------------------------------------------------------+---------+-------+
```

## Adaptive Hash Index

The adaptive hash index (AHI) lets InnoDB perform more like an in-memory database on systems with appropriate combinations of workload and ample memory for the buffer pool, without sacrificing any transactional features or reliability. This feature is enabled by the **innodb_adaptive_hash_index** option, or turned off by --skip-innodb_adaptive_hash_index at server startup.

Based on the observed pattern of searches, MySQL builds a hash index using a prefix of the index key. The prefix of the key can be any length, and it may be that only some of the values in the B-tree appear in the hash index. Hash indexes are built on demand for those pages of the index that are often accessed.

If a table fits almost entirely in main memory, a hash index can speed up queries by enabling direct lookup of any element, turning the index value into a sort of pointer. InnoDB has a mechanism that monitors index searches. If InnoDB notices that queries could benefit from building a hash index, it does so automatically.

With some workloads, the speedup from hash index lookups greatly outweighs the extra work to monitor index lookups and maintain the hash index structure. Sometimes, the read/write lock that guards access to the adaptive hash index can become a source of contention under heavy workloads, such as multiple concurrent joins. Queries with LIKE operators and % wildcards also tend not to benefit from the AHI. For workloads where the adaptive hash index is not needed, turning it off reduces unnecessary performance overhead. Because it is difficult to predict in advance whether this feature is appropriate for a particular system, consider running benchmarks with it both enabled and disabled, using a realistic workload. The architectural changes in MySQL 5.6 and higher make more workloads suitable for disabling the adaptive hash index than in earlier releases, although it is still enabled by default.

In MySQL 5.7, the adaptive hash index search system is partitioned. Each index is bound to a specific partition, and each partition is protected by a separate latch. Partitioning is controlled by the **innodb_adaptive_hash_index_parts** configuration option. In earlier releases, the adaptive hash index search system was protected by a single latch which could become a point of contention under heavy workloads. The innodb_adaptive_hash_index_parts option is set to 8 by default. The maximum setting is 512.

The hash index is always built based on an existing B-tree index on the table. InnoDB can build a hash index on a prefix of any length of the key defined for the B-tree, depending on the pattern of searches that InnoDB observes for the B-tree index. A hash index can be partial, covering only those pages of the index that are often accessed.

You can monitor the use of the adaptive hash index and the contention for its use in the SEMAPHORES section of the output of the SHOW ENGINE INNODB STATUS command. If you see many threads waiting on an RW-latch created in btr0sea.c, then it might be useful to disable adaptive hash indexing.

## Redo Log Buffer

The redo log buffer is the memory area that holds data to be written to the redo log. Redo log buffer size is defined by the **innodb_log_buffer_size** configuration option. The redo log buffer is periodically flushed to the log file on disk. A large redo log buffer enables large transactions to run without the need to write redo log to disk before the transactions commit. Thus, if you have transactions that update, insert, or delete many rows, making the log buffer larger saves disk I/O. The **innodb_flush_log_at_trx_commit** option controls how the contents of the redo log buffer are written to the log file. The **innodb_flush_log_at_timeout** option controls redo log flushing frequency.

## System Tablespace

The InnoDB system tablespace contains the InnoDB data dictionary (metadata for InnoDB-related objects) and is the storage area for the doublewrite buffer, the change buffer, and undo logs. The system tablespace also contains table and index data for any user-created tables that are created in the system tablespace. The system tablespace is considered a shared tablespace since it is shared by multiple tables.

The system tablespace is represented by one or more data files. By default, one system data file, named ibdata1, is created in the MySQL data directory. The size and number of system data files is controlled by the **innodb_data_file_path** startup option.

## InnoDB Data Dictionary

The InnoDB data dictionary is comprised of internal system tables that contain metadata used to keep track of objects such as tables, indexes, and table columns. The metadata is physically located in the InnoDB system tablespace. For historical reasons, data dictionary metadata overlaps to some degree
with information stored in InnoDB table metadata files (.frm files).

## **Doublewrite Buffer**

The doublewrite buffer is a storage area located in the system tablespace where InnoDB writes pages that are flushed from the InnoDB buffer pool, before the pages are written to their proper positions in the data file. Only after flushing and writing pages to the doublewrite buffer, does InnoDB write pages to their proper positions. If there is an operating system, storage subsystem, or mysqld process crash in the middle of a page write, InnoDB can later find a good copy of the page from the doublewrite buffer during crash recovery.

Although data is always written twice, the doublewrite buffer does not require twice as much I/O overhead or twice as many I/O operations. Data is written to the doublewrite buffer itself as a large sequential chunk, with a single fsync() call to the operating system.

The doublewrite buffer is enabled by default in most cases. To disable the doublewrite buffer, set **innodb_doublewrite** to 0.

If system tablespace files (“ibdata files”) are located on Fusion-io devices that support atomic writes, doublewrite buffering is automatically disabled and Fusion-io atomic writes are used for all data files. 

Because the doublewrite buffer setting is global, doublewrite buffering is also disabled for data files residing on non-Fusion-io hardware. This feature is only supported on Fusion-io hardware and is only enabled for Fusion-io NVMFS on Linux. To take full advantage of this feature, an innodb_flush_method setting of O_DIRECT is recommended.

## Undo Logs

An undo log is a collection of undo log records associated with a single transaction. An undo log record contains information about how to undo the latest change by a transaction to a clustered index record. If another transaction needs to see the original data (as part of a consistent read operation), the unmodified data is retrieved from the undo log records. Undo logs exist within undo log segments, which are contained within rollback segments. Rollback segments reside in the system tablespace, temporary tablespace, and undo tablespaces. 

InnoDB supports 128 rollback segments, 32 of which are reserved as non-redo rollback segments for temporary table transactions. Each transaction that updates a temporary table (excluding read-only transactions) is assigned two rollback segments, one redo-enabled rollback segment and one non-redo
rollback segment. Read-only transactions are only assigned non-redo rollback segments, as read-only transactions are only permitted to modify temporary tables.

This leaves 96 available rollback segments, each of which supports up to 1023 concurrent datamodifying
transactions, for a total limit of approximately 96K concurrent data-modifying transactions. The 96K limit assumes that transactions do not modify temporary tables. If all data-modifying transactions also modify temporary tables, the total limit is approximately 32K concurrent data modifying transactions. The **innodb_rollback_segments** option defines the number of rollback segments used by InnoDB.

## **File-Per-Table Tablespaces**

A file-per-table tablespace is a single-table tablespace that is created in its own data file rather than in the system tablespace. Tables are created in file-per-table tablespaces when the innodb_file_per_table option is enabled. Otherwise, InnoDB tables are created in the system tablespace. Each file-per-table tablespace is represented by a single .ibd data file, which is created in the database directory by default.

File per-table tablespaces support DYNAMIC and COMPRESSED row formats which support features such as off-page storage for variable length data and table compression.

## General Tablespaces

A shared InnoDB tablespace created using CREATE TABLESPACE syntax. General tablespaces can be created outside of the MySQL data directory, are capable of holding multiple tables, and support tables of all row formats.

Tables are added to a general tablespace using **CREATE TABLE tbl_name ... TABLESPACE [=] tablespace_name or ALTER TABLE tbl_name TABLESPACE [=] tablespace_name** syntax.

## Undo Tablespace

An undo tablespace comprises one or more files that contain undo logs. The number of undo tablespaces used by InnoDB is defined by the innodb_undo_tablespaces configuration option.

> **Note**
> innodb_undo_tablespaces is deprecated and will be removed in a future release.

## Temporary Tablespace

In MySQL 5.7, non-compressed, user-created InnoDB temporary tables and on-disk internal InnoDB temporary tables are created in a shared temporary tablespace. The **innodb_temp_data_file_path** configuration option defines the relative path, name, size, and attributes for temporary tablespace data files. If no value is specified for innodb_temp_data_file_path, the default behavior is to create an auto-extending data file named ibtmp1 in the data directory that is slightly larger than 12MB.

In previous MySQL releases, temporary tables were created in individual file-per-table tablespaces in the temporary file directory, or in the InnoDB system tablespace in the data directory if innodb_file_per_table was disabled. Compressed temporary tables, which are temporary tables created using the ROW_FORMAT=COMPRESSED attribute, are still created in file-per-table tablespaces in the temporary file directory.

A shared tablespace for temporary tables removes performance costs associated with creating and removing a file-per-table tablespace for each InnoDB temporary table. A dedicated tablespace for temporary tables also means that it is no longer necessary to save the metadata associated with those
tables to the InnoDB system tables.

The shared temporary tablespace is removed on normal shutdown or on an aborted initialization, and is recreated each time the server is started. The temporary tablespace receives a dynamically generated space ID when it is created, which prevents conflicts with existing space IDs. Startup is refused if the temporary tablespace cannot be created. The temporary tablespace is not removed if the server halts
unexpectedly. In this case, a database administrator can remove the temporary tablespace manually or
restart the server, which removes and recreates the temporary tablespace.

The temporary tablespace cannot reside on a raw device. INFORMATION_SCHEMA.FILES provides metadata about the InnoDB temporary tablespace. Issue a query similar to this one to view temporary tablespace metadata:

```sql
mysql> SELECT * FROM INFORMATION_SCHEMA.FILES WHERE TABLESPACE_NAME='innodb_temporary'\G;
*************************** 1. row ***************************
             FILE_ID: 89
           FILE_NAME: .\ibtmp1
           FILE_TYPE: TEMPORARY
     TABLESPACE_NAME: innodb_temporary
       TABLE_CATALOG:
        TABLE_SCHEMA: NULL
          TABLE_NAME: NULL
  LOGFILE_GROUP_NAME: NULL
LOGFILE_GROUP_NUMBER: NULL
              ENGINE: InnoDB
       FULLTEXT_KEYS: NULL
        DELETED_ROWS: NULL
        UPDATE_COUNT: NULL
        FREE_EXTENTS: 4
       TOTAL_EXTENTS: 12
         EXTENT_SIZE: 1048576
        INITIAL_SIZE: 12582912
        MAXIMUM_SIZE: NULL
     AUTOEXTEND_SIZE: 67108864
       CREATION_TIME: NULL
    LAST_UPDATE_TIME: NULL
    LAST_ACCESS_TIME: NULL
        RECOVER_TIME: NULL
 TRANSACTION_COUNTER: NULL
             VERSION: NULL
          ROW_FORMAT: NULL
          TABLE_ROWS: NULL
      AVG_ROW_LENGTH: NULL
         DATA_LENGTH: NULL
     MAX_DATA_LENGTH: NULL
        INDEX_LENGTH: NULL
           DATA_FREE: 8388608
         CREATE_TIME: NULL
         UPDATE_TIME: NULL
          CHECK_TIME: NULL
            CHECKSUM: NULL
              STATUS: NORMAL
               EXTRA: NULL
```

INFORMATION_SCHEMA.INNODB_TEMP_TABLE_INFO provides metadata about user-created temporary tables that are currently active within an InnoDB instance. 

**Temporary Table Undo Logs**

Temporary table undo logs are used for temporary tables and related objects. This type of undo log is not a redo log, as temporary tables are not recovered during crash recovery and do not require redo logs. 

Temporary table undo logs are, however, used for rollback while the server is running. This special type of non-redo undo log benefits performance by avoiding redo logging I/O for temporary tables and related objects. Temporary table undo logs reside in the temporary tablespace. 32 rollback segments are reserved for temporary table undo logs for transactions that modify temporary tables and related objects.

## Redo Log

The redo log is a disk-based data structure used during crash recovery to correct data written by incomplete transactions. During normal operations, the redo log encodes requests to change InnoDB table data that result from SQL statements or low-level API calls. Modifications that did not finish updating the data files before an unexpected shutdown are replayed automatically during initialization, and before the connections are accepted.

By default, the redo log is physically represented on disk as a set of files, named ib_logfile0 and ib_logfile1. MySQL writes to the redo log files in a circular fashion. Data in the redo log is encoded in terms of records affected; this data is collectively referred to as redo. The passage of data through the redo log is represented by an ever-increasing LSN value.

## **Group Commit for Redo Log Flushing**

InnoDB, like any other ACID-compliant database engine, flushes the redo log of a transaction before it is committed. InnoDB uses group commit functionality to group multiple such flush requests together to avoid one flush for each commit. With group commit, InnoDB issues a single write to the log file to perform the commit action for multiple user transactions that commit at about the same time, significantly improving throughput.

# InnoDB Locking and Transaction Model

## InnoDB Locking

**Shared and Exclusive Locks**

InnoDB implements standard row-level locking where there are two types of locks, shared (S) locks
and exclusive (X) locks.

• A shared (S) lock permits the transaction that holds the lock to read a row.

• An exclusive (X) lock permits the transaction that holds the lock to update or delete a row.

If transaction T1 holds a shared (S) lock on row r, then requests from some distinct transaction T2 for
a lock on row r are handled as follows:

• A request by T2 for an S lock can be granted immediately. As a result, both T1 and T2 hold an S lock
on r.

• A request by T2 for an X lock cannot be granted immediately.

If a transaction T1 holds an exclusive (X) lock on row r, a request from some distinct transaction T2 for a lock of either type on r cannot be granted immediately. Instead, transaction T2 has to wait for transaction T1 to release its lock on row r.

**Intention Locks**

InnoDB supports multiple granularity locking which permits coexistence of row-level locks and locks on entire tables. To make locking at multiple granularity levels practical, additional types of locks called intention locks are used. Intention locks are table-level locks in InnoDB that indicate which type of lock
(shared or exclusive) a transaction requires later for a row in that table. There are two types of intention
locks used in InnoDB (assume that transaction T has requested a lock of the indicated type on table t):

• Intention shared (IS): Transaction T intends to set S locks on individual rows in table t.

• Intention exclusive (IX): Transaction T intends to set X locks on those rows.

For example, `SELECT ... LOCK IN SHARE MODE` sets an IS lock and `SELECT ... FOR UPDATE` sets an IX lock.

The intention locking protocol is as follows:

• Before a transaction can acquire an S lock on a row in table t, it must first acquire an IS or stronger lock on t.

• Before a transaction can acquire an X lock on a row, it must first acquire an IX lock on t.

These rules can be conveniently summarized by means of the following lock type compatibility matrix.

|      | X        | IX         | S          | IS         |
| ---- | -------- | ---------- | ---------- | ---------- |
| X    | Conflict | Conflict   | Conflict   | Conflict   |
| IX   | Conflict | Compatible | Conflict   | Compatible |
| S    | Conflict | Conflict   | Compatible | Compatible |
| IS   | Conflict | Compatible | Compatible | Compatible |

A lock is granted to a requesting transaction if it is compatible with existing locks, but not if it conflicts with existing locks. A transaction waits until the conflicting existing lock is released. If a lock request conflicts with an existing lock and cannot be granted because it would cause deadlock, an error occurs. Thus, intention locks do not block anything except full table requests (for example, `LOCK TABLES ... WRITE`). The main purpose of IX and IS locks is to show that someone is locking a row, or going to lock a row in the table.

Transaction data for an intention lock appears similar to the following in SHOW ENGINE INNODB STATUS and InnoDB monitor output:

> TABLE LOCK table test.t trx id 10080 lock mode IX

**Record Locks**

A record lock is a lock on an **index record**. For example, `SELECT c1 FROM t WHERE c1 = 10 FOR UPDATE;` prevents any other transaction from inserting, updating, or deleting rows where the value of t.c1 is 10.

Record locks always lock index records, even if a table is defined with no indexes. For such cases, InnoDB creates a hidden clustered index and uses this index for record locking. 

Transaction data for a record lock appears similar to the following in SHOW ENGINE INNODB STATUS
and InnoDB monitor output:

> RECORD LOCKS space id 58 page no 3 n bits 72 index PRIMARY of table test.t
> trx id 10078 lock_mode X locks rec but not gap
> Record lock, heap no 2 PHYSICAL RECORD: n_fields 3; compact format; info bits 0
> 0: len 4; hex 8000000a; asc ;;
> 1: len 6; hex 00000000274f; asc 'O;;
> 2: len 7; hex b60000019d0110; asc ;;

**Gap Locks**

A gap lock is a lock on a gap between index records, or a lock on the gap before the first or after the last index record. For example, `SELECT c1 FROM t WHERE c1 BETWEEN 10 and 20 FOR UPDATE;` prevents other transactions from inserting a value of 15 into column t.c1, whether or not there was already any such value in the column, because the gaps between all existing values in the range are locked.

A gap might span a single index value, multiple index values, or even be empty.

Gap locks are part of the tradeoff between performance and concurrency, and are used in some transaction isolation levels and not others.

Gap locking is not needed for statements that lock rows using a unique index to search for a unique row. (This does not include the case that the search condition includes only some columns of a multiple-column unique index; in that case, gap locking does occur.) For example, if the id column has a unique index, the following statement uses only an index-record lock for the row having id value 100 and it does not matter whether other sessions insert rows in the preceding gap:

```sql
SELECT * FROM child WHERE id = 100;
```

If id is not indexed or has a nonunique index, the statement does lock the preceding gap. It is also worth noting here that conflicting locks can be held on a gap by different transactions. For example, transaction A can hold a shared gap lock (gap S-lock) on a gap while transaction B holds an exclusive gap lock (gap X-lock) on the same gap. The reason conflicting gap locks are allowed is that if a record is purged from an index, the gap locks held on the record by different transactions must be merged.

Gap locks in InnoDB are “purely inhibitive”, which means they only stop other transactions from inserting to the gap. They do not prevent different transactions from taking gap locks on the same gap. Thus, a gap X-lock has the same effect as a gap S-lock.

Gap locking can be disabled explicitly. This occurs if you change the transaction isolation level to READ COMMITTED. Under this circumstances, gap locking is disabled for searches and index scans and is used only for foreign-key constraint checking and duplicate-key checking.

There are also other effects of using the READ COMMITTED isolation level. Record locks for nonmatching rows are released after MySQL has evaluated the WHERE condition. For UPDATE statements, InnoDB does a “semi-consistent” read, such that it returns the latest committed version to MySQL so that MySQL can determine whether the row matches the WHERE condition of the UPDATE.

**Next-Key Locks**

A next-key lock is a combination of a record lock on the index record and a gap lock on the gap before the index record.

InnoDB performs row-level locking in such a way that when it searches or scans a table index, it sets shared or exclusive locks on the index records it encounters. Thus, the row-level locks are actually index-record locks. A next-key lock on an index record also affects the “gap” before that index record. That is, a next-key lock is an index-record lock plus a gap lock on the gap preceding the index record. If one session has a shared or exclusive lock on record *R* in an index, another session cannot insert a new index record in the gap immediately before *R* in the index order.

Suppose that an index contains the values 10, 11, 13, and 20. The possible next-key locks for this index cover the following intervals, where a round bracket denotes exclusion of the interval endpoint and a square bracket denotes inclusion of the endpoint:

> (-∞, 10]
> (10, 11]
> (11, 13]
> (13, 20]
> (20, +∞)

For the last interval, the next-key lock locks the gap above the largest value in the index and the “supremum” pseudo-record having a value higher than any value actually in the index. The supremum
is not a real index record, so, in effect, this next-key lock locks only the gap following the largest index
value.

By default, InnoDB operates in **REPEATABLE READ** transaction isolation level. In this case, InnoDB uses next-key locks for searches and index scans, which prevents phantom rows .

Transaction data for a next-key lock appears similar to the following in SHOW ENGINE INNODB STATUS and InnoDB monitor output:

> RECORD LOCKS space id 58 page no 3 n bits 72 index PRIMARY of table test.t
> trx id 10080 lock_mode X
> Record lock, heap no 1 PHYSICAL RECORD: n_fields 1; compact format; info bits 0
> 0: len 8; hex 73757072656d756d; asc supremum;;
> Record lock, heap no 2 PHYSICAL RECORD: n_fields 3; compact format; info bits 0
> 0: len 4; hex 8000000a; asc ;;
> 1: len 6; hex 00000000274f; asc 'O;;
> 2: len 7; hex b60000019d0110; asc ;;

**Insert Intention Locks**

An insert intention lock is a type of gap lock set by INSERT operations prior to row insertion. This lock signals the intent to insert in such a way that multiple transactions inserting into the same index gap need not wait for each other if they are not inserting at the same position within the gap. Suppose that there are index records with values of 4 and 7. Separate transactions that attempt to insert values of 5 and 6, respectively, each lock the gap between 4 and 7 with insert intention locks prior to obtaining the exclusive lock on the inserted row, but do not block each other because the rows are nonconflicting.

The following example demonstrates a transaction taking an insert intention lock prior to obtaining an
exclusive lock on the inserted record. The example involves two clients, A and B.

Client A creates a table containing two index records (90 and 102) and then starts a transaction that places an exclusive lock on index records with an ID greater than 100. The exclusive lock includes a gap lock before record 102:

```sql
mysql> CREATE TABLE child (id int(11) NOT NULL, PRIMARY KEY(id)) ENGINE=InnoDB;
mysql> INSERT INTO child (id) values (90),(102);
mysql> START TRANSACTION;
mysql> SELECT * FROM child WHERE id > 100 FOR UPDATE;
+-------+
|    id |
+-------+
|   102 |
+-------+
```

Client B begins a transaction to insert a record into the gap. The transaction takes an insert intention lock while it waits to obtain an exclusive lock.

```sql
mysql> START TRANSACTION;
mysql> INSERT INTO child (id) VALUES (101);
```

Transaction data for an insert intention lock appears similar to the following in SHOW ENGINE INNODB
STATUS and InnoDB monitor output:

> RECORD LOCKS space id 31 page no 3 n bits 72 index `PRIMARY` of table `test`.`child`
> trx id 8731 lock_mode X locks gap before rec insert intention waiting
> Record lock, heap no 3 PHYSICAL RECORD: n_fields 3; compact format; info bits 0
> 0: len 4; hex 80000066; asc f;;
> 1: len 6; hex 000000002215; asc " ;;
> 2: len 7; hex 9000000172011c; asc r ;;...

**AUTO-INC Locks**

An AUTO-INC lock is a special table-level lock taken by transactions inserting into tables with AUTO_INCREMENT columns. In the simplest case, if one transaction is inserting values into the table, any other transactions must wait to do their own inserts into that table, so that rows inserted by the first
transaction receive consecutive primary key values.

The innodb_autoinc_lock_mode configuration option controls the algorithm used for autoincrement
locking. It allows you to choose how to trade off between predictable sequences of autoincrement
values and maximum concurrency for insert operations.

## **InnoDB Transaction Model**

In the InnoDB transaction model, the goal is to combine the best properties of a multi-versioning database with traditional two-phase locking. InnoDB performs locking at the row level and runs queries as nonlocking consistent reads by default, in the style of Oracle. The lock information in InnoDB is stored space-efficiently so that lock escalation is not needed. Typically, several users are permitted to lock every row in InnoDB tables, or any random subset of the rows, without causing InnoDB memory exhaustion.

### Transaction Isolation Levels

*Transaction isolation is one of the foundations of database processing.* Isolation is the **I** in the acronym ACID; the isolation level is the setting that fine-tunes the balance between performance and reliability, consistency, and reproducibility of results when multiple transactions are making changes and performing queries at the same time.

InnoDB offers all four transaction isolation levels described by the SQL:1992 standard: READ UNCOMMITTED, READ COMMITTED, REPEATABLE READ, and SERIALIZABLE. **The default isolation level for InnoDB is *REPEATABLE READ*.**

A user can change the isolation level for a single session or for all subsequent connections with the SET TRANSACTION statement. To set the server's default isolation level for all connections, use the --transaction-isolation option on the command line or in an option file. 

InnoDB supports each of the transaction isolation levels described here using different locking strategies. You can enforce a high degree of consistency with the default REPEATABLE READ level, for operations on crucial data where ACID compliance is important. Or you can relax the consistency rules with READ COMMITTED or even READ UNCOMMITTED, in situations such as bulk reporting where precise consistency and repeatable results are less important than minimizing the amount of overhead for locking. SERIALIZABLE enforces even stricter rules than REPEATABLE READ, and is used mainly in specialized situations, such as with XA transactions and for troubleshooting issues with concurrency and deadlocks.

**• REPEATABLE READ**

This is the default isolation level for InnoDB. Consistent reads within the same transaction read the snapshot established by the first read. This means that if you issue several plain (nonlocking) SELECT statements within the same transaction, these SELECT statements are consistent also with respect to each other. 

For locking reads (SELECT with FOR UPDATE or LOCK IN SHARE MODE), UPDATE, and DELETE statements, locking depends on whether the statement uses a unique index with a unique search condition, or a range-type search condition.

• For a unique index with a unique search condition, InnoDB locks only the index record found, not
the gap before it.

• For other search conditions, InnoDB locks the index range scanned, using gap locks or next-key locks to block insertions by other sessions into the gaps covered by the range. 

**• READ COMMITTED**

Each consistent read, even within the same transaction, sets and reads its own fresh snapshot. For locking reads (SELECT with FOR UPDATE or LOCK IN SHARE MODE), UPDATE statements, and DELETE statements, InnoDB locks only index records, not the gaps before them, and thus permits the free insertion of new records next to locked records. Gap locking is only used for foreign-key constraint checking and duplicate-key checking.

Because gap locking is disabled, phantom problems may occur, as other sessions can insert new
rows into the gaps. 

If you use READ COMMITTED, you must use row-based binary logging.

Using READ COMMITTED has additional effects:

• For UPDATE or DELETE statements, InnoDB holds locks only for rows that it updates or deletes. Record locks for nonmatching rows are released after MySQL has evaluated the WHERE condition. This greatly reduces the probability of deadlocks, but they can still happen.

• For UPDATE statements, if a row is already locked, InnoDB performs a “semi-consistent” read, returning the latest committed version to MySQL so that MySQL can determine whether the row matches the WHERE condition of the UPDATE. If the row matches (must be updated), MySQL reads the row again and this time InnoDB either locks it or waits for a lock on it.

Consider the following example, beginning with this table:

```sql
CREATE TABLE t (a INT NOT NULL, b INT) ENGINE = InnoDB;
INSERT INTO t VALUES (1,2),(2,3),(3,2),(4,3),(5,2);
COMMIT;
```

In this case, table has no indexes, so searches and index scans use the hidden clustered index for record locking.

Suppose that one client performs an UPDATE using these statements:

```sql
SET autocommit = 0;
UPDATE t SET b = 5 WHERE b = 3;
```

Suppose also that a second client performs an UPDATE by executing these statements following those of the first client:

```sql
SET autocommit = 0;
UPDATE t SET b = 4 WHERE b = 2;
```

As InnoDB executes each UPDATE, it first acquires an exclusive lock for each row, and then determines whether to modify it. If InnoDB does not modify the row, it releases the lock. Otherwise, InnoDB retains the lock until the end of the transaction. This affects transaction processing as follows.

When using the default REPEATABLE READ isolation level, the first UPDATE acquires x-locks and does not release any of them:

> x-lock(1,2); retain x-lock
> x-lock(2,3); update(2,3) to (2,5); retain x-lock
> x-lock(3,2); retain x-lock
> x-lock(4,3); update(4,3) to (4,5); retain x-lock
> x-lock(5,2); retain x-lock

The second UPDATE blocks as soon as it tries to acquire any locks (because first update has retained locks on all rows), and does not proceed until the first UPDATE commits or rolls back:

> x-lock(1,2); block and wait for first UPDATE to commit or roll back

If READ COMMITTED is used instead, the first UPDATE acquires x-locks and releases those for rows that it does not modify:

> x-lock(1,2); unlock(1,2)
> x-lock(2,3); update(2,3) to (2,5); retain x-lock
> x-lock(3,2); unlock(3,2)
> x-lock(4,3); update(4,3) to (4,5); retain x-lock
> x-lock(5,2); unlock(5,2)

For the second UPDATE, InnoDB does a “semi-consistent” read, returning the latest committed version of each row to MySQL so that MySQL can determine whether the row matches the WHERE condition of the UPDATE:

> x-lock(1,2); update(1,2) to (1,4); retain x-lock
> x-lock(2,3); unlock(2,3)
> x-lock(3,2); update(3,2) to (3,4); retain x-lock
> x-lock(4,3); unlock(4,3)
> x-lock(5,2); update(5,2) to (5,4); retain x-lock

**• READ UNCOMMITTED**

SELECT statements are performed in a nonlocking fashion, but a possible earlier version of a row might be used. Thus, using this isolation level, such reads are not consistent. This is also called a dirty read. Otherwise, this isolation level works like READ COMMITTED.

**• SERIALIZABLE**

This level is like REPEATABLE READ, but InnoDB implicitly converts all plain SELECT statements to SELECT ... LOCK IN SHARE MODE if autocommit is disabled. If autocommit is enabled, the SELECT is its own transaction. It therefore is known to be read only and can be serialized if performed as a consistent (nonlocking) read and need not block for other transactions. (To force a plain SELECT to block if other transactions have modified the selected rows, disable autocommit.)

### autocommit, Commit, and Rollback

**In InnoDB, all user activity occurs inside a transaction.** If autocommit mode is enabled, each SQL statement forms a single transaction on its own. By default, MySQL starts the session for each new connection with autocommit enabled, so MySQL does a commit after each SQL statement if that statement did not return an error. If a statement returns an error, the commit or rollback behavior depends on the error.

A session that has autocommit enabled can perform a multiple-statement transaction by starting it with an explicit START TRANSACTION or BEGIN statement and ending it with a COMMIT or ROLLBACK statement. 

If autocommit mode is disabled within a session with SET autocommit = 0, the session always has a transaction open. A COMMIT or ROLLBACK statement ends the current transaction and a new one starts.

If a session that has autocommit disabled ends without explicitly committing the final transaction, MySQL rolls back that transaction.

Some statements implicitly end a transaction, as if you had done a COMMIT before executing the statement. 

A COMMIT means that the changes made in the current transaction are made permanent and become visible to other sessions. A ROLLBACK statement, on the other hand, cancels all modifications made by the current transaction. Both COMMIT and ROLLBACK release all InnoDB locks that were set during the current transaction.

**Grouping DML Operations with Transactions**

By default, connection to the MySQL server begins with autocommit mode enabled, which automatically commits every SQL statement as you execute it. This mode of operation might be unfamiliar if you have experience with other database systems, where it is standard practice to issue a sequence of DML statements and commit them or roll them back all together.

To use multiple-statement transactions, switch autocommit off with the SQL statement SET autocommit = 0 and end each transaction with COMMIT or ROLLBACK as appropriate. To leave autocommit on, begin each transaction with START TRANSACTION and end it with COMMIT or ROLLBACK. The following example shows two transactions. The first is committed; the second is rolled back.

```sql
shell> mysql test

mysql> CREATE TABLE customer (a INT, b CHAR (20), INDEX (a));
Query OK, 0 rows affected (0.00 sec)
mysql> -- Do a transaction with autocommit turned on.
mysql> START TRANSACTION;
Query OK, 0 rows affected (0.00 sec)
mysql> INSERT INTO customer VALUES (10, 'Heikki');
Query OK, 1 row affected (0.00 sec)
mysql> COMMIT;
Query OK, 0 rows affected (0.00 sec)
mysql> -- Do another transaction with autocommit turned off.
mysql> SET autocommit=0;
Query OK, 0 rows affected (0.00 sec)
mysql> INSERT INTO customer VALUES (15, 'John');
Query OK, 1 row affected (0.00 sec)
mysql> INSERT INTO customer VALUES (20, 'Paul');
Query OK, 1 row affected (0.00 sec)
mysql> DELETE FROM customer WHERE b = 'Heikki';
Query OK, 1 row affected (0.00 sec)
mysql> -- Now we undo those last 2 inserts and the delete.
mysql> ROLLBACK;
Query OK, 0 rows affected (0.00 sec)
mysql> SELECT * FROM customer;
+------+--------+
| a    | b      |
+------+--------+
| 10   | Heikki |
+------+--------+
1 row in set (0.00 sec)
mysql>
```

**Transactions in Client-Side Languages**

In APIs such as PHP, Perl DBI, JDBC, ODBC, or the standard C call interface of MySQL, you can send transaction control statements such as COMMIT to the MySQL server as strings just like any other SQL
statements such as SELECT or INSERT. Some APIs also offer separate special transaction commit and rollback functions or methods.

### Consistent Nonlocking Reads

A consistent read means that InnoDB uses multi-versioning to present to a query a snapshot of the database at a point in time. The query sees the changes made by transactions that committed before that point of time, and no changes made by later or uncommitted transactions. The exception to this rule is that the query sees the changes made by earlier statements within the same transaction. This exception causes the following anomaly: If you update some rows in a table, a SELECT sees the latest version of the updated rows, but it might also see older versions of any rows. If other sessions simultaneously update the same table, the anomaly means that you might see the table in a state that never existed in the database.

If the transaction isolation level is REPEATABLE READ (the default level), all consistent reads within the same transaction read the snapshot established by the first such read in that transaction. You can get a fresher snapshot for your queries by committing the current transaction and after that issuing new queries.

With READ COMMITTED isolation level, each consistent read within a transaction sets and reads its own fresh snapshot.

Consistent read is the default mode in which InnoDB processes SELECT statements in READ COMMITTED and REPEATABLE READ isolation levels. A consistent read does not set any locks on the tables it accesses, and therefore other sessions are free to modify those tables at the same time a consistent read is being performed on the table.

Suppose that you are running in the default REPEATABLE READ isolation level. When you issue a consistent read (that is, an ordinary SELECT statement), InnoDB gives your transaction a timepoint according to which your query sees the database. If another transaction deletes a row and commits after your timepoint was assigned, you do not see the row as having been deleted. Inserts and updates are treated similarly.

> **Note**
>
> The snapshot of the database state applies to SELECT statements within a transaction, not necessarily to DML statements. If you insert or modify some rows and then commit that transaction, a DELETE or UPDATE statement issued from another concurrent REPEATABLE READ transaction could affect those justcommitted rows, even though the session could not query them. If a transaction does update or delete rows committed by a different transaction, those changes do become visible to the current transaction. For example, you might encounter a situation like the following:
>
> ```sql
> SELECT COUNT(c1) FROM t1 WHERE c1 = 'xyz';
> -- Returns 0: no rows match.
> DELETE FROM t1 WHERE c1 = 'xyz';
> -- Deletes several rows recently committed by other transaction.
> SELECT COUNT(c2) FROM t1 WHERE c2 = 'abc';
> -- Returns 0: no rows match.
> UPDATE t1 SET c2 = 'cba' WHERE c2 = 'abc';
> -- Affects 10 rows: another txn just committed 10 rows with 'abc' values.
> SELECT COUNT(c2) FROM t1 WHERE c2 = 'cba';
> -- Returns 10: this txn can now see the rows it just updated.
> ```

You can advance your timepoint by committing your transaction and then doing another SELECT or
START TRANSACTION WITH CONSISTENT SNAPSHOT.

This is called **multi-versioned concurrency control.**

In the following example, session A sees the row inserted by B only when B has committed the insert
and A has committed as well, so that the timepoint is advanced past the commit of B.

```sql
 						Session A 				Session B
 						SET autocommit=0; 		SET autocommit=0;
time
| 						SELECT * FROM t;
| 						empty set
| 												INSERT INTO t VALUES (1, 2);
|
v 						SELECT * FROM t;
 						empty set
 												COMMIT;
 						SELECT * FROM t;
 						empty set
 						COMMIT;
 						SELECT * FROM t;
 						-----------------
						| 1 	| 2 	|
						-----------------
```

If you want to see the “freshest” state of the database, use either the READ COMMITTED isolation level
or a locking read:

```sql
SELECT * FROM t LOCK IN SHARE MODE;
```

With READ COMMITTED isolation level, each consistent read within a transaction sets and reads its own fresh snapshot. With LOCK IN SHARE MODE, a locking read occurs instead: A SELECT blocks until the transaction containing the freshest rows ends.

Consistent read does not work over certain DDL statements:

• Consistent read does not work over DROP TABLE, because MySQL cannot use a table that has been dropped and InnoDB destroys the table.

• Consistent read does not work over ALTER TABLE, because that statement makes a temporary copy of the original table and deletes the original table when the temporary copy is built. When you reissue a consistent read within a transaction, rows in the new table are not visible because those rows did not exist when the transaction's snapshot was taken. In this case, the transaction returns an error: ER_TABLE_DEF_CHANGED, “Table definition has changed, please retry transaction”. 

The type of read varies for selects in clauses like INSERT INTO ... SELECT, UPDATE ... (SELECT), and CREATE TABLE ... SELECT that do not specify FOR UPDATE or LOCK IN SHAREMODE:

• By default, InnoDB uses stronger locks and the SELECT part acts like READ COMMITTED, where each consistent read, even within the same transaction, sets and reads its own fresh snapshot.

• To use a consistent read in such cases, enable the innodb_locks_unsafe_for_binlog option and set the isolation level of the transaction to READ UNCOMMITTED, READ COMMITTED, or REPEATABLE READ (that is, anything other than SERIALIZABLE). In this case, no locks are set on rows read from the selected table.

### Locking Reads

If you query data and then insert or update related data within the same transaction, the regular SELECT statement does not give enough protection. Other transactions can update or delete the same rows you just queried. InnoDB supports two types of locking reads that offer extra safety:

**• SELECT ... LOCK IN SHARE MODE**

Sets a shared mode lock on any rows that are read. Other sessions can read the rows, but cannot modify them until your transaction commits. If any of these rows were changed by another transaction that has not yet committed, your query waits until that transaction ends and then uses the latest values.

**• SELECT ... FOR UPDATE**

For index records the search encounters, locks the rows and any associated index entries, the same as if you issued an UPDATE statement for those rows. Other transactions are blocked from updating those rows, from doing SELECT ... LOCK IN SHARE MODE, or from reading the data in certain transaction isolation levels. Consistent reads ignore any locks set on the records that exist in the read view. (Old versions of a record cannot be locked; they are reconstructed by applying undo logs on an in-memory copy of the record.)

These clauses are primarily useful when dealing with tree-structured or graph-structured data, either in a single table or split across multiple tables. You traverse edges or tree branches from one place to another, while reserving the right to come back and change any of these “pointer” values.

All locks set by LOCK IN SHARE MODE and FOR UPDATE queries are released when the transaction is committed or rolled back.

> **Note**
> Locking of rows for update using SELECT FOR UPDATE only applies when autocommit is disabled (either by beginning transaction with START TRANSACTION or by setting autocommit to 0. If autocommit is enabled, the rows matching the specification are not locked.

### Locking Read Examples

Suppose that you want to insert a new row into a table child, and make sure that the child row has a parent row in table parent. Your application code can ensure referential integrity throughout this sequence of operations.

First, use a consistent read to query the table PARENT and verify that the parent row exists. Can you safely insert the child row to table CHILD? No, because some other session could delete the parent row in the moment between your SELECT and your INSERT, without you being aware of it. To avoid this potential issue, perform the SELECT using LOCK IN SHARE MODE:

```sql
SELECT * FROM parent WHERE NAME = 'Jones' LOCK IN SHARE MODE;
```

After the LOCK IN SHARE MODE query returns the parent 'Jones', you can safely add the child record to the CHILD table and commit the transaction. Any transaction that tries to acquire an exclusive lock in the applicable row in the PARENT table waits until you are finished, that is, until the data in all tables is in a consistent state.

For another example, consider an integer counter field in a table CHILD_CODES, used to assign a unique identifier to each child added to table CHILD. Do not use either consistent read or a shared mode read to read the present value of the counter, because two users of the database could see the same value for the counter, and a duplicate-key error occurs if two transactions attempt to add rows with the same identifier to the CHILD table.

Here, LOCK IN SHARE MODE is not a good solution because if two users read the counter at the same time, at least one of them ends up in deadlock when it attempts to update the counter.

To implement reading and incrementing the counter, first perform a locking read of the counter using FOR UPDATE, and then increment the counter. For example:

```sql
SELECT counter_field FROM child_codes FOR UPDATE;
UPDATE child_codes SET counter_field = counter_field + 1;
```

A SELECT ... FOR UPDATE reads the latest available data, setting exclusive locks on each row it reads. Thus, it sets the same locks a searched SQL UPDATE would set on the rows.

The preceding description is merely an example of how SELECT ... FOR UPDATE works. In MySQL, the specific task of generating a unique identifier actually can be accomplished using only a single access to the table:

```sql
UPDATE child_codes SET counter_field = LAST_INSERT_ID(counter_field + 1);
SELECT LAST_INSERT_ID();
```

The SELECT statement merely retrieves the identifier information (specific to the current connection). It does not access any table.

### Locks Set by Different SQL Statements in InnoDB

A locking read, an UPDATE, or a DELETE generally set record locks on every index record that is scanned in the processing of the SQL statement. It does not matter whether there are WHERE conditions in the statement that would exclude the row. InnoDB does not remember the exact WHERE condition, but only knows which index ranges were scanned. The locks are normally nextkey locks that also block inserts into the “gap” immediately before the record. However, gap locking can be disabled explicitly, which causes next-key locking not to be used. The transaction isolation level also can affect which locks are set.

If a secondary index is used in a search and index record locks to be set are exclusive, InnoDB also retrieves the corresponding clustered index records and sets locks on them.

If you have no indexes suitable for your statement and MySQL must scan the entire table to process the statement, every row of the table becomes locked, which in turn blocks all inserts by other users to the table. It is important to create good indexes so that your queries do not unnecessarily scan many rows.

For SELECT ... FOR UPDATE or SELECT ... LOCK IN SHARE MODE, locks are acquired for scanned rows, and expected to be released for rows that do not qualify for inclusion in the result set (for example, if they do not meet the criteria given in the WHERE clause). However, in some cases, rows might not be unlocked immediately because the relationship between a result row and its original source is lost during query execution. For example, in a UNION, scanned (and locked) rows from a table might be inserted into a temporary table before evaluation whether they qualify for the result set. In this circumstance, the relationship of the rows in the temporary table to the rows in the original table is lost and the latter rows are not unlocked until the end of query execution.

InnoDB sets specific types of locks as follows.

• SELECT ... FROM is a consistent read, reading a snapshot of the database and setting no locks unless the transaction isolation level is set to SERIALIZABLE. For SERIALIZABLE level, the search sets shared next-key locks on the index records it encounters. However, only an index record lock is required for statements that lock rows using a unique index to search for a unique row.

• SELECT ... FROM ... LOCK IN SHARE MODE sets shared next-key locks on all index records the search encounters. However, only an index record lock is required for statements that lock rows using a unique index to search for a unique row.

• SELECT ... FROM ... FOR UPDATE sets an exclusive next-key lock on every record the search encounters. However, only an index record lock is required for statements that lock rows using a unique index to search for a unique row.

For index records the search encounters, SELECT ... FROM ... FOR UPDATE blocks other sessions from doing SELECT ... FROM ... LOCK IN SHARE MODE or from reading in certain transaction isolation levels. Consistent reads ignore any locks set on the records that exist in the read view.

• UPDATE ... WHERE ... sets an exclusive next-key lock on every record the search encounters. However, only an index record lock is required for statements that lock rows using a unique index to search for a unique row.

• When UPDATE modifies a clustered index record, implicit locks are taken on affected secondary index records. The UPDATE operation also takes shared locks on affected secondary index records when performing duplicate check scans prior to inserting new secondary index records, and when inserting new secondary index records.

• DELETE FROM ... WHERE ... sets an exclusive next-key lock on every record the search encounters. However, only an index record lock is required for statements that lock rows using a unique index to search for a unique row.

• INSERT sets an exclusive lock on the inserted row. This lock is an index-record lock, not a next-key lock (that is, there is no gap lock) and does not prevent other sessions from inserting into the gap before the inserted row.

Prior to inserting the row, a type of gap lock called an insert intention gap lock is set. This lock signals the intent to insert in such a way that multiple transactions inserting into the same index gap need not wait for each other if they are not inserting at the same position within the gap. Suppose that there are index records with values of 4 and 7. Separate transactions that attempt to insert values of 5 and 6 each lock the gap between 4 and 7 with insert intention locks prior to obtaining the exclusive lock on the inserted row, but do not block each other because the rows are nonconflicting. 

If a duplicate-key error occurs, a shared lock on the duplicate index record is set. This use of a shared lock can result in deadlock should there be multiple sessions trying to insert the same row if another session already has an exclusive lock. This can occur if another session deletes the row. Suppose that an InnoDB table t1 has the following structure:

```sql
CREATE TABLE t1 (i INT, PRIMARY KEY (i)) ENGINE = InnoDB;
```

Now suppose that three sessions perform the following operations in order:

Session 1:

```sql
START TRANSACTION;
INSERT INTO t1 VALUES(1);
```

Session 2:

```sql
START TRANSACTION;
INSERT INTO t1 VALUES(1);
```

Session 3:

```sql
START TRANSACTION;
INSERT INTO t1 VALUES(1);
```

Session 1:

```sql
ROLLBACK;
```

The first operation by session 1 acquires an exclusive lock for the row. The operations by sessions 2 and 3 both result in a duplicate-key error and they both request a shared lock for the row. When session 1 rolls back, it releases its exclusive lock on the row and the queued shared lock requests for sessions 2 and 3 are granted. At this point, sessions 2 and 3 deadlock: Neither can acquire an exclusive lock for the row because of the shared lock held by the other.

A similar situation occurs if the table already contains a row with key value 1 and three sessions perform the following operations in order:

Session 1:

```sql
START TRANSACTION;
DELETE FROM t1 WHERE i = 1;
```

Session 2:

```sql
START TRANSACTION;
INSERT INTO t1 VALUES(1);
```

Session 3:

```sql
START TRANSACTION;
INSERT INTO t1 VALUES(1);
```

Session 1:

```sql
COMMIT;
```

The first operation by session 1 acquires an exclusive lock for the row. The operations by sessions 2 and 3 both result in a duplicate-key error and they both request a shared lock for the row. When session 1 commits, it releases its exclusive lock on the row and the queued shared lock requests for sessions 2 and 3 are granted. At this point, sessions 2 and 3 deadlock: Neither can acquire an exclusive lock for the row because of the shared lock held by the other.

• INSERT ... ON DUPLICATE KEY UPDATE differs from a simple INSERT in that an exclusive lock rather than a shared lock is placed on the row to be updated when a duplicate-key error occurs. An exclusive index-record lock is taken for a duplicate primary key value. An exclusive next-key lock is taken for a duplicate unique key value.

• REPLACE is done like an INSERT if there is no collision on a unique key. Otherwise, an exclusive next-key lock is placed on the row to be replaced.

• INSERT INTO T SELECT ... FROM S WHERE ... sets an exclusive index record lock (without a gap lock) on each row inserted into T. If the transaction isolation level is READ COMMITTED, or innodb_locks_unsafe_for_binlog is enabled and the transaction isolation level is not SERIALIZABLE, InnoDB does the search on S as a consistent read (no locks). Otherwise, InnoDB sets shared next-key locks on rows from S. InnoDB has to set locks in the latter case: In roll-forward recovery from a backup, every SQL statement must be executed in exactly the same way it was done originally.

CREATE TABLE ... SELECT ... performs the SELECT with shared next-key locks or as a consistent read, as for INSERT ... SELECT.

When a SELECT is used in the constructs REPLACE INTO t SELECT ... FROM s WHERE ... or UPDATE t ... WHERE col IN (SELECT ... FROM s ...), InnoDB sets shared next-key locks on rows from table s.

• While initializing a previously specified AUTO_INCREMENT column on a table, InnoDB sets an exclusive lock on the end of the index associated with the AUTO_INCREMENT column. In accessing the auto-increment counter, InnoDB uses a specific AUTO-INC table lock mode where the lock lasts only to the end of the current SQL statement, not to the end of the entire transaction. Other sessions cannot insert into the table while the AUTO-INC table lock is held.

InnoDB fetches the value of a previously initialized AUTO_INCREMENT column without setting any locks.

• If a FOREIGN KEY constraint is defined on a table, any insert, update, or delete that requires the constraint condition to be checked sets shared record-level locks on the records that it looks at to check the constraint. InnoDB also sets these locks in the case where the constraint fails.

• LOCK TABLES sets table locks, but it is the higher MySQL layer above the InnoDB layer that sets these locks. InnoDB is aware of table locks if innodb_table_locks = 1 (the default) and autocommit = 0, and the MySQL layer above InnoDB knows about row-level locks.

Otherwise, InnoDB's automatic deadlock detection cannot detect deadlocks where such table locks are involved. Also, because in this case the higher MySQL layer does not know about row-level locks, it is possible to get a table lock on a table where another session currently has row-level locks. However, this does not endanger transaction integrity.

Phantom Rows

The so-called phantom problem occurs within a transaction when the same query produces different sets of rows at different times. For example, if a SELECT is executed twice, but returns a row the second time that was not returned the first time, the row is a “phantom” row.

Suppose that there is an index on the id column of the child table and that you want to read and lock all rows from the table having an identifier value larger than 100, with the intention of updating some column in the selected rows later:

```sql
SELECT * FROM child WHERE id > 100 FOR UPDATE;
```

The query scans the index starting from the first record where id is bigger than 100. Let the table contain rows having id values of 90 and 102. If the locks set on the index records in the scanned range do not lock out inserts made in the gaps (in this case, the gap between 90 and 102), another session can insert a new row into the table with an id of 101. If you were to execute the same SELECT within the same transaction, you would see a new row with an id of 101 (a “phantom”) in the result set returned by the query. If we regard a set of rows as a data item, the new phantom child would violate the isolation principle of transactions that a transaction should be able to run so that the data it has read does not change during the transaction.

**To prevent phantoms, InnoDB uses an algorithm called next-key locking that combines index-row locking with gap locking.** InnoDB performs row-level locking in such a way that when it searches or scans a table index, it sets shared or exclusive locks on the index records it encounters. Thus, the rowlevel locks are actually index-record locks. In addition, a next-key lock on an index record also affects the “gap” before that index record. That is, a next-key lock is an index-record lock plus a gap lock on the gap preceding the index record. If one session has a shared or exclusive lock on record R in an index, another session cannot insert a new index record in the gap immediately before R in the index order.

When InnoDB scans an index, it can also lock the gap after the last record in the index. Just that happens in the preceding example: To prevent any insert into the table where id would be bigger than 100, the locks set by InnoDB include a lock on the gap following id value 102.

You can use next-key locking to implement a uniqueness check in your application: If you read your data in share mode and do not see a duplicate for a row you are going to insert, then you can safely insert your row and know that the next-key lock set on the successor of your row during the read prevents anyone meanwhile inserting a duplicate for your row. Thus, the next-key locking enables you to “lock” the nonexistence of something in your table.

Gap locking can be disabled. This may cause phantom problems because other sessions can insert new rows into the gaps when gap locking is disabled.

### Deadlocks in InnoDB

A deadlock is a situation where different transactions are unable to proceed because each holds a lock that the other needs. Because both transactions are waiting for a resource to become available, neither ever release the locks it holds.

A deadlock can occur when transactions lock rows in multiple tables (through statements such as UPDATE or SELECT ... FOR UPDATE), but in the opposite order. A deadlock can also occur when such statements lock ranges of index records and gaps, with each transaction acquiring some locks but not others due to a timing issue. 

To reduce the possibility of deadlocks, use transactions rather than LOCK TABLES statements; keep transactions that insert or update data small enough that they do not stay open for long periods of time; when different transactions update multiple tables or large ranges of rows, use the same order of operations (such as SELECT ... FOR UPDATE) in each transaction; create indexes on the columns used in SELECT ... FOR UPDATE and UPDATE ... WHERE statements. The possibility of deadlocks is not affected by the isolation level, because the isolation level changes the behavior of read operations, while deadlocks occur because of write operations.

When deadlock detection is enabled (the default) and a deadlock does occur, InnoDB detects the condition and rolls back one of the transactions (the victim). If deadlock detection is disabled using the **innodb_deadlock_detect** configuration option, InnoDB relies on the **innodb_lock_wait_timeout** setting to roll back transactions in case of a deadlock. Thus, even if your application logic is correct, you must still handle the case where a transaction must be retried.

To see the last deadlock in an InnoDB user transaction, use the **SHOW ENGINE INNODB STATUS** command. If frequent deadlocks highlight a problem with transaction structure or application error handling, run with the **innodb_print_all_deadlocks** setting enabled to print information about all deadlocks to the mysqld error log.

#### An InnoDB Deadlock Example

The following example illustrates how an error can occur when a lock request would cause a deadlock. The example involves two clients, A and B.

First, client A creates a table containing one row, and then begins a transaction. Within the transaction, A obtains an S lock on the row by selecting it in share mode:

```sql
mysql> CREATE TABLE t (i INT) ENGINE = InnoDB;
Query OK, 0 rows affected (1.07 sec)
mysql> INSERT INTO t (i) VALUES(1);
Query OK, 1 row affected (0.09 sec)
mysql> START TRANSACTION;
Query OK, 0 rows affected (0.00 sec)
mysql> SELECT * FROM t WHERE i = 1 LOCK IN SHARE MODE;
+------+
| 	 i |
+------+
| 	 1 |
+------+
```

Next, client B begins a transaction and attempts to delete the row from the table:

```sql
mysql> START TRANSACTION;
Query OK, 0 rows affected (0.00 sec)
mysql> DELETE FROM t WHERE i = 1;
```

The delete operation requires an X lock. The lock cannot be granted because it is incompatible with the S lock that client A holds, so the request goes on the queue of lock requests for the row and client B blocks.

Finally, client A also attempts to delete the row from the table:

```sql
mysql> DELETE FROM t WHERE i = 1;
ERROR 1213 (40001): Deadlock found when trying to get lock;
try restarting transaction
```

Deadlock occurs here because client A needs an X lock to delete the row. However, that lock request cannot be granted because client B already has a request for an X lock and is waiting for client A to release its S lock. Nor can the S lock held by A be upgraded to an X lock because of the prior request by B for an X lock. As a result, InnoDB generates an error for one of the clients and releases its locks.

At that point, the lock request for the other client can be granted and it deletes the row from the table.

#### Deadlock Detection and Rollback

When deadlock detection is enabled (the default), InnoDB automatically detects transaction deadlocks and rolls back a transaction or transactions to break the deadlock. InnoDB tries to pick small transactions to roll back, where the size of a transaction is determined by the number of rows inserted, updated, or deleted.

InnoDB is aware of table locks if innodb_table_locks = 1 (the default) and autocommit = 0, and the MySQL layer above it knows about row-level locks. Otherwise, InnoDB cannot detect deadlocks where a table lock set by a MySQL LOCK TABLES statement or a lock set by a storage engine other than InnoDB is involved. Resolve these situations by setting the value of the innodb_lock_wait_timeout system variable.

When InnoDB performs a complete rollback of a transaction, all locks set by the transaction are released. However, if just a single SQL statement is rolled back as a result of an error, some of the locks set by the statement may be preserved. This happens because InnoDB stores row locks in a format such that it cannot know afterward which lock was set by which statement.

If a SELECT calls a stored function in a transaction, and a statement within the function fails, that statement rolls back. Furthermore, if ROLLBACK is executed after that, the entire transaction rolls back.

If the LATEST DETECTED DEADLOCK section of InnoDB Monitor output includes a message stating, “TOO DEEP OR LONG SEARCH IN THE LOCK TABLE WAITS-FOR GRAPH, WE WILL ROLL BACK FOLLOWING TRANSACTION,” this indicates that the number of transactions on the wait-for list has reached a limit of 200. A wait-for list that exceeds 200 transactions is treated as a deadlock and the transaction attempting to check the wait-for list is rolled back. The same error may also occur if the locking thread must look at more than 1,000,000 locks owned by transactions on the wait-for list.

**Disabling Deadlock Detection**

On high concurrency systems, deadlock detection can cause a slowdown when numerous threads wait for the same lock. At times, it may be more efficient to disable deadlock detection and rely on the innodb_lock_wait_timeout setting for transaction rollback when a deadlock occurs. Deadlock detection can be disabled using the innodb_deadlock_detect configuration option.

#### How to Minimize and Handle Deadlocks

This section explains how to organize database operations to minimize deadlocks and the subsequent error handling required in applications.

Deadlocks are a classic problem in transactional databases, but they are not dangerous unless they are so frequent that you cannot run certain transactions at all. Normally, you must write your applications so that they are always prepared to re-issue a transaction if it gets rolled back because of a deadlock.

InnoDB uses automatic row-level locking. You can get deadlocks even in the case of transactions that just insert or delete a single row. That is because these operations are not really “atomic”; they automatically set locks on the (possibly several) index records of the row inserted or deleted.

You can cope with deadlocks and reduce the likelihood of their occurrence with the following techniques:

• At any time, issue the SHOW ENGINE INNODB STATUS command to determine the cause of the most recent deadlock. That can help you to tune your application to avoid deadlocks.

• If frequent deadlock warnings cause concern, collect more extensive debugging information by enabling the innodb_print_all_deadlocks configuration option. Information about each deadlock, not just the latest one, is recorded in the MySQL error log. Disable this option when you are finished debugging.

• Always be prepared to re-issue a transaction if it fails due to deadlock. Deadlocks are not dangerous.
Just try again.

• Keep transactions small and short in duration to make them less prone to collision.

• Commit transactions immediately after making a set of related changes to make them less prone to collision. In particular, do not leave an interactive mysql session open for a long time with an uncommitted transaction.

• If you use locking reads (SELECT ... FOR UPDATE or SELECT ... LOCK IN SHARE MODE), try using a lower isolation level such as READ COMMITTED.

• When modifying multiple tables within a transaction, or different sets of rows in the same table, do those operations in a consistent order each time. Then transactions form well-defined queues and do not deadlock. For example, organize database operations into functions within your application, or call stored routines, rather than coding multiple similar sequences of INSERT, UPDATE, and DELETE statements in different places.

• Add well-chosen indexes to your tables. Then your queries need to scan fewer index records and consequently set fewer locks. Use EXPLAIN SELECT to determine which indexes the MySQL server regards as the most appropriate for your queries.

• Use less locking. If you can afford to permit a SELECT to return data from an old snapshot, do not add the clause FOR UPDATE or LOCK IN SHARE MODE to it. Using the READ COMMITTED isolation level is good here, because each consistent read within the same transaction reads from its own fresh snapshot.

• If nothing else helps, serialize your transactions with table-level locks. The correct way to use LOCK TABLES with transactional tables, such as InnoDB tables, is to begin a transaction with SET autocommit = 0 (not START TRANSACTION) followed by LOCK TABLES, and to not call UNLOCK TABLES until you commit the transaction explicitly. For example, if you need to write to table t1 and read from table t2, you can do this:

```sql
SET autocommit=0;
LOCK TABLES t1 WRITE, t2 READ, ...;
... do something with tables t1 and t2 here ...
COMMIT;
UNLOCK TABLES;
```

Table-level locks prevent concurrent updates to the table, avoiding deadlocks at the expense of less responsiveness for a busy system.

• Another way to serialize transactions is to create an auxiliary “semaphore” table that contains just a single row. Have each transaction update that row before accessing other tables. In that way, all transactions happen in a serial fashion. Note that the InnoDB instant deadlock detection algorithm also works in this case, because the serializing lock is a row-level lock. With MySQL table-level locks, the timeout method must be used to resolve deadlocks.

# InnoDB Tables and Indexes

## InnoDB Tables

### Creating InnoDB Tables

To create an InnoDB table, use the CREATE TABLE statement.

```sql
CREATE TABLE t1 (a INT, b CHAR (20), PRIMARY KEY (a)) ENGINE=InnoDB;
```

You do not need to specify the ENGINE=InnoDB clause if InnoDB is defined as the default storage engine, which it is by default. To check the default storage engine, issue the following statement:

```sql
mysql> SELECT @@default_storage_engine;
+--------------------------+
| @@default_storage_engine |
+--------------------------+
| InnoDB                   |
+--------------------------+
```

You might still use ENGINE=InnoDB clause if you plan to use mysqldump or replication to replay the CREATE TABLE statement on a server where the default storage engine is not InnoDB.

An InnoDB table and its indexes can be created in the system tablespace, in a file-per-table tablespace, or in a general tablespace. When innodb_file_per_table is enabled, which is the default, an InnoDB table is implicitly created in an individual file-per-table tablespace. Conversely, when innodb_file_per_table is disabled, an InnoDB table is implicitly created in the InnoDB system tablespace. To create a table in a general tablespace, use CREATE TABLE ... TABLESPACE syntax.

When you create an InnoDB table, MySQL creates a .frm file in the database directory under the MySQL data directory. For a table created in a file-per-table tablespace, MySQL also creates an .ibd tablespace file in the database directory, by default. A table created in the InnoDB system tablespace is created in an existing ibdata file, which resides in the MySQL data directory. A table created in a general tablespace is created in an existing general tablespace .ibd file. General tablespace files can be created inside or outside of the MySQL data directory.

Internally, InnoDB adds an entry for each table to the InnoDB data dictionary. The entry includes the database name. For example, if table t1 is created in the test database, the data dictionary entry for the database name is 'test/t1'. This means you can create a table of the same name (t1) in a different database, and the table names do not collide inside InnoDB.

**InnoDB Tables and .frm Files**

MySQL stores data dictionary information for tables in .frm files in database directories. Unlike other MySQL storage engines, InnoDB also encodes information about the table in its own internal data dictionary inside the system tablespace. When MySQL drops a table or a database, it deletes one or more .frm files as well as the corresponding entries inside the InnoDB data dictionary. You cannot move InnoDB tables between databases simply by moving the .frm files.

**InnoDB Tables and Row Formats**

The default row format for InnoDB tables is defined by the **innodb_default_row_format** configuration option, which has a default value of DYNAMIC. Dynamic and Compressed row format allow you to take advantage of InnoDB features such as table compression and efficient off-page storage of long column values. To use these row formats, innodb_file_per_table must be enabled (the default as of MySQL 5.6.6) and innodb_file_format must be set to Barracuda.

```sql
SET GLOBAL innodb_file_per_table=1;
SET GLOBAL innodb_file_format=barracuda;
CREATE TABLE t3 (a INT, b CHAR (20), PRIMARY KEY (a)) ROW_FORMAT=DYNAMIC;
CREATE TABLE t4 (a INT, b CHAR (20), PRIMARY KEY (a)) ROW_FORMAT=COMPRESSED;
```

Alternatively, you can use CREATE TABLE ... TABLESPACE syntax to create an InnoDB table in a general tablespace. General tablespaces support all row formats.

```sql
CREATE TABLE t1 (c1 INT PRIMARY KEY) TABLESPACE ts1 ROW_FORMAT=DYNAMIC;
```

CREATE TABLE ... TABLESPACE syntax can also be used to create InnoDB tables with a Dynamic row format in the system tablespace, alongside tables with a Compact or Redundant row format.

```sql
CREATE TABLE t1 (c1 INT PRIMARY KEY) TABLESPACE = innodb_system ROW_FORMAT=DYNAMIC;
```

**InnoDB Tables and Primary Keys**

Always define a primary key for an InnoDB table, specifying the column or columns that:

• Are referenced by the most important queries.
• Are never left blank.
• Never have duplicate values.
• Rarely if ever change value once inserted.

For example, in a table containing information about people, you would not create a primary key on (firstname, lastname) because more than one person can have the same name, some people have blank last names, and sometimes people change their names. With so many constraints, often there is not an obvious set of columns to use as a primary key, so you create a new column with a numeric ID to serve as all or part of the primary key. You can declare an auto-increment column so that ascending values are filled in automatically as rows are inserted:

```sql
-- The value of ID can act like a pointer between related items in different tables.
CREATE TABLE t5 (id INT AUTO_INCREMENT, b CHAR (20), PRIMARY KEY (id));
-- The primary key can consist of more than one column. Any autoinc column must come first.
CREATE TABLE t6 (id INT AUTO_INCREMENT, a INT, b CHAR (20), PRIMARY KEY (id,a));
```

Although the table works correctly without defining a primary key, the primary key is involved with many aspects of performance and is a crucial design aspect for any large or frequently used table. It is recommended that you always specify a primary key in the CREATE TABLE statement. If you create the table, load data, and then run ALTER TABLE to add a primary key later, that operation is much **slower** than defining the primary key when creating the table.

**Viewing InnoDB Table Properties**

To view the properties of an InnoDB table, issue a SHOW TABLE STATUS statement:

```sql
mysql> SHOW TABLE STATUS FROM test LIKE 't%' \G;
```

InnoDB table properties may also be queried using the InnoDB Information Schema system tables:

```sql
mysql> SELECT * FROM INFORMATION_SCHEMA.INNODB_SYS_TABLES WHERE NAME='test/t1' \G;
```

### The Physical Row Structure of an InnoDB Table

The physical row structure of an InnoDB table depends on the row format specified when the table is created. If a row format is not specified, the default row format is used. The default row format for InnoDB tables is defined by the **innodb_default_row_format** configuration option, which has a default value of **DYNAMIC**.

**Determining the Row Format of an InnoDB Table**

To determine the row format of an InnoDB table, use SHOW TABLE STATUS. For example:

```sql
mysql> SHOW TABLE STATUS IN test1\G；
```

You can also determine the row format of an InnoDB table by querying INFORMATION_SCHEMA.INNODB_SYS_TABLES.

```sql
mysql> SELECT NAME, ROW_FORMAT FROM INFORMATION_SCHEMA.INNODB_SYS_TABLES WHERE NAME='test1/t1';
```

**Redundant Row Format Characteristics**

The REDUNDANT format is available to retain compatibility with older versions of MySQL. Rows in InnoDB tables that use REDUNDANT row format have the following characteristics:

• Each index record contains a 6-byte header. The header is used to link together consecutive records, and also in row-level locking.

• Records in the clustered index contain fields for all user-defined columns. In addition, there is a 6-byte transaction ID field and a 7-byte roll pointer field.

• If no primary key was defined for a table, each clustered index record also contains a 6-byte row ID field.

• Each secondary index record also contains all the primary key fields defined for the clustered index key that are not in the secondary index.

• A record contains a pointer to each field of the record. If the total length of the fields in a record is less than 128 bytes, the pointer is one byte; otherwise, two bytes. The array of these pointers is called the record directory. The area where these pointers point is called the data part of the record.

• Internally, InnoDB stores fixed-length character columns such as CHAR(10) in a fixed-length format.
InnoDB does not truncate trailing spaces from VARCHAR columns.

• InnoDB encodes fixed-length fields greater than or equal to 768 bytes in length as variable-length fields, which can be stored off-page. For example, a CHAR(255) column can exceed 768 bytes if the maximum byte length of the character set is greater than 3, as it is with utf8mb4.

• An SQL NULL value reserves one or two bytes in the record directory. Besides that, an SQL NULL value reserves zero bytes in the data part of the record if stored in a variable length column. In a fixed-length column, it reserves the fixed length of the column in the data part of the record.Reserving the fixed space for NULL values enables an update of the column from NULL to a non-NULL value to be done in place without causing fragmentation of the index page.

**COMPACT Row Format Characteristics**

The COMPACT row format decreases row storage space by about 20% compared to the REDUNDANT format at the cost of increasing CPU use for some operations. If your workload is a typical one that is limited by cache hit rates and disk speed, COMPACT format is likely to be faster. If the workload is a rare
case that is limited by CPU speed, compact format might be slower.

Rows in InnoDB tables that use COMPACT row format have the following characteristics:

• Each index record contains a 5-byte header that may be preceded by a variable-length header. The
header is used to link together consecutive records, and also in row-level locking.

• The variable-length part of the record header contains a bit vector for indicating NULL columns. If the
number of columns in the index that can be NULL is N, the bit vector occupies CEILING(N/8) bytes. (For example, if there are anywhere from 9 to 15 columns that can be NULL, the bit vector uses two bytes.) Columns that are NULL do not occupy space other than the bit in this vector. The variablelength part of the header also contains the lengths of variable-length columns. Each length takes one or two bytes, depending on the maximum length of the column. If all columns in the index are NOT NULL and have a fixed length, the record header has no variable-length part.

• For each non-NULL variable-length field, the record header contains the length of the column in one or two bytes. Two bytes are only needed if part of the column is stored externally in overflow pages or the maximum length exceeds 255 bytes and the actual length exceeds 127 bytes. For an externally stored column, the 2-byte length indicates the length of the internally stored part plus the 20-byte pointer to the externally stored part. The internal part is 768 bytes, so the length is 768+20. The 20-byte pointer stores the true length of the column.

• The record header is followed by the data contents of the non-NULL columns.

• Records in the clustered index contain fields for all user-defined columns. In addition, there is a 6-
byte transaction ID field and a 7-byte roll pointer field.

• If no primary key was defined for a table, each clustered index record also contains a 6-byte row ID
field.

• Each secondary index record also contains all the primary key fields defined for the clustered index
key that are not in the secondary index. If any of these primary key fields are variable length, the record header for each secondary index has a variable-length part to record their lengths, even if the secondary index is defined on fixed-length columns.

• Internally, for nonvariable-length character sets, InnoDB stores fixed-length character columns such
as CHAR(10) in a fixed-length format. 

InnoDB does not truncate trailing spaces from VARCHAR columns.

• Internally, for variable-length character sets such as utf8mb3 and utf8mb4, InnoDB attempts to store CHAR(N) in N bytes by trimming trailing spaces. If the byte length of a CHAR(N) column value exceeds N bytes, InnoDB trims trailing spaces to a minimum of the column value byte length. The maximum length of a CHAR(N) column is the maximum character byte length × N.

InnoDB reserves a minimum of N bytes for CHAR(N). Reserving the minimum space N in many cases enables column updates to be done in place without causing fragmentation of the index page. By comparison, for ROW_FORMAT=REDUNDANT, CHAR(N) columns occupy the maximum character byte length × N.

InnoDB encodes fixed-length fields greater than or equal to 768 bytes in length as variable-length fields, which can be stored off-page. For example, a CHAR(255) column can exceed 768 bytes if the maximum byte length of the character set is greater than 3, as it is with utf8mb4. ROW_FORMAT=DYNAMIC and ROW_FORMAT=COMPRESSED handle CHAR storage in the same way as ROW_FORMAT=COMPACT.

**DYNAMIC and COMPRESSED Row Format Characteristics**

DYNAMIC and COMPRESSED row formats are variations of the COMPACT row format.

### AUTO_INCREMENT Handling in InnoDB

InnoDB provides a configurable locking mechanism that can significantly improve scalability and performance of SQL statements that add rows to tables with AUTO_INCREMENT columns. To use the AUTO_INCREMENT mechanism with an InnoDB table, an AUTO_INCREMENT column must be defined as part of an index such that it is possible to perform the equivalent of an indexed SELECT MAX(ai_col) lookup on the table to obtain the maximum column value. Typically, this is achieved by making the column the first column of some table index.

**InnoDB AUTO_INCREMENT Lock Modes**

This section describes the behavior of AUTO_INCREMENT lock modes used to generate auto-increment values, and how each lock mode affects replication. Auto-increment lock modes are configured at startup using the **innodb_autoinc_lock_mode** configuration parameter.

The following terms are used in describing innodb_autoinc_lock_mode settings:

• “INSERT-like” statements

All statements that generate new rows in a table, including INSERT, INSERT ... SELECT, REPLACE, REPLACE ... SELECT, and LOAD DATA. Includes “simple-inserts”, “bulk-inserts”, and “mixed-mode” inserts.

• “Simple inserts”

Statements for which the number of rows to be inserted can be determined in advance (when the statement is initially processed). This includes single-row and multiple-row INSERT and REPLACE statements that do not have a nested subquery, but not INSERT ... ON DUPLICATE KEY UPDATE.

• “Bulk inserts”

Statements for which the number of rows to be inserted (and the number of required autoincrement values) is not known in advance. This includes INSERT ... SELECT, REPLACE ... SELECT, and LOAD DATA statements, but not plain INSERT. InnoDB assigns new values for the AUTO_INCREMENT column one at a time as each row is processed.

• “Mixed-mode inserts”

These are “simple insert” statements that specify the auto-increment value for some (but not all) of the new rows. An example follows, where c1 is an AUTO_INCREMENT column of table t1:

```sql
INSERT INTO t1 (c1,c2) VALUES (1,'a'), (NULL,'b'), (5,'c'), (NULL,'d');
```

Another type of “mixed-mode insert” is INSERT ... ON DUPLICATE KEY UPDATE, which in the worst case is in effect an INSERT followed by a UPDATE, where the allocated value for the AUTO_INCREMENT column may or may not be used during the update phase.

There are three possible settings for the innodb_autoinc_lock_mode configuration parameter. The settings are 0, 1, or 2, for “traditional”, “consecutive”, or “interleaved” lock mode, respectively.

• innodb_autoinc_lock_mode = 0 (“traditional” lock mode)

The traditional lock mode provides the same behavior that existed before the innodb_autoinc_lock_mode configuration parameter was introduced in MySQL 5.1. The traditional lock mode option is provided for backward compatibility, performance testing, and working around issues with “mixed-mode inserts”, due to possible differences in semantics.

// TODO

In this lock mode, all “INSERT-like” statements obtain a special table-level AUTO-INC lock for inserts
into tables with AUTO_INCREMENT columns. This lock is normally held to the end of the statement
(not to the end of the transaction) to ensure that auto-increment values are assigned in a predictable
and repeatable order for a given sequence of INSERT statements, and to ensure that auto-increment
values assigned by any given statement are consecutive.
In the case of statement-based replication, this means that when an SQL statement is replicated on
a slave server, the same values are used for the auto-increment column as on the master server.
The result of execution of multiple INSERT statements is deterministic, and the slave reproduces
the same data as on the master. If auto-increment values generated by multiple INSERT statements
were interleaved, the result of two concurrent INSERT statements would be nondeterministic, and
could not reliably be propagated to a slave server using statement-based replication.