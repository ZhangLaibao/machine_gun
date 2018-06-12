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
##### 整合Spring
