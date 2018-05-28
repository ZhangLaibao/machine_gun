### BiMap
This is a brief introduction of com.google.common.collect.BiMap interface, including code clips that demonstrates the usage we oftenly applied in our codes.    
A BiMap means a Bidirectional Map, it is an extendtion of java.util.Map<K, V> interface. Additionally, It provides the functionality to preserve the uniqueness 
of its values as well as that of its keys. So the put() method will throw an IllegalArgumentException if the given value is already bound to a different key 
in this bimap, the bimap will remain unmodified in thtis event. To overide, call forcePut() instead.
We can get key via value as well as we get value via key in java.util.Map just after we call the inverse() method. This method returns an inverse view of this bimao, 
not a new map, so the two bimaps are backed by the same data, any changes to one will appear in another.    
Example:
```java
public class ABiMapExample {

    public static void main(String[] args) {
        BiMap<String, String> aweek = HashBiMap.create();
        aweek.put("星期一","Monday");
        aweek.put("星期二","Tuesday");
        aweek.put("星期三","Wednesday");
        aweek.put("星期四","Thursday");
        aweek.put("星期五","Friday");
        aweek.put("星期六","Saturday");
        aweek.put("星期日","Sunday");

        System.out.println("星期日的英文名是" + aweek.get("星期日"));
        System.out.println("Sunday的中文是" + aweek.inverse().get("Sunday"));
    }
}
```

### Joiner
In JDK 8 and new, we can use java.lang.String.join() to transfer a List<String> to a delimeter seperated String like this:    
```java
public class StringJoinExammple {

    public static void main(String[] args) {
        List<String> list = new ArrayList<String>();  
        list.add("aaa");  
        list.add("bss");  
        list.add("cdd");  
        String str = String.join(",", list);  
    }
}
```
But in JDK 7 or older, the situation is not that convinient:
but the goole guava Joiner will help:
```java
public class GuavaJoinerExammple {

    public static void main(String[] args) {
        List<String> list = new ArrayList<String>();  
        list.add("aaa");  
        list.add("bss");  
        list.add("cdd");  
        String str = Joiner.on(",").join(list);  
    }
}
```
