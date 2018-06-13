#### 使用Java操作MongoDB

##### 使用原生MongoDB-Driver
使用Java客户端操作MongDB的步骤和我们已经熟知的JDBC操作很像，MongoDB同样提供了JDBC驱动。
首先我们要将mongo-java-driver-x.x.x.jar放置到我们应用的classpath下，或者使用maven管理
    
    <dependency>
        <groupId>org.mongodb</groupId>
        <artifactId>mongodb-driver</artifactId>
        <version>x.x.x</version>
    </dependency>
    
下面的例子可以简单地说明如何使用Java获取MongoDB连接并操作集合：
```java
package *;

import *;

public class Demo {

    public static void main(String[] args) {
        // 创建客户端
        MongoClient client = new MongoClient("192.168.137.27", 27017);
        // 连接数据库
        MongoDatabase database = client.getDatabase("test");
        // 连接集合
        MongoCollection<Document> collection = database.getCollection("test");
        // CRUD
        // insert()
        Document doc = new Document()
                .append("name", "Rambo")
                .append("age", 28)
                .append("addr", "Pudong Avenue")
                .append("job", "programmer");
        collection.insertOne(doc);
        // find()
        // for in loop
        FindIterable<Document> documentsAfterInsert = collection.find();
        for (Document document : documentsAfterInsert) {
            System.out.println(document);
        }

        // update()
        // updateOne 需要传入以"$set"为key, Document为值的参数
        collection.updateOne(Filters.eq("name", "Rambo"),
                new Document().append("$set", new Document().append("name", "Rambo John J.")));
//        collection.updateMany();
        // save()
        // 使用ID查询需要将ID封装成ObjectId类型的对象
        collection.replaceOne(Filters.eq("_id", new ObjectId("5b1f9bd66aa9370c6405c1a3")),
                new Document().append("blank", ""));
        // remove()
        collection.deleteOne(Filters.eq("_id", new ObjectId("5b1f9bd66aa9370c6405c1a3")));
//        collection.deleteMany();
        FindIterable<Document> documentsAfterDelete = collection.find();
        MongoCursor<Document> cursor = documentsAfterDelete.iterator();
        // iterator loop
        while (cursor.hasNext()) {
            System.out.println(cursor.next());
        }

    }
}
```
##### 整合Spring Boot
关于Spring Data的简介，我们可以参照官网：

    Spring Data’s mission is to provide a familiar and consistent, Spring-based programming model 
    for data access while still retaining the special traits of the underlying data store.
    It makes it easy to use data access technologies, relational and non-relational databases, 
    map-reduce frameworks, and cloud-based data services. This is an umbrella project 
    which contains many subprojects that are specific to a given database. 
    The projects are developed by working together with many of the companies and developers 
    that are behind these exciting technologies.
    
Spring Data对关系型数据库、非关系型数据库、搜索引擎还有Hadoop都提供了专门的子项目支持，官方的支持包括
    
    Spring Data for Apache Cassandra
    Spring Data Commons
    Spring Data Couchbase
    Spring Data Elasticsearch
    Spring Data Envers
    Spring Data for Pivotal GemFire
    Spring Data Graph
    Spring Data JDBC
    Spring Data JDBC Extensions
    Spring Data JPA
    Spring Data LDAP
    Spring Data MongoDB
    Spring Data Neo4J
    Spring Data Redis
    Spring Data REST
    Spring Data for Apache Solr
    Spring for Apache Hadoop
此外Spring Data的社区还提供了很多其他数据库的支持

    Spring Data Aerospike - Spring Data module for Aerospike.
    Spring Data ArangoDB - Spring Data module for ArangoDB.
    Spring Data Couchbase - Spring Data module for Couchbase.
    Spring Data Azure DocumentDB - Spring Data module for Microsoft Azure DocumentDB.
    Spring Data DynamoDB - Spring Data module for DynamoDB.
    Spring Data Elasticsearch - Spring Data module for Elasticsearch.
    Spring Data Hazelcast - Provides Spring Data repository support for Hazelcast.
    Spring Data Jest - Spring Data for Elasticsearch based on the Jest REST client.
    Spring Data Neo4j - Spring based, object-graph support and repositories for Neo4j.
    Spring Data Vault - Vault repositories built on top of Spring Data KeyValue.
最终我们是通过Spring Data提供的支持来整合MongoDB的，Spring Boot提供了starter，我们只需要引入如下的依赖：
    
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-mongodb</artifactId>
    </dependency>
当然，MongoDB的驱动包还是需要的。  
我们使用@org.springframework.boot.autoconfigure.SpringBootApplication来注解我们的Application，
这个注解会继承同一个包下的@EnableAutoConfiguration注解。整合时我们只需要在默认的
application.properties添加MongoDB的连接地址即可，例如：
    
    spring.data.mongodb.uri=mongodb://127.0.0.1:27017/test
这个key被spring-configuration-metadata.json引用，Spring会帮我们做好一切工作，我们只需要在我们的代码里这样

    @Autowired
    private MongoTemplate mongoTemplate;
装配mongoTemplate，就可以使用Spring为我们提供的API了。
由于MongoDB的自动创建数据库和集合的特性，我们不需要预先准备好数据库和集合，例如在使用insert()方法时，
我们只需要把我们的实体对象传入，Spring会根据对象的类名生成collection名称，而MongoDB会为我们创建这个collection。
(我们的实体类需要通过@org.springframework.data.annotation.Id注解指定ID字段)