```java
/**
 * A hash table supporting full concurrency of retrievals and high expected concurrency for updates. This class 
 * obeys the same functional specification as java.util.Hashtable, and includes versions of methods corresponding 
 * to each method of Hashtable. However, even though all operations are thread-safe, retrieval operations do not 
 * entail locking, and there is not any support for locking the entire table in a way that prevents all access. 
 * This class is fully interoperable with Hashtable in programs that rely on its thread safety but not on its 
 * synchronization details.
 *
 * Retrieval operations (including get()) generally do not block, so may overlap with update operations (including 
 * put() and remove()). Retrievals reflect the results of the most recently completed update operations holding 
 * upon their onset. (More formally, an update operation for a given key bears a happens-before relation with any 
 * (non-null) retrieval for that key reporting the updated value.)  For aggregate operations such as putAll() and 
 * clear(), concurrent retrievals may reflect insertion or removal of only some entries. Similarly, Iterators, 
 * Spliterators and Enumerations return elements reflecting the state of the hash table at some point at or since 
 * the creation of the iterator/enumeration. They do not throw ConcurrentModificationException. However, iterators 
 * are designed to be used by only one thread at a time. Bear in mind that the results of aggregate status methods 
 * including size(), isEmpty(), and containsValue() are typically useful only when a map is not undergoing 
 * concurrent updates in other threads. Otherwise the results of these methods reflect transient states that may be 
 * adequate for monitoring or estimation purposes, but not for program control.
 *
 * The table is dynamically expanded when there are too many collisions (i.e., keys that have distinct hash codes 
 * but fall into the same slot modulo the table size), with the expected average effect of maintaining roughly 
 * two bins per mapping (corresponding to a 0.75 load factor threshold for resizing). There may be much variance 
 * around this average as mappings are added and removed, but overall, this maintains a commonly accepted time/space 
 * tradeoff for hash tables. However, resizing this or any other kind of hash table may be a relatively slow 
 * operation. When possible, it is a good idea to provide a size estimate as an optional initialCapacity 
 * constructor argument. An additional optional loadFactor constructor argument provides a further means of
 * customizing initial table capacity by specifying the table density to be used in calculating the amount of 
 * space to allocate for the given number of elements. Also, for compatibility with previous versions of this 
 * class, constructors may optionally specify an expected concurrencyLevel as an additional hint for internal 
 * sizing. Note that using many keys with exactly the same hashCode() is a sure way to slow down performance of any
 * hash table. To ameliorate impact, when keys are Comparable, this class may use comparison order among keys 
 * to help break ties.
 *
 * A Set projection of a ConcurrentHashMap may be created (using newKeySet() or newKeySet(int)), or viewed
 * (using keySet(Object) when only keys are of interest, and the mapped values are (perhaps transiently) not used 
 * or all take the same mapping value.
 *
 * A ConcurrentHashMap can be used as scalable frequency map(可扩展频率图) (a form of histogram or multiset
 * (直方图或多重集的一种形式)) by using java.util.concurrent.atomic.LongAdder values and initializing via 
 * computeIfAbsent(). For example, to add a count to a ConcurrentHashMap<String,LongAdder> named freqs, 
 * you can use freqs.computeIfAbsent(k -> new LongAdder()).increment();
 *
 * This class and its views and iterators implement all of the optional methods of the Map and Iterator interfaces.
 *
 * Like Hashtable but unlike HashMap, this class does not allow null to be used as a key or value.
 *
 * ConcurrentHashMaps support a set of sequential and parallel bulk operations that, unlike most Stream methods, 
 * are designed to be safely, and often sensibly, applied even with maps that are being concurrently updated by 
 * other threads; for example, when computing a snapshot summary of the values in a shared registry. There are 
 * three kinds of operation, each with four forms, accepting functions with Keys, Values, Entries, and (Key, Value) 
 * arguments and/or return values. Because the elements of a ConcurrentHashMap are not ordered in any particular way, 
 * and may be processed in different orders in different parallel executions, the correctness of supplied functions 
 * should not depend on any ordering, or on any other objects or values that may transiently change while 
 * computation is in progress; and except for forEach actions, should ideally be side-effect-free. Bulk operations 
 * on java.util.Map.Entry objects do not support method setValue.
 *
 * 1.forEach: Perform a given action on each element. A variant form applies a given transformation on each 
 * element before performing the action.
 *
 * 2.search: Return the first available non-null result of applying a given function on each element; 
 * skipping further search when a result is found.
 *
 * 3.reduce: Accumulate each element. The supplied reduction function cannot rely on ordering (more formally, 
 * it should be both associative and commutative). There are five variants:
 *  3.1.Plain reductions. (There is not a form of this method for (key, value) function arguments since there is 
 *  no corresponding return type.)
 *  3.2.Mapped reductions that accumulate the results of a given function applied to each element.
 *  3.3.Reductions to scalar doubles, longs, and ints, using a given basis value.
 *
 * These bulk operations accept a parallelismThreshold argument. Methods proceed sequentially if the current 
 * map size is estimated to be less than the given threshold. Using a value of Long.MAX_VALUE suppresses all 
 * parallelism.  Using a value of 1 results in maximal parallelism by partitioning into enough subtasks to 
 * fully utilize the ForkJoinPool.commonPool() that is used for all parallel computations. Normally, you would 
 * initially choose one of these extreme values, and then measure performance of using in-between values that 
 * trade off overhead versus throughput.
 *
 * The concurrency properties of bulk operations follow from those of ConcurrentHashMap: Any non-null result 
 * returned from get(key) and related access methods bears a happens-before relation with the associated insertion 
 * or update. The result of any bulk operation reflects the composition of these per-element relations (but is not
 * necessarily atomic with respect to the map as a whole unless it is somehow known to be quiescent). Conversely, 
 * because keys and values in the map are never null, null serves as a reliable atomic indicator of the current 
 * lack of any result. To maintain this property, null serves as an implicit basis for all non-scalar reduction 
 * operations. For the double, long, and int versions, the basis should be one that, when combined with any other 
 * value, returns that other value (more formally, it should be the identity element for the reduction). Most common
 * reductions have these properties; for example, computing a sum with basis 0 or a minimum with basis MAX_VALUE.
 *
 * Search and transformation functions provided as arguments should similarly return null to indicate the lack of 
 * any result (in which case it is not used). In the case of mapped reductions, this also enables transformations 
 * to serve as filters, returning null (or, in the case of primitive specializations, the identity basis) if the 
 * element should not be combined. You can create compound transformations and filterings by composing them yourself 
 * under this "null means there is nothing there now" rule before using them in search or reduce operations.
 *
 * Methods accepting and/or returning Entry arguments maintain key-value associations. They may be useful for 
 * example when finding the key for the greatest value. Note that "plain" Entry arguments can be supplied using 
 * new AbstractMap.SimpleEntry(k,v).
 *
 * Bulk operations may complete abruptly, throwing an exception encountered in the application of a supplied
 * function. Bear in mind when handling such exceptions that other concurrently executing functions could also 
 * have thrown exceptions, or would have done so if the first exception had not occurred.
 *
 * Speedups for parallel compared to sequential forms are common but not guaranteed. Parallel operations 
 * involving brief functions on small maps may execute more slowly than sequential forms if the underlying 
 * work to parallelize the computation is more expensive than the computation itself. Similarly, parallelization 
 * may not lead to much actual parallelism if all processors are busy performing unrelated tasks.
 *
 * All arguments to all task methods must be non-null.
 */
public class ConcurrentHashMap<K,V> extends AbstractMap<K,V> implements ConcurrentMap<K,V>, Serializable {

    /*
     * Overview:
     *
     * The primary design goal of this hash table is to maintain concurrent readability (typically method get(), 
     * but also iterators and related methods) while minimizing update contention. Secondary goals are to keep 
     * space consumption about the same or better than java.util.HashMap, and to support high initial insertion 
     * rates on an empty table by many threads.
     * ConcurrentHashMap的两大设计目标：
     * 1.通过最小化写操作的锁竞争来保证并发可读性
     * 2.提供不高于HashMap内存占用，并支持对空表的高并发写入
     *
     * This map usually acts as a binned (bucketed) hash table. Each key-value mapping is held in a Node. 
     * Most nodes are instances of the basic Node class with hash, key, value, and next fields. However, 
     * various subclasses exist: TreeNodes are arranged in balanced trees, not lists. TreeBins hold the roots
     * of sets of TreeNodes. ForwardingNodes are placed at the heads of bins during resizing. ReservationNodes 
     * are used as placeholders while establishing values in computeIfAbsent and related methods. The types 
     * TreeBin, ForwardingNode, and ReservationNode do not hold normal user keys, values, or hashes, and are 
     * readily distinguishable during search etc because they have negative hash fields and null key and value
     * fields. (These special nodes are either uncommon or transient, so the impact of carrying around some 
     * unused fields is insignificant.)
     *
     * The table is lazily initialized to a power-of-two size upon the first insertion. Each bin in the table 
     * normally contains a list of Nodes (most often, the list has only zero or one Node). Table accesses require 
     * volatile/atomic reads, writes, and CASes.  Because there is no other way to arrange this without adding 
     * further indirections, we use intrinsics (sun.misc.Unsafe) operations.
     *
     * We use the top (sign) bit of Node hash fields for control purposes -- it is available anyway because 
     * of addressing constraints. Nodes with negative hash fields are specially handled or ignored in map methods.
     *
     * Insertion (via put or its variants) of the first node in an empty bin is performed by just CASing it to 
     * the bin. This is by far the most common case for put operations under most key/hash distributions. Other 
     * update operations (insert, delete, and replace) require locks. We do not want to waste the space required 
     * to associate a distinct lock object with each bin, so instead use the first node of a bin list itself as
     * a lock. Locking support for these locks relies on builtin "synchronized" monitors.
     *
     * Using the first node of a list as a lock does not by itself suffice though: When a node is locked, any 
     * update must first validate that it is still the first node after locking it, and retry if not. Because 
     * new nodes are always appended to lists, once a node is first in a bin, it remains first until deleted
     * or the bin becomes invalidated (upon resizing).
     *
     * The main disadvantage of per-bin locks is that other update operations on other nodes in a bin list 
     * protected by the same lock can stall, for example when user equals() or mapping functions take a long time. 
     * However, statistically, under random hash codes, this is not a common problem. Ideally, the frequency of 
     * nodes in bins follows a Poisson distribution with a parameter of about 0.5 on average, given the resizing 
     * threshold of 0.75, although with a large variance because of resizing granularity. Ignoring variance, 
     * the expected occurrences of list size k are (exp(-0.5) * pow(0.5, k) / factorial(k)). The first values are:
     *
     * 0:    0.60653066
     * 1:    0.30326533
     * 2:    0.07581633
     * 3:    0.01263606
     * 4:    0.00157952
     * 5:    0.00015795
     * 6:    0.00001316
     * 7:    0.00000094
     * 8:    0.00000006
     * more: less than 1 in ten million
     *
     * Lock contention probability for two threads accessing distinct elements is roughly 1 / (8 * elements) under 
     * random hashes.
     *
     * Actual hash code distributions encountered in practice sometimes deviate significantly from uniform randomness.
     * (在实践中遇到的实际哈希码分布有时会明显偏离均匀随机性). This includes the case when N > (1<<30), so some keys 
     * MUST collide. Similarly for dumb or hostile usages in which multiple keys are designed to have identical 
     * hash codes or ones that differs only in masked-out high bits. So we use a secondary strategy that applies 
     * when the number of nodes in a bin exceeds a threshold. These TreeBins use a balanced tree to hold nodes (a
     * specialized form of red-black trees), bounding search time to O(log N). Each search step in a TreeBin is 
     * at least twice as slow as in a regular list, but given that N cannot exceed (1<<64) (before running out of 
     * addresses) this bounds search steps, lock hold times, etc, to reasonable constants (roughly 100 nodes 
     * inspected per operation worst case) so long as keys are Comparable (which is very common -- String, Long, etc).
     * TreeBin nodes (TreeNodes) also maintain the same "next" traversal pointers as regular nodes, so can be 
     * traversed in iterators in the same way.
     *
     * The table is resized when occupancy exceeds a percentage threshold (nominally, 0.75, but see below). 
     * Any thread noticing an overfull bin may assist in resizing after the initiating thread allocates and sets up 
     * the replacement array. However, rather than stalling, these other threads may proceed with insertions etc. 
     * The use of TreeBins shields us from the worst case effects of overfilling while resizes are in progress. 
     * Resizing proceeds by transferring bins, one by one, from the table to the next table. However, threads 
     * claim small blocks of indices to transfer (via field transferIndex) before doing so, reducing contention. 
     * A generation stamp in field sizeCtl ensures that resizings do not overlap. Because we are using power-of-two 
     * expansion, the elements from each bin must either stay at same index, or move with a power of two offset. 
     * We eliminate unnecessary node creation by catching cases where old nodes can be reused because their next 
     * fields won't change. On average, only about one-sixth of them need cloning when a table doubles. The nodes 
     * they replace will be garbage collectable as soon as they are no longer referenced by any reader thread that 
     * may be in the midst of concurrently traversing table. Upon transfer, the old table bin contains only a 
     * special forwarding node (with hash field "MOVED") that contains the next table as its key. On encountering a
     * forwarding node, access and update operations restart, using the new table.
     *
     * Each bin transfer requires its bin lock, which can stall waiting for locks while resizing. However, because 
     * other threads can join in and help resize rather than contend for locks, average aggregate waits become 
     * shorter as resizing progresses. The transfer operation must also ensure that all accessible bins in both 
     * the old and new table are usable by any traversal. This is arranged in part by proceeding from the last bin 
     * (table.length - 1) up towards the first. Upon seeing a forwarding node, traversals (see class Traverser) 
     * arrange to move to the new table without revisiting nodes. To ensure that no intervening nodes are skipped 
     * even when moved out of order, a stack (see class TableStack) is created on first encounter of a forwarding 
     * node during a traversal, to maintain its place if later processing the current table. The need for these
     * save/restore mechanics is relatively rare, but when one forwarding node is encountered, typically many 
     * more will be. So Traversers use a simple caching scheme to avoid creating so many new TableStack nodes. 
     *
     * The traversal scheme also applies to partial traversals of ranges of bins (via an alternate Traverser 
     * constructor) to support partitioned aggregate operations. Also, read-only operations give up if ever 
     * forwarded to a null table, which provides support for shutdown-style clearing, which is also not
     * currently implemented.
     *
     * Lazy table initialization minimizes footprint until first use, and also avoids resizings when the first 
     * operation is from a putAll, constructor with map argument, or deserialization. These cases attempt to 
     * override the initial capacity settings, but harmlessly fail to take effect in cases of races.
     *
     * The element count is maintained using a specialization of LongAdder. We need to incorporate a 
     * specialization rather than just use a LongAdder in order to access implicit contention-sensing that leads 
     * to creation of multiple CounterCells. The counter mechanics avoid contention on updates but can encounter 
     * cache thrashing if read too frequently during concurrent access. To avoid reading so often, resizing under 
     * contention is attempted only upon adding to a bin already holding two or more nodes. Under uniform hash
     * distributions, the probability of this occurring at threshold is around 13%, meaning that only about 1 in 8 
     * puts check threshold (and after resizing, many fewer do so).
     *
     * TreeBins use a special form of comparison for search and related operations (which is the main reason we 
     * cannot use existing collections such as TreeMaps). TreeBins contain Comparable elements, but may contain 
     * others, as well as elements that are Comparable but not necessarily Comparable for the same T, so we cannot 
     * invoke compareTo among them. To handle this, the tree is ordered primarily by hash value, then by 
     * Comparable.compareTo order if applicable. On lookup at a node, if elements are not comparable or compare 
     * as 0 then both left and right children may need to be searched in the case of tied hash values. 
     * (This corresponds to the full list search that would be necessary if all elements were non-Comparable 
     * and had tied hashes.) On insertion, to keep a total ordering (or as close as is required here) across 
     * rebalancings, we compare classes and identityHashCodes as tie-breakers. The red-black balancing code is 
     * updated from pre-jdk-collections(http://gee.cs.oswego.edu/dl/classes/collections/RBCell.java) based in turn 
     * on Cormen, Leiserson, and Rivest "Introduction to Algorithms" (CLR).
     *
     * TreeBins also require an additional locking mechanism. While list traversal is always possible by readers 
     * even during updates, tree traversal is not, mainly because of tree-rotations that may change the root node 
     * and/or its linkages. TreeBins include a simple read-write lock mechanism parasitic on the main 
     * bin-synchronization strategy: Structural adjustments associated with an insertion or removal are 
     * already bin-locked (and so cannot conflict with other writers) but must wait for ongoing readers to finish. 
     * Since there can be only one such waiter, we use a simple scheme using a single "waiter" field to block 
     * writers. However, readers need never block. If the root lock is held, they proceed along the slow traversal 
     * path (via next-pointers) until the lock becomes available or the list is exhausted, whichever comes first. 
     * These cases are not fast, but maximize aggregate expected throughput.
     *
     * Maintaining API and serialization compatibility with previous versions of this class introduces several 
     * oddities. Mainly: We leave untouched but unused constructor arguments refering to concurrencyLevel. 
     * We accept a loadFactor constructor argument, but apply it only to initial table capacity (which is the only
     * time that we can guarantee to honor it.) We also declare an unused "Segment" class that is instantiated 
     * in minimal form only when serializing. Also, solely for compatibility with previous versions of this class, 
     * it extends AbstractMap, even though all of its methods are overridden, so it is just useless baggage.
     */
    
    /* ---------------- Constants -------------- */

    /**
     * The largest possible table capacity. This value must be exactly 1<<30 to stay within Java array allocation 
     * and indexing bounds for power of two table sizes, and is further required because the top two bits of 32bit 
     * hash fields are used for control purposes.
     */
    private static final int MAXIMUM_CAPACITY = 1 << 30;

    /** The default initial table capacity. Must be a power of 2, at least 1 and at most MAXIMUM_CAPACITY */
    private static final int DEFAULT_CAPACITY = 16;

    /** The largest possible (non-power of two) array size. Needed by toArray and related methods */
    static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

    /**
     * The load factor for this table. Overrides of this value in constructors affect only the initial table capacity.  
     * The actual floating point value isn't normally used -- it is simpler to use expressions such as (n - (n >>> 2)) 
     * for the associated resizing threshold.
     */
    private static final float LOAD_FACTOR = 0.75f;

    /**
     * The bin count threshold for using a tree rather than list for a bin. Bins are converted to trees when 
     * adding an element to a bin with at least this many nodes. The value must be greater than 2, and should be 
     * at least 8 to mesh with assumptions in tree removal about conversion back to plain bins upon shrinkage.
     */
    static final int TREEIFY_THRESHOLD = 8;

    /**
     * The bin count threshold for untreeifying a (split) bin during a resize operation. Should be less than 
     * TREEIFY_THRESHOLD, and at most 6 to mesh with shrinkage detection under removal.
     */
    static final int UNTREEIFY_THRESHOLD = 6;

    /**
     * The smallest table capacity for which bins may be treeified. (Otherwise the table is resized if too many 
     * nodes in a bin.) The value should be at least 4 * TREEIFY_THRESHOLD to avoid conflicts between resizing 
     * and treeification thresholds.
     */
    static final int MIN_TREEIFY_CAPACITY = 64;

    /**
     * Minimum number of rebinnings per transfer step. Ranges are subdivided to allow multiple resizer threads. 
     * This value serves as a lower bound to avoid resizers encountering excessive memory contention. The value 
     * should be at least DEFAULT_CAPACITY.
     */
    private static final int MIN_TRANSFER_STRIDE = 16;

    /** The number of bits used for generation stamp in sizeCtl. Must be at least 6 for 32bit arrays */
    private static int RESIZE_STAMP_BITS = 16;

    /** The maximum number of threads that can help resize. Must fit in 32 - RESIZE_STAMP_BITS bits */
    private static final int MAX_RESIZERS = (1 << (32 - RESIZE_STAMP_BITS)) - 1;

    /** The bit shift for recording size stamp in sizeCtl */
    private static final int RESIZE_STAMP_SHIFT = 32 - RESIZE_STAMP_BITS;

    /* Encodings for Node hash fields. See above for explanation */
    static final int MOVED     = -1; // hash for forwarding nodes
    static final int TREEBIN   = -2; // hash for roots of trees
    static final int RESERVED  = -3; // hash for transient reservations
    static final int HASH_BITS = 0x7fffffff; // usable bits of normal node hash

    /** Number of CPUS, to place bounds on some sizings */
    static final int NCPU = Runtime.getRuntime().availableProcessors();

    /** For serialization compatibility. */
    private static final ObjectStreamField[] serialPersistentFields = {
        new ObjectStreamField("segments", Segment[].class),
        new ObjectStreamField("segmentMask", Integer.TYPE),
        new ObjectStreamField("segmentShift", Integer.TYPE)
    };

    /* ---------------- Nodes -------------- */
    /**
     * Key-value entry. This class is never exported out as a user-mutable Map.Entry (i.e., one supporting setValue; 
     * see MapEntry below), but can be used for read-only traversals used in bulk tasks. Subclasses of Node with 
     * a negative hash field are special, and contain null keys and values (but are never exported). Otherwise, 
     * keys and vals are never null.
     */
    static class Node<K,V> implements Map.Entry<K,V> {
        final int hash;
        final K key;
        volatile V val;
        volatile Node<K,V> next;

        Node(int hash, K key, V val, Node<K,V> next) {
            this.hash = hash;
            this.key = key;
            this.val = val;
            this.next = next;
        }

        public final K getKey()       { return key; }
        public final V getValue()     { return val; }
        public final int hashCode()   { return key.hashCode() ^ val.hashCode(); }
        public final String toString(){ return key + "=" + val; }
        public final V setValue(V value) {
            throw new UnsupportedOperationException();
        }

        public final boolean equals(Object o) {
            Object k, v, u; 
            Map.Entry<?,?> e;
            return ((o instanceof Map.Entry) && (k = (e = (Map.Entry<?,?>)o).getKey()) != null &&
                    (v = e.getValue()) != null && (k == key || k.equals(key)) && (v == (u = val) || v.equals(u)));
        }

        /** Virtualized support for map.get(); overridden in subclasses */
        Node<K,V> find(int h, Object k) {
            Node<K,V> e = this;
            if (k != null) {
                do {
                    K ek;
                    if (e.hash == h && ((ek = e.key) == k || (ek != null && k.equals(ek))))
                        return e;
                } while ((e = e.next) != null);
            }
            return null;
        }
    }
    
    /* ---------------- Static utilities -------------- */

    /**
     * Spreads (XORs) higher bits of hash to lower and also forces top bit to 0. Because the table uses 
     * power-of-two masking, sets of hashes that vary only in bits above the current mask will always collide. 
     * (Among known examples are sets of Float keys holding consecutive whole numbers in small tables.) So we
     * apply a transform that spreads the impact of higher bits downward. There is a tradeoff between speed, 
     * utility, and quality of bit-spreading. Because many common sets of hashes are already reasonably distributed 
     * (so don't benefit from spreading), and because we use trees to handle large sets of collisions in bins, 
     * we just XOR some shifted bits in the cheapest possible way to reduce systematic lossage, as well as to 
     * incorporate impact of the highest bits that would otherwise never be used in index calculations because 
     * of table bounds.
     */
    static final int spread(int h) {
        return (h ^ (h >>> 16)) & HASH_BITS;
    }

    /** Returns a power of two table size for the given desired capacity */
    private static final int tableSizeFor(int c) {
        int n = c - 1;
        n |= n >>> 1;
        n |= n >>> 2;
        n |= n >>> 4;
        n |= n >>> 8;
        n |= n >>> 16;
        return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
    }

    /** Returns x's Class if it is of the form "class C implements Comparable<C>", else null */
    static Class<?> comparableClassFor(Object x) {
        if (x instanceof Comparable) {
            Class<?> c; Type[] ts, as; Type t; ParameterizedType p;
            if ((c = x.getClass()) == String.class) // bypass checks
                return c;
            if ((ts = c.getGenericInterfaces()) != null) {
                for (int i = 0; i < ts.length; ++i) {
                    if (((t = ts[i]) instanceof ParameterizedType) &&
                        ((p = (ParameterizedType)t).getRawType() ==
                         Comparable.class) &&
                        (as = p.getActualTypeArguments()) != null &&
                        as.length == 1 && as[0] == c) // type arg is c
                        return c;
                }
            }
        }
        return null;
    }

    /** Returns k.compareTo(x) if x matches kc (k's screened comparable class), else 0 */
    @SuppressWarnings({"rawtypes","unchecked"}) // for cast to Comparable
    static int compareComparables(Class<?> kc, Object k, Object x) {
        return (x == null || x.getClass() != kc ? 0 : ((Comparable)k).compareTo(x));
    }
    
    /* ---------------- Table element access -------------- */

    /*
     * Volatile access methods are used for table elements as well as elements of in-progress next table while 
     * resizing. All uses of the tab arguments must be null checked by callers. All callers also paranoically 
     * precheck that tab's length is not zero (or an equivalent check), thus ensuring that any index argument 
     * taking the form of a hash value anded with (length - 1) is a valid index. Note that, to be correct wrt 
     * arbitrary concurrency errors by users, these checks must operate on local variables, which accounts for 
     * some odd-looking inline assignments below. Note that calls to setTabAt always occur within locked regions,
     * and so in principle require only release ordering, not full volatile semantics, but are currently coded 
     * as volatile writes to be conservative.
     */
    @SuppressWarnings("unchecked")
    static final <K,V> Node<K,V> tabAt(Node<K,V>[] tab, int i) {
        return (Node<K,V>)U.getObjectVolatile(tab, ((long)i << ASHIFT) + ABASE);
    }
    static final <K,V> boolean casTabAt(Node<K,V>[] tab, int i, Node<K,V> c, Node<K,V> v) {
        return U.compareAndSwapObject(tab, ((long)i << ASHIFT) + ABASE, c, v);
    }
    static final <K,V> void setTabAt(Node<K,V>[] tab, int i, Node<K,V> v) {
        U.putObjectVolatile(tab, ((long)i << ASHIFT) + ABASE, v);
    }
    
    /* ---------------- Fields -------------- */

    /**
     * The array of bins. Lazily initialized upon first insertion.
     * Size is always a power of two. Accessed directly by iterators.
     */
    transient volatile Node<K,V>[] table;

    /** The next table to use; non-null only while resizing */
    private transient volatile Node<K,V>[] nextTable;

    /**
     * Base counter value, used mainly when there is no contention, but also as a fallback during table 
     * initialization races. Updated via CAS.
     */
    private transient volatile long baseCount;

    /**
     * Table initialization and resizing control. When negative, the table is being initialized or resized: 
     * -1 for initialization, else -(1 + the number of active resizing threads). Otherwise, when table is null, 
     * holds the initial table size to use upon creation, or 0 for default. After initialization, holds the
     * next element count value upon which to resize the table.
     */
    private transient volatile int sizeCtl;

    /** The next table index (plus one) to split while resizing */
    private transient volatile int transferIndex;

    /** Spinlock (locked via CAS) used when resizing and/or creating CounterCells */
    private transient volatile int cellsBusy;

    /** Table of counter cells. When non-null, size is a power of 2 */
    private transient volatile CounterCell[] counterCells;

    // views
    private transient KeySetView<K,V> keySet;
    private transient ValuesView<K,V> values;
    private transient EntrySetView<K,V> entrySet;
    
    /* ---------------- Public operations -------------- */

    /** Creates a new, empty map with the default initial table size (16) */
    public ConcurrentHashMap() { }
    /**
     * Creates a new, empty map with an initial table size accommodating the specified number of elements 
     * without the need to dynamically resize.
     */
    public ConcurrentHashMap(int initialCapacity) {
        if (initialCapacity < 0) throw new IllegalArgumentException();
        int cap = ((initialCapacity >= (MAXIMUM_CAPACITY >>> 1)) ? MAXIMUM_CAPACITY : 
            tableSizeFor(initialCapacity + (initialCapacity >>> 1) + 1));
        this.sizeCtl = cap;
    }
    /** Creates a new map with the same mappings as the given map */
    public ConcurrentHashMap(Map<? extends K, ? extends V> m) {
        this.sizeCtl = DEFAULT_CAPACITY;
        putAll(m);
    }
    /**
     * Creates a new, empty map with an initial table size based on the given number of elements (initialCapacity) 
     * and initial table density (loadFactor).
     */
    public ConcurrentHashMap(int initialCapacity, float loadFactor) {
        this(initialCapacity, loadFactor, 1);
    }

    /**
     * Creates a new, empty map with an initial table size based on the given number of elements(initialCapacity), 
     * table density(loadFactor), and number of concurrently updating threads (concurrencyLevel).
     */
    public ConcurrentHashMap(int initialCapacity, float loadFactor, int concurrencyLevel) {
        if (!(loadFactor > 0.0f) || initialCapacity < 0 || concurrencyLevel <= 0)
            throw new IllegalArgumentException();
        if (initialCapacity < concurrencyLevel)   // Use at least as many bins
            initialCapacity = concurrencyLevel;   // as estimated threads
        long size = (long)(1.0 + (long)initialCapacity / loadFactor);
        int cap = (size >= (long)MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : tableSizeFor((int)size);
        this.sizeCtl = cap;
    }

    // Original (since JDK1.2) Map methods
    public int size() {
        long n = sumCount();
        return ((n < 0L) ? 0 : (n > (long)Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int)n);
    }

    public boolean isEmpty() {
        return sumCount() <= 0L; // ignore transient negative values
    }

    /**
     * If this map contains a mapping from a key k to a value v such that key.equals(k), then 
     * this method returns v; otherwise it returns null.  (There can be at most one such mapping.)
     */
    public V get(Object key) {
        Node<K,V>[] tab; 
        Node<K,V> e, p; 
        int n, eh; 
        K ek;
        int h = spread(key.hashCode());
        if ((tab = table) != null && (n = tab.length) > 0 && (e = tabAt(tab, (n - 1) & h)) != null) {
            if ((eh = e.hash) == h) {
                if ((ek = e.key) == key || (ek != null && key.equals(ek)))
                    return e.val;
            }
            else if (eh < 0)
                return (p = e.find(h, key)) != null ? p.val : null;
            while ((e = e.next) != null) {
                if (e.hash == h && ((ek = e.key) == key || (ek != null && key.equals(ek))))
                    return e.val;
            }
        }
        return null;
    }

    /** Tests if the specified object is a key in this table */
    public boolean containsKey(Object key) {
        return get(key) != null;
    }

    /**
     * Returns {@code true} if this map maps one or more keys to the
     * specified value. Note: This method may require a full traversal
     * of the map, and is much slower than method {@code containsKey}.
     *
     * @param value value whose presence in this map is to be tested
     * @return {@code true} if this map maps one or more keys to the
     *         specified value
     * @throws NullPointerException if the specified value is null
     */
    public boolean containsValue(Object value) {
        if (value == null)
            throw new NullPointerException();
        Node<K,V>[] t;
        if ((t = table) != null) {
            Traverser<K,V> it = new Traverser<K,V>(t, t.length, 0, t.length);
            for (Node<K,V> p; (p = it.advance()) != null; ) {
                V v;
                if ((v = p.val) == value || (v != null && value.equals(v)))
                    return true;
            }
        }
        return false;
    }

    /**
     * Maps the specified key to the specified value in this table.
     * Neither the key nor the value can be null.
     *
     * <p>The value can be retrieved by calling the {@code get} method
     * with a key that is equal to the original key.
     *
     * @param key key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @return the previous value associated with {@code key}, or
     *         {@code null} if there was no mapping for {@code key}
     * @throws NullPointerException if the specified key or value is null
     */
    public V put(K key, V value) {
        return putVal(key, value, false);
    }

    /** Implementation for put and putIfAbsent */
    final V putVal(K key, V value, boolean onlyIfAbsent) {
        if (key == null || value == null) throw new NullPointerException();
        int hash = spread(key.hashCode());
        int binCount = 0;
        for (Node<K,V>[] tab = table;;) {
            Node<K,V> f; int n, i, fh;
            if (tab == null || (n = tab.length) == 0)
                tab = initTable();
            else if ((f = tabAt(tab, i = (n - 1) & hash)) == null) {
                if (casTabAt(tab, i, null,
                             new Node<K,V>(hash, key, value, null)))
                    break;                   // no lock when adding to empty bin
            }
            else if ((fh = f.hash) == MOVED)
                tab = helpTransfer(tab, f);
            else {
                V oldVal = null;
                synchronized (f) {
                    if (tabAt(tab, i) == f) {
                        if (fh >= 0) {
                            binCount = 1;
                            for (Node<K,V> e = f;; ++binCount) {
                                K ek;
                                if (e.hash == hash &&
                                    ((ek = e.key) == key ||
                                     (ek != null && key.equals(ek)))) {
                                    oldVal = e.val;
                                    if (!onlyIfAbsent)
                                        e.val = value;
                                    break;
                                }
                                Node<K,V> pred = e;
                                if ((e = e.next) == null) {
                                    pred.next = new Node<K,V>(hash, key,
                                                              value, null);
                                    break;
                                }
                            }
                        }
                        else if (f instanceof TreeBin) {
                            Node<K,V> p;
                            binCount = 2;
                            if ((p = ((TreeBin<K,V>)f).putTreeVal(hash, key,
                                                           value)) != null) {
                                oldVal = p.val;
                                if (!onlyIfAbsent)
                                    p.val = value;
                            }
                        }
                    }
                }
                if (binCount != 0) {
                    if (binCount >= TREEIFY_THRESHOLD)
                        treeifyBin(tab, i);
                    if (oldVal != null)
                        return oldVal;
                    break;
                }
            }
        }
        addCount(1L, binCount);
        return null;
    }

    /**
     * Copies all of the mappings from the specified map to this one.
     * These mappings replace any mappings that this map had for any of the
     * keys currently in the specified map.
     *
     * @param m mappings to be stored in this map
     */
    public void putAll(Map<? extends K, ? extends V> m) {
        tryPresize(m.size());
        for (Map.Entry<? extends K, ? extends V> e : m.entrySet())
            putVal(e.getKey(), e.getValue(), false);
    }

    /**
     * Removes the key (and its corresponding value) from this map.
     * This method does nothing if the key is not in the map.
     *
     * @param  key the key that needs to be removed
     * @return the previous value associated with {@code key}, or
     *         {@code null} if there was no mapping for {@code key}
     * @throws NullPointerException if the specified key is null
     */
    public V remove(Object key) {
        return replaceNode(key, null, null);
    }

    /**
     * Implementation for the four public remove/replace methods:
     * Replaces node value with v, conditional upon match of cv if
     * non-null.  If resulting value is null, delete.
     */
    final V replaceNode(Object key, V value, Object cv) {
        int hash = spread(key.hashCode());
        for (Node<K,V>[] tab = table;;) {
            Node<K,V> f; int n, i, fh;
            if (tab == null || (n = tab.length) == 0 ||
                (f = tabAt(tab, i = (n - 1) & hash)) == null)
                break;
            else if ((fh = f.hash) == MOVED)
                tab = helpTransfer(tab, f);
            else {
                V oldVal = null;
                boolean validated = false;
                synchronized (f) {
                    if (tabAt(tab, i) == f) {
                        if (fh >= 0) {
                            validated = true;
                            for (Node<K,V> e = f, pred = null;;) {
                                K ek;
                                if (e.hash == hash &&
                                    ((ek = e.key) == key ||
                                     (ek != null && key.equals(ek)))) {
                                    V ev = e.val;
                                    if (cv == null || cv == ev ||
                                        (ev != null && cv.equals(ev))) {
                                        oldVal = ev;
                                        if (value != null)
                                            e.val = value;
                                        else if (pred != null)
                                            pred.next = e.next;
                                        else
                                            setTabAt(tab, i, e.next);
                                    }
                                    break;
                                }
                                pred = e;
                                if ((e = e.next) == null)
                                    break;
                            }
                        }
                        else if (f instanceof TreeBin) {
                            validated = true;
                            TreeBin<K,V> t = (TreeBin<K,V>)f;
                            TreeNode<K,V> r, p;
                            if ((r = t.root) != null &&
                                (p = r.findTreeNode(hash, key, null)) != null) {
                                V pv = p.val;
                                if (cv == null || cv == pv ||
                                    (pv != null && cv.equals(pv))) {
                                    oldVal = pv;
                                    if (value != null)
                                        p.val = value;
                                    else if (t.removeTreeNode(p))
                                        setTabAt(tab, i, untreeify(t.first));
                                }
                            }
                        }
                    }
                }
                if (validated) {
                    if (oldVal != null) {
                        if (value == null)
                            addCount(-1L, -1);
                        return oldVal;
                    }
                    break;
                }
            }
        }
        return null;
    }

    /**
     * Removes all of the mappings from this map.
     */
    public void clear() {
        long delta = 0L; // negative number of deletions
        int i = 0;
        Node<K,V>[] tab = table;
        while (tab != null && i < tab.length) {
            int fh;
            Node<K,V> f = tabAt(tab, i);
            if (f == null)
                ++i;
            else if ((fh = f.hash) == MOVED) {
                tab = helpTransfer(tab, f);
                i = 0; // restart
            }
            else {
                synchronized (f) {
                    if (tabAt(tab, i) == f) {
                        Node<K,V> p = (fh >= 0 ? f :
                                       (f instanceof TreeBin) ?
                                       ((TreeBin<K,V>)f).first : null);
                        while (p != null) {
                            --delta;
                            p = p.next;
                        }
                        setTabAt(tab, i++, null);
                    }
                }
            }
        }
        if (delta != 0L)
            addCount(delta, -1);
    }

    /**
     * Returns a {@link Set} view of the keys contained in this map.
     * The set is backed by the map, so changes to the map are
     * reflected in the set, and vice-versa. The set supports element
     * removal, which removes the corresponding mapping from this map,
     * via the {@code Iterator.remove}, {@code Set.remove},
     * {@code removeAll}, {@code retainAll}, and {@code clear}
     * operations.  It does not support the {@code add} or
     * {@code addAll} operations.
     *
     * <p>The view's iterators and spliterators are
     * <a href="package-summary.html#Weakly"><i>weakly consistent</i></a>.
     *
     * <p>The view's {@code spliterator} reports {@link Spliterator#CONCURRENT},
     * {@link Spliterator#DISTINCT}, and {@link Spliterator#NONNULL}.
     *
     * @return the set view
     */
    public KeySetView<K,V> keySet() {
        KeySetView<K,V> ks;
        return (ks = keySet) != null ? ks : (keySet = new KeySetView<K,V>(this, null));
    }

    /**
     * Returns a {@link Collection} view of the values contained in this map.
     * The collection is backed by the map, so changes to the map are
     * reflected in the collection, and vice-versa.  The collection
     * supports element removal, which removes the corresponding
     * mapping from this map, via the {@code Iterator.remove},
     * {@code Collection.remove}, {@code removeAll},
     * {@code retainAll}, and {@code clear} operations.  It does not
     * support the {@code add} or {@code addAll} operations.
     *
     * <p>The view's iterators and spliterators are
     * <a href="package-summary.html#Weakly"><i>weakly consistent</i></a>.
     *
     * <p>The view's {@code spliterator} reports {@link Spliterator#CONCURRENT}
     * and {@link Spliterator#NONNULL}.
     *
     * @return the collection view
     */
    public Collection<V> values() {
        ValuesView<K,V> vs;
        return (vs = values) != null ? vs : (values = new ValuesView<K,V>(this));
    }

    /**
     * Returns a {@link Set} view of the mappings contained in this map.
     * The set is backed by the map, so changes to the map are
     * reflected in the set, and vice-versa.  The set supports element
     * removal, which removes the corresponding mapping from the map,
     * via the {@code Iterator.remove}, {@code Set.remove},
     * {@code removeAll}, {@code retainAll}, and {@code clear}
     * operations.
     *
     * <p>The view's iterators and spliterators are
     * <a href="package-summary.html#Weakly"><i>weakly consistent</i></a>.
     *
     * <p>The view's {@code spliterator} reports {@link Spliterator#CONCURRENT},
     * {@link Spliterator#DISTINCT}, and {@link Spliterator#NONNULL}.
     *
     * @return the set view
     */
    public Set<Map.Entry<K,V>> entrySet() {
        EntrySetView<K,V> es;
        return (es = entrySet) != null ? es : (entrySet = new EntrySetView<K,V>(this));
    }

    /**
     * Returns the hash code value for this {@link Map}, i.e.,
     * the sum of, for each key-value pair in the map,
     * {@code key.hashCode() ^ value.hashCode()}.
     *
     * @return the hash code value for this map
     */
    public int hashCode() {
        int h = 0;
        Node<K,V>[] t;
        if ((t = table) != null) {
            Traverser<K,V> it = new Traverser<K,V>(t, t.length, 0, t.length);
            for (Node<K,V> p; (p = it.advance()) != null; )
                h += p.key.hashCode() ^ p.val.hashCode();
        }
        return h;
    }

    /**
     * Returns a string representation of this map.  The string
     * representation consists of a list of key-value mappings (in no
     * particular order) enclosed in braces ("{@code {}}").  Adjacent
     * mappings are separated by the characters {@code ", "} (comma
     * and space).  Each key-value mapping is rendered as the key
     * followed by an equals sign ("{@code =}") followed by the
     * associated value.
     *
     * @return a string representation of this map
     */
    public String toString() {
        Node<K,V>[] t;
        int f = (t = table) == null ? 0 : t.length;
        Traverser<K,V> it = new Traverser<K,V>(t, f, 0, f);
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        Node<K,V> p;
        if ((p = it.advance()) != null) {
            for (;;) {
                K k = p.key;
                V v = p.val;
                sb.append(k == this ? "(this Map)" : k);
                sb.append('=');
                sb.append(v == this ? "(this Map)" : v);
                if ((p = it.advance()) == null)
                    break;
                sb.append(',').append(' ');
            }
        }
        return sb.append('}').toString();
    }

    /**
     * Compares the specified object with this map for equality.
     * Returns {@code true} if the given object is a map with the same
     * mappings as this map.  This operation may return misleading
     * results if either map is concurrently modified during execution
     * of this method.
     *
     * @param o object to be compared for equality with this map
     * @return {@code true} if the specified object is equal to this map
     */
    public boolean equals(Object o) {
        if (o != this) {
            if (!(o instanceof Map))
                return false;
            Map<?,?> m = (Map<?,?>) o;
            Node<K,V>[] t;
            int f = (t = table) == null ? 0 : t.length;
            Traverser<K,V> it = new Traverser<K,V>(t, f, 0, f);
            for (Node<K,V> p; (p = it.advance()) != null; ) {
                V val = p.val;
                Object v = m.get(p.key);
                if (v == null || (v != val && !v.equals(val)))
                    return false;
            }
            for (Map.Entry<?,?> e : m.entrySet()) {
                Object mk, mv, v;
                if ((mk = e.getKey()) == null ||
                    (mv = e.getValue()) == null ||
                    (v = get(mk)) == null ||
                    (mv != v && !mv.equals(v)))
                    return false;
            }
        }
        return true;
    }

    /**
     * Stripped-down version of helper class used in previous version,
     * declared for the sake of serialization compatibility
     */
    static class Segment<K,V> extends ReentrantLock implements Serializable {
        private static final long serialVersionUID = 2249069246763182397L;
        final float loadFactor;
        Segment(float lf) { this.loadFactor = lf; }
    }

    /**
     * Saves the state of the {@code ConcurrentHashMap} instance to a
     * stream (i.e., serializes it).
     * @param s the stream
     * @throws java.io.IOException if an I/O error occurs
     * @serialData
     * the key (Object) and value (Object)
     * for each key-value mapping, followed by a null pair.
     * The key-value mappings are emitted in no particular order.
     */
    private void writeObject(java.io.ObjectOutputStream s)
        throws java.io.IOException {
        // For serialization compatibility
        // Emulate segment calculation from previous version of this class
        int sshift = 0;
        int ssize = 1;
        while (ssize < DEFAULT_CONCURRENCY_LEVEL) {
            ++sshift;
            ssize <<= 1;
        }
        int segmentShift = 32 - sshift;
        int segmentMask = ssize - 1;
        @SuppressWarnings("unchecked")
        Segment<K,V>[] segments = (Segment<K,V>[])
            new Segment<?,?>[DEFAULT_CONCURRENCY_LEVEL];
        for (int i = 0; i < segments.length; ++i)
            segments[i] = new Segment<K,V>(LOAD_FACTOR);
        s.putFields().put("segments", segments);
        s.putFields().put("segmentShift", segmentShift);
        s.putFields().put("segmentMask", segmentMask);
        s.writeFields();

        Node<K,V>[] t;
        if ((t = table) != null) {
            Traverser<K,V> it = new Traverser<K,V>(t, t.length, 0, t.length);
            for (Node<K,V> p; (p = it.advance()) != null; ) {
                s.writeObject(p.key);
                s.writeObject(p.val);
            }
        }
        s.writeObject(null);
        s.writeObject(null);
        segments = null; // throw away
    }

    /**
     * Reconstitutes the instance from a stream (that is, deserializes it).
     * @param s the stream
     * @throws ClassNotFoundException if the class of a serialized object
     *         could not be found
     * @throws java.io.IOException if an I/O error occurs
     */
    private void readObject(java.io.ObjectInputStream s)
        throws java.io.IOException, ClassNotFoundException {
        /*
         * To improve performance in typical cases, we create nodes
         * while reading, then place in table once size is known.
         * However, we must also validate uniqueness and deal with
         * overpopulated bins while doing so, which requires
         * specialized versions of putVal mechanics.
         */
        sizeCtl = -1; // force exclusion for table construction
        s.defaultReadObject();
        long size = 0L;
        Node<K,V> p = null;
        for (;;) {
            @SuppressWarnings("unchecked")
            K k = (K) s.readObject();
            @SuppressWarnings("unchecked")
            V v = (V) s.readObject();
            if (k != null && v != null) {
                p = new Node<K,V>(spread(k.hashCode()), k, v, p);
                ++size;
            }
            else
                break;
        }
        if (size == 0L)
            sizeCtl = 0;
        else {
            int n;
            if (size >= (long)(MAXIMUM_CAPACITY >>> 1))
                n = MAXIMUM_CAPACITY;
            else {
                int sz = (int)size;
                n = tableSizeFor(sz + (sz >>> 1) + 1);
            }
            @SuppressWarnings("unchecked")
            Node<K,V>[] tab = (Node<K,V>[])new Node<?,?>[n];
            int mask = n - 1;
            long added = 0L;
            while (p != null) {
                boolean insertAtFront;
                Node<K,V> next = p.next, first;
                int h = p.hash, j = h & mask;
                if ((first = tabAt(tab, j)) == null)
                    insertAtFront = true;
                else {
                    K k = p.key;
                    if (first.hash < 0) {
                        TreeBin<K,V> t = (TreeBin<K,V>)first;
                        if (t.putTreeVal(h, k, p.val) == null)
                            ++added;
                        insertAtFront = false;
                    }
                    else {
                        int binCount = 0;
                        insertAtFront = true;
                        Node<K,V> q; K qk;
                        for (q = first; q != null; q = q.next) {
                            if (q.hash == h &&
                                ((qk = q.key) == k ||
                                 (qk != null && k.equals(qk)))) {
                                insertAtFront = false;
                                break;
                            }
                            ++binCount;
                        }
                        if (insertAtFront && binCount >= TREEIFY_THRESHOLD) {
                            insertAtFront = false;
                            ++added;
                            p.next = first;
                            TreeNode<K,V> hd = null, tl = null;
                            for (q = p; q != null; q = q.next) {
                                TreeNode<K,V> t = new TreeNode<K,V>
                                    (q.hash, q.key, q.val, null, null);
                                if ((t.prev = tl) == null)
                                    hd = t;
                                else
                                    tl.next = t;
                                tl = t;
                            }
                            setTabAt(tab, j, new TreeBin<K,V>(hd));
                        }
                    }
                }
                if (insertAtFront) {
                    ++added;
                    p.next = first;
                    setTabAt(tab, j, p);
                }
                p = next;
            }
            table = tab;
            sizeCtl = n - (n >>> 2);
            baseCount = added;
        }
    }

    // ConcurrentMap methods

    /**
     * {@inheritDoc}
     *
     * @return the previous value associated with the specified key,
     *         or {@code null} if there was no mapping for the key
     * @throws NullPointerException if the specified key or value is null
     */
    public V putIfAbsent(K key, V value) {
        return putVal(key, value, true);
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException if the specified key is null
     */
    public boolean remove(Object key, Object value) {
        if (key == null)
            throw new NullPointerException();
        return value != null && replaceNode(key, null, value) != null;
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException if any of the arguments are null
     */
    public boolean replace(K key, V oldValue, V newValue) {
        if (key == null || oldValue == null || newValue == null)
            throw new NullPointerException();
        return replaceNode(key, newValue, oldValue) != null;
    }

    /**
     * {@inheritDoc}
     *
     * @return the previous value associated with the specified key,
     *         or {@code null} if there was no mapping for the key
     * @throws NullPointerException if the specified key or value is null
     */
    public V replace(K key, V value) {
        if (key == null || value == null)
            throw new NullPointerException();
        return replaceNode(key, value, null);
    }

    // Overrides of JDK8+ Map extension method defaults

    /**
     * Returns the value to which the specified key is mapped, or the
     * given default value if this map contains no mapping for the
     * key.
     *
     * @param key the key whose associated value is to be returned
     * @param defaultValue the value to return if this map contains
     * no mapping for the given key
     * @return the mapping for the key, if present; else the default value
     * @throws NullPointerException if the specified key is null
     */
    public V getOrDefault(Object key, V defaultValue) {
        V v;
        return (v = get(key)) == null ? defaultValue : v;
    }

    public void forEach(BiConsumer<? super K, ? super V> action) {
        if (action == null) throw new NullPointerException();
        Node<K,V>[] t;
        if ((t = table) != null) {
            Traverser<K,V> it = new Traverser<K,V>(t, t.length, 0, t.length);
            for (Node<K,V> p; (p = it.advance()) != null; ) {
                action.accept(p.key, p.val);
            }
        }
    }

    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        if (function == null) throw new NullPointerException();
        Node<K,V>[] t;
        if ((t = table) != null) {
            Traverser<K,V> it = new Traverser<K,V>(t, t.length, 0, t.length);
            for (Node<K,V> p; (p = it.advance()) != null; ) {
                V oldValue = p.val;
                for (K key = p.key;;) {
                    V newValue = function.apply(key, oldValue);
                    if (newValue == null)
                        throw new NullPointerException();
                    if (replaceNode(key, newValue, oldValue) != null ||
                        (oldValue = get(key)) == null)
                        break;
                }
            }
        }
    }

    /**
     * If the specified key is not already associated with a value,
     * attempts to compute its value using the given mapping function
     * and enters it into this map unless {@code null}.  The entire
     * method invocation is performed atomically, so the function is
     * applied at most once per key.  Some attempted update operations
     * on this map by other threads may be blocked while computation
     * is in progress, so the computation should be short and simple,
     * and must not attempt to update any other mappings of this map.
     *
     * @param key key with which the specified value is to be associated
     * @param mappingFunction the function to compute a value
     * @return the current (existing or computed) value associated with
     *         the specified key, or null if the computed value is null
     * @throws NullPointerException if the specified key or mappingFunction
     *         is null
     * @throws IllegalStateException if the computation detectably
     *         attempts a recursive update to this map that would
     *         otherwise never complete
     * @throws RuntimeException or Error if the mappingFunction does so,
     *         in which case the mapping is left unestablished
     */
    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        if (key == null || mappingFunction == null)
            throw new NullPointerException();
        int h = spread(key.hashCode());
        V val = null;
        int binCount = 0;
        for (Node<K,V>[] tab = table;;) {
            Node<K,V> f; int n, i, fh;
            if (tab == null || (n = tab.length) == 0)
                tab = initTable();
            else if ((f = tabAt(tab, i = (n - 1) & h)) == null) {
                Node<K,V> r = new ReservationNode<K,V>();
                synchronized (r) {
                    if (casTabAt(tab, i, null, r)) {
                        binCount = 1;
                        Node<K,V> node = null;
                        try {
                            if ((val = mappingFunction.apply(key)) != null)
                                node = new Node<K,V>(h, key, val, null);
                        } finally {
                            setTabAt(tab, i, node);
                        }
                    }
                }
                if (binCount != 0)
                    break;
            }
            else if ((fh = f.hash) == MOVED)
                tab = helpTransfer(tab, f);
            else {
                boolean added = false;
                synchronized (f) {
                    if (tabAt(tab, i) == f) {
                        if (fh >= 0) {
                            binCount = 1;
                            for (Node<K,V> e = f;; ++binCount) {
                                K ek; V ev;
                                if (e.hash == h &&
                                    ((ek = e.key) == key ||
                                     (ek != null && key.equals(ek)))) {
                                    val = e.val;
                                    break;
                                }
                                Node<K,V> pred = e;
                                if ((e = e.next) == null) {
                                    if ((val = mappingFunction.apply(key)) != null) {
                                        added = true;
                                        pred.next = new Node<K,V>(h, key, val, null);
                                    }
                                    break;
                                }
                            }
                        }
                        else if (f instanceof TreeBin) {
                            binCount = 2;
                            TreeBin<K,V> t = (TreeBin<K,V>)f;
                            TreeNode<K,V> r, p;
                            if ((r = t.root) != null &&
                                (p = r.findTreeNode(h, key, null)) != null)
                                val = p.val;
                            else if ((val = mappingFunction.apply(key)) != null) {
                                added = true;
                                t.putTreeVal(h, key, val);
                            }
                        }
                    }
                }
                if (binCount != 0) {
                    if (binCount >= TREEIFY_THRESHOLD)
                        treeifyBin(tab, i);
                    if (!added)
                        return val;
                    break;
                }
            }
        }
        if (val != null)
            addCount(1L, binCount);
        return val;
    }

    /**
     * If the value for the specified key is present, attempts to
     * compute a new mapping given the key and its current mapped
     * value.  The entire method invocation is performed atomically.
     * Some attempted update operations on this map by other threads
     * may be blocked while computation is in progress, so the
     * computation should be short and simple, and must not attempt to
     * update any other mappings of this map.
     *
     * @param key key with which a value may be associated
     * @param remappingFunction the function to compute a value
     * @return the new value associated with the specified key, or null if none
     * @throws NullPointerException if the specified key or remappingFunction
     *         is null
     * @throws IllegalStateException if the computation detectably
     *         attempts a recursive update to this map that would
     *         otherwise never complete
     * @throws RuntimeException or Error if the remappingFunction does so,
     *         in which case the mapping is unchanged
     */
    public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        if (key == null || remappingFunction == null)
            throw new NullPointerException();
        int h = spread(key.hashCode());
        V val = null;
        int delta = 0;
        int binCount = 0;
        for (Node<K,V>[] tab = table;;) {
            Node<K,V> f; int n, i, fh;
            if (tab == null || (n = tab.length) == 0)
                tab = initTable();
            else if ((f = tabAt(tab, i = (n - 1) & h)) == null)
                break;
            else if ((fh = f.hash) == MOVED)
                tab = helpTransfer(tab, f);
            else {
                synchronized (f) {
                    if (tabAt(tab, i) == f) {
                        if (fh >= 0) {
                            binCount = 1;
                            for (Node<K,V> e = f, pred = null;; ++binCount) {
                                K ek;
                                if (e.hash == h &&
                                    ((ek = e.key) == key ||
                                     (ek != null && key.equals(ek)))) {
                                    val = remappingFunction.apply(key, e.val);
                                    if (val != null)
                                        e.val = val;
                                    else {
                                        delta = -1;
                                        Node<K,V> en = e.next;
                                        if (pred != null)
                                            pred.next = en;
                                        else
                                            setTabAt(tab, i, en);
                                    }
                                    break;
                                }
                                pred = e;
                                if ((e = e.next) == null)
                                    break;
                            }
                        }
                        else if (f instanceof TreeBin) {
                            binCount = 2;
                            TreeBin<K,V> t = (TreeBin<K,V>)f;
                            TreeNode<K,V> r, p;
                            if ((r = t.root) != null &&
                                (p = r.findTreeNode(h, key, null)) != null) {
                                val = remappingFunction.apply(key, p.val);
                                if (val != null)
                                    p.val = val;
                                else {
                                    delta = -1;
                                    if (t.removeTreeNode(p))
                                        setTabAt(tab, i, untreeify(t.first));
                                }
                            }
                        }
                    }
                }
                if (binCount != 0)
                    break;
            }
        }
        if (delta != 0)
            addCount((long)delta, binCount);
        return val;
    }

    /**
     * Attempts to compute a mapping for the specified key and its
     * current mapped value (or {@code null} if there is no current
     * mapping). The entire method invocation is performed atomically.
     * Some attempted update operations on this map by other threads
     * may be blocked while computation is in progress, so the
     * computation should be short and simple, and must not attempt to
     * update any other mappings of this Map.
     *
     * @param key key with which the specified value is to be associated
     * @param remappingFunction the function to compute a value
     * @return the new value associated with the specified key, or null if none
     * @throws NullPointerException if the specified key or remappingFunction
     *         is null
     * @throws IllegalStateException if the computation detectably
     *         attempts a recursive update to this map that would
     *         otherwise never complete
     * @throws RuntimeException or Error if the remappingFunction does so,
     *         in which case the mapping is unchanged
     */
    public V compute(K key,
                     BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        if (key == null || remappingFunction == null)
            throw new NullPointerException();
        int h = spread(key.hashCode());
        V val = null;
        int delta = 0;
        int binCount = 0;
        for (Node<K,V>[] tab = table;;) {
            Node<K,V> f; int n, i, fh;
            if (tab == null || (n = tab.length) == 0)
                tab = initTable();
            else if ((f = tabAt(tab, i = (n - 1) & h)) == null) {
                Node<K,V> r = new ReservationNode<K,V>();
                synchronized (r) {
                    if (casTabAt(tab, i, null, r)) {
                        binCount = 1;
                        Node<K,V> node = null;
                        try {
                            if ((val = remappingFunction.apply(key, null)) != null) {
                                delta = 1;
                                node = new Node<K,V>(h, key, val, null);
                            }
                        } finally {
                            setTabAt(tab, i, node);
                        }
                    }
                }
                if (binCount != 0)
                    break;
            }
            else if ((fh = f.hash) == MOVED)
                tab = helpTransfer(tab, f);
            else {
                synchronized (f) {
                    if (tabAt(tab, i) == f) {
                        if (fh >= 0) {
                            binCount = 1;
                            for (Node<K,V> e = f, pred = null;; ++binCount) {
                                K ek;
                                if (e.hash == h &&
                                    ((ek = e.key) == key ||
                                     (ek != null && key.equals(ek)))) {
                                    val = remappingFunction.apply(key, e.val);
                                    if (val != null)
                                        e.val = val;
                                    else {
                                        delta = -1;
                                        Node<K,V> en = e.next;
                                        if (pred != null)
                                            pred.next = en;
                                        else
                                            setTabAt(tab, i, en);
                                    }
                                    break;
                                }
                                pred = e;
                                if ((e = e.next) == null) {
                                    val = remappingFunction.apply(key, null);
                                    if (val != null) {
                                        delta = 1;
                                        pred.next =
                                            new Node<K,V>(h, key, val, null);
                                    }
                                    break;
                                }
                            }
                        }
                        else if (f instanceof TreeBin) {
                            binCount = 1;
                            TreeBin<K,V> t = (TreeBin<K,V>)f;
                            TreeNode<K,V> r, p;
                            if ((r = t.root) != null)
                                p = r.findTreeNode(h, key, null);
                            else
                                p = null;
                            V pv = (p == null) ? null : p.val;
                            val = remappingFunction.apply(key, pv);
                            if (val != null) {
                                if (p != null)
                                    p.val = val;
                                else {
                                    delta = 1;
                                    t.putTreeVal(h, key, val);
                                }
                            }
                            else if (p != null) {
                                delta = -1;
                                if (t.removeTreeNode(p))
                                    setTabAt(tab, i, untreeify(t.first));
                            }
                        }
                    }
                }
                if (binCount != 0) {
                    if (binCount >= TREEIFY_THRESHOLD)
                        treeifyBin(tab, i);
                    break;
                }
            }
        }
        if (delta != 0)
            addCount((long)delta, binCount);
        return val;
    }

    /**
     * If the specified key is not already associated with a
     * (non-null) value, associates it with the given value.
     * Otherwise, replaces the value with the results of the given
     * remapping function, or removes if {@code null}. The entire
     * method invocation is performed atomically.  Some attempted
     * update operations on this map by other threads may be blocked
     * while computation is in progress, so the computation should be
     * short and simple, and must not attempt to update any other
     * mappings of this Map.
     *
     * @param key key with which the specified value is to be associated
     * @param value the value to use if absent
     * @param remappingFunction the function to recompute a value if present
     * @return the new value associated with the specified key, or null if none
     * @throws NullPointerException if the specified key or the
     *         remappingFunction is null
     * @throws RuntimeException or Error if the remappingFunction does so,
     *         in which case the mapping is unchanged
     */
    public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        if (key == null || value == null || remappingFunction == null)
            throw new NullPointerException();
        int h = spread(key.hashCode());
        V val = null;
        int delta = 0;
        int binCount = 0;
        for (Node<K,V>[] tab = table;;) {
            Node<K,V> f; int n, i, fh;
            if (tab == null || (n = tab.length) == 0)
                tab = initTable();
            else if ((f = tabAt(tab, i = (n - 1) & h)) == null) {
                if (casTabAt(tab, i, null, new Node<K,V>(h, key, value, null))) {
                    delta = 1;
                    val = value;
                    break;
                }
            }
            else if ((fh = f.hash) == MOVED)
                tab = helpTransfer(tab, f);
            else {
                synchronized (f) {
                    if (tabAt(tab, i) == f) {
                        if (fh >= 0) {
                            binCount = 1;
                            for (Node<K,V> e = f, pred = null;; ++binCount) {
                                K ek;
                                if (e.hash == h &&
                                    ((ek = e.key) == key ||
                                     (ek != null && key.equals(ek)))) {
                                    val = remappingFunction.apply(e.val, value);
                                    if (val != null)
                                        e.val = val;
                                    else {
                                        delta = -1;
                                        Node<K,V> en = e.next;
                                        if (pred != null)
                                            pred.next = en;
                                        else
                                            setTabAt(tab, i, en);
                                    }
                                    break;
                                }
                                pred = e;
                                if ((e = e.next) == null) {
                                    delta = 1;
                                    val = value;
                                    pred.next =
                                        new Node<K,V>(h, key, val, null);
                                    break;
                                }
                            }
                        }
                        else if (f instanceof TreeBin) {
                            binCount = 2;
                            TreeBin<K,V> t = (TreeBin<K,V>)f;
                            TreeNode<K,V> r = t.root;
                            TreeNode<K,V> p = (r == null) ? null :
                                r.findTreeNode(h, key, null);
                            val = (p == null) ? value :
                                remappingFunction.apply(p.val, value);
                            if (val != null) {
                                if (p != null)
                                    p.val = val;
                                else {
                                    delta = 1;
                                    t.putTreeVal(h, key, val);
                                }
                            }
                            else if (p != null) {
                                delta = -1;
                                if (t.removeTreeNode(p))
                                    setTabAt(tab, i, untreeify(t.first));
                            }
                        }
                    }
                }
                if (binCount != 0) {
                    if (binCount >= TREEIFY_THRESHOLD)
                        treeifyBin(tab, i);
                    break;
                }
            }
        }
        if (delta != 0)
            addCount((long)delta, binCount);
        return val;
    }

    // Hashtable legacy methods

    /**
     * Legacy method testing if some key maps into the specified value
     * in this table.  This method is identical in functionality to
     * {@link #containsValue(Object)}, and exists solely to ensure
     * full compatibility with class {@link java.util.Hashtable},
     * which supported this method prior to introduction of the
     * Java Collections framework.
     *
     * @param  value a value to search for
     * @return {@code true} if and only if some key maps to the
     *         {@code value} argument in this table as
     *         determined by the {@code equals} method;
     *         {@code false} otherwise
     * @throws NullPointerException if the specified value is null
     */
    public boolean contains(Object value) {
        return containsValue(value);
    }

    /**
     * Returns an enumeration of the keys in this table.
     *
     * @return an enumeration of the keys in this table
     * @see #keySet()
     */
    public Enumeration<K> keys() {
        Node<K,V>[] t;
        int f = (t = table) == null ? 0 : t.length;
        return new KeyIterator<K,V>(t, f, 0, f, this);
    }

    /**
     * Returns an enumeration of the values in this table.
     *
     * @return an enumeration of the values in this table
     * @see #values()
     */
    public Enumeration<V> elements() {
        Node<K,V>[] t;
        int f = (t = table) == null ? 0 : t.length;
        return new ValueIterator<K,V>(t, f, 0, f, this);
    }

    // ConcurrentHashMap-only methods

    /**
     * Returns the number of mappings. This method should be used
     * instead of {@link #size} because a ConcurrentHashMap may
     * contain more mappings than can be represented as an int. The
     * value returned is an estimate; the actual count may differ if
     * there are concurrent insertions or removals.
     *
     * @return the number of mappings
     * @since 1.8
     */
    public long mappingCount() {
        long n = sumCount();
        return (n < 0L) ? 0L : n; // ignore transient negative values
    }

    /**
     * Creates a new {@link Set} backed by a ConcurrentHashMap
     * from the given type to {@code Boolean.TRUE}.
     *
     * @param <K> the element type of the returned set
     * @return the new set
     * @since 1.8
     */
    public static <K> KeySetView<K,Boolean> newKeySet() {
        return new KeySetView<K,Boolean>
            (new ConcurrentHashMap<K,Boolean>(), Boolean.TRUE);
    }

    /**
     * Creates a new {@link Set} backed by a ConcurrentHashMap
     * from the given type to {@code Boolean.TRUE}.
     *
     * @param initialCapacity The implementation performs internal
     * sizing to accommodate this many elements.
     * @param <K> the element type of the returned set
     * @return the new set
     * @throws IllegalArgumentException if the initial capacity of
     * elements is negative
     * @since 1.8
     */
    public static <K> KeySetView<K,Boolean> newKeySet(int initialCapacity) {
        return new KeySetView<K,Boolean>
            (new ConcurrentHashMap<K,Boolean>(initialCapacity), Boolean.TRUE);
    }

    /**
     * Returns a {@link Set} view of the keys in this map, using the
     * given common mapped value for any additions (i.e., {@link
     * Collection#add} and {@link Collection#addAll(Collection)}).
     * This is of course only appropriate if it is acceptable to use
     * the same value for all additions from this view.
     *
     * @param mappedValue the mapped value to use for any additions
     * @return the set view
     * @throws NullPointerException if the mappedValue is null
     */
    public KeySetView<K,V> keySet(V mappedValue) {
        if (mappedValue == null)
            throw new NullPointerException();
        return new KeySetView<K,V>(this, mappedValue);
    }
    
}
```