```java
/**
 * Hash table based implementation of the Map interface. This implementation provides all of the optional map 
 * operations, and permits null values and the null key. (The HashMap class is roughly equivalent to Hashtable, 
 * except that it is unsynchronized and permits nulls.) This class makes no guarantees as to the order of the map; 
 * in particular, it does not guarantee that the order will remain constant over time.
 * 1.与HashTable基本等同，除了允许null key/value; 2.不保证顺序
 * 
 * This implementation provides constant-time performance for the basic operations (get() and put()), assuming 
 * the hash function disperses the elements properly among the buckets. Iteration over collection views requires 
 * time proportional to the "capacity" of the HashMap instance (the number of buckets) plus its size (the number
 * of key-value mappings). Thus, it's very important not to set the initial capacity too high (or the load factor 
 * too low) if iteration performance is important.
 * 假设元素的Hash函数能够在桶之间正确地分散元素，该实现在基本操作(get()和put())时花费恒定的时间。对集合视图的迭代
 * 需要花费与HashMap实例的“容量”(桶的数量)加上其大小(key-value mapping数量)成比例的时间。因此，如果迭代性能很重要，
 * 则不要将初始容量设置得太高(或负载因子太低)。
 *
 * An instance of HashMap has two parameters that affect its performance: initial capacity and load factor. 
 * The capacity is the number of buckets in the hash table, and the initial capacity is simply the capacity 
 * at the time the hash table is created. The load factor is a measure of how full the hash table is allowed to
 * get before its capacity is automatically increased. When the number of entries in the hash table exceeds 
 * the product of the load factor and the current capacity, the hash table is rehashed(that is, internal data
 * structures are rebuilt) so that the hash table has approximately twice the number of buckets.
 *
 * As a general rule, the default load factor (.75) offers a good tradeoff between time and space costs. Higher 
 * values decrease the space overhead but increase the lookup cost (reflected in most of the operations of the 
 * HashMap class, including get() and put()). The expected number of entries in the map and its load factor 
 * should be taken into account when setting its initial capacity, so as to minimize the number of rehash operations. 
 * If the initial capacity is greater than the maximum number of entries divided by the load factor, no rehash
 * operations will ever occur.
 * 较高的负载因子会减少空间开销，但是会降低查找的性能。
 *
 * If many mappings are to be stored in a HashMap instance, creating it with a sufficiently large capacity 
 * will allow the mappings to be stored more efficiently than letting it perform automatic rehashing as needed 
 * to grow the table. Note that using many keys with the same hashCode() is a sure way to slow down performance 
 * of any hash table. To ameliorate impact, when keys are Comparable, this class may use comparison order among
 * keys to help break ties.
 * 如果要将很多个key-value mapping存储在HashMap实例中，则使用足够大的初始容量会减少HashMap的自动扩容和rehash，
 * 从而提高性能。注意，多个具有相同hashCode()的键必然会是减慢任何基于Hash算法的数据结构的性能。
 *
 * Note that this implementation is not synchronized. If multiple threads access a hash map concurrently, and 
 * at least one of the threads modifies the map structurally, it must be synchronized externally. (A structural 
 * modification is any operation that adds or deletes one or more mappings; merely changing the value associated 
 * with a key that an instance already contains is not a structural modification.) This is typically accomplished 
 * by synchronizing on some object that naturally encapsulates the map.
 * structural modification: 结构修改是指添加或删除一个或多个key-value的任何操作; 仅更改已包含的键关联的值不是结构修改
 *
 * If no such object exists, the map should be "wrapped" using the Collections.synchronizedMap() method. This is 
 * best done at creation time, to prevent accidental unsynchronized access to the map.
 * 不推荐使用Collections.synchronizedMap()包装一个HashMap以获得线程安全性，因为其线程安全性来自于对HashMap方法
 * 使用synchronized，这样的处理方式性能很低。推荐使用ConcurrentHashMap。
 * 
 * The iterators returned by all of this class's "collection view methods" are fail-fast: if the map is 
 * structurally modified at any time after the iterator is created, in any way except through the iterator's 
 * own remove method, the iterator will throw a ConcurrentModificationException. Thus, in the face of concurrent
 * modification, the iterator fails quickly and cleanly, rather than risking arbitrary, non-deterministic behavior 
 * at an undetermined time in the future. Note that the fail-fast behavior of an iterator cannot be guaranteed 
 * as it is, generally speaking, impossible to make any hard guarantees in the presence of unsynchronized 
 * concurrent modification. Fail-fast iterators throw ConcurrentModificationException on a best-effort basis. 
 * Therefore, it would be wrong to write a program that depended on this exception for its correctness: 
 * the fail-fast behavior of iterators should be used only to detect bugs.
 * fail-fast: 在迭代器被创建之后和使用完毕之前，任何对原始HashMap的结构型修改(除非是迭代器自己的remove()方法)
 * 都会导致ConcurrentModificationException。但是这一特性只能用来检测程序bug而不能用来保证Map数据的正确性。
 */
public class HashMap<K,V> extends AbstractMap<K,V> implements Map<K,V>, Cloneable, Serializable {

    /*
     * This map usually acts as a binned (bucketed) hash table, but when bins get too large, they are transformed 
     * into bins of TreeNodes, each structured similarly to those in java.util.TreeMap. Most methods try to use 
     * normal bins, but relay to TreeNode methods when applicable (simply by checking instanceof a node). 
     * Bins of TreeNodes may be traversed and used like any others, but additionally support faster lookup
     * when overpopulated. However, since the vast majority of bins in normal use are not overpopulated, 
     * checking for existence of tree bins may be delayed in the course of table methods.
     *
     * Tree bins (i.e., bins whose elements are all TreeNodes) are ordered primarily by hashCode, but in the 
     * case of ties, if two elements are of the same "class C implements Comparable<C>" type then their compareTo 
     * method is used for ordering. (We conservatively check generic types via reflection to validate this -- 
     * see method comparableClassFor()). The added complexity of tree bins is worthwhile in providing worst-case 
     * O(log n) operations when keys either have distinct hashes or are orderable, Thus, performance degrades 
     * gracefully under accidental or malicious usages in which hashCode() methods return values that are poorly 
     * distributed, as well as those in which many keys share a hashCode, so long as they are also Comparable. 
     * (If neither of these apply, we may waste about a factor of two in time and space compared to taking no
     * precautions. But the only known cases stem from poor user programming practices that are already so slow 
     * that this makes little difference.)
     * 当bin的元素被优化成红黑树数据结构时，主要根据hashCode排序。如果两个元素满足"class C implements Comparable<C>"，
     * 那么它们的compareTo方法会被用于排序。(HashMap保守地通过反射来检查泛型类型以验证这一点，参考equivalentClassFor())。 
     * 当key的哈希值相同也不可排序时，这种优化增加的复杂性在最坏情况下的时间复杂度为O(log n)，这是值得的。
     * 在偶然状况或恶意用法造成的hashCode()方法返回值分布很烂甚至返回相同的值时，性能会优雅地降级，只要元素之间是可比较的。
     * (如果这些都不适用，与不采取任何预防措施相比，我们可能在时间和空间上浪费大约两倍。唯一已知的情况源于用户代码写的很烂。)
     *
     * Because TreeNodes are about twice the size of regular nodes, we use them only when bins contain enough 
     * nodes to warrant use (see TREEIFY_THRESHOLD). And when they become too small (due to removal or resizing) 
     * they are converted back to plain bins. In usages with well-distributed user hashCodes, tree bins are
     * rarely used. Ideally, under random hashCodes, the frequency of nodes in bins follows a Poisson distribution
     * with a parameter of about 0.5 on average for the default resizing threshold of 0.75, although with a large 
     * variance because of resizing granularity. Ignoring variance, the expected occurrences of list size k are 
     * (exp(-0.5) * pow(0.5, k) / factorial(k)).
     * 由于TreeNode是常规Node大小的两倍，所以只有某个或某些bin中元素个数达到TREEIFY_THRESHOLD(8)并且HashMap的容量达到
     * MIN_TREEIFY_CAPACITY(64)的时候，才会从列表优化成树，当元素最多的bin的元素个数减少到UNTREEIFY_THRESHOLD(6)的时候，
     * 这个树又会降级为list。 当加载因子为默认值0.75，hashCode分布较为理想时，每个bin中元素个数k满足λ=0.5的泊松分布。
     *
     * The root of a tree bin is normally its first node. However, sometimes (currently only upon Iterator.remove), 
     * the root might be elsewhere, but can be recovered following parent links(method TreeNode.root()).
     *
     * All applicable internal methods accept a hash code as an argument (as normally supplied from a public method), 
     * allowing them to call each other without recomputing user hashCodes. Most internal methods also accept a 
     * "tab" argument, that is normally the current table, but may be a new or old one when resizing or converting.
     *
     * When bin lists are treeified, split, or untreeified, we keep them in the same relative access/traversal 
     * order (i.e., field Node.next) to better preserve locality, and to slightly simplify handling of splits and 
     * traversals that invoke iterator.remove. When using comparators on insertion, to keep a total ordering 
     * (or as close as is required here) across rebalancings, we compare classes and identityHashCodes as
     * tie-breakers.
     *
     * The use and transitions among plain vs tree modes is complicated by the existence of subclass 
     * LinkedHashMap. See below for hook methods defined to be invoked upon insertion, removal and access that 
     * allow LinkedHashMap internals to otherwise remain independent of these mechanics. (This also requires 
     * that a map instance be passed to some utility methods that may create new nodes.)
     *
     * The concurrent-programming-like SSA-based coding style helps avoid aliasing errors amid all of the 
     * twisty pointer operations.
     */


}
```