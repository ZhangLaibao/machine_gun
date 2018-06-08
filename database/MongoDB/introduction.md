#### MongoDB introduction
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