我们在分析java.util.concurrent包中很多重要工具，如FutureTask，多线程辅助工具CountDownLatch/Semaphore，线程池
ThreandPoolExecutor.Worker的时候都会发现AbstractQueuedSynchronizer的身影。事实上AQS的设计也是出于此目的 - 为锁
和其他同步工具类提供实现的基础框架。AQS内部管理一个关于状态信息的单一整数，该整数可以表现任何状态。
比如，Semaphore用它来表示剩余的许可数，ReentrantLock用它来表示拥有它的线程已经请求了多少次锁；
FutureTask用它来表现任务的状态(尚未开始、运行、完成和取消)。现在我们就深入分析一下这个工具的原理。
首先我们来阅读以下JDK的源代码，并整理一下其中的注释：    
```java
/**
 * Provides a framework for implementing blocking locks and related synchronizers (semaphores, events, etc) 
 * that rely on first-in-first-out (FIFO) wait queues. This class is designed to be a useful basis for 
 * most kinds of synchronizers that rely on a single atomic int value to represent state. Subclasses
 * must define the protected methods that change this state, and which define what that state means in terms of 
 * this object being acquired or released. Given these, the other methods in this class carry out all 
 * queuing and blocking mechanics. Subclasses can maintain other state fields, but only the atomically updated int 
 * value manipulated using methods getState(), setState() and compareAndSetState() is tracked with respect
 * to synchronization.
 * 
 * AQS提供了通过先进先出的阻塞队列来实现阻塞式锁和相关的同步器的基础框架。设计的理念是通过维护一个原子操作的整数来表示锁的状态。
 * 子类必须定义修改此状态的protected方法，并定义该状态值的含义，其他方法实现具体的队列和阻塞机制。子类也可以维护其他的状态量，
 * 但是只有AQS里的state状态量可以用来维护同步逻辑，并且需要通过AQS重定义好的getState()/setState()和compareAndSetState()
 * 来操作这个状态量。官方推荐的使用方式是在同步工具类中定义内部类继承AQS并重写tryAcquire(), tryRelease(), tryAcquireShared(),
 * tryReleaseShared(), isHeldExclusively()等方法，并通过AQS提供的原子方法getState(), setState(), compareAndSetState()
 * 操作状态量。在JUC包提供的并发工具类中我们也可以看到确实是这样使用的。
 * 
 * 以ReentrantLock为例，采用独占模式实现，state初始化为0，表示未锁定状态。A线程lock()时，会调用tryAcquire()独占该锁并将state+1。
 * 此后，其他线程再tryAcquire()时就会失败，直到A线程unlock()到state=0(即释放锁)为止，其它线程才有机会获取该锁。
 * 当然，释放锁之前，A线程自己是可以重复获取此锁的(state会累加)，这就是可重入的概念。但要注意，获取多少次就要释放多么次，
 * 这样才能保证state是能回到零态的。
 * 再以CountDownLatch以例，采用share模式实现，我们将一个任务分为N个子线程去执行，state也初始化为N(注意N要与线程个数一致)。
 * 这N个子线程是并行执行的，每个子线程执行完后countDown()一次，state会CAS减1。等到所有子线程都执行完后(即state=0)，
 * 会unpark()主调用线程，然后主调用线程就会从await()函数返回，继续后续动作。
 * 
 * Subclasses should be defined as non-public internal helper classes that are used to implement the 
 * synchronization properties of their enclosing class. Class AbstractQueuedSynchronizer does not implement any
 * synchronization interface. Instead it defines methods such as acquireInterruptibly() that can be invoked as
 * appropriate by concrete locks and related synchronizers to implement their public methods.
 * 
 * 子类应该被定义为非公有的内部类用来帮助实现其外部类的同步逻辑。AQS没有实现任何同步接口，而是定义了诸如
 * acquireInterruptibly()之类的方法，具体的锁和同步器调用这些方法来实现它们的公共方法。
 *
 * This class supports either or both a default exclusive mode and a shared mode. When acquired in exclusive mode,
 * attempted acquires by other threads cannot succeed. Shared mode acquires by multiple threads may (but need not) 
 * succeed. This class does not understand these differences except in the mechanical sense that when a shared mode 
 * acquire succeeds, the next waiting thread (if one exists) must also determine whether it can acquire as well. 
 * Threads waiting in the different modes share the same FIFO queue. Usually, implementation subclasses support only
 * one of these modes, but both can come into play for example in a ReadWriteLock. Subclasses that support only 
 * exclusive or only shared modes need not define the methods supporting the unused mode.
 * 
 * AQS支持独占模式和共享模式或者同时支持二者。独占模式下，一个线程获取锁之后，除非已经释放了锁，否则其他线程获取锁的操作不会成功。
 * 共享模式下多个线程同时获取锁应该会(但不一定会)返回成功。AQS不感知这些区别，除了在共享模式的机制上，当一个线程获取锁成功之后，
 * 下一个等待的线程(如果有)必须检查它自己是否能够获取锁成功。在不同模式下等待的线程共用一个队列。通常，实现类只需支持一种模式，
 * 不需要实现另外一种模式下的逻辑，但是像ReadWriteLock这种比较屌的同时支持两种模式也是可以的。
 * 
 * This class defines a nested ConditionObject class that can be used as a Condition implementation by subclasses
 * supporting exclusive mode for which method isHeldExclusively() reports whether synchronization is exclusively
 * held with respect to the current thread, method release() invoked with the current getState() value fully releases
 * this object, and acquire(), given this saved state value, eventually restores this object to its previous 
 * acquired state. No AbstractQueuedSynchronizer method otherwise creates such a condition, so if this constraint 
 * cannot be met, do not use it. The behavior of ConditionObject depends of course on the semantics of 
 * its synchronizer implementation.
 *
 * This class provides inspection, instrumentation, and monitoring methods for the internal queue, as well as 
 * similar methods for condition objects. These can be exported as desired into classes using an 
 * AbstractQueuedSynchronizer for their synchronization mechanics.
 *
 * Serialization of this class stores only the underlying atomic integer maintaining state, so deserialized objects 
 * have empty thread queues. Typical subclasses requiring serializability will define a readObject method 
 * that restores this to a known initial state upon deserialization.
 *
 * To use this class as the basis of a synchronizer, redefine tryAcquire(), tryRelease(), tryAcquireShared(), 
 * tryReleaseShared(), isHeldExclusively(), by inspecting and/or modifying the synchronization state using 
 * getState(), setState() and/or compareAndSetState():
 * 
 * Each of these methods by default throws UnsupportedOperationException. Implementations of these methods 
 * must be internally thread-safe, and should in general be short and not block. Defining these methods is the 
 * only supported means of using this class. All other methods are declared final because they cannot be 
 * independently varied.
 *
 * You may also find the inherited methods from AbstractOwnableSynchronizer useful to keep track of the thread
 * owning an exclusive synchronizer.You are encouraged to use them -- this enables monitoring and diagnostic 
 * tools to assist users in determining which threads hold locks.
 *
 * Even though this class is based on an internal FIFO queue, it does not automatically enforce FIFO acquisition 
 * policies. The core of exclusive synchronization takes the form (Shared mode is similar but may involve 
 * cascading signals.)
 * 
 * Acquire:
 *     while (!tryAcquire(arg)) {
 *        // enqueue thread if it is not already queued;
 *        // possibly block current thread;
 *     }
 *
 * Release:
 *     if (tryRelease(arg))
 *        // unblock the first queued thread;
 *
 * Because checks in acquire are invoked before enqueuing, a newly acquiring thread may barge ahead of others 
 * that are blocked and queued. However, you can, if desired, define tryAcquire() and/or tryAcquireShared() to
 * disable barging by internally invoking one or more of the inspection methods, thereby providing a fair 
 * FIFO acquisition order. In particular, most fair synchronizers can define tryAcquire() to return false 
 * if hasQueuedPredecessors() (a method specifically designed to be used by fair synchronizers) returns true.
 * Other variations are possible.
 *
 * Throughput and scalability are generally highest for the default barging (also known as greedy, renouncement, 
 * and convoy-avoidance) strategy. While this is not guaranteed to be fair or starvation-free, earlier queued 
 * threads are allowed to recontend before later queued threads, and each recontention has an unbiased chance 
 * to succeed against incoming threads. Also, while acquires do not spin in the usual sense, they may perform 
 * multiple invocations of tryAcquire interspersed with other computations before blocking. This gives most of 
 * the benefits of spins when exclusive synchronization is only briefly held, without most of the liabilities 
 * when it isn't. If so desired, you can augment this by preceding calls to acquire methods with "fast-path" 
 * checks, possibly prechecking hasContended() and/or hasQueuedThreads() to only do so if the synchronizer
 * is likely not to be contended.
 *
 * This class provides an efficient and scalable basis for synchronization in part by specializing its range of 
 * use to synchronizers that can rely on int state, acquire, and release parameters, and an internal FIFO wait queue. 
 * When this does not suffice, you can build synchronizers from a lower level using java.util.concurrent.atomic 
 * classes, your own custom java.util.Queue classes, and LockSupport blocking support.
 *
 * @since 1.5
 */
public abstract class AbstractQueuedSynchronizer extends AbstractOwnableSynchronizer implements java.io.Serializable {

    /** Creates a new AbstractQueuedSynchronizer instance with initial synchronization state of zero */
    protected AbstractQueuedSynchronizer() { }

    /**
     * Wait queue node class.
     * The wait queue is a variant of a "CLH" lock queue. CLH locks are normally used for spinlocks. We instead 
     * use them for blocking synchronizers, but use the same basic tactic of holding some of the control
     * information about a thread in the predecessor of its node. A "status" field in each node keeps track of 
     * whether a thread should block. A node is signalled when its predecessor releases. Each node of the queue 
     * otherwise serves as a specific-notification-style monitor holding a single waiting thread. The status 
     * field does NOT control whether threads are granted locks etc though. A thread may try to acquire if it is
     * first in the queue. But being first does not guarantee success; it only gives the right to contend. 
     * So the currently released contender thread may need to rewait.
     *
     * To enqueue into a CLH lock, you atomically splice it in as new tail. To dequeue, you just set the head field.
     *      +------+  prev +-----+       +-----+
     * head |      | <---- |     | <---- |     |  tail
     *      +------+       +-----+       +-----+
     * AQS中的等待队列是CLH lock queue的一种变体。CLH锁通常被用于自旋锁，而AQS用它来实现阻塞式同步器，但是使用了相同的基本策略。
     * 每一个Node对象中都维护一个"status"属性来决定一个线程是否应该被阻塞。当前驱节点被释放后，当前节点会被通知。
     * 所以每个节点都被用作保存单个等待线程的特定通知样式监视器。但是这个"status"并不会体现当前线程是否被授予了锁。
     * 一个线程所在的Node成为队列的第一个元素时，这个线程会尝试获取锁，但这仅仅是获取了竞争的机会，不保证成功。失败后会重新排队。
     * 
     * 所谓CLH队列锁是一种基于链表的可扩展、高性能、公平的自旋锁，有锁需求的线程仅仅在本地变量上自旋，它不断轮询前驱的状态，
     * 假设发现前驱释放了锁就结束自旋，表示自己获取到了锁。
     * 
     * 关于AQS中维护的Node队列，更详细的示意如图：      
     * +======+       +========+       +========+       +========+       +========+
     * | AQS  |       | Node   |       | Node   |       | Node   |       | Node   |
     * | head | ----→ | prev   | ←---- | prev   | ←---- | prev   | ←---- | prev   |
     * |      |       | next   | ----→ | next   | ----→ | next   | ----→ | next   |
     * | tail | --+   | thread |       | thread |       | thread |       | thread |
     * +======+   |   +========+       +========+       +========+       +========+
     *            +--------------------------------------------------------↑
     *
     *  setHead() +------------------------↓
     * +======+   |   +--------+       +========+       +========+       +========+
     * | AQS  |   |   ¦ Node   ¦       | Node   |       | Node   |       | Node   |
     * | head | --+   ¦ prev   ¦ ←-×-- | prev   | ←---- | prev   | ←---- | prev   |
     * |      |       ¦ next   ¦ --×-→ | next   | ----→ | next   | ----→ | next   |
     * | tail | --+   ¦ thread ¦       | thread |       | thread |       | thread |
     * +======+   |   +--------+       +========+       +========+       +========+
     *            +----------------------------------------------------------↑     
     *            
     *  setTail() 
     * +======+       +========+       +========+       +========+       +========+       +--------+
     * | AQS  |       | Node   |       | Node   |       | Node   |       | Node   |       ¦ Node   ¦
     * | head | ----→ | prev   | ←---- | prev   | ←---- | prev   | ←---- | prev   | ←---- ¦ prev   ¦
     * |      | --+   | next   | ----→ | next   | ----→ | next   | ----→ | next   | ----→ ¦ next   ¦
     * | tail | --+   | thread |       | thread |       | thread |       | thread |       ¦ thread ¦
     * +======+   |   +========+       +========+       +========+       +========+       +--------+
     *            +---------------------------------------------------------------------------↑
     *
     * Insertion into a CLH queue requires only a single atomic operation on "tail", so there is a simple atomic 
     * point of demarcation from unqueued to queued. Similarly, dequeuing involves only updating the "head". 
     * However, it takes a bit more work for nodes to determine who their successors are, in part to deal with 
     * possible cancellation due to timeouts and interrupts.
     * 
     * CLH队列的插入只需要对"tail"进行一次原子操作，因此未入列和已入列之间存在一个的原子性的临界点。同样，出列只涉及更新"head"。
     * 但是，节点需要更多的工作来确定他们的"next"是谁，部分是为了处理由于超时和中断而可能造成的取消。
     *
     * The "prev" links (not used in original CLH locks), are mainly needed to handle cancellation. If a node is 
     * cancelled, its successor is (normally) relinked to a non-cancelled predecessor. 
     * 
     * We also use "next" links to implement blocking mechanics. The thread id for each node is kept in its own node, 
     * so a predecessor signals the next node to wake up by traversing next link to determine which thread it is.
     * Determination of successor must avoid races with newly queued nodes to set the "next" fields of their 
     * predecessors.  This is solved when necessary by checking backwards from the atomically updated "tail" when 
     * a node's successor appears to be null. (Or, said differently, the next-links are an optimization so that 
     * we don't usually need a backward scan.)
     *
     * Cancellation introduces some conservatism to the basic algorithms. Since we must poll for cancellation of other
     * nodes, we can miss noticing whether a cancelled node is ahead or behind us. This is dealt with by always 
     * unparking successors upon cancellation, allowing them to stabilize on a new predecessor, unless we can 
     * identify an uncancelled predecessor who will carry this responsibility.
     *
     * CLH queues need a dummy header node to get started. But we don't create them on construction, because it 
     * would be wasted effort if there is never contention. Instead, the node is constructed and head and tail 
     * pointers are set upon first contention.
     *
     * Threads waiting on Conditions use the same nodes, but use an additional link. Conditions only need to 
     * link nodes in simple (non-concurrent) linked queues because they are only accessed when exclusively held. 
     * Upon await, a node is inserted into a condition queue. Upon signal, the node is transferred to the main queue. 
     * A special value of status field is used to mark which queue a node is on.
     */
    static final class Node {
        /** Marker to indicate a node is waiting in shared/exclusive mode */
        static final Node SHARED = new Node();
        static final Node EXCLUSIVE = null;

        /**
         * Status field, taking on only the values:
         * SIGNAL=-1:    The successor of this node is (or will soon be) blocked (via park), so the current node must
         *               unpark its successor when it releases or cancels. To avoid races, acquire methods must
         *               first indicate they need a signal, then retry the atomic acquire, and then, on failure, block.
         *               被标识为该等待唤醒状态的后继结点，当其前继结点的线程释放了同步锁或被取消，将会通知该后继结点的线程执行
         *               说白了，就是处于唤醒状态，只要前继结点释放锁，就会通知标识为SIGNAL状态的后继结点的线程执行。
         *               
         * CANCELLED=1:  This node is cancelled due to timeout or interrupt. Nodes never leave this state. 
         *               In particular, a thread with cancelled node never again blocks.
         *               在同步队列中等待的线程等待超时或被中断，需要从同步队列中取消该Node的结点，
         *               进入该状态后的结点将不会再变化。
         *               
         * CONDITION=-2: This node is currently on a condition queue. It will not be used as a sync queue node
         *               until transferred, at which time the status will be set to 0. (Use of this value here has
         *               nothing to do with the other uses of the field, but simplifies mechanics.)
         *               与Condition相关，该标识的结点处于等待队列中，结点的线程等待在Condition上，
         *               当其他线程调用了Condition的signal()方法后，CONDITION状态的结点将从等待队列转移到同步队列中，
         *               等待获取同步锁。
         *               
         * PROPAGATE=-3: A releaseShared should be propagated to other nodes. This is set (for head node only) in
         *               doReleaseShared to ensure propagation continues, even if other operations have since intervened
         *               与共享模式相关，在共享模式中，该状态标识结点的线程处于可运行状态
         *               
         *   0:          None of the above
         *
         * The values are arranged numerically to simplify use. Non-negative values mean that a node doesn't need to
         * signal. So, most code doesn't need to check for particular values, just for sign.
         *
         * The field is initialized to 0 for normal sync nodes, and CONDITION for condition nodes. It is modified 
         * using CAS (or when possible, unconditional volatile writes).
         */
        volatile int waitStatus;
        static final int CANCELLED =  1;
        static final int SIGNAL    = -1;
        static final int CONDITION = -2;
        static final int PROPAGATE = -3;
        
        /**
         * Link to predecessor node that current node/thread relies on for checking waitStatus. Assigned during 
         * enqueuing, and nulled out (for sake of GC) only upon dequeuing. Also, upon cancellation of a predecessor, 
         * we short-circuit while finding a non-cancelled one, which will always exist because the head node 
         * is never cancelled: A node becomes head only as a result of successful acquire. A cancelled thread 
         * never succeeds in acquiring, and a thread only cancels itself, not any other node.
         */
        volatile Node prev;

        /**
         * Link to the successor node that the current node/thread unparks upon release. Assigned during enqueuing, 
         * adjusted when bypassing cancelled predecessors, and nulled out (for sake of GC) when dequeued. 
         * The enq operation does not assign next field of a predecessor until after attachment, so seeing a null 
         * next field does not necessarily mean that node is at end of queue. However, if a next field appears
         * to be null, we can scan prev's from the tail to double-check. The next field of cancelled nodes is set to
         * point to the node itself instead of null, to make life easier for isOnSyncQueue.
         */
        volatile Node next;

        /** The thread that enqueued this node. Initialized on construction and nulled out after use */
        volatile Thread thread;

        /**
         * Link to next node waiting on condition, or the special value SHARED. Because condition queues 
         * are accessed only when holding in exclusive mode, we just need a simple linked queue to hold nodes 
         * while they are waiting on conditions. They are then transferred to the queue to re-acquire. 
         * And because conditions can only be exclusive, we save a field by using special value to indicate shared
         * mode.
         */
        Node nextWaiter;

        /** Returns true if node is waiting in shared mode */
        final boolean isShared() {
            return nextWaiter == SHARED;
        }

        /**
         * Returns previous node, or throws NullPointerException if null.
         * Use when predecessor cannot be null. The null check could be elided, but is present to help the VM.
         */
        final Node predecessor() throws NullPointerException {
            Node p = prev;
            if (p == null)
                throw new NullPointerException();
            else
                return p;
        }

        Node() { }   // Used to establish initial head or SHARED marker
        Node(Thread thread, Node mode) { }    // Used by addWaiter
        Node(Thread thread, int waitStatus) { }// Used by Condition

    }

    /**
     * Head of the wait queue, lazily initialized. Except for initialization, it is modified only via method setHead.
     * Tail of the wait queue, lazily initialized. Modified only via method enq to add new wait node.
     * Note: If head exists, its waitStatus is guaranteed not to be CANCELLED.
     */
    private transient volatile Node head;
    private transient volatile Node tail;

    // 整个AQS的核心，使用CAS原子读写volatile变量state实现线程通信
    private volatile int state;

    /**
     * Atomically sets synchronization state to the given updated value if the current state value equals the 
     * expected value. This operation has memory semantics of a volatile read and write. return true if successful. 
     * false return indicates that the actual value was not equal to the expected value.
     */
    protected final boolean compareAndSetState(int expect, int update) {
        return unsafe.compareAndSwapInt(this, stateOffset, expect, update);
    }

    // =====================================Queuing utilities=====================================
    /**
     * The number of nanoseconds for which it is faster to spin rather than to use timed park. A rough estimate 
     * suffices to improve responsiveness with very short timeouts.
     */
    static final long spinForTimeoutThreshold = 1000L;

    /** Inserts node into queue, initializing if necessary, return node's predecessor */
    private Node enq(final Node node) {
        for (;;) {
            Node t = tail;
            if (t == null) { // Must initialize
                if (compareAndSetHead(new Node()))
                    tail = head;//  队列为空，创建一个空的结点作为head结点，并将tail也指向它
            } else {
                node.prev = t;
                // 采用CAS自旋保证入队效果可见 基于CAS的操作可认为是无阻塞的
                // 一个线程的失败或挂起不会引起其它线程也失败或挂起 并且由于CAS操作是CPU原语，所以性能比较好。
                if (compareAndSetTail(t, node)) {
                    t.next = node;
                    return t;
                }
            }
        }
    }

    /** Creates and enqueues node for current thread and given mode */
    private Node addWaiter(Node mode) {
        Node node = new Node(Thread.currentThread(), mode);
        // Try the fast path of enq; backup to full enq on failure
        Node pred = tail;
        if (pred != null) {
            node.prev = pred;
            if (compareAndSetTail(pred, node)) {
                pred.next = node;
                return node;
            }
        }
        enq(node);
        return node;
    }

    /**
     * Sets head of queue to be node, thus dequeuing. Called only by acquire methods. Also nulls out unused fields 
     * for sake of GC and to suppress unnecessary signals and traversals.
     */
    private void setHead(Node node) {
        head = node;
        node.thread = null;
        node.prev = null;
    }

    /** Wakes up node's successor, if one exists */
    private void unparkSuccessor(Node node) {
        /*
         * If status is negative (i.e., possibly needing signal) try to clear in anticipation of signalling. 
         * It is OK if this fails or if status is changed by waiting thread.
         */
        int ws = node.waitStatus;
        if (ws < 0)
            compareAndSetWaitStatus(node, ws, 0);

        /*
         * Thread to unpark is held in successor, which is normally just the next node. But if cancelled or 
         * apparently null, traverse backwards from tail to find the actual non-cancelled successor.
         */
        Node s = node.next;
        if (s == null || s.waitStatus > 0) {
            s = null;
            for (Node t = tail; t != null && t != node; t = t.prev)
                if (t.waitStatus <= 0)
                    s = t;
        }
        if (s != null)
            LockSupport.unpark(s.thread);
    }

    /**
     * Release action for shared mode -- signals successor and ensures
     * propagation. (Note: For exclusive mode, release just amounts
     * to calling unparkSuccessor of head if it needs signal.)
     */
    private void doReleaseShared() {
        /*
         * Ensure that a release propagates, even if there are other
         * in-progress acquires/releases.  This proceeds in the usual
         * way of trying to unparkSuccessor of head if it needs
         * signal. But if it does not, status is set to PROPAGATE to
         * ensure that upon release, propagation continues.
         * Additionally, we must loop in case a new node is added
         * while we are doing this. Also, unlike other uses of
         * unparkSuccessor, we need to know if CAS to reset status
         * fails, if so rechecking.
         */
        for (;;) {
            Node h = head;
            if (h != null && h != tail) {
                int ws = h.waitStatus;
                if (ws == Node.SIGNAL) {
                    if (!compareAndSetWaitStatus(h, Node.SIGNAL, 0))
                        continue;            // loop to recheck cases
                    unparkSuccessor(h);
                }
                else if (ws == 0 && !compareAndSetWaitStatus(h, 0, Node.PROPAGATE))
                    continue;                // loop on failed CAS
            }
            if (h == head)                   // loop if head changed
                break;
        }
    }

    /**
     * Sets head of queue, and checks if successor may be waiting in shared mode, if so propagating if either 
     * propagate > 0 or PROPAGATE status was set.
     */
    private void setHeadAndPropagate(Node node, int propagate) {
        Node h = head; // Record old head for check below
        setHead(node);
        /*
         * Try to signal next queued node if:
         *   Propagation was indicated by caller, or was recorded (as h.waitStatus either before or after setHead) 
         *   by a previous operation (note: this uses sign-check of waitStatus because PROPAGATE status may 
         *   transition to SIGNAL.)
         * and
         *   The next node is waiting in shared mode, or we don't know, because it appears null
         *
         * The conservatism in both of these checks may cause unnecessary wake-ups, but only when there are multiple
         * racing acquires/releases, so most need signals now or soon anyway.
         */
        if (propagate > 0 || h == null || h.waitStatus < 0 || (h = head) == null || h.waitStatus < 0) {
            Node s = node.next;
            if (s == null || s.isShared())
                doReleaseShared();
        }
    }

    // ============================Utilities for various versions of acquire============================
    /** Cancels an ongoing attempt to acquire */
    private void cancelAcquire(Node node) {
        // Ignore if node doesn't exist
        if (node == null)
            return;

        node.thread = null;

        // Skip cancelled predecessors
        Node pred = node.prev;
        while (pred.waitStatus > 0)
            node.prev = pred = pred.prev;

        // predNext is the apparent node to unsplice. CASes below will
        // fail if not, in which case, we lost race vs another cancel
        // or signal, so no further action is necessary.
        Node predNext = pred.next;

        // Can use unconditional write instead of CAS here.
        // After this atomic step, other Nodes can skip past us.
        // Before, we are free of interference from other threads.
        node.waitStatus = Node.CANCELLED;

        // If we are the tail, remove ourselves.
        if (node == tail && compareAndSetTail(node, pred)) {
            compareAndSetNext(pred, predNext, null);
        } else {
            // If successor needs signal, try to set pred's next-link
            // so it will get one. Otherwise wake it up to propagate.
            int ws;
            if (pred != head && ((ws = pred.waitStatus) == Node.SIGNAL ||
                 (ws <= 0 && compareAndSetWaitStatus(pred, ws, Node.SIGNAL))) && pred.thread != null) {
                Node next = node.next;
                if (next != null && next.waitStatus <= 0)
                    compareAndSetNext(pred, predNext, next);
            } else {
                unparkSuccessor(node);
            }

            node.next = node; // help GC
        }
    }

    /**
     * Checks and updates status for a node that failed to acquire. Returns true if thread should block. 
     * This is the main signal control in all acquire loops.  Requires that pred == node.prev.
     */
    private static boolean shouldParkAfterFailedAcquire(Node pred, Node node) {
        int ws = pred.waitStatus;
        if (ws == Node.SIGNAL)
            // This node has already set status asking a release to signal it, so it can safely park.
            // 前驱会通知当前节点，所以可以安心地等待
            return true;
        if (ws > 0) {
            // Predecessor was cancelled. Skip over predecessors and indicate retry.
            // 前驱处于CANCELL了，那么一直往前找，直到找到正常竞争的前驱，并排在他的后面
            do {
                node.prev = pred = pred.prev;
            } while (pred.waitStatus > 0);
            pred.next = node;
        } else {
            // waitStatus must be 0 or PROPAGATE.  Indicate that we need a signal, but don't park yet.  
            // Caller will need to retry to make sure it cannot acquire before parking.
            compareAndSetWaitStatus(pred, ws, Node.SIGNAL);
        }
        return false;
    }

    /** Convenience method to interrupt current thread */
    static void selfInterrupt() {
        Thread.currentThread().interrupt();
    }

    /** Convenience method to park and then check if interrupted */
    private final boolean parkAndCheckInterrupt() {
        LockSupport.park(this);
        return Thread.interrupted();
    }

    /*
     * Various flavors of acquire, varying in exclusive/shared and control modes.  Each is mostly the same, 
     * but annoyingly different. Only a little bit of factoring is possible due to interactions of 
     * exception mechanics (including ensuring that we cancel if tryAcquire throws exception) and other control, 
     * at least not without hurting performance too much.
     */

    /**
     * Acquires in exclusive uninterruptible mode for thread already in queue. 
     * Used by condition wait methods as well as acquire.
     */
    final boolean acquireQueued(final Node node, int arg) {
        boolean failed = true;
        try {
            boolean interrupted = false;
            for (;;) {// 自旋
                final Node p = node.predecessor();// 获得前趋元素
                if (p == head && tryAcquire(arg)) {// 前驱元素为head则有资格继续获取锁
                    setHead(node);// 获取到锁之后，当前元素设置为头结点，清空其前驱后继
                    p.next = null; // help GC
                    failed = false;
                    return interrupted;
                }
                if (shouldParkAfterFailedAcquire(p, node) && parkAndCheckInterrupt())
                    interrupted = true;
            }
        } finally {
            if (failed)
                cancelAcquire(node);
        }
    }

    /** Acquires in exclusive interruptible mode */
    private void doAcquireInterruptibly(int arg)
        throws InterruptedException {
        final Node node = addWaiter(Node.EXCLUSIVE);
        boolean failed = true;
        try {
            for (;;) {
                final Node p = node.predecessor();
                if (p == head && tryAcquire(arg)) {
                    setHead(node);
                    p.next = null; // help GC
                    failed = false;
                    return;
                }
                if (shouldParkAfterFailedAcquire(p, node) && parkAndCheckInterrupt())
                    throw new InterruptedException();
            }
        } finally {
            if (failed)
                cancelAcquire(node);
        }
    }

    /** Acquires in exclusive timed mode */
    private boolean doAcquireNanos(int arg, long nanosTimeout) throws InterruptedException {
        if (nanosTimeout <= 0L)
            return false;
        final long deadline = System.nanoTime() + nanosTimeout;
        final Node node = addWaiter(Node.EXCLUSIVE);
        boolean failed = true;
        try {
            for (;;) {
                final Node p = node.predecessor();
                if (p == head && tryAcquire(arg)) {
                    setHead(node);
                    p.next = null; // help GC
                    failed = false;
                    return true;
                }
                nanosTimeout = deadline - System.nanoTime();
                if (nanosTimeout <= 0L)
                    return false;
                if (shouldParkAfterFailedAcquire(p, node) &&
                    nanosTimeout > spinForTimeoutThreshold)
                    LockSupport.parkNanos(this, nanosTimeout);
                if (Thread.interrupted())
                    throw new InterruptedException();
            }
        } finally {
            if (failed)
                cancelAcquire(node);
        }
    }

    /** Acquires in shared uninterruptible mode */
    private void doAcquireShared(int arg) {
        final Node node = addWaiter(Node.SHARED);
        boolean failed = true;
        try {
            boolean interrupted = false;
            for (;;) {
                final Node p = node.predecessor();
                if (p == head) {
                    int r = tryAcquireShared(arg);
                    if (r >= 0) {
                        setHeadAndPropagate(node, r);
                        p.next = null; // help GC
                        if (interrupted)
                            selfInterrupt();
                        failed = false;
                        return;
                    }
                }
                if (shouldParkAfterFailedAcquire(p, node) && parkAndCheckInterrupt())
                    interrupted = true;
            }
        } finally {
            if (failed)
                cancelAcquire(node);
        }
    }

    /** Acquires in shared interruptible mode */
    private void doAcquireSharedInterruptibly(int arg)
        throws InterruptedException {
        final Node node = addWaiter(Node.SHARED);
        boolean failed = true;
        try {
            for (;;) {
                final Node p = node.predecessor();
                if (p == head) {
                    int r = tryAcquireShared(arg);
                    if (r >= 0) {
                        setHeadAndPropagate(node, r);
                        p.next = null; // help GC
                        failed = false;
                        return;
                    }
                }
                if (shouldParkAfterFailedAcquire(p, node) && parkAndCheckInterrupt())
                    throw new InterruptedException();
            }
        } finally {
            if (failed)
                cancelAcquire(node);
        }
    }

    /** Acquires in shared timed mode */
    private boolean doAcquireSharedNanos(int arg, long nanosTimeout) throws InterruptedException {
        if (nanosTimeout <= 0L)
            return false;
        final long deadline = System.nanoTime() + nanosTimeout;
        final Node node = addWaiter(Node.SHARED);
        boolean failed = true;
        try {
            for (;;) {
                final Node p = node.predecessor();
                if (p == head) {
                    int r = tryAcquireShared(arg);
                    if (r >= 0) {
                        setHeadAndPropagate(node, r);
                        p.next = null; // help GC
                        failed = false;
                        return true;
                    }
                }
                nanosTimeout = deadline - System.nanoTime();
                if (nanosTimeout <= 0L)
                    return false;
                if (shouldParkAfterFailedAcquire(p, node) && nanosTimeout > spinForTimeoutThreshold)
                    LockSupport.parkNanos(this, nanosTimeout);
                if (Thread.interrupted())
                    throw new InterruptedException();
            }
        } finally {
            if (failed)
                cancelAcquire(node);
        }
    }

    // ======================================Main exported methods======================================↓
    // AQS在这些方法中仅仅是抛出了UnsupportedOperationException，其逻辑需要实现类来维护，并在这些方法中维护状态量
    /**
     * Attempts to acquire in exclusive/shared mode. This method should query if the state of the object permits it 
     * to be acquired in the exclusive/shared mode, and if so to acquire it. This method is always invoked by 
     * the thread performing acquire. If this method reports failure, the acquire method may queue the thread, 
     * if it is not already queued, until it is signalled by a release from some other thread.
     * tryAcquireShared() returns a negative value on failure; zero if acquisition in shared mode succeeded but 
     * no subsequent shared-mode acquire can succeed; and a positive value if acquisition in shared mode succeeded 
     * and subsequent shared-mode acquires might also succeed, in which case a subsequent waiting thread must 
     * check availability. (Support for three different return values enables this method to be used in contexts 
     * where acquires only sometimes act exclusively.)  Upon success, this object has been acquired.
     */
    protected boolean tryAcquire(int arg) {
        throw new UnsupportedOperationException();
    }
    protected int tryAcquireShared(int arg) {
        throw new UnsupportedOperationException();
    }
    
    /** 
     * Attempts to set the state to reflect a release in exclusive/shared mode
     * This method is always invoked by the thread performing release
     */
    protected boolean tryRelease(int arg) {
        throw new UnsupportedOperationException();
    }
    protected boolean tryReleaseShared(int arg) {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns true if synchronization is held exclusively with respect to the current (calling) thread. 
     * This method is invoked upon each call to a non-waiting ConditionObject method. Waiting methods instead 
     * invoke release(). This method is invoked internally only within ConditionObject methods, so need not be 
     * defined if conditions are not used.
     */
    protected boolean isHeldExclusively() {
        throw new UnsupportedOperationException();
    }
    
    // ======================================Main exported methods======================================↑
    /**
     * 首先调用tryAcqurire()，如果获取到锁立即返回，
     * 如果获取不到锁，将当前队列以独占模式加入队列，并在队列中等待继续获取直到成功
     * 所谓的对中断不敏感，也就是由于线程获取同步状态失败后进入同步队列中，后续对线程进行中断操作时，线程不会从同步队列中移出.
     * Acquires in exclusive mode, ignoring interrupts. Implemented by invoking at least once tryAcquire(),
     * returning on success. Otherwise the thread is queued, possibly repeatedly blocking and unblocking, 
     * invoking tryAcquire() until success. This method can be used to implement method Lock.lock() 
     */
    public final void acquire(int arg) {
        if (!tryAcquire(arg) && acquireQueued(addWaiter(Node.EXCLUSIVE), arg))
            selfInterrupt();
    }

    /**
     * Acquires in exclusive mode, aborting if interrupted. Implemented by first checking interrupt status, 
     * then invoking at least once tryAcquire(), returning on success. Otherwise the thread is queued, 
     * possibly repeatedly blocking and unblocking, invoking tryAcquire() until success or the thread is interrupted. 
     * This method can be used to implement method Lock.lockInterruptibly()
     */
    public final void acquireInterruptibly(int arg) throws InterruptedException {
        if (Thread.interrupted())
            throw new InterruptedException();
        if (!tryAcquire(arg))
            doAcquireInterruptibly(arg);
    }

    /**
     * Attempts to acquire in exclusive mode, aborting if interrupted, and failing if the given timeout elapses.
     * Implemented by first checking interrupt status, then invoking at least once tryAcquire(), returning on success.
     * Otherwise, the thread is queued, possibly repeatedly blocking and unblocking, invoking tryAcquire() until 
     * success or the thread is interrupted or the timeout elapses. This method can be used to implement method
     * Lock.tryLock(long, TimeUnit).
     */
    public final boolean tryAcquireNanos(int arg, long nanosTimeout) throws InterruptedException {
        if (Thread.interrupted())
            throw new InterruptedException();
        return tryAcquire(arg) || doAcquireNanos(arg, nanosTimeout);
    }

    /**
     * Releases in exclusive mode.  Implemented by unblocking one or more threads if tryRelease() returns true.
     * This method can be used to implement method Lock.unlock()
     */
    public final boolean release(int arg) {
        if (tryRelease(arg)) {
            Node h = head;
            if (h != null && h.waitStatus != 0)
                unparkSuccessor(h);// 释放成功，唤醒后继
            return true;
        }
        return false;
    }

    /**
     * Acquires in shared mode, ignoring interrupts. Implemented by first invoking at least once tryAcquireShared,
     * returning on success. Otherwise the thread is queued, possibly repeatedly blocking and unblocking, invoking 
     * tryAcquireShared() until success.
     */
    public final void acquireShared(int arg) {
        if (tryAcquireShared(arg) < 0)
            doAcquireShared(arg);
    }

    /**
     * Acquires in shared mode, aborting if interrupted. Implemented by first checking interrupt status, then 
     * invoking at least once tryAcquireShared(), returning on success. Otherwise the thread is queued, 
     * possibly repeatedly blocking and unblocking, invoking tryAcquireShared() until success or the thread
     * is interrupted.
     */
    public final void acquireSharedInterruptibly(int arg) throws InterruptedException {
        if (Thread.interrupted())
            throw new InterruptedException();
        if (tryAcquireShared(arg) < 0)
            doAcquireSharedInterruptibly(arg);
    }

    /**
     * Attempts to acquire in shared mode, aborting if interrupted, and failing if the given timeout elapses. 
     * Implemented by first checking interrupt status, then invoking at least once tryAcquireShared(), 
     * returning on success. Otherwise, the thread is queued, possibly repeatedly blocking and unblocking,
     * invoking tryAcquireShared() until success or the thread is interrupted or the timeout elapses.
     */
    public final boolean tryAcquireSharedNanos(int arg, long nanosTimeout) throws InterruptedException {
        if (Thread.interrupted())
            throw new InterruptedException();
        return tryAcquireShared(arg) >= 0 || doAcquireSharedNanos(arg, nanosTimeout);
    }

    /** Releases in shared mode. Implemented by unblocking one or more threads if tryReleaseShared() returns true */
    public final boolean releaseShared(int arg) {
        if (tryReleaseShared(arg)) {
            doReleaseShared();
            return true;
        }
        return false;
    }

    // =================================Queue inspection methods=================================
    /**
     * Queries whether any threads are waiting to acquire. Note that because cancellations due to interrupts and 
     * timeouts may occur at any time, a true return does not guarantee that any other thread will ever acquire.
     * In this implementation, this operation returns in constant time
     */
    public final boolean hasQueuedThreads() {
        return head != tail;
    }

    /**
     * Queries whether any threads have ever contended to acquire this synchronizer; that is if an acquire method 
     * has ever blocked. In this implementation, this operation returns in constant time.
     */
    public final boolean hasContended() {
        return head != null;
    }

    /**
     * Returns the first (longest-waiting) thread in the queue, or null if no threads are currently queued.
     * In this implementation, this operation normally returns in constant time, but may iterate upon contention 
     * if other threads are concurrently modifying the queue.
     */
    public final Thread getFirstQueuedThread() {
        // handle only fast path, else relay
        return (head == tail) ? null : fullGetFirstQueuedThread();
    }

    /** Version of getFirstQueuedThread called when fastpath fails */
    private Thread fullGetFirstQueuedThread() {
        /*
         * The first node is normally head.next. Try to get its thread field, ensuring consistent reads: If thread
         * field is nulled out or s.prev is no longer head, then some other thread(s) concurrently performed setHead() 
         * in between some of our reads. We try this twice before resorting to traversal.
         */
        Node h, s;
        Thread st;
        if (((h = head) != null && (s = h.next) != null && s.prev == head && (st = s.thread) != null) ||
            ((h = head) != null && (s = h.next) != null && s.prev == head && (st = s.thread) != null))
            return st;

        /*
         * Head's next field might not have been set yet, or may have been unset after setHead(). So we must check 
         * to see if tail is actually first node. If not, we continue on, safely traversing from tail back to head 
         * to find first, guaranteeing termination.
         */

        Node t = tail;
        Thread firstThread = null;
        while (t != null && t != head) {
            Thread tt = t.thread;
            if (tt != null)
                firstThread = tt;
            t = t.prev;
        }
        return firstThread;
    }

    /** Returns true if the given thread is currently queued */
    public final boolean isQueued(Thread thread) {
        if (thread == null)
            throw new NullPointerException();
        for (Node p = tail; p != null; p = p.prev)
            if (p.thread == thread)
                return true;
        return false;
    }

    /**
     * Returns true if the apparent first queued thread, if one exists, is waiting in exclusive mode. If this method 
     * returns true, and the current thread is attempting to acquire in shared mode (that is, this method is invoked 
     * from tryAcquireShared()) then it is guaranteed that the current thread is not the first queued thread.  
     * Used only as a heuristic in ReentrantReadWriteLock.
     */
    final boolean apparentlyFirstQueuedIsExclusive() {
        Node h, s;
        return (h = head) != null && (s = h.next) != null && !s.isShared() && s.thread != null;
    }

    /**
     * Queries whether any threads have been waiting to acquire longer than the current thread.
     *
     * An invocation of this method is equivalent to (but may be more efficient than):
     * getFirstQueuedThread() != Thread.currentThread() && hasQueuedThreads()
     *
     * Note that because cancellations due to interrupts and timeouts may occur at any time, a true return does not
     * guarantee that some other thread will acquire before the current thread. Likewise, it is possible for 
     * another thread to win a race to enqueue after this method has returned false due to the queue being empty.
     *
     * This method is designed to be used by a fair synchronizer to avoid "barging". Such a synchronizer's 
     * tryAcquire() method should return false, and its tryAcquireShared() method should return a negative value, 
     * if this method returns true (unless this is a reentrant acquire).  For example, the  tryAcquire() method 
     * for a fair, reentrant, exclusive mode synchronizer might look like this:
     * 
     * protected boolean tryAcquire(int arg) {
     *   if (isHeldExclusively()) {
     *     // A reentrant acquire; increment hold count
     *     return true;
     *   } else if (hasQueuedPredecessors()) {
     *     return false;
     *   } else {
     *     // try to acquire normally
     *   }
     * }
     */
    public final boolean hasQueuedPredecessors() {
        // The correctness of this depends on head being initialized
        // before tail and on head.next being accurate if the current
        // thread is first in queue.
        Node t = tail; // Read fields in reverse initialization order
        Node h = head;
        Node s;
        return h != t && ((s = h.next) == null || s.thread != Thread.currentThread());
    }

    // ============================Instrumentation and monitoring methods============================
    /**
     * Returns an estimate of the number of threads waiting to acquire. The value is only an estimate 
     * because the number of threads may change dynamically while this method traverses internal data structures.  
     * This method is designed for use in monitoring system state, not for synchronization control.
     */
    public final int getQueueLength() {
        int n = 0;
        for (Node p = tail; p != null; p = p.prev) {
            if (p.thread != null)
                ++n;
        }
        return n;
    }

    /**
     * Returns a collection containing threads that may be waiting to acquire. Because the actual set of threads 
     * may change dynamically while constructing this result, the returned collection is only a best-effort estimate. 
     * The elements of the returned collection are in no particular order. This method is designed to facilitate 
     * construction of subclasses that provide more extensive monitoring facilities.
     */
    public final Collection<Thread> getQueuedThreads() {
        ArrayList<Thread> list = new ArrayList<Thread>();
        for (Node p = tail; p != null; p = p.prev) {
            Thread t = p.thread;
            if (t != null)
                list.add(t);
        }
        return list;
    }

    /** Returns a collection containing threads that may be waiting to acquire in exclusive/shared mode */
    public final Collection<Thread> getExclusiveQueuedThreads() {
        ArrayList<Thread> list = new ArrayList<Thread>();
        for (Node p = tail; p != null; p = p.prev) {
            if (!p.isShared()) {
                Thread t = p.thread;
                if (t != null)
                    list.add(t);
            }
        }
        return list;
    }
    public final Collection<Thread> getSharedQueuedThreads() {
        ArrayList<Thread> list = new ArrayList<Thread>();
        for (Node p = tail; p != null; p = p.prev) {
            if (p.isShared()) {
                Thread t = p.thread;
                if (t != null)
                    list.add(t);
            }
        }
        return list;
    }

    // ========================Internal support methods for Conditions========================

    /**
     * Returns true if a node, always one that was initially placed on a condition queue, is now waiting to 
     * reacquire on sync queue.
     */
    final boolean isOnSyncQueue(Node node) {
        if (node.waitStatus == Node.CONDITION || node.prev == null)
            return false;
        if (node.next != null) // If has successor, it must be on queue
            return true;
        /*
         * node.prev can be non-null, but not yet on queue because the CAS to place it on queue can fail. 
         * So we have to traverse from tail to make sure it actually made it. It will always be near the tail 
         * in calls to this method, and unless the CAS failed (which is unlikely), it will be there, 
         * so we hardly ever traverse much.
         */
        return findNodeFromTail(node);
    }

    /**
     * Returns true if node is on sync queue by searching backwards from tail. 
     * Called only when needed by isOnSyncQueue.
     */
    private boolean findNodeFromTail(Node node) {
        Node t = tail;
        for (;;) {
            if (t == node)
                return true;
            if (t == null)
                return false;
            t = t.prev;
        }
    }

    /** Transfers a node from a condition queue onto sync queue. Returns true if successful */
    final boolean transferForSignal(Node node) {
        /* If cannot change waitStatus, the node has been cancelled */
        if (!compareAndSetWaitStatus(node, Node.CONDITION, 0))
            return false;

        /*
         * Splice onto queue and try to set waitStatus of predecessor to indicate that thread is (probably) waiting. 
         * If cancelled or attempt to set waitStatus fails, wake up to resync (in which case the waitStatus 
         * can be transiently and harmlessly wrong).
         */
        Node p = enq(node);
        int ws = p.waitStatus;
        if (ws > 0 || !compareAndSetWaitStatus(p, ws, Node.SIGNAL))
            LockSupport.unpark(node.thread);
        return true;
    }

    /**
     * Transfers node, if necessary, to sync queue after a cancelled wait.
     * Returns true if thread was cancelled before being signalled.
     */
    final boolean transferAfterCancelledWait(Node node) {
        if (compareAndSetWaitStatus(node, Node.CONDITION, 0)) {
            enq(node);
            return true;
        }
        /*
         * If we lost out to a signal(), then we can't proceed until it finishes its enq(). Cancelling during an
         * incomplete transfer is both rare and transient, so just spin.
         */
        while (!isOnSyncQueue(node))
            Thread.yield();
        return false;
    }

    /** Invokes release with current state value; returns saved state. Cancels node and throws exception on failure */
    final int fullyRelease(Node node) {
        boolean failed = true;
        try {
            int savedState = getState();
            if (release(savedState)) {
                failed = false;
                return savedState;
            } else {
                throw new IllegalMonitorStateException();
            }
        } finally {
            if (failed)
                node.waitStatus = Node.CANCELLED;
        }
    }

    // =============================Instrumentation methods for conditions=============================

    /** Queries whether the given ConditionObject uses this synchronizer as its lock */
    public final boolean owns(ConditionObject condition) {
        return condition.isOwnedBy(this);
    }

    /**
     * Queries whether any threads are waiting on the given condition associated with this synchronizer. Note 
     * that because timeouts and interrupts may occur at any time, a true return does not guarantee that a future 
     * signal() will awaken any threads. This method is designed primarily for use in monitoring of the system state.
     */
    public final boolean hasWaiters(ConditionObject condition) {
        if (!owns(condition))
            throw new IllegalArgumentException("Not owner");
        return condition.hasWaiters();
    }

    /**
     * Returns an estimate of the number of threads waiting on the given condition associated with this synchronizer. 
     * Note that because timeouts and interrupts may occur at any time, the estimate serves only as an upper bound 
     * on the actual number of waiters. This method is designed for use in monitoring of the system state, 
     * not for synchronization control.
     */
    public final int getWaitQueueLength(ConditionObject condition) {
        if (!owns(condition))
            throw new IllegalArgumentException("Not owner");
        return condition.getWaitQueueLength();
    }

    /**
     * Returns a collection containing those threads that may be waiting on the given condition associated with this
     * synchronizer. Because the actual set of threads may change dynamically while constructing this result, 
     * the returned collection is only a best-effort estimate. The elements of the returned collection are in 
     * no particular order
     */
    public final Collection<Thread> getWaitingThreads(ConditionObject condition) {
        if (!owns(condition))
            throw new IllegalArgumentException("Not owner");
        return condition.getWaitingThreads();
    }

    /**
     * Condition implementation for an AbstractQueuedSynchronizer serving as the basis of a Lock implementation.
     *
     * Method documentation for this class describes mechanics, not behavioral specifications from the point of 
     * view of Lock and Condition users. Exported versions of this class will in general need to be accompanied 
     * by documentation describing condition semantics that rely on those of the associated AbstractQueuedSynchronizer
     *
     * This class is Serializable, but all fields are transient, so deserialized conditions have no waiters.
     */
    public class ConditionObject implements Condition, java.io.Serializable {
        /** First node of condition queue. */
        private transient Node firstWaiter;
        /** Last node of condition queue. */
        private transient Node lastWaiter;

        // ======================================Internal methods======================================
        /** Adds a new waiter to wait queue */
        private Node addConditionWaiter() {
            Node t = lastWaiter;
            // If lastWaiter is cancelled, clean out.
            if (t != null && t.waitStatus != Node.CONDITION) {
                unlinkCancelledWaiters();
                t = lastWaiter;
            }
            Node node = new Node(Thread.currentThread(), Node.CONDITION);
            if (t == null)
                firstWaiter = node;
            else
                t.nextWaiter = node;
            lastWaiter = node;
            return node;
        }

        /**
         * Removes and transfers nodes until hit non-cancelled one or null. Split out from signal in part to 
         * encourage compilers to inline the case of no waiters.
         */
        private void doSignal(Node first) {
            do {
                if ( (firstWaiter = first.nextWaiter) == null)
                    lastWaiter = null;
                first.nextWaiter = null;
            } while (!transferForSignal(first) && (first = firstWaiter) != null);
        }

        /** Removes and transfers all nodes */
        private void doSignalAll(Node first) {
            lastWaiter = firstWaiter = null;
            do {
                Node next = first.nextWaiter;
                first.nextWaiter = null;
                transferForSignal(first);
                first = next;
            } while (first != null);
        }

        /**
         * Unlinks cancelled waiter nodes from condition queue. Called only while holding lock. This is called 
         * when cancellation occurred during condition wait, and upon insertion of a new waiter when lastWaiter 
         * is seen to have been cancelled. This method is needed to avoid garbage retention in the absence of 
         * signals. So even though it may require a full traversal, it comes into play only when timeouts or 
         * cancellations occur in the absence of signals. It traverses all nodes rather than stopping at a
         * particular target to unlink all pointers to garbage nodes without requiring many re-traversals 
         * during cancellation storms.
         */
        private void unlinkCancelledWaiters() {
            Node t = firstWaiter;
            Node trail = null;
            while (t != null) {
                Node next = t.nextWaiter;
                if (t.waitStatus != Node.CONDITION) {
                    t.nextWaiter = null;
                    if (trail == null)
                        firstWaiter = next;
                    else
                        trail.nextWaiter = next;
                    if (next == null)
                        lastWaiter = trail;
                }
                else
                    trail = t;
                t = next;
            }
        }

        // =========================public methods=========================
        /**
         * Moves the longest-waiting thread, if one exists, from the wait queue for this condition to the 
         * wait queue for the owning lock.
         */
        public final void signal() {
            if (!isHeldExclusively())
                throw new IllegalMonitorStateException();
            Node first = firstWaiter;
            if (first != null)
                doSignal(first);
        }

        /** Moves all threads from the wait queue for this condition to the wait queue for the owning lock */
        public final void signalAll() {
            if (!isHeldExclusively())
                throw new IllegalMonitorStateException();
            Node first = firstWaiter;
            if (first != null)
                doSignalAll(first);
        }

        /**
         * Implements uninterruptible condition wait.
         * 1.Save lock state returned by getState().
         * 2.Invoke release() with saved state as argument, throwing IllegalMonitorStateException if it fails.
         * 3.Block until signalled.
         * 4.Reacquire by invoking specialized version of acquire() with saved state as argument.
         */
        public final void awaitUninterruptibly() {
            Node node = addConditionWaiter();
            int savedState = fullyRelease(node);
            boolean interrupted = false;
            while (!isOnSyncQueue(node)) {
                LockSupport.park(this);
                if (Thread.interrupted())
                    interrupted = true;
            }
            if (acquireQueued(node, savedState) || interrupted)
                selfInterrupt();
        }

        /*
         * For interruptible waits, we need to track whether to throw InterruptedException, if interrupted while 
         * blocked on condition, versus reinterrupt current thread, if interrupted while blocked waiting to re-acquire
         */

        /** Mode meaning to reinterrupt on exit from wait */
        private static final int REINTERRUPT =  1;
        /** Mode meaning to throw InterruptedException on exit from wait */
        private static final int THROW_IE    = -1;

        /**
         * Checks for interrupt, returning THROW_IE if interrupted before signalled, 
         * REINTERRUPT if after signalled, or 0 if not interrupted.
         */
        private int checkInterruptWhileWaiting(Node node) {
            return Thread.interrupted() ? (transferAfterCancelledWait(node) ? THROW_IE : REINTERRUPT) : 0;
        }

        /** Throws InterruptedException, reinterrupts current thread, or does nothing, depending on mode */
        private void reportInterruptAfterWait(int interruptMode) throws InterruptedException {
            if (interruptMode == THROW_IE)
                throw new InterruptedException();
            else if (interruptMode == REINTERRUPT)
                selfInterrupt();
        }

        /**
         * Implements interruptible condition wait.
         * 1.If current thread is interrupted, throw InterruptedException.
         * 2.Save lock state returned by getState().
         * 3.Invoke release() with saved state as argument, throwing IllegalMonitorStateException if it fails.
         * 4.Block until signalled or interrupted.
         * 5.Reacquire by invoking specialized version of acquire() with saved state as argument.
         * 6.If interrupted while blocked in step 4, throw InterruptedException.
         */
        public final void await() throws InterruptedException {
            if (Thread.interrupted())
                throw new InterruptedException();
            Node node = addConditionWaiter();
            int savedState = fullyRelease(node);
            int interruptMode = 0;
            while (!isOnSyncQueue(node)) {
                LockSupport.park(this);
                if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
                    break;
            }
            if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
                interruptMode = REINTERRUPT;
            if (node.nextWaiter != null) // clean up if cancelled
                unlinkCancelledWaiters();
            if (interruptMode != 0)
                reportInterruptAfterWait(interruptMode);
        }

        /**
         * Implements timed condition wait.
         * 1.If current thread is interrupted, throw InterruptedException.
         * 2.Save lock state returned by getState().
         * 3.Invoke release() with saved state as argument, throwing IllegalMonitorStateException if it fails.
         * 4.Block until signalled, interrupted, or timed out.
         * 5.Reacquire by invoking specialized version of acquire() with saved state as argument.
         * 6.If interrupted while blocked in step 4, throw InterruptedException.
         */
        public final long awaitNanos(long nanosTimeout) throws InterruptedException {
            if (Thread.interrupted())
                throw new InterruptedException();
            Node node = addConditionWaiter();
            int savedState = fullyRelease(node);
            final long deadline = System.nanoTime() + nanosTimeout;
            int interruptMode = 0;
            while (!isOnSyncQueue(node)) {
                if (nanosTimeout <= 0L) {
                    transferAfterCancelledWait(node);
                    break;
                }
                if (nanosTimeout >= spinForTimeoutThreshold)
                    LockSupport.parkNanos(this, nanosTimeout);
                if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
                    break;
                nanosTimeout = deadline - System.nanoTime();
            }
            if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
                interruptMode = REINTERRUPT;
            if (node.nextWaiter != null)
                unlinkCancelledWaiters();
            if (interruptMode != 0)
                reportInterruptAfterWait(interruptMode);
            return deadline - System.nanoTime();
        }

        /**
         * Implements absolute timed condition wait.
         * 1.If current thread is interrupted, throw InterruptedException.
         * 2.Save lock state returned by getState()
         * 3.Invoke release() with saved state as argument, throwing IllegalMonitorStateException if it fails.
         * 4.Block until signalled, interrupted, or timed out.
         * 5.Reacquire by invoking specialized version of acquire() with saved state as argument.
         * 6.If interrupted while blocked in step 4, throw InterruptedException.
         * 7.If timed out while blocked in step 4, return false, else true.
         */
        public final boolean awaitUntil(Date deadline) throws InterruptedException {
            long abstime = deadline.getTime();
            if (Thread.interrupted())
                throw new InterruptedException();
            Node node = addConditionWaiter();
            int savedState = fullyRelease(node);
            boolean timedout = false;
            int interruptMode = 0;
            while (!isOnSyncQueue(node)) {
                if (System.currentTimeMillis() > abstime) {
                    timedout = transferAfterCancelledWait(node);
                    break;
                }
                LockSupport.parkUntil(this, abstime);
                if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
                    break;
            }
            if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
                interruptMode = REINTERRUPT;
            if (node.nextWaiter != null)
                unlinkCancelledWaiters();
            if (interruptMode != 0)
                reportInterruptAfterWait(interruptMode);
            return !timedout;
        }

        /**
         * Implements timed condition wait.
         * 1.If current thread is interrupted, throw InterruptedException.
         * 2.Save lock state returned by getState()
         * 3.Invoke release() with saved state as argument, throwing IllegalMonitorStateException if it fails.
         * 4.Block until signalled, interrupted, or timed out.
         * 5.Reacquire by invoking specialized version of acquire() with saved state as argument.
         * 6.If interrupted while blocked in step 4, throw InterruptedException.
         * 7.If timed out while blocked in step 4, return false, else true.
         */
        public final boolean await(long time, TimeUnit unit) throws InterruptedException {
            long nanosTimeout = unit.toNanos(time);
            if (Thread.interrupted())
                throw new InterruptedException();
            Node node = addConditionWaiter();
            int savedState = fullyRelease(node);
            final long deadline = System.nanoTime() + nanosTimeout;
            boolean timedout = false;
            int interruptMode = 0;
            while (!isOnSyncQueue(node)) {
                if (nanosTimeout <= 0L) {
                    timedout = transferAfterCancelledWait(node);
                    break;
                }
                if (nanosTimeout >= spinForTimeoutThreshold)
                    LockSupport.parkNanos(this, nanosTimeout);
                if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
                    break;
                nanosTimeout = deadline - System.nanoTime();
            }
            if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
                interruptMode = REINTERRUPT;
            if (node.nextWaiter != null)
                unlinkCancelledWaiters();
            if (interruptMode != 0)
                reportInterruptAfterWait(interruptMode);
            return !timedout;
        }

        // ========================support for instrumentation========================
        /** Returns true if this condition was created by the given synchronization object */
        final boolean isOwnedBy(AbstractQueuedSynchronizer sync) {
            return sync == AbstractQueuedSynchronizer.this;
        }

        /** Queries whether any threads are waiting on this condition */
        protected final boolean hasWaiters() {
            if (!isHeldExclusively())
                throw new IllegalMonitorStateException();
            for (Node w = firstWaiter; w != null; w = w.nextWaiter) {
                if (w.waitStatus == Node.CONDITION)
                    return true;
            }
            return false;
        }

        /** Returns an estimate of the number of threads waiting on this condition. */
        protected final int getWaitQueueLength() {
            if (!isHeldExclusively())
                throw new IllegalMonitorStateException();
            int n = 0;
            for (Node w = firstWaiter; w != null; w = w.nextWaiter) {
                if (w.waitStatus == Node.CONDITION)
                    ++n;
            }
            return n;
        }

        /**
         * Returns a collection containing those threads that may be waiting on this Condition.
         */
        protected final Collection<Thread> getWaitingThreads() {
            if (!isHeldExclusively())
                throw new IllegalMonitorStateException();
            ArrayList<Thread> list = new ArrayList<Thread>();
            for (Node w = firstWaiter; w != null; w = w.nextWaiter) {
                if (w.waitStatus == Node.CONDITION) {
                    Thread t = w.thread;
                    if (t != null)
                        list.add(t);
                }
            }
            return list;
        }
    }

    /**
     * Setup to support compareAndSet. We need to natively implement this here: For the sake of 
     * permitting future enhancements, we cannot explicitly subclass AtomicInteger, which would be efficient 
     * and useful otherwise. So, as the lesser of evils, we natively implement using hotspot intrinsics API. 
     * And while we  are at it, we do the same for other CASable fields (which could otherwise be done 
     * with atomic field updaters).
     */
    private static final Unsafe unsafe = Unsafe.getUnsafe();
    // ...
    
    // CAS写队列，传入当前线程，当前线程认为的队列某处Node值，当前线程想修改此处Node值为什么值
    /** CAS head field. Used only by enq */
    private final boolean compareAndSetHead(Node update) {
        return unsafe.compareAndSwapObject(this, headOffset, null, update);
    }

    /** CAS tail field. Used only by enq */
    private final boolean compareAndSetTail(Node expect, Node update) {
        return unsafe.compareAndSwapObject(this, tailOffset, expect, update);
    }

    /** CAS waitStatus field of a node */
    private static final boolean compareAndSetWaitStatus(Node node, int expect, int update) {
        return unsafe.compareAndSwapInt(node, waitStatusOffset, expect, update);
    }

    /** CAS next field of a node */
    private static final boolean compareAndSetNext(Node node, Node expect, Node update) {
        return unsafe.compareAndSwapObject(node, nextOffset, expect, update);
    }    
}
```
Special thanks:
https://javadoop.com/2017/06/16/AbstractQueuedSynchronizer/
https://www.cnblogs.com/showing/p/6858410.html
https://www.cnblogs.com/waterystone/p/4920797.html