# HIVE - NOTES

HIVE - 将类SQL语句HQL转换成Map-Reduce任务，在hadoop集群上执行。

### ARCHITECTURES

hive架构和流程图：

![hive-archi](https://github.com/ZhangLaibao/machine_gun/blob/master/images/hive-archi.png)

hive的HQL解析流程图：

![hive-parse-hql](https://github.com/ZhangLaibao/machine_gun/blob/master/images/hive-parse-hql.png)

### INSTALLATION

1.安装MySQL并配置用户/权限/数据库

2.下载/解压/环境变量配置

3.准备hdfs目录

```shell
  $ $HADOOP_HOME/bin/hadoop fs -mkdir       /tmp
  $ $HADOOP_HOME/bin/hadoop fs -mkdir       /user/hive/warehouse
  $ $HADOOP_HOME/bin/hadoop fs -chmod g+w   /tmp
  $ $HADOOP_HOME/bin/hadoop fs -chmod g+w   /user/hive/warehouse
```

4.初始化schema

```shell
$ $HIVE_HOME/bin/schematool -dbType mysql -initSchema
```

5.配置hive

```xml
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<?xml-stylesheet type="text/xsl" href="configuration.xsl"?>
<configuration>
    <property>
        <name>hive.metastore.warehouse.dir</name>
        <value>/user/hive/warehouse</value>
    </property>
    <property>
        <name>hive.metastore.local</name>
        <value>true</value>
    </property>
    <property>
        <name>javax.jdo.option.ConnectionURL</name>
        <value>jdbc:mysql://127.0.0.1:3306/hive?createDatabaseIfNotExist=true</value>
    </property>
    <property>
        <name>javax.jdo.option.ConnectionDriverName</name>
        <value>com.mysql.jdbc.Driver</value>
    </property>
    <property>
        <name>javax.jdo.option.ConnectionUserName</name>
        <value>hive</value>
    </property>
    <property>
        <name>javax.jdo.option.ConnectionPassword</name>
        <value>123456</value>
    </property>
</configuration>
```

### 一个HIVE数据操作的例子

1.创建表：

```sql
CREATE TABLE page_view(
    viewTime BIGINT, 
    userid BIGINT,
    page_url STRING, 
    referrer_url STRING,
    ip STRING COMMENT 'IP Address of the User',
    viewers array<STRING> COMMENT 'viewers of this page',
    attrs map<STRING,STRING> COMMENT 'extentions'
)COMMENT 'This is the page view table'
 PARTITIONED BY(dt STRING, country STRING)
 CLUSTERED BY(userid) 
 SORTED BY(viewTime) INTO 32 BUCKETS
 ROW FORMAT DELIMITED
   FIELDS TERMINATED BY '\044'
   COLLECTION ITEMS TERMINATED BY '\059'
   MAP KEYS TERMINATED BY '\058'
 STORED AS SEQUENCEFILE;
```

2.准备数据：

