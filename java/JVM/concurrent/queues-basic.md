```java
/**
 * A collection designed for holding elements prior to processing. Besides basic java.util.Collection operations,
 * queues provide additional insertion, extraction, and inspection operations. Each of these methods exists in 
 * two forms: one throws an exception if the operation fails, the other returns a special value (either null or 
 * false, depending on the operation). The latter form of the insert operation is designed specifically for use 
 * with capacity-restricted Queue implementations; in most implementations, insert operations cannot fail.
 *
 * +---------+------------------+---------------+
 * |         | Throws exception | Special value |
 * +---------+------------------+---------------+
 * | Insert  | add(e)           | offer(e)      |
 * +---------+------------------+---------------+
 * | Remove  | remove()         | poll()        |
 * +---------+------------------+---------------+
 * | Examine | element()        | peek()        |
 * +---------+------------------+---------------+
 * 
 * Queues typically, but do not necessarily, order elements in a FIFO (first-in-first-out) manner. Among the 
 * exceptions are priority queues, which order elements according to a supplied comparator, or the elements' 
 * natural ordering, and LIFO queues (or stacks) which order the elements LIFO (last-in-first-out). Whatever the 
 * ordering used, the head of the queue is that element which would be removed by a call to remove() or poll(). 
 * In a FIFO queue, all new elements are inserted at the tail of the queue. Other kinds of queues may use
 * different placement rules. Every Queue implementation must specify its ordering properties.
 *
 * The offer() method inserts an element if possible, otherwise returning false. This differs from the 
 * Collection.add() method, which can fail to add an element only by throwing an unchecked exception. The offer() 
 * method is designed for use when failure is a normal, rather than exceptional occurrence, for example, 
 * in fixed-capacity (or "bounded") queues.
 *
 * Exactly which element is removed from the queue is a function of the queue's ordering policy, which differs from
 * implementation to implementation. The remove() and poll() methods differ only in their behavior when the
 * queue is empty: the remove() method throws an exception, while the poll() method returns null. The element() 
 * and peek() methods return, but do not remove, the head of the queue.
 *
 * The Queue interface does not define the blocking queue methods, which are common in concurrent programming. 
 * These methods, which wait for elements to appear or for space to become available, are defined in the 
 * java.util.concurrent.BlockingQueue interface, which extends this interface.
 *
 * Queue implementations generally do not allow insertion of null elements, although some implementations, such as
 * LinkedList, do not prohibit insertion of null. Even in the implementations that permit it, null should not be 
 * inserted into a Queue, as null is also used as a special return value by the poll method to indicate that the 
 * queue contains no elements.
 *
 * Queue implementations generally do not define element-based versions of methods equals and hashCode but instead 
 * inherit the identity based versions from class Object, because element-based equality is not always well-defined 
 * for queues with the same elements but different ordering properties.
 */
public interface Queue<E> extends Collection<E> {
    /**
     * Inserts the specified element into this queue if it is possible to do so immediately without violating 
     * capacity restrictions. using a capacity-restricted queue, add(e) will returning true upon success and 
     * throwing an IllegalStateException if no space is currently available while offer(e) will return false.
     * So the later is preferable.
     */
    boolean add(E e);
    boolean offer(E e);

    /**
     * Retrieves and removes the head of this queue. This method differs from poll() only in that it 
     * throws an exception if this queue is empty while poll() will return null.
     */
    E remove();
    E poll();

    /**
     * Retrieves, but does not remove, the head of this queue. This method differs from peek() only in that 
     * it throws an exception if this queue is empty while peek() will return null.
     */
    E element();
    E peek();
}
```
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
```java
/**
 * This class provides skeletal implementations of some Queue operations. The implementations in this class 
 * are appropriate when the base implementation does not allow null elements. Methods add(), remove(), and element() 
 * are based on offer(), poll(), and peek(), respectively, but throw exceptions instead of indicating failure via 
 * false or null returns.
 *
 * A Queue implementation that extends this class must minimally define a method offer() which does not permit
 * insertion of null elements, along with methods peek(), poll(), size(), and iterator(). Typically, additional 
 * methods will be overridden as well. If these requirements cannot be met, consider instead subclassing 
 * AbstractCollection
 */
public abstract class AbstractQueue<E> extends AbstractCollection<E> implements Queue<E> {

    // Constructor for use by subclasses.
    protected AbstractQueue() { }

    // This implementation returns true if offer()succeeds, else throws an IllegalStateException
    public boolean add(E e) {
        if (offer(e))
            return true;
        else
            throw new IllegalStateException("Queue full");
    }

    /**
     * Retrieves and removes the head of this queue. This implementation returns the result of poll() 
     * or throws an exception if this queue is empty.
     */
    public E remove() {
        E x = poll();
        if (x != null)
            return x;
        else
            throw new NoSuchElementException();
    }

    /**
     * Retrieves but not removes the head of this queue. This implementation returns the result of 
     * peek() or throws an exception if this queue is empty.
     */
    public E element() {
        E x = peek();
        if (x != null)
            return x;
        else
            throw new NoSuchElementException();
    }

    /**
     * Repeatedly invokes poll() until it returns null to removes all of the elements from this queue. 
     * The queue will be empty after this call returns.
     */
    public void clear() {
        while (poll() != null);
    }

    /**
     * Adds all of the elements in the specified collection to this queue. Attempts to addAll of a queue to 
     * itself result in IllegalArgumentException. Further, the behavior of this operation is undefined if the 
     * specified collection is modified while the operation is in progress.
     *
     * This implementation iterates over the specified collection, and adds each element returned by the iterator 
     * to this queue, in turn. A runtime exception encountered while trying to add an element (including, 
     * in particular, a null element) may result in only some of the elements having been successfully added 
     * when the associated exception is thrown.
     */
    public boolean addAll(Collection<? extends E> c) {
        if (c == null)
            throw new NullPointerException();
        if (c == this)
            throw new IllegalArgumentException();
        boolean modified = false;
        for (E e : c)
            if (add(e))
                modified = true;
        return modified;
    }
}
```