#### MongoDB introduction
##### Basic Concepts
我们首先通过一个简单的表格和一张图来对比MongoDB和SQL数据库，通过类比可以获得一个直观地认识并且快速理解MongoDB的一些概念：

|SQL术语/概念|MongoDB术语/概念|解释/说明|
|:---:|:---:|:---:|
|database|database|数据库|
|table|collection|数据库表/集合|
|row|document|数据记录行/文档|
|column|field|数据字段/域|
|index|index|索引|
|table joins||表连接，MongoDB不支持|
|primary key|primary key|主键，MongoDB自动将_id字段设置为主键|

![Mapping-Table-to-Collection](https://github.com/ZhangLaibao/machine_gun/blob/master/images/Mapping-Table-to-Collection.png)

MongoDB是一个基于分布式文件存储的数据库。由C++语言编写。旨在为WEB应用提供可扩展的高性能数据存储解决方案。
mongoDB是一个介于关系数据库和非关系数据库之间的产品，是非关系数据库当中功能最丰富，最像关系数据库的。
它支持的数据结构非常松散，是类似json的bson格式，因此可以存储比较复杂的数据类型。
Mongo最大的特点是他支持的查询语言非常强大，其语法有点类似于面向对象的查询语言，
几乎可以实现类似关系数据库单表查询的绝大部分功能，而且还支持对数据建立索引。
##### Database
MongoDB的Database可以理解为SQL数据库的Database，MongoDB的单个实例可以容纳多个独立的数据库，
每一个都有自己的collections和权限，不同的数据库数据放置在不同的文件中。使用show dbs命令可以查看当前实例的所有数据库，例如：
    
    > show dbs
    admin  0.000GB
    local  0.000GB
使用db命令可以查看当前数据库
    
    > db
    test
使用use命令可以切换数据库

    > use local
    switched to db local
如果database或者collection不存在，在第一次向其中插入数据的时候MongoDB会帮我们创建这个database或者collection，假设下述的
myNewDB和myNewCloocetion都不存在，那么当我们执行以下命令的时候，
MongoDB会帮我们创建一个叫做myNewDB的数据库和一个myNewCollection的集合。
    
    use myNewDB
    db.myNewCollection1.insertOne( { x: 1 } )
关于命名规则，我们可以查阅文档https://docs.mongodb.com/manual/reference/limits/#restrictions-on-db-names
在第一次安装并启动MongoDB之后我们可以看到一些已有的数据库，这些数据库的简介如下：

    admin： 从权限的角度来看，这是"root"数据库。要是将一个用户添加到这个数据库，
        这个用户自动继承所有数据库的权限。一些特定的服务器端命令也只能从这个数据库运行，比如列出所有的数据库或者关闭服务器。
    local: 这个数据永远不会被复制，可以用来存储限于本地单台服务器的任意集合
    config: 当Mongo用于分片设置时，config数据库在内部使用，用于保存分片的相关信息。
    
##### Collection
MongoDB的collection可以理解为SQL数据库的table，用于存放MongoDB的document。除了使用命令触发MongoDB为我们自动创建collection，
我们还可以使用db.createCollection()方法显式创建collection，这个方法提供了很多可选选项，例如设置单条document的大小的上线或者
document的校验规则。   
关于document的格式校验，MongoDB是模式自由(Schema-free)的，采用无模式存储数据是其区别于RDBMS中的表的一个重要特征。
但是在3.2版本开始，MongoDB提供了Schema Validation来规定document的格式。

##### Document
MongoDB使用BSON格式存储数据记录，MongoDB的官方文档这么解释：BSON is a binary representation of JSON documents，
BSON的[官网](http://bsonspec.org/)这么解释：
    
    BSON, short for Binary JSON, is a binary encoded serialization of JSON-like doc­u­ments. 
    Like JSON, BSON supports the embedding of documents and arrays within other documents and arrays. 
    BSON also contains extensions that allow representation of data types that are not part of the JSON spec. 
    For ex­ample, BSON has a Date type and a BinData type.
    
    BSON can be compared to binary interchange formats, like Protocol Buffers. BSON is more "schema-less" 
    than Protocol Buffers, which can give it an advantage in flexibility but also a slight disadvantage 
    in space efficiency (BSON has overhead for field names within the serialized data).
    
    BSON was designed to have the following three characteristics:
    Lightweight
        Keeping spatial overhead to a minimum is important for any data representation format, especially when 
        used over the network.
    Traversable
        BSON is designed to be traversed easily. This is a vital property in its role as the primary 
        data representation for MongoDB.
    Efficient
        Encoding data to BSON and decoding from BSON can be performed very quickly 
        in most languages due to the use of C data types.
##### MongoDB 数据类型
下表为MongoDB中常用的几种数据类型，每种数据类型还有一个编号和别名。

|数据类型|编号ID|别名|说明|
|:---:|:---:|:---:|:---:|
|Double|1|“double”|双精度浮点值。用于存储浮点值|
|String|2|“string”|在 MongoDB 中，UTF-8 编码的字符串才是合法的| 
|Object|3|“object”|用于内嵌文档|
|Array|4|“array”|用于将数组或列表或多个值存储为一个键|
|Binary data|5|“binData”|用于存储二进制数据|
|Undefined|6|“undefined”|Deprecated|
|ObjectId|7|“objectId”|用于创建文档的 ID|	 
|Boolean|8|“bool”||	 
|Date|9|“date”|用UNIX时间格式来存储当前日期或时间，也可以指定自己的日期时间|
|Null|10|“null”||
|Regular Expression|11|“regex”||	 
|DBPointer|12|“dbPointer”|Deprecated||
|JavaScript|13|“javascript”||
|Symbol|14|“symbol”|Deprecated||
|JavaScript (with scope)|15|“javascriptWithScope”||
|32-bit integer|16|“int”|根据你所采用的服务器，可分为 32 位或 64 位|
|Timestamp|17|“timestamp”|记录文档修改或添加的具体时间|
|64-bit integer|18|“long”||
|Decimal128|19|“decimal”|New in version 3.4||
|Min key|-1|“minKey”|将一个值与BSON元素的最高值相对比|
|Max key|127|“maxKey”|将一个值与BSON元素的最低值相对比|

#### MongoDB quick start 
**启动命令，例如**
    
    ./mongod --dbpath=/data/db
**创建/删除 database**   
当我们使用use命令切换数据库时，如果指定名称的数据库不存在，MongoDB会自动帮我们创建，例如：
    
    use DATABASE_NAME
我们需要使用dropDatabase()函数删除当前use的数据库，例如

    db.dropDatabase()
**创建/删除collection**   
MongoDB 中使用 createCollection() 方法来创建集合。

    db.createCollection(name, options)
支持的options参数如下:

|参数名称|类型|说明|
|:---:|:---:|:---:|
|capped|bool|true:创建固定大小的集合，当达到最大值时，它会自动覆盖最早的文档，当该值为 true 时，必须指定 size 参数|
|autoIndexId|bool|true:自动在 _id 字段创建索引 默认为 false|
|size|int|为固定集合指定一个最大值(字节计)|
|max|int|指定固定集合中包含文档的最大数量|

MongoDB 中使用 drop() 方法来删除集合。例如：
    
    db.collection.drop()
如果成功删除选定集合，则 drop() 方法返回 true，否则返回 false。
**数据CRUD**
MongoDB 使用 insert() 或 save() 方法向集合中插入文档，如果不指定 _id 字段 save() 方法类似于 insert() 方法。
如果指定 _id 字段，则会替换该 _id 的数据。而update() 方法用于更新已存在的文档内容  。语法如下：

    db.COLLECTION_NAME.insert(document)
例如：

    db.test.insert({title: 'hello mongo', 
        description: 'MongoDB is a Nosql database',
        url: 'http://www.mongodb.com',
        tags: ['mongodb', 'database', 'NoSQL'],
        likes: 100
    })
我们可以通过

    db.COLLECTION_NAME.find().pretty()
查看我们刚刚保存的数据，其中pretty()函数是用来格式化返回BSON数据的

    {
        "_id" : ObjectId("5b1e57218d3cee1cac2c493a"),
        "title" : "hello mongo",
        "description" : "MongoDB is a Nosql database",
        "url" : "http://www.mongodb.com",
        "tags" : [
            "mongodb",
            "database",
            "NoSQL"
        ],
        "likes" : 100
    }
    
update函数可以理解为MyBatis生成Mapper里面的updateByExampleSelective()，语法结构如下：
    
    db.collection.update(
       <query>,
       <update>,
       {
         upsert: <boolean>,
         multi: <boolean>,
         writeConcern: <document>
       }
    )

    db.test.update({title: 'hello mongo'},{$set:{description: 'MongoDB is a talent Nosql database'}})
    可以理解为SQL中的update语句，将title='hello mongo'的数据的修改为'MongoDB is a talent Nosql database'
    
将_id="5b1e57218d3cee1cac2c493a"的数据记录整个替换。

    db.collection.save({_id : ObjectId("5b1e57218d3cee1cac2c493a"),
        title: 'hello mongo', 
        description: 'MongoDB is a Nosql database',
        url: 'http://www.mongodb.com',
        tags: ['mongodb', 'database', 'NoSQL'],
        likes: 100
    })
关于WriteConcern的值有如下几种说明：
    
    WriteConcern.NONE:没有异常抛出
    WriteConcern.NORMAL:仅抛出网络错误异常，没有服务器错误异常
    WriteConcern.SAFE:抛出网络错误异常、服务器错误异常；并等待服务器完成写操作。
    WriteConcern.MAJORITY: 抛出网络错误异常、服务器错误异常；并等待一个主服务器完成写操作。
    WriteConcern.FSYNC_SAFE: 抛出网络错误异常、服务器错误异常；写操作等待服务器将数据刷新到磁盘。
    WriteConcern.JOURNAL_SAFE:抛出网络错误异常、服务器错误异常；写操作等待服务器提交到磁盘的日志文件。
    WriteConcern.REPLICAS_SAFE:抛出网络错误异常、服务器错误异常；等待至少2台服务器完成写操作。

remove()函数是用来移除集合中的数据。在执行remove()函数前先执行find()命令来判断执行的条件是否正确，
这是一个比较好的习惯。remove()函数的语法如下：

    db.collection.remove(
       <query>,
       {
         justOne: <boolean>,
         writeConcern: <document>
       }
    )
find()函数用来查询数据，语法格式如下：

    db.collection.find(query, projection)
    query:可选，使用查询操作符指定查询条件，可以类比SQL中的where条件
    projection:可选，使用投影操作符指定返回的键，可以类比为SQL中的where子句，默认全部

query子句中比较操作符的语法如下：   

|操作|格式|范例|RDBMS中的类似语句|
|:---:|:---:|:---:|:---:|
|等于|{<key>:<value>}|db.col.find({"by":"some"})|where by = 'some'|
|小于|{<key>:{$lt:<value>}}|db.col.find({"likes":{$lt:50}})|	where likes < 50|
|小于或等于|{<key>:{$lte:<value>}}|db.col.find({"likes":{$lte:50}})|where likes <= 50|
|大于|{<key>:{$gt:<value>}}|db.col.find({"likes":{$gt:50}})|	where likes > 50|
|大于或等于|{<key>:{$gte:<value>}}|db.col.find({"likes":{$gte:50}})|	where likes >= 50|
|不等于|{<key>:{$ne:<value>}}|db.col.find({"likes":{$ne:50}})|where likes != 50| 

当我们在query参数中传入多个条件时，他们之间是 AND 的关系，例如：

    db.col.find({key1:value1, key2:value2})
如果多个条件之间是 OR 的关系，那么应该写成这样：
    
    db.col.find({$or: [{key1: value1}, {key2:value2}]})
**分页**
MongoDB提供了一个limit()函数和一个skip()函数，用来指定查询数据的位置，例如：

    db.collection.find().limit(10) -- 读取前10条数据
    db.collection.find().limit(10).skip(5) -- 读取前10条数据，跳过这10条数据的前5条
所以我们就可以利用这两个函数写出类似于MySQL中limit(100,10)的效果了：

    db.collection.find().skip(100).limit(10)
**排序**
MongoDB的排序使用sort()函数，例如：

    db.collection.find().sort({KEY:1}) -- KEY是排序字段名，1为升序，-1为降序
当skip(), limilt(), sort()三个函数放在一起执行的时候，
执行的顺序是先 sort(), 然后是 skip()，最后是显示的 limit()。
**索引**
与RDBMS一样，MongoDB也支持索引，其创建方法如下：

    db.collection.createIndex(keys, options)
    例如：db.test.createIndex({"title":1,"description":-1})
keys可以理解为RDBMS索引中的索引列，列对应的1/-1表示的语义与sort()函数一样，options是一些可选参数。

|Parameter|Type|default|Description|
|:---:|:---:|:---:|:---:|
|background|Boolean|false|建索引过程会阻塞其它数据库操作，background可指定以后台方式创建索引|
|unique|Boolean|false|建立的索引是否唯一|
|name|string|索引的字段名和排序顺序|索引的名称|
|dropDups|Boolean|false|在建立唯一索引时是否删除重复记录|.
|sparse|Boolean|false|对文档中不存在的字段数据不启用索引；特别注意，为true时，在索引字段中不会查询出不包含对应字段的文档|
|expireAfterSeconds|integer||指定一个以秒为单位的数值，完成 TTL设定，设定集合的生存时间|
|v|index version|默认的索引版本取决于mongod创建索引时运行的版本|索引的版本号|
|weights|document|1~99999|索引权重值，表示该索引相对于其他索引字段的得分权重|
|default_language|string|英文|对于文本索引，该参数决定了停用词及词干和词器的规则的列表|
|language_override|string|language|对于文本索引，该参数指定了包含在文档中的字段名|
**聚合**
聚合(aggregate)主要用于处理数据(诸如统计平均值，求和等)，并返回计算后的数据结果。有点类似sql语句中的 count(*)。
MongoDB中聚合的方法使用aggregate()。语法如下：

    db.collection.aggregate(AGGREGATE_OPERATION)
    例如：
    db.collection.aggregate([{$group : {_id : "$user", num_tutorial : {$sum : 1}}}])
    =
    select user, count(*) from collection group by user
以下是AGGREGATE_OPERATION中的一些聚合表达式的例子

|表达式|描述|实例|
|:---:|:---:|:---:|
|$sum|总和|db.collection.aggregate([{$group : {_id : "$user", num_tutorial : {$sum : "$likes"}}}])
|$avg|平均值|db.collection.aggregate([{$group : {_id : "$user", num_tutorial : {$avg : "$likes"}}}])
|$min|最小值|db.collection.aggregate([{$group : {_id : "$user", num_tutorial : {$min : "$likes"}}}])
|$max|最大值|db.collection.aggregate([{$group : {_id : "$user", num_tutorial : {$max : "$likes"}}}])
|$push|在结果文档中插入值到一个数组中|db.collection.aggregate([{$group : {_id : "$user", url : {$push: "$url"}}}])
|$addToSet|在结果文档中插入值到一个数组中，但不创建副本|db.collection.aggregate([{$group : {_id : "$user", url : {$addToSet : "$url"}}}])
|$first|获取第一个文档数据|db.collection.aggregate([{$group : {_id : "$user", first_url : {$first : "$url"}}}])
|$last|获取最后一个文档数据|db.collection.aggregate([{$group : {_id : "$user", last_url : {$last : "$url"}}}])
**管道**
MongoDB的管道可以类比unix/linux操作系统的管道，是将上一步操作的结果输入到下一步操作。
    
    $project：修改输入文档的结构。可以用来重命名、增加或删除域，也可以用于创建计算结果以及嵌套文档。
    $match：用于过滤数据，只输出符合条件的文档。$match使用MongoDB的标准查询操作。
    $limit：用来限制MongoDB聚合管道返回的文档数。
    $skip：在聚合管道中跳过指定数量的文档，并返回余下的文档。
    $unwind：将文档中的某一个数组类型字段拆分成多条，每条包含数组中的一个值。
    $group：将集合中的文档分组，可用于统计结果。
    $sort：将输入文档排序后输出。
    $geoNear：输出接近某一地理位置的有序文档。