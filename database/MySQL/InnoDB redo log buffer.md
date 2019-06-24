# InnoDB redo log 

## redo log

A disk-based data structure used during crash recovery, to correct data written by incomplete transactions. During normal operation, it encodes requests to change InnoDB table data, which result from SQL statements or low-level API calls through NoSQL interfaces. Modifications that did not finish updating the data files before an unexpected shutdown are replayed automatically.

The redo log is physically represented as a set of files, typically named ib_logfile0 and ib_logfile1. The data in the redo log is encoded in terms of records affected; this data is collectively referred to as redo. The passage of data through the redo logs is represented by the ever-increasing LSN value. The original 4GB limit on maximum size for the redo log is raised to 512GB in MySQL 5.6.3.

The disk layout of the redo log is influenced by the configuration options **innodb_log_file_size**,
**innodb_log_group_home_dir**, and (rarely) **innodb_log_files_in_group**. The performance of redo log operations is also affected by the log buffer, which is controlled by the innodb_log_buffer_size configuration option.

### InnoDB Log File Configuration

By default, InnoDB creates two 48MB log files in the MySQL data directory (datadir) named ib_logfile0 and ib_logfile1. The following options can be used to modify the default configuration:

• **innodb_log_group_home_dir** defines directory path to the InnoDB log files (the redo logs). If this option is not configured, InnoDB log files are created in the MySQL data directory (datadir). You might use this option to place InnoDB log files in a different physical storage location than InnoDB data files to avoid potential I/O resource conflicts. For example:

```properties
[mysqld]
innodb_log_group_home_dir = /dr3/iblogs
```

> **Note**
> InnoDB does not create directories, so make sure that the log directory exists before you start the server. Use the Unix or DOS mkdir command to create any necessary directories.
>
> Make sure that the MySQL server has the proper access rights to create files in the log directory. More generally, the server must have access rights in any directory where it needs to create log files.

• **innodb_log_files_in_group** defines the number of log files in the log group. The default and recommended value is 2.

• **innodb_log_file_size** defines the size in bytes of each log file in the log group. The combined size of log files (innodb_log_file_size * innodb_log_files_in_group) cannot exceed a maximum value that is slightly less than 512GB. A pair of 255 GB log files, for example, approaches the limit but does not exceed it. The default log file size is 48MB. Generally, the combined size of the log files should be large enough that the server can smooth out peaks and troughs in workload activity, which often means that there is enough redo log space to handle more than an hour of write activity. The larger the value, the less checkpoint flush activity is needed in the buffer pool, saving disk I/O.

### Changing the Number or Size of InnoDB Redo Log Files

To change the number or the size of your InnoDB redo log files, perform the following steps:

1. Stop the MySQL server and make sure that it shuts down without errors.
2. Edit my.cnf to change the log file configuration. To change the log file size, configure innodb_log_file_size. To increase the number of log files, configure innodb_log_files_in_group.
3. Start the MySQL server again.

If InnoDB detects that the innodb_log_file_size differs from the redo log file size, it writes a log checkpoint, closes and removes the old log files, creates new log files at the requested size, and opens the new log files.

### InnoDB Crash Recovery

To recover from a MySQL server crash, the only requirement is to restart the MySQL server. InnoDB automatically checks the logs and performs a roll-forward of the database to the present. InnoDB automatically rolls back uncommitted transactions that were present at the time of the crash. During recovery, mysqld displays output similar to this:

> InnoDB: Log scan progressed past the checkpoint lsn 369163704
> InnoDB: Doing recovery: scanned up to log sequence number 374340608
> InnoDB: Doing recovery: scanned up to log sequence number 379583488
> InnoDB: Doing recovery: scanned up to log sequence number 384826368
> InnoDB: Doing recovery: scanned up to log sequence number 390069248
> InnoDB: Doing recovery: scanned up to log sequence number 395312128
> InnoDB: Doing recovery: scanned up to log sequence number 400555008
> InnoDB: Doing recovery: scanned up to log sequence number 405797888
> InnoDB: Doing recovery: scanned up to log sequence number 411040768
> InnoDB: Doing recovery: scanned up to log sequence number 414724794
> InnoDB: Database was not shutdown normally!
> InnoDB: Starting crash recovery.
> InnoDB: 1 transaction(s) which must be rolled back or cleaned up in
> total 518425 row operations to undo
> InnoDB: Trx id counter is 1792
> InnoDB: Starting an apply batch of log records to the database...
> InnoDB: Progress in percent: 0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15
> 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31 32 33 34 35 36 37
> 38 39 40 41 42 43 44 45 46 47 48 49 50 51 52 53 54 55 56 57 58 59
> 60 61 62 63 64 65 66 67 68 69 70 71 72 73 74 75 76 77 78 79 80 81
> 82 83 84 85 86 87 88 89 90 91 92 93 94 95 96 97 98 99
> InnoDB: Apply batch completed
> ...
> InnoDB: Starting in background the rollback of uncommitted transactions
> InnoDB: Rolling back trx with id 1511, 518425 rows to undo
> ...
> InnoDB: Waiting for purge to start
> InnoDB: 5.7.18 started; log sequence number 414724794
> ...
> ./mysqld: ready for connections.

InnoDB crash recovery consists of several steps:

• **Tablespace discovery**

Tablespace discovery is the process that InnoDB uses to identify tablespaces that require redo log application.

• **Redo log application**

Redo log application is performed during initialization, before accepting any connections. If all changes are flushed from the buffer pool to the tablespaces (ibdata* and *.ibd files) at the time of the shutdown or crash, redo log application is skipped. InnoDB also skips redo log application if redo log files are missing at startup.

Removing redo logs to speed up recovery is not recommended, even if some data loss is acceptable. Removing redo logs should only be considered after a clean shutdown, with innodb_fast_shutdown set to 0 or 1.

• **Roll back of incomplete transactions**

Incomplete transactions are any transactions that were active at the time of crash or fast shutdown. The time it takes to roll back an incomplete transaction can be three or four times the amount of time a transaction is active before it is interrupted, depending on server load.

You cannot cancel transactions that are being rolled back. In extreme cases, when rolling back transactions is expected to take an exceptionally long time, it may be faster to start InnoDB with an **innodb_force_recovery** setting of 3 or greater. 

• **Change buffer merge**

Applying changes from the change buffer (part of the system tablespace) to leaf pages of secondary indexes, as the index pages are read to the buffer pool.

• **Purge**

Deleting delete-marked records that are no longer visible to active transactions. 

The steps that follow redo log application do not depend on the redo log (other than for logging the writes) and are performed in parallel with normal processing. Of these, only rollback of incomplete transactions is special to crash recovery. The insert buffer merge and the purge are performed during normal processing.

After redo log application, InnoDB attempts to accept connections as early as possible, to reduce downtime. As part of crash recovery, InnoDB rolls back transactions that were not committed or in XA PREPARE state when the server crashed. The rollback is performed by a background thread, executed in parallel with transactions from new connections. Until the rollback operation is completed, new connections may encounter locking conflicts with recovered transactions.

In most situations, even if the MySQL server was killed unexpectedly in the middle of heavy activity, the recovery process happens automatically and no action is required of the DBA. If a hardware failure or severe system error corrupted InnoDB data, MySQL might refuse to start.

### Optimizing InnoDB Redo Logging

Consider the following guidelines for optimizing redo logging:

• Make your redo log files big, even as big as the buffer pool. When InnoDB has written the redo log files full, it must write the modified contents of the buffer pool to disk in a checkpoint. Small redo log files cause many unnecessary disk writes. Although historically big redo log files caused lengthy recovery times, recovery is now much faster and you can confidently use large redo log files.

• Consider increasing the size of the log buffer. A large log buffer enables large transactions to run without a need to write the log to disk before the transactions commit. Thus, if you have transactions that update, insert, or delete many rows, making the log buffer larger saves s disk I/O.

## redo log buffer

The redo log buffer is the memory area that holds data to be written to the redo log. Redo log buffer size is defined by the **innodb_log_buffer_size** configuration option. The redo log buffer is periodically flushed to the log file on disk. A large redo log buffer enables large transactions to run without the need to write redo log to disk before the transactions commit. Thus, if you have transactions that update, insert, or delete many rows, making the log buffer larger saves disk I/O.

The **innodb_flush_log_at_trx_commit** option controls how the contents of the redo log buffer are written to the log file. The **innodb_flush_log_at_timeout** option controls redo log flushing frequency.