# Slow Query Log

The slow query log consists of SQL statements that took more than **long_query_time** seconds to execute and required at least **min_examined_row_limit** rows to be examined. The minimum and default values of long_query_time are 0 and 10, respectively. The value can be specified to a resolution of microseconds. For logging to a file, times are written including the microseconds part. For logging to tables, only integer times are written; the microseconds part is ignored.

By default, administrative statements are not logged, nor are queries that do not use indexes for lookups. This behavior can be changed using **log_slow_admin_statements** and **log_queries_not_using_indexes**.

The time to acquire the initial locks is not counted as execution time. mysqld writes a statement to the slow query log after it has been executed and after all locks have been released, so log order might differ from execution order.

By default, the slow query log is disabled. To specify the initial slow query log state explicitly, use
--slow_query_log[={0|1}]. With no argument or an argument of 1, --slow_query_log enables the log. With an argument of 0, this option disables the log. To specify a log file name, use --slow_query_log_file=file_name. To specify the log destination, use --log-output.

If you specify no name for the slow query log file, the default name is host_name-slow.log. The server creates the file in the data directory unless an absolute path name is given to specify a different directory.

To disable or enable the slow query log or change the log file name at runtime, use the global slow_query_log and slow_query_log_file system variables. Set slow_query_log to 0 (or OFF) to disable the log or to 1 (or ON) to enable it. Set slow_query_log_file to specify the name of the log file. If a log file already is open, it is closed and the new file is opened.

When the slow query log is enabled, the server writes output to any destinations specified by the --log-output option or log_output system variable. If you enable the log, the server opens the log file and writes startup messages to it. However, further logging of queries to the file does not occur unless the FILE log destination is selected. If the destination is NONE, the server writes no queries even if the slow query log is enabled. Setting the log file name has no effect on logging if the log destination value does not contain FILE.

The server writes less information to the slow query log if you use the --log-short-format option. To include slow administrative statements in the statements written to the slow query log, use the log_slow_admin_statements system variable. Administrative statements include ALTER TABLE, ANALYZE TABLE, CHECK TABLE, CREATE INDEX, DROP INDEX, OPTIMIZE TABLE, and REPAIR TABLE.

To include queries that do not use indexes for row lookups in the statements written to the slow query log, enable the log_queries_not_using_indexes system variable. When such queries are logged, the slow query log may grow quickly. It is possible to put a rate limit on these queries by setting the log_throttle_queries_not_using_indexes system variable. By default, this variable is 0, which means there is no limit. Positive values impose a per-minute limit on logging of queries that do not use indexes. The first such query opens a 60-second window within which the server logs queries up to the given limit, then suppresses additional queries. If there are suppressed queries when the window ends, the server logs a summary that indicates how many there were and the aggregate time spent in them. The next 60-second window begins when the server logs the next query that does not use indexes.

The server uses the controlling parameters in the following order to determine whether to write a query
to the slow query log:

1. The query must either not be an administrative statement, or log_slow_admin_statements must be enabled.
2. The query must have taken at least long_query_time seconds, or log_queries_not_using_indexes must be enabled and the query used no indexes for row lookups.
3. The query must have examined at least min_examined_row_limit rows.
4. The query must not be suppressed according to the log_throttle_queries_not_using_indexes setting.

The log_timestamps system variable controls the time zone of timestamps in messages written to the slow query log file (as well as to the general query log file and the error log). It does not affect the time zone of general query log and slow query log messages written to log tables, but rows retrieved from those tables can be converted from the local system time zone to any desired time zone with CONVERT_TZ() or by setting the session time_zone system variable.

All log lines contain a timestamp.

The server does not write queries handled by the query cache to the slow query log, nor queries that would not benefit from the presence of an index because the table has zero rows or one row.

By default, a replication slave does not write replicated queries to the slow query log. To change this, use the log_slow_slave_statements system variable.

Passwords in statements written to the slow query log are rewritten by the server not to occur literally in
plain text.

The slow query log can be used to find queries that take a long time to execute and are therefore candidates for optimization. However, examining a long slow query log can become a difficult task. To make this easier, you can process a slow query log file using the **mysqldumpslow** command to summarize the queries that appear in the log.