# InnoDB System Tablespace

## System Tablespace

The InnoDB system tablespace contains the InnoDB data dictionary (metadata for InnoDB-related objects) and is the storage area for the doublewrite buffer, the change buffer, and undo logs. The system tablespace also contains table and index data for any user-created tables that are created in the system tablespace. The system tablespace is considered a shared tablespace since it is shared by multiple tables.

The system tablespace is represented by one or more data files. By default, one system data file, named ibdata1, is created in the MySQL data directory. The size and number of system data files is controlled by the innodb_data_file_path startup option.

## System Tablespace Data File Configuration

The **innodb_data_file_path** configuration option defines the name, size, and attributes of InnoDB system tablespace data files. If you do not specify a value for innodb_data_file_path, the default behavior is to create a single auto-extending data file, slightly larger than 12MB, named ibdata1. To specify more than one data file, separate them by semicolon (;) characters:

```properties
innodb_data_file_path=datafile_spec1[;datafile_spec2]...
```

The following setting configures a single 12MB data file named ibdata1 that is auto-extending. No location for the file is given, so by default, InnoDB creates it in the MySQL data directory:

```properties
[mysqld]
innodb_data_file_path=ibdata1:12M:autoextend
```

File size is specified using K, M, or G suffix letters to indicate units of KB, MB, or GB. If specifying the data file size in kilobytes (KB), do so in multiples of 1024. Otherwise, KB values are rounded to nearest megabyte (MB) boundary. The sum of the sizes of the files must be at least slightly larger than 12MB. A minimum file size is enforced for the first system tablespace data file to ensure that there is enough space for doublewrite buffer pages:

> • For an innodb_page_size value of 16KB or less, the minimum file size is 3MB.
> • For an innodb_page_size value of 32KB, the minimum file size is 6MB.
> • For an innodb_page_size value of 64KB, the minimum file size is 12MB.

A system tablespace with a fixed-size 50MB data file named ibdata1 and a 50MB auto-extending file named ibdata2 can be configured like this:

```properties
[mysqld]
innodb_data_file_path=ibdata1:50M;ibdata2:50M:autoextend
```

The full syntax for a data file specification includes the file name, file size, and optional autoextend and max attributes:

```properties
file_name:file_size[:autoextend[:max:max_file_size]]
```

The autoextend and max attributes can be used only for the data file that is specified last in the
innodb_data_file_path setting.

If you specify the autoextend option for the last data file, InnoDB extends the data file if it runs out of free space in the tablespace. The autoextend increment is 64MB at a time by default. To modify the increment, change the **innodb_autoextend_increment** system variable. If the disk becomes full, you might want to add another data file on another disk.

The size limit of individual files is determined by your operating system. You can set the file size to more than 4GB on operating systems that support large files. You can also use raw disk partitions as data files.

InnoDB is not aware of the file system maximum file size, so be cautious on file systems where the maximum file size is a small value such as 2GB. To specify a maximum size for an auto-extending data file, use the max attribute following the autoextend attribute. Use the max attribute only in cases where constraining disk usage is of critical importance, because exceeding the maximum size causes a fatal error, possibly causing the server to exit. The following configuration permits ibdata1 to grow to a limit of 500MB:

```properties
[mysqld]
innodb_data_file_path=ibdata1:12M:autoextend:max:500M
```

InnoDB creates system tablespace files in the MySQL data directory by default (datadir). To specify a location explicitly, use the innodb_data_home_dir option. For example, to create two files named ibdata1 and ibdata2 in a directory named myibdata, configure InnoDB like this:

```properties
[mysqld]
innodb_data_home_dir = /path/to/myibdata/
innodb_data_file_path=ibdata1:50M;ibdata2:50M:autoextend
```

> **Note**
> A trailing slash is required when specifying a value for innodb_data_home_dir.
> InnoDB does not create directories, so make sure that the myibdata directory exists before you start the server. Use the Unix or DOS mkdir command to create directories.
> Make sure that the MySQL server has the proper access rights to create files in the data directory. More generally, the server must have access rights in any directory where it needs to create data files.

InnoDB forms the directory path for each data file by textually concatenating the value of
innodb_data_home_dir to the data file name. If the innodb_data_home_dir option is not specified, the default value is the “dot” directory ./, which means the MySQL data directory. (The MySQL server changes its current working directory to its data directory when it begins executing.)

If you specify innodb_data_home_dir as an empty string, you can specify absolute paths for data files listed in the innodb_data_file_path value. The following example is equivalent to the preceding one:

```properties
[mysqld]
innodb_data_home_dir =
innodb_data_file_path=/path/to/myibdata/ibdata1:50M;/path/to/myibdata/ibdata2:50M:autoextend
```

## Resizing the InnoDB System Tablespace

**Increasing the Size of the InnoDB System Tablespace**

The easiest way to increase the size of the InnoDB system tablespace is to configure it from the beginning to be auto-extending. Specify the autoextend attribute for the last data file in the tablespace definition. Then InnoDB increases the size of that file automatically in 64MB increments when it runs out of space. The increment size can be changed by setting the value of the innodb_autoextend_increment system variable, which is measured in megabytes.

You can expand the system tablespace by a defined amount by adding another data file:

1. Shut down the MySQL server.
2. If the previous last data file is defined with the keyword autoextend, change its definition to use a fixed size, based on how large it has actually grown. Check the size of the data file, round it down to the closest multiple of 1024 × 1024 bytes (= 1MB), and specify this rounded size explicitly in innodb_data_file_path.
3. Add a new data file to the end of innodb_data_file_path, optionally making that file autoextending.
    Only the last data file in the innodb_data_file_path can be specified as autoextending.
4. Start the MySQL server again.

For example, this tablespace has just one auto-extending data file ibdata1:

```properties
innodb_data_home_dir =
innodb_data_file_path = /ibdata/ibdata1:10M:autoextend
```

Suppose that this data file, over time, has grown to 988MB. Here is the configuration line after modifying the original data file to use a fixed size and adding a new auto-extending data file:

```properties
innodb_data_home_dir =
innodb_data_file_path = /ibdata/ibdata1:988M;/disk2/ibdata2:50M:autoextend
```

When you add a new data file to the system tablespace configuration, make sure that the filename does not refer to an existing file. InnoDB creates and initializes the file when you restart the server.

**Decreasing the Size of the InnoDB System Tablespace**

You cannot remove a data file from the system tablespace. To decrease the system tablespace size,
use this procedure:

1. Use mysqldump to dump all your InnoDB tables, including InnoDB tables located in the MySQL database. As of 5.6, there are five InnoDB tables included in the MySQL database:

  ```sql
  mysql> SELECT TABLE_NAME from INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA='mysql' and ENGINE='InnoDB';
  +---------------------------+
  | TABLE_NAME                |
  +---------------------------+
  | engine_cost               |
  | gtid_executed             |
  | help_category             |
  | help_keyword              |
  | help_relation             |
  | help_topic                |
  | innodb_index_stats        |
  | innodb_table_stats        |
  | plugin                    |
  | server_cost               |
  | servers                   |
  | slave_master_info         |
  | slave_relay_log_info      |
  | slave_worker_info         |
  | time_zone                 |
  | time_zone_leap_second     |
  | time_zone_name            |
  | time_zone_transition      |
  | time_zone_transition_type |
  +---------------------------+
  ```

2. Stop the server.

3. Remove all the existing tablespace files (*.ibd), including the ibdata and ib_log files. Do not
    forget to remove *.ibd files for tables located in the MySQL database.

4. Remove any .frm files for InnoDB tables.

5. Configure a new tablespace.

6. Restart the server.

7. Import the dump files.

    > **Note**
    > If your databases only use the InnoDB engine, it may be simpler to dump all
    > databases, stop the server, remove all databases and InnoDB log files, restart
    > the server, and import the dump files.