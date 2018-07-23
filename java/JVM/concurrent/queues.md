```java
/**
 * A java.util.Queue that additionally supports operations that wait for the queue to become non-empty when 
 * retrieving an element, and wait for space to become available in the queue when storing an element.
 *
 * BlockingQueue methods come in four forms, with different ways of handling operations that cannot be satisfied 
 * immediately, but may be satisfied at some point in the future:
 * 1.one throws an exception
 * 2.second returns a special value (either null or false, depending on the operation), 
 * 3.third blocks the current thread indefinitely until the operation can succeed
 * 4.the fourth blocks for only a given maximum time limit before giving up. 
 * 
 * These methods are summarized in the following table:
 * +---------+------------------+---------------+-----------+-----------------------+
 * |         | Throws exception | Special value | Blocks    | Times out             |
 * +---------+------------------+---------------+-----------+-----------------------+
 * | Insert  | add(e)           | offer(e)      | put(e)    | offer(e, time, unit)  |
 * +---------+------------------+---------------+-----------+-----------------------+
 * | Remove  | remove()         | poll()        | take()    | poll(time, unit)      |
 * +---------+------------------+---------------+-----------+-----------------------+
 * | Examine | element()        | peek()        | ----      | ----                  |
 * +---------+------------------+---------------+-----------+-----------------------+
 * 
 * A BlockingQueue does not accept null elements. Implementations throw NullPointerException on attempts to
 * add(), put() or offer() a null. A null is used as a sentinel value to indicate failure of poll() operations.
 *
 * A BlockingQueue may be capacity bounded. At any given time it may have a remainingCapacity beyond which no
 * additional elements can be put without blocking. A BlockingQueue without any intrinsic capacity constraints 
 * always reports a remaining capacity of Integer.MAX_VALUE.
 *
 * BlockingQueue implementations are designed to be used primarily for producer-consumer queues, but additionally 
 * support the java.util.Collection interface. So, for example, it is possible to remove an arbitrary element from 
 * a queue using remove(x). However, such operations are in general not performed very efficiently, and are 
 * intended for only occasional use, such as when a queued message is cancelled.
 *
 * BlockingQueue implementations are thread-safe. All queuing methods achieve their effects atomically using internal
 * locks or other forms of concurrency control. However, the bulk Collection operations addAll(), containsAll(), 
 * retainAll() and removeAll are not necessarily performed atomically unless specified otherwise in an implementation. 
 * So it is possible, for example, for addAll(c) to fail (throwing an exception) after adding only some of the elements 
 *
 * A BlockingQueue does not intrinsically support any kind of 'close' or 'shutdown' operation to indicate that 
 * no more items will be added. The needs and usage of such features tend to be implementation-dependent. For example, 
 * a common tactic is for producers to insert special end-of-stream or poison objects, that are interpreted 
 * accordingly when taken by consumers.
 * 
 * Memory consistency effects: As with other concurrent collections, actions in a thread prior to placing an object 
 * into a BlockingQueue happen-before actions subsequent to the access or removal of that element from the 
 * BlockingQueue in another thread.
 */
public interface BlockingQueue<E> extends Queue<E> {
    /**
     * Inserts the specified element into this queue if it is possible to do so immediately without violating 
     * capacity restrictions, returning true upon success and throwing an IllegalStateException if no space is 
     * currently available. When using a capacity-restricted queue, it is generally preferable to use offer(Object).
     */
    boolean add(E e);

    /**
     * Inserts the specified element into this queue if it is possible to do so immediately without violating 
     * capacity restrictions, returning true upon success and false if no space is currently available. When using 
     * a capacity-restricted queue, this method is generally preferable to add(), which can fail to insert an
     * element only by throwing an exception.
     */
    boolean offer(E e);

    // Inserts the specified element into this queue, waiting if necessary for space to become available.
    void put(E e) throws InterruptedException;

    /**
     * Inserts the specified element into this queue, waiting up to the specified wait time if necessary for 
     * space to become available.
     */
    boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException;

    // Retrieves and removes the head of this queue, waiting if necessary until an element becomes available.
    E take() throws InterruptedException;

    /**
     * Retrieves and removes the head of this queue, waiting up to the specified wait time if necessary 
     * for an element to become available.
     */
    E poll(long timeout, TimeUnit unit) throws InterruptedException;

    /**
     * Returns the number of additional elements that this queue can ideally (in the absence of memory or resource 
     * constraints) accept without blocking, or Integer.MAX_VALUE if there is no intrinsic limit.
     * Note that you cannot always tell if an attempt to insert an element will succeed by inspecting
     * remainingCapacity because it may be the case that another thread is about to insert or remove an element.
     */
    int remainingCapacity();

    /**
     * Removes a single instance of the specified element from this queue, if it is present. More formally, 
     * removes an element e such that o.equals(e), if this queue contains one or more such elements.
     * Returns true if this queue contained the specified element (or equivalently, if this queue changed as a 
     * result of the call).
     */
    boolean remove(Object o);

    // Returns true if this queue contains the specified element.
    boolean contains(Object o);

    /**
     * Removes all available elements from this queue and adds them to the given collection. This operation may be 
     * more efficient than repeatedly polling this queue. A failure encountered while attempting to add elements to
     * collection c may result in elements being in neither, either or both collections when the associated 
     * exception is thrown. Attempts to drain a queue to itself result in IllegalArgumentException. Further, the 
     * behavior of this operation is undefined if the specified collection is modified while the operation is 
     * in progress.
     */
    int drainTo(Collection<? super E> c);

    /**
     * Removes at most the given number of available elements from this queue and adds them to the given collection.  
     * A failure encountered while attempting to add elements to collection c may result in elements being in neither,
     * either or both collections when the associated exception is thrown. Attempts to drain a queue to itself result 
     * in IllegalArgumentException. Further, the behavior of this operation is undefined if the specified collection 
     * is modified while the operation is in progress.
     */
    int drainTo(Collection<? super E> c, int maxElements);
}

```