```java
/**
 * An optionally-bounded BlockingQueue based on linked nodes.
 * This queue orders elements FIFO (first-in-first-out). The head of the queue is that element that has been 
 * on the queue the longest time. The tail of the queue is that element that has been on the queue the shortest 
 * time. New elements are inserted at the tail of the queue, and the queue retrieval operations obtain elements 
 * at the head of the queue. Linked queues typically have higher throughput than array-based queues but less 
 * predictable performance in most concurrent applications.
 *
 * The optional capacity bound constructor argument serves as a way to prevent excessive queue expansion. 
 * The capacity, if unspecified, is equal to Integer.MAX_VALUE. Linked nodes are dynamically created upon each 
 * insertion unless this would bring the queue above capacity.
 *
 * This class and its iterator implement all of the optional methods of the Collection and Iterator interfaces.
 */
public class LinkedBlockingQueue<E> extends AbstractQueue<E> implements BlockingQueue<E>, java.io.Serializable {

    /*
     * A variant of the "two lock queue" algorithm. The putLock gates entry to put (and offer), and has an 
     * associated condition for waiting puts. Similarly for the takeLock. The "count" field that they both rely on 
     * is maintained as an atomic to avoid needing to get both locks in most cases. Also, to minimize need
     * for puts to get takeLock and vice-versa, cascading notifies are used. When a put notices that it has 
     * enabled at least one take, it signals taker. That taker in turn signals others if more items have been 
     * entered since the signal. And symmetrically for takes signalling puts. Operations such as remove(Object) and
     * iterators acquire both locks.
     *
     * Visibility between writers and readers is provided as follows:
     *
     * Whenever an element is enqueued, the putLock is acquired and count updated. A subsequent reader guarantees 
     * visibility to the enqueued Node by either acquiring the putLock (via fullyLock) or by acquiring the takeLock, 
     * and then reading n = count.get(); this gives visibility to the first n items.
     *
     * To implement weakly consistent iterators, it appears we need to keep all Nodes GC-reachable from a 
     * predecessor dequeued Node. That would cause two problems: 1.allow a rogue Iterator to cause unbounded 
     * memory retention; 2.cause cross-generational linking of old Nodes to new Nodes if a Node was tenured while 
     * live, which generational GCs have a hard time dealing with, causing repeated major collections.
     * However, only non-deleted Nodes need to be reachable from dequeued Nodes, and reachability does not 
     * necessarily have to be of the kind understood by the GC. We use the trick of linking a Node that has 
     * just been dequeued to itself. Such a self-link implicitly means to advance to head.next.
     */

    /** Linked list node class */
    static class Node<E> {
        E item;

        /**
         * 1.the real successor Node; 
         * 2.this Node, meaning the successor is head.next;
         * 3.null, meaning there is no successor (this is the last node)
         */
        Node<E> next;

        Node(E x) { item = x; }
    }

    /** The capacity bound, or Integer.MAX_VALUE if none */
    private final int capacity;

    /** Current number of elements */
    private final AtomicInteger count = new AtomicInteger();

    /** Head of linked list: head.item == null */
    transient Node<E> head;

    /** Tail of linked list: last.next == null */
    private transient Node<E> last;

    /** Lock held by take, poll, etc */
    private final ReentrantLock takeLock = new ReentrantLock();
    /** Lock held by put, offer, etc */
    private final ReentrantLock putLock = new ReentrantLock();

    /** Wait queue for waiting takes */
    private final Condition notEmpty = takeLock.newCondition();
    /** Wait queue for waiting puts */
    private final Condition notFull = putLock.newCondition();

    /** Signals a waiting take. Called only from put/offer (which do not otherwise ordinarily lock takeLock.) */
    private void signalNotEmpty() {
        final ReentrantLock takeLock = this.takeLock;
        takeLock.lock();
        try {
            notEmpty.signal();
        } finally {
            takeLock.unlock();
        }
    }

    /** Signals a waiting put. Called only from take/poll */
    private void signalNotFull() {
        final ReentrantLock putLock = this.putLock;
        putLock.lock();
        try {
            notFull.signal();
        } finally {
            putLock.unlock();
        }
    }
    
    // 关于链表结构，重点就是其入队和出队的实现

    /** 
     * Links node at end of queue
     * +------+         +------+         +------+         +------+         +------+         +------+
     * | head | ----→   | first| ----→   |      | ----→   | .... | ----→   |      | ----→   | tail | 
     * +------+         +------+         +------+         +------+         +------+         +------+
     *      |
     *      | enqueue(node): 1.tail.next = node; 2.tail = node
     *      ↓
     * +------+         +------+         +------+         +------+         +------+         +------+         +------+
     * | head | ----→   | first| ----→   |      | ----→   | .... | ----→   |      | ----→   |      | ----→   | tail |   
     * +------+         +------+         +------+         +------+         +------+         +------+         +------+
     */
    private void enqueue(Node<E> node) {
        last = last.next = node;
    }

    /** 
     * Removes a node from head of queue 
     * 
     * +------+         +------+         +------+         +------+         +------+         +------+
     * | head | ----→   | first| ----→   |      | ----→   | .... | ----→   |      | ----→   | tail | 
     * +------+         +------+         +------+         +------+         +------+         +------+
     *      |
     *      | dequeue(): 1.head = first; first.item = x; first.item = null; return x;
     *      ↓
     *                  +------+         +------+         +------+         +------+         +------+
     *                  | head | ----→   |      | ----→   | .... | ----→   |      | ----→   | tail |   
     *                  +------+         +------+         +------+         +------+         +------+
     */
    private E dequeue() {
        Node<E> h = head;
        Node<E> first = h.next;
        h.next = h; // help GC
        head = first;
        E x = first.item;
        first.item = null;
        return x;
    }

    /** Locks/Unlocks to prevent/allow both puts and takes */
    void fullyLock() {
        putLock.lock();
        takeLock.lock();
    }
    void fullyUnlock() {
        takeLock.unlock();
        putLock.unlock();
    }

    // ===========================constructors===========================
    public LinkedBlockingQueue() { }
    public LinkedBlockingQueue(int capacity) { }
    public LinkedBlockingQueue(Collection<? extends E> c) { }

    public int size() {
        return count.get();
    }
    public int remainingCapacity() {
        return capacity - count.get();
    }

    // ===========================utilities===========================
    /**
     * Inserts the specified element at the tail of this queue, waiting if
     * necessary for space to become available.
     */
    public void put(E e) throws InterruptedException {
        if (e == null) throw new NullPointerException();
        // Note: convention in all put/take/etc is to preset local var
        // holding count negative to indicate failure unless set.
        int c = -1;
        Node<E> node = new Node<E>(e);
        final ReentrantLock putLock = this.putLock;
        final AtomicInteger count = this.count;
        putLock.lockInterruptibly();
        try {
            /*
             * Note that count is used in wait guard even though it is not protected by lock. This works because 
             * count can only decrease at this point (all other puts are shut out by lock), and we (or some other 
             * waiting put) are signalled if it ever changes from capacity. Similarly for all other uses of count 
             * in other wait guards.
             */
            while (count.get() == capacity) {
                notFull.await();
            }
            enqueue(node);
            c = count.getAndIncrement();
            if (c + 1 < capacity)
                notFull.signal();
        } finally {
            putLock.unlock();
        }
        if (c == 0)
            signalNotEmpty();
    }

    /**
     * Inserts the specified element at the tail of this queue, waiting if
     * necessary up to the specified wait time for space to become available.
     */
    public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {

        if (e == null) throw new NullPointerException();
        long nanos = unit.toNanos(timeout);
        int c = -1;
        final ReentrantLock putLock = this.putLock;
        final AtomicInteger count = this.count;
        putLock.lockInterruptibly();
        try {
            while (count.get() == capacity) {
                if (nanos <= 0)
                    return false;
                nanos = notFull.awaitNanos(nanos);
            }
            enqueue(new Node<E>(e));
            c = count.getAndIncrement();
            if (c + 1 < capacity)
                notFull.signal();
        } finally {
            putLock.unlock();
        }
        if (c == 0)
            signalNotEmpty();
        return true;
    }

    /**
     * Inserts the specified element at the tail of this queue if it is possible to do so immediately without 
     * exceeding the queue's capacity, returning true upon success and false if this queue is full.
     * When using a capacity-restricted queue, this method is generally preferable to method BlockingQueue.add 
     * which can fail to insert an element only by throwing an exception.
     */
    public boolean offer(E e) {
        if (e == null) throw new NullPointerException();
        final AtomicInteger count = this.count;
        if (count.get() == capacity)
            return false;
        int c = -1;
        Node<E> node = new Node<E>(e);
        final ReentrantLock putLock = this.putLock;
        putLock.lock();
        try {
            if (count.get() < capacity) {
                enqueue(node);
                c = count.getAndIncrement();
                if (c + 1 < capacity)
                    notFull.signal();
            }
        } finally {
            putLock.unlock();
        }
        if (c == 0)
            signalNotEmpty();
        return c >= 0;
    }

    public E take() throws InterruptedException {
        E x;
        int c = -1;
        final AtomicInteger count = this.count;
        final ReentrantLock takeLock = this.takeLock;
        takeLock.lockInterruptibly();
        try {
            while (count.get() == 0) {
                notEmpty.await();
            }
            x = dequeue();
            c = count.getAndDecrement();
            if (c > 1)
                notEmpty.signal();
        } finally {
            takeLock.unlock();
        }
        if (c == capacity)
            signalNotFull();
        return x;
    }

    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        E x = null;
        int c = -1;
        long nanos = unit.toNanos(timeout);
        final AtomicInteger count = this.count;
        final ReentrantLock takeLock = this.takeLock;
        takeLock.lockInterruptibly();
        try {
            while (count.get() == 0) {
                if (nanos <= 0)
                    return null;
                nanos = notEmpty.awaitNanos(nanos);
            }
            x = dequeue();
            c = count.getAndDecrement();
            if (c > 1)
                notEmpty.signal();
        } finally {
            takeLock.unlock();
        }
        if (c == capacity)
            signalNotFull();
        return x;
    }

    public E poll() {
        final AtomicInteger count = this.count;
        if (count.get() == 0)
            return null;
        E x = null;
        int c = -1;
        final ReentrantLock takeLock = this.takeLock;
        takeLock.lock();
        try {
            if (count.get() > 0) {
                x = dequeue();
                c = count.getAndDecrement();
                if (c > 1)
                    notEmpty.signal();
            }
        } finally {
            takeLock.unlock();
        }
        if (c == capacity)
            signalNotFull();
        return x;
    }

    public E peek() {
        if (count.get() == 0)
            return null;
        final ReentrantLock takeLock = this.takeLock;
        takeLock.lock();
        try {
            Node<E> first = head.next;
            if (first == null)
                return null;
            else
                return first.item;
        } finally {
            takeLock.unlock();
        }
    }

    /** Unlinks interior Node p with predecessor trail */
    void unlink(Node<E> p, Node<E> trail) {
        // p.next is not changed, to allow iterators that are
        // traversing p to maintain their weak-consistency guarantee.
        p.item = null;
        trail.next = p.next;
        if (last == p)
            last = trail;
        if (count.getAndDecrement() == capacity)
            notFull.signal();
    }

    /**
     * Removes a single instance of the specified element from this queue, if it is present. Returns true if 
     * this queue contained the specified element (or equivalently, if this queue changed as a result of the call).
     */
    public boolean remove(Object o) {
        if (o == null) return false;
        fullyLock();
        try {
            for (Node<E> trail = head, p = trail.next;
                 p != null;
                 trail = p, p = p.next) {
                if (o.equals(p.item)) {
                    unlink(p, trail);
                    return true;
                }
            }
            return false;
        } finally {
            fullyUnlock();
        }
    }

    /** Returns true if this queue contains the specified element */
    public boolean contains(Object o) {
        if (o == null) return false;
        fullyLock();
        try {
            for (Node<E> p = head.next; p != null; p = p.next)
                if (o.equals(p.item))
                    return true;
            return false;
        } finally {
            fullyUnlock();
        }
    }

    /** Atomically removes all of the elements from this queue. The queue will be empty after this call returns */
    public void clear() {
        fullyLock();
        try {
            for (Node<E> p, h = head; (p = h.next) != null; h = p) {
                h.next = h;
                p.item = null;
            }
            head = last;
            if (count.getAndSet(0) == capacity)
                notFull.signal();
        } finally {
            fullyUnlock();
        }
    }

    public int drainTo(Collection<? super E> c) {
        return drainTo(c, Integer.MAX_VALUE);
    }

    public int drainTo(Collection<? super E> c, int maxElements) {
        if (c == null)
            throw new NullPointerException();
        if (c == this)
            throw new IllegalArgumentException();
        if (maxElements <= 0)
            return 0;
        boolean signalNotFull = false;
        final ReentrantLock takeLock = this.takeLock;
        takeLock.lock();
        try {
            int n = Math.min(maxElements, count.get());
            // count.get provides visibility to first n Nodes
            Node<E> h = head;
            int i = 0;
            try {
                while (i < n) {
                    Node<E> p = h.next;
                    c.add(p.item);
                    p.item = null;
                    h.next = h;
                    h = p;
                    ++i;
                }
                return n;
            } finally {
                // Restore invariants even if c.add() threw
                if (i > 0) {
                    // assert h.item == null;
                    head = h;
                    signalNotFull = (count.getAndAdd(-i) == capacity);
                }
            }
        } finally {
            takeLock.unlock();
            if (signalNotFull)
                signalNotFull();
        }
    }

    /**
     * Returns an iterator over the elements in this queue in proper sequence. The elements will be returned 
     * in order from first (head) to last (tail). The returned iterator is weakly consistent
     */
    public Iterator<E> iterator() {
        return new Itr();
    }

    private class Itr implements Iterator<E> {
        /*
         * Basic weakly-consistent iterator. At all times hold the next item to hand out so that if hasNext() 
         * reports true, we will still have it to return even if lost race with a take etc.
         */
        private Node<E> current;
        private Node<E> lastRet;
        private E currentElement;

        Itr() {
            fullyLock();
            try {
                current = head.next;
                if (current != null)
                    currentElement = current.item;
            } finally {
                fullyUnlock();
            }
        }

        public boolean hasNext() {
            return current != null;
        }

        /**
         * Returns the next live successor of p, or null if no such.
         *
         * Unlike other traversal methods, iterators need to handle both:
         * - dequeued nodes (p.next == p)
         * - (possibly multiple) interior removed nodes (p.item == null)
         */
        private Node<E> nextNode(Node<E> p) {
            for (;;) {
                Node<E> s = p.next;
                if (s == p)
                    return head.next;
                if (s == null || s.item != null)
                    return s;
                p = s;
            }
        }

        public E next() {
            fullyLock();
            try {
                if (current == null)
                    throw new NoSuchElementException();
                E x = currentElement;
                lastRet = current;
                current = nextNode(current);
                currentElement = (current == null) ? null : current.item;
                return x;
            } finally {
                fullyUnlock();
            }
        }

        public void remove() {
            if (lastRet == null)
                throw new IllegalStateException();
            fullyLock();
            try {
                Node<E> node = lastRet;
                lastRet = null;
                for (Node<E> trail = head, p = trail.next;
                     p != null;
                     trail = p, p = p.next) {
                    if (p == node) {
                        unlink(p, trail);
                        break;
                    }
                }
            } finally {
                fullyUnlock();
            }
        }
    }

    /** A customized variant of Spliterators.IteratorSpliterator */
    static final class LBQSpliterator<E> implements Spliterator<E> {
        static final int MAX_BATCH = 1 << 25;  // max batch array size;
        final LinkedBlockingQueue<E> queue;
        Node<E> current;    // current node; null until initialized
        int batch;          // batch size for splits
        boolean exhausted;  // true when no more nodes
        long est;           // size estimate
        LBQSpliterator(LinkedBlockingQueue<E> queue) {
            this.queue = queue;
            this.est = queue.size();
        }

        public long estimateSize() { return est; }

        public Spliterator<E> trySplit() {
            Node<E> h;
            final LinkedBlockingQueue<E> q = this.queue;
            int b = batch;
            int n = (b <= 0) ? 1 : (b >= MAX_BATCH) ? MAX_BATCH : b + 1;
            if (!exhausted &&
                ((h = current) != null || (h = q.head.next) != null) &&
                h.next != null) {
                Object[] a = new Object[n];
                int i = 0;
                Node<E> p = current;
                q.fullyLock();
                try {
                    if (p != null || (p = q.head.next) != null) {
                        do {
                            if ((a[i] = p.item) != null)
                                ++i;
                        } while ((p = p.next) != null && i < n);
                    }
                } finally {
                    q.fullyUnlock();
                }
                if ((current = p) == null) {
                    est = 0L;
                    exhausted = true;
                }
                else if ((est -= i) < 0L)
                    est = 0L;
                if (i > 0) {
                    batch = i;
                    return Spliterators.spliterator
                        (a, 0, i, Spliterator.ORDERED | Spliterator.NONNULL |
                         Spliterator.CONCURRENT);
                }
            }
            return null;
        }

        public void forEachRemaining(Consumer<? super E> action) {
            if (action == null) throw new NullPointerException();
            final LinkedBlockingQueue<E> q = this.queue;
            if (!exhausted) {
                exhausted = true;
                Node<E> p = current;
                do {
                    E e = null;
                    q.fullyLock();
                    try {
                        if (p == null)
                            p = q.head.next;
                        while (p != null) {
                            e = p.item;
                            p = p.next;
                            if (e != null)
                                break;
                        }
                    } finally {
                        q.fullyUnlock();
                    }
                    if (e != null)
                        action.accept(e);
                } while (p != null);
            }
        }

        public boolean tryAdvance(Consumer<? super E> action) {
            if (action == null) throw new NullPointerException();
            final LinkedBlockingQueue<E> q = this.queue;
            if (!exhausted) {
                E e = null;
                q.fullyLock();
                try {
                    if (current == null)
                        current = q.head.next;
                    while (current != null) {
                        e = current.item;
                        current = current.next;
                        if (e != null)
                            break;
                    }
                } finally {
                    q.fullyUnlock();
                }
                if (current == null)
                    exhausted = true;
                if (e != null) {
                    action.accept(e);
                    return true;
                }
            }
            return false;
        }

        public int characteristics() {
            return Spliterator.ORDERED | Spliterator.NONNULL |
                Spliterator.CONCURRENT;
        }
    }

    /** Returns a weakly consistent Spliterator over the elements in this queue */
    public Spliterator<E> spliterator() {
        return new LBQSpliterator<E>(this);
    }

    /** Saves this queue to a stream (that is, serializes it) */
    private void writeObject(java.io.ObjectOutputStream s) throws java.io.IOException {

        fullyLock();
        try {
            // Write out any hidden stuff, plus capacity
            s.defaultWriteObject();

            // Write out all elements in the proper order.
            for (Node<E> p = head.next; p != null; p = p.next)
                s.writeObject(p.item);

            // Use trailing null as sentinel
            s.writeObject(null);
        } finally {
            fullyUnlock();
        }
    }

    /** Reconstitutes this queue from a stream (that is, deserializes it) */
    private void readObject(java.io.ObjectInputStream s) throws java.io.IOException, ClassNotFoundException {
        // Read in capacity, and any hidden stuff
        s.defaultReadObject();

        count.set(0);
        last = head = new Node<E>(null);

        // Read in all elements and place in queue
        for (;;) {
            @SuppressWarnings("unchecked")
            E item = (E)s.readObject();
            if (item == null)
                break;
            add(item);
        }
    }
}
```
#### references:
two-lock-queue:http://www.cs.rochester.edu/research/synchronization/pseudocode/queues.html
