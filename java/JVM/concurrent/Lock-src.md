### java.util.concurrent.locks.Lock
Lock接口是JUC包里面所有锁实现的根接口。关于Lock的要点一般有两个，一是Lock与synchronized的比较，一个是他提供的四种获取锁
的接口的具体行为，这两个问题在源码文档里都有详细的说明，所以只贴源码和注释文档并在关键点做说明，不再全文翻译和总结。
```java
/**
 * Lock implementations provide more extensive locking operations than can be obtained using synchronized methods
 * and statements. They allow more flexible structuring, may have quite different properties, and may support 
 * multiple associated Condition objects.
 *
 * The use of synchronized methods or statements provides access to the implicit monitor lock associated with 
 * every object, but forces all lock acquisition and release to occur in a block-structured way: when multiple 
 * locks are acquired they must be released in the opposite order, and all locks must be released in the same 
 * lexical scope in which they were acquired.
 *
 * While the scoping mechanism for synchronized methods and statements makes it much easier to program with monitor 
 * locks, and helps avoid many common programming errors involving locks, there are occasions where you need to work 
 * with locks in a more flexible way. For example, some algorithms for traversing concurrently accessed data 
 * structures require the use of hand-over-hand or chain locking: you acquire the lock of node A, then node B, 
 * then release A and acquire C, then release B and acquire D and so on. Implementations of the Lock interface 
 * enable the use of such techniques by allowing a lock to be acquired and released in different scopes, and allowing 
 * multiple locks to be acquired and released in any order.
 *
 * With this increased flexibility comes additional responsibility. The absence of block-structured locking removes 
 * the automatic release of locks that occurs with synchronized methods and statements. 
 *
 * When locking and unlocking occur in different scopes, care must be taken to ensure that all code that is executed 
 * while the lock is held is protected by try-finally or try-catch to ensure that the lock is released when necessary.
 *
 * Lock implementations provide additional functionality over the use of synchronized methods and statements by
 * providing a non-blocking attempt to acquire a lock (tryLock()), an attempt to acquire the lock that can be
 * interrupted (lockInterruptibly(), and an attempt to acquire the lock that can timeout (tryLock(long, TimeUnit)).
 *
 * A Lock class can also provide behavior and semantics that is quite different from that of the implicit monitor 
 * lock, such as guaranteed ordering, non-reentrant usage, or deadlock detection. 
 *
 * Note that Lock instances are just normal objects and can themselves be used as the target in a synchronized 
 * statement. Acquiring the monitor lock of a Lock instance has no specified relationship with invoking any of the 
 * lock() methods of that instance. It is recommended that to avoid confusion you never use Lock instances in this 
 * way, except within their own implementation.
 *
 * Except where noted, passing a null value for any parameter will result in a NullPointerException being thrown.
 *
 * All Lock implementations must enforce the same memory synchronization semantics as provided by the built-in 
 * monitor lock, as described in The Java Language Specification (17.4 Memory Model):
 * A successful lock operation has the same memory synchronization effects as a successful Lock action.
 * A successful unlock operation has the same memory synchronization effects as a successful Unlock action.
 * Unsuccessful locking and unlocking operations, and reentrant locking/unlocking operations, do not require any 
 * memory synchronization effects.
 *
 * The three forms of lock acquisition (interruptible, non-interruptible, and timed) may differ in their performance
 * characteristics, ordering guarantees, or other implementation qualities. Further, the ability to interrupt the 
 * ongoing acquisition of a lock may not be available in a given Lock class. Consequently, an implementation is not 
 * required to define exactly the same guarantees or semantics for all three forms of lock acquisition, nor is it 
 * required to support interruption of an ongoing lock acquisition. An implementation is required to clearly document 
 * the semantics and guarantees provided by each of the locking methods. It must also obey the interruption semantics 
 * as defined in this interface, to the extent that interruption of lock acquisition is supported: which is either 
 * totally, or only on method entry.
 *
 * As interruption generally implies cancellation, and checks for interruption are often infrequent, an 
 * implementation can favor responding to an interrupt over normal method return. This is true even if it can be
 * shown that the interrupt occurred after another action may have unblocked the thread. 
 * An implementation should document this behavior.
 */
public interface Lock {

    /**
     * Acquires the lock. If the lock is not available then the current thread becomes disabled for thread 
     * scheduling purposes and lies dormant until the lock has been acquired.
     */
    void lock();

    /**
     * Acquires the lock unless the current thread is interrupted. Acquires the lock if it is available and 
     * returns immediately. If the lock is not available then the current thread becomes disabled for thread 
     * scheduling purposes and lies dormant until the lock is acquired by the current thread or ome other 
     * thread interrupts the current thread, and interruption of lock acquisition is supported.
     *
     * If the current thread has its interrupted status set on entry to this method or is interrupted while 
     * acquiring the lock, and interruption of lock acquisition is supported, then InterruptedException is thrown 
     * and the current thread's interrupted status is cleared.
     */
    void lockInterruptibly() throws InterruptedException;

    /**
     * Acquires the lock if it is available and returns immediately with true. If the lock is not 
     * available then this method will return immediately with false.
     */
    boolean tryLock();

    /**
     * Acquires the lock if it is free within the given waiting time and the current thread has not been interrupted.
     *
     * If the lock is available this method returns immediately with the value true. If the lock is not available 
     * then the current thread becomes disabled for thread scheduling purposes and lies dormant until the lock is 
     * acquired by the current thread or ome other thread interrupts the current thread, and interruption of lock 
     * acquisition is supported or he specified waiting time elapses
     *
     * If the lock is acquired then the value true is returned. If the current thread has its interrupted status 
     * set on entry to this method or is interrupted while acquiring the lock, and interruption of lock acquisition 
     * is supported, then InterruptedException is thrown and the current thread's interrupted status is cleared.
     *
     * If the specified waiting time elapses then the value false is returned. If the time is less than or equal 
     * to zero, the method will not wait at all.
     */
    boolean tryLock(long time, TimeUnit unit) throws InterruptedException;

    /** Releases the lock */
    void unlock();

    /**
     * Returns a new Condition instance that is bound to this Lock instance. Before waiting on the condition the 
     * lock must be held by the current thread. A call to Condition.await() will atomically release the lock before 
     * waiting and re-acquire the lock before the wait returns.
     */
    Condition newCondition();
}
```
```java
/**
 * A reentrant mutual exclusion Lock with the same basic behavior and semantics as the implicit monitor 
 * lock accessed using synchronized methods and statements, but with extended capabilities.
 *
 * A ReentrantLock is owned by the thread last successfully locking, but not yet unlocking it. A thread invoking
 * lock() will return, successfully acquiring the lock, when the lock is not owned by another thread. The method 
 * will return immediately if the current thread already owns the lock. This can be checked using methods 
 * isHeldByCurrentThread(), and getHoldCount().
 *
 * The constructor for this class accepts an optional fairness parameter. When set true, under contention, locks 
 * favor granting access to the longest-waiting thread. Otherwise this lock does not guarantee any particular
 * access order. Programs using fair locks accessed by many threads may display lower overall throughput 
 * (i.e., are slower; often much slower) than those using the default setting, but have smaller variances in times 
 * to obtain locks and guarantee lack of starvation. Note however, that fairness of locks does not guarantee
 * fairness of thread scheduling. Thus, one of many threads using a fair lock may obtain it multiple times in 
 * succession while other active threads are not progressing and not currently holding the lock.
 * Also note that the untimed tryLock() method does not honor the fairness setting. It will succeed if the lock
 * is available even if other threads are waiting.
 *
 * In addition to implementing the Lock interface, this class defines a number of public and protected methods for 
 * inspecting the state of the lock. Some of these methods are only useful for instrumentation and monitoring.
 *
 * Serialization of this class behaves in the same way as built-in locks: a deserialized lock is in the unlocked 
 * state, regardless of its state when serialized.
 *
 * This lock supports a maximum of 2147483647 recursive locks by the same thread. Attempts to exceed this limit 
 * result in Error throws from locking methods.
 */
public class ReentrantLock implements Lock, java.io.Serializable {
    /** Synchronizer providing all implementation mechanics */
    private final Sync sync;

    /**
     * Base of synchronization control for this lock. Subclassed
     * into fair and nonfair versions below. Uses AQS state to
     * represent the number of holds on the lock.
     */
    abstract static class Sync extends AbstractQueuedSynchronizer {

        /** Performs lock(). The main reason for subclassing is to allow fast path for nonfair version */
        abstract void lock();

        /**
         * Performs non-fair tryLock. tryAcquire is implemented in subclasses, 
         * but both need nonfair try for trylock method.
         */
        final boolean nonfairTryAcquire(int acquires) {
            final Thread current = Thread.currentThread();
            int c = getState();
            if (c == 0) {
                if (compareAndSetState(0, acquires)) {
                    setExclusiveOwnerThread(current);
                    return true;
                }
            }
            else if (current == getExclusiveOwnerThread()) {
                int nextc = c + acquires;
                if (nextc < 0) // overflow
                    throw new Error("Maximum lock count exceeded");
                setState(nextc);
                return true;
            }
            return false;
        }

        protected final boolean tryRelease(int releases) {
            int c = getState() - releases;
            if (Thread.currentThread() != getExclusiveOwnerThread())
                throw new IllegalMonitorStateException();
            boolean free = false;
            if (c == 0) {
                free = true;
                setExclusiveOwnerThread(null);
            }
            setState(c);
            return free;
        }

        protected final boolean isHeldExclusively() {
            // While we must in general read state before owner,
            // we don't need to do so to check if current thread is owner
            return getExclusiveOwnerThread() == Thread.currentThread();
        }

        final ConditionObject newCondition() {
            return new ConditionObject();
        }

        // Methods relayed from outer class
        final Thread getOwner() {
            return getState() == 0 ? null : getExclusiveOwnerThread();
        }

        final int getHoldCount() {
            return isHeldExclusively() ? getState() : 0;
        }

        final boolean isLocked() {
            return getState() != 0;
        }

        /** Reconstitutes the instance from a stream (that is, deserializes it) */
        private void readObject(java.io.ObjectInputStream s) throws java.io.IOException, ClassNotFoundException {
            s.defaultReadObject();
            setState(0); // reset to unlocked state
        }
    }

    /** Sync object for non-fair locks */
    static final class NonfairSync extends Sync {
        /** Performs lock. Try immediate barge, backing up to normal acquire on failure */
        final void lock() {
            if (compareAndSetState(0, 1))
                setExclusiveOwnerThread(Thread.currentThread());
            else
                acquire(1);
        }

        protected final boolean tryAcquire(int acquires) {
            return nonfairTryAcquire(acquires);
        }
    }

    /** Sync object for fair locks */
    static final class FairSync extends Sync {
        final void lock() {
            acquire(1);
        }

        /** Fair version of tryAcquire. Don't grant access unless recursive call or no waiters or is first */
        protected final boolean tryAcquire(int acquires) {
            final Thread current = Thread.currentThread();
            int c = getState();
            if (c == 0) {
                if (!hasQueuedPredecessors() && compareAndSetState(0, acquires)) {
                    setExclusiveOwnerThread(current);
                    return true;
                }
            }
            else if (current == getExclusiveOwnerThread()) {
                int nextc = c + acquires;
                if (nextc < 0)
                    throw new Error("Maximum lock count exceeded");
                setState(nextc);
                return true;
            }
            return false;
        }
    }

    /** Creates an instance of ReentrantLock with the given fairness policy */
    public ReentrantLock() {
        sync = new NonfairSync();
    }
    public ReentrantLock(boolean fair) {
        sync = fair ? new FairSync() : new NonfairSync();
    }

    /**
     * Acquires the lock if it is not held by another thread and returns immediately, setting the lock hold count 
     * to one. If the current thread already holds the lock then the hold count is incremented by one and the 
     * method returns immediately. If the lock is held by another thread then the current thread becomes disabled 
     * for thread scheduling purposes and lies dormant until the lock has been acquired, at which time the lock 
     * hold count is set to one.
     */
    public void lock() {
        sync.lock();
    }

    /**
     * Acquires the lock unless the current thread is interrupted. Acquires the lock if it is not held by another 
     * thread and returns immediately, setting the lock hold count to one. If the current thread already holds 
     * this lock then the hold count is incremented by one and the method returns immediately. If the lock is held 
     * by another thread then the current thread becomes disabled for thread scheduling purposes and lies dormant 
     * until the lock is acquired by the current thread or some other thread interrupts the current thread.
     * If the lock is acquired by the current thread then the lock hold count is set to one. If the current thread
     * has its interrupted status set on entry to this method or is interrupted while acquiring the lock, then 
     * InterruptedException is thrown and the current thread's interrupted status is cleared.
     *
     * In this implementation, as this method is an explicit interruption point, preference is given to responding 
     * to the interrupt over normal or reentrant acquisition of the lock.
     */
    public void lockInterruptibly() throws InterruptedException {
        sync.acquireInterruptibly(1);
    }

    /**
     * Acquires the lock only if it is not held by another thread at the time of invocation and returns immediately 
     * with the value true, setting the lock hold count to one. Even when this lock has been set to use a fair 
     * ordering policy, a call to tryLock() will immediately acquire the lock if it is available, whether or not
     * other threads are currently waiting for the lock. This "barging" behavior can be useful in certain 
     * circumstances, even though it breaks fairness. If you want to honor the fairness setting for this lock, 
     * then use tryLock(long, TimeUnit) which is almost equivalent (it also detects interruption).
     *
     * If the current thread already holds this lock then the hold count is incremented by one and the method 
     * returns true. If the lock is held by another thread then this method will return immediately with false.
     */
    public boolean tryLock() {
        return sync.nonfairTryAcquire(1);
    }

    /**
     * Acquires the lock if it is not held by another thread within the given waiting time and the current thread 
     * has not been interrupted, returns immediately with the value true, setting the lock hold count to one. 
     * If this lock has been set to use a fair ordering policy then an available lock will not be acquired if any 
     * other threads are waiting for the lock. This is in contrast to the tryLock() method. If you want a timed 
     * tryLock that does permit barging on a fair lock then combine the timed and un-timed forms together:
     * if (lock.tryLock() || lock.tryLock(timeout, unit)) {
     *   ...
     * }
     *
     * If the current thread already holds this lock then the hold count is incremented by one and the method 
     * returns true. If the lock is held by another thread then the current thread becomes disabled for thread 
     * scheduling purposes and lies dormant until the lock is acquired by the current thread or some other thread 
     * interrupts the current thread or the specified waiting time elapses. If the lock is acquired then
     * true is returned and the lock hold count is set to one.
     *
     * If the current thread has its interrupted status set on entry to this method or interrupted while acquiring 
     * the lock, then InterruptedException is thrown and the current thread's interrupted status is cleared.
     *
     * If the specified waiting time elapses then false is returned. If the time is less than or equal to zero,
     * the method will not wait at all.
     *
     * In this implementation, as this method is an explicit interruption point, preference is given to responding 
     * to the interrupt over normal or reentrant acquisition of the lock, and over reporting the elapse of the 
     * waiting time.
     */
    public boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException {
        return sync.tryAcquireNanos(1, unit.toNanos(timeout));
    }

    /**
     * Attempts to release this lock. If the current thread is the holder of this lock then the hold count is 
     * decremented. If the hold count is now zero then the lock is released. If the current thread is not the 
     * holder of this lock then IllegalMonitorStateException is thrown.
     */
    public void unlock() {
        sync.release(1);
    }

    /**
     * Returns a Condition instance for use with this Lock instance. The returned Condition instance supports the 
     * same usages as do the Object monitor methods (wait(), notify(), and notifyAll()) when used with the built-in
     * monitor lock.
     *
     * If this lock is not held when any of the Condition.await() or Condition.signal() methods are called, then 
     * an IllegalMonitorStateException is thrown.
     *
     * When the Condition.await() methods are called the lock is released and, before they return, the lock is 
     * reacquired and the lock hold count restored to what it was when the method was called.
     *
     * If a thread is interrupted while waiting then the wait will terminate, an InterruptedException will be thrown, 
     * and the thread's interrupted status will be cleared.
     *
     * Waiting threads are signalled in FIFO order. The ordering of lock reacquisition for threads returning from 
     * waiting methods is the same as for threads initially acquiring the lock, which is in the default case 
     * not specified, but for fair locks favors those threads that have been waiting the longest.
     */
    public Condition newCondition() {
        return sync.newCondition();
    }

    /**
     * Queries the number of holds on this lock by the current thread. A thread has a hold on a lock for each 
     * lock action that is not matched by an unlock action. The hold count information is typically only used 
     * for testing and debugging purposes. 
     */
    public int getHoldCount() {
        return sync.getHoldCount();
    }

    /**
     * Queries if this lock is held by the current thread. Analogous to the Thread.holdsLock(Object) method for
     * built-in monitor locks, this method is typically used for debugging and testing. 
     */
    public boolean isHeldByCurrentThread() {
        return sync.isHeldExclusively();
    }

    /**
     * Queries if this lock is held by any thread. This method is designed for use in monitoring of 
     * the system state, not for synchronization control.
     */
    public boolean isLocked() {
        return sync.isLocked();
    }

    /** Returns true if this lock has fairness set true*/
    public final boolean isFair() {
        return sync instanceof FairSync;
    }

    /**
     * Returns the thread that currently owns this lock, or null if not owned. When this method is called by a 
     * thread that is not the owner, the return value reflects a best-effort approximation of current lock status. 
     * For example, the owner may be momentarily null even if there are threads trying to acquire the lock but 
     * have not yet done so. This method is designed to facilitate construction of subclasses that provide more 
     * extensive lock monitoring facilities.
     */
    protected Thread getOwner() {
        return sync.getOwner();
    }

    /**
     * Queries whether any threads are waiting to acquire this lock. Note that because cancellations may occur 
     * at any time, a true return does not guarantee that any other thread will ever acquire this lock. This method 
     * is designed primarily for use in monitoring of the system state.
     */
    public final boolean hasQueuedThreads() {
        return sync.hasQueuedThreads();
    }
    /** Queries whether the given thread is waiting to acquire this lock */
    public final boolean hasQueuedThread(Thread thread) {
        return sync.isQueued(thread);
    }

    /**
     * Returns an estimate of the number of threads waiting to acquire this lock. The value is only an estimate 
     * because the number of threads may change dynamically while this method traverses internal data structures. 
     * This method is designed for use in monitoring of the system state, not for synchronization control.
     */
    public final int getQueueLength() {
        return sync.getQueueLength();
    }

    /**
     * Returns a collection containing threads that may be waiting to acquire this lock. Because the actual set 
     * of threads may change dynamically while constructing this result, the returned collection is only a 
     * best-effort estimate. The elements of the returned collection are in no particular order. This method is
     * designed to facilitate construction of subclasses that provide more extensive monitoring facilities.
     */
    protected Collection<Thread> getQueuedThreads() {
        return sync.getQueuedThreads();
    }

    /**
     * Queries whether any threads are waiting on the given condition associated with this lock. Note that because 
     * timeouts and interrupts may occur at any time, a true return does not guarantee that a future signal will 
     * awaken any threads. This method is designed primarily for use in monitoring of the system state.
     */
    public boolean hasWaiters(Condition condition) {
        if (condition == null)
            throw new NullPointerException();
        if (!(condition instanceof AbstractQueuedSynchronizer.ConditionObject))
            throw new IllegalArgumentException("not owner");
        return sync.hasWaiters((AbstractQueuedSynchronizer.ConditionObject)condition);
    }

    /**
     * Returns an estimate of the number of threads waiting on the given condition associated with this lock. 
     * Note that because timeouts and interrupts may occur at any time, the estimate serves only as an upper bound 
     * on the actual number of waiters. This method is designed for use in monitoring of the system state, not 
     * for synchronization control.
     */
    public int getWaitQueueLength(Condition condition) {
        if (condition == null)
            throw new NullPointerException();
        if (!(condition instanceof AbstractQueuedSynchronizer.ConditionObject))
            throw new IllegalArgumentException("not owner");
        return sync.getWaitQueueLength((AbstractQueuedSynchronizer.ConditionObject)condition);
    }

    /**
     * Returns a collection containing those threads that may be waiting on the given condition associated 
     * with this lock. Because the actual set of threads may change dynamically while constructing this result, 
     * the returned collection is only a best-effort estimate. The elements of the returned collection are in no 
     * particular order. This method is designed to facilitate construction of subclasses that provide more
     * extensive condition monitoring facilities.
     */
    protected Collection<Thread> getWaitingThreads(Condition condition) {
        if (condition == null)
            throw new NullPointerException();
        if (!(condition instanceof AbstractQueuedSynchronizer.ConditionObject))
            throw new IllegalArgumentException("not owner");
        return sync.getWaitingThreads((AbstractQueuedSynchronizer.ConditionObject)condition);
    }
}
```
```java
/**
 * A ReadWriteLock maintains a pair of associated locks, one for read-only operations and one for writing. 
 * The read lock may be held simultaneously by multiple reader threads, so long as there are no writers. 
 * The write lock is exclusive.
 *
 * All ReadWriteLock implementations must guarantee that the memory synchronization effects of writeLock operations
 * (as specified in the Lock interface) also hold with respect to the associated readLock. That is, a thread 
 * successfully acquiring the read lock will see all updates made upon previous release of the write lock.
 *
 * A read-write lock allows for a greater level of concurrency in accessing shared data than that permitted by a 
 * mutual exclusion lock. It exploits the fact that while only a single thread at a time (a writer thread) can 
 * modify the shared data, in many cases any number of threads can concurrently read the data (hence reader threads).
 * In theory, the increase in concurrency permitted by the use of a read-write lock will lead to performance 
 * improvements over the use of a mutual exclusion lock. In practice this increase in concurrency will only be 
 * fully realized on a multi-processor, and then only if the access patterns for the shared data are suitable.
 *
 * Whether or not a read-write lock will improve performance over the use of a mutual exclusion lock depends on 
 * the frequency that the data is read compared to being modified, the duration of the read and write operations, 
 * and the contention for the data - that is, the number of threads that will try to read or write the data 
 * at the same time. For example, a collection that is initially populated with data and thereafter infrequently 
 * modified, while being frequently searched is an ideal candidate for the use of a read-write lock. However, 
 * if updates become frequent then the data spends most of its time being exclusively locked and there is little, 
 * if any increase in concurrency. Further, if the read operations are too short the overhead of the read-write 
 * lock implementation (which is inherently more complex than a mutual exclusion lock) can dominate the execution
 * cost, particularly as many read-write lock implementations still serialize all threads through a small section 
 * of code. Ultimately, only profiling and measurement will establish whether the use of a read-write lock is
 * suitable for your application.
 *
 * Although the basic operation of a read-write lock is straight-forward, there are many policy decisions that an 
 * implementation must make, which may affect the effectiveness of the read-write lock in a given application.
 * Examples of these policies include:
 * 1.Determining whether to grant the read lock or the write lock, when both readers and writers are waiting, 
 * at the time that a writer releases the write lock. Writer preference is common, as writes are expected to be
 * short and infrequent. Reader preference is less common as it can lead to lengthy delays for a write if the 
 * readers are frequent and long-lived as expected. Fair, or 'in-order' implementations are also possible.
 * 2.Determining whether readers that request the read lock while a reader is active and a writer is waiting, 
 * are granted the read lock. Preference to the reader can delay the writer indefinitely, while preference to 
 * the writer can reduce the potential for concurrency.
 * 3.Determining whether the locks are reentrant: can a thread with the write lock reacquire it? Can it acquire a 
 * read lock while holding the write lock? Is the read lock itself reentrant? Can the write lock be downgraded to 
 * a read lock without allowing an intervening writer? Can a read lock be upgraded to a write lock, in preference 
 * to other waiting readers or writers?
 * 
 * You should consider all of these things when evaluating the suitability of a given implementation 
 * for your application.
 */
public interface ReadWriteLock {
    /** Returns the lock used for reading */
    Lock readLock();

    /** Returns the lock used for writing */
    Lock writeLock();
}
```
```java
/**
 * An implementation of ReadWriteLock supporting similar semantics to ReentrantLock.
 * This class has the following properties:
 *
 * 1.Acquisition order
 * This class does not impose a reader or writer preference ordering for lock access. However, it does support 
 * an optional fairness policy.
 * 
 * 1.1 Non-fair mode (default)
 * When constructed as non-fair (the default), the order of entry to the read and write lock is unspecified, 
 * subject to reentrancy constraints. A nonfair lock that is continuously contended may indefinitely postpone 
 * one or more reader or writer threads, but will normally have higher throughput than a fair lock.
 * 
 * 1.2 Fair mode
 * When constructed as fair, threads contend for entry using an approximately arrival-order policy. When the 
 * currently held lock is released, either the longest-waiting single writer thread will be assigned the write lock, 
 * or if there is a group of reader threads waiting longer than all waiting writer threads, that group will be
 * assigned the read lock.
 *
 * A thread that tries to acquire a fair read lock (non-reentrantly) will block if either the write lock is held, 
 * or there is a waiting writer thread. The thread will not acquire the read lock until after the oldest currently 
 * waiting writer thread has acquired and released the write lock. Of course, if a waiting writer abandons its wait, 
 * leaving one or more reader threads as the longest waiters in the queue with the write lock free, then those 
 * readers will be assigned the read lock.
 *
 * A thread that tries to acquire a fair write lock (non-reentrantly) will block unless both the read lock and 
 * write lock are free (which implies there are no waiting threads). (Note that the non-blocking ReadLock.tryLock()
 * and WriteLock.tryLock() methods do not honor this fair setting and will immediately acquire the lock if it 
 * is possible, regardless of waiting threads.)
 * 
 * 2.Reentrancy
 * This lock allows both readers and writers to reacquire read or write locks in the style of a ReentrantLock. 
 * Non-reentrant readers are not allowed until all write locks held by the writing thread have been released.
 *
 * Additionally, a writer can acquire the read lock, but not vice-versa. Among other applications, reentrancy 
 * can be useful when write locks are held during calls or callbacks to methods that perform reads under read locks. 
 * If a reader tries to acquire the write lock it will never succeed.
 *
 * 3.Lock downgrading
 * Reentrancy also allows downgrading from the write lock to a read lock, by acquiring the write lock, then the 
 * read lock and then releasing the write lock. However, upgrading from a read lock to the write lock is not possible.
 *
 * 4.Interruption of lock acquisition
 * The read lock and write lock both support interruption during lock acquisition.
 *
 * 5.Condition support
 * The write lock provides a Condition implementation that behaves in the same way, with respect to the write lock, 
 * as the Condition implementation provided by ReentrantLock.newCondition() does for ReentrantLock. This Condition 
 * can only be used with the write lock. The read lock does not support a Condition and readLock().newCondition() 
 * throws UnsupportedOperationException.
 *
 * 6.Instrumentation
 * This class supports methods to determine whether locks are held or contended. These methods are designed for 
 * monitoring system state, not for synchronization control.
 *
 * Serialization of this class behaves in the same way as built-in locks: a deserialized lock is in the 
 * unlocked state, regardless of its state when serialized.
 *
 * ReentrantReadWriteLocks can be used to improve concurrency in some uses of some kinds of Collections. This is 
 * typically worthwhile only when the collections are expected to be large, accessed by more reader threads than 
 * writer threads, and entail operations with overhead that outweighs synchronization overhead. 
 * 
 * This lock supports a maximum of 65535 recursive write locks and 65535 read locks. Attempts to exceed these 
 * limits result in Error throws from locking methods.
 */
public class ReentrantReadWriteLock implements ReadWriteLock, java.io.Serializable {

    /** Inner class providing readlock */
    private final ReentrantReadWriteLock.ReadLock readerLock;
    public ReentrantReadWriteLock.ReadLock  readLock()  { return readerLock; }
    /** Inner class providing writelock */
    private final ReentrantReadWriteLock.WriteLock writerLock
    public ReentrantReadWriteLock.WriteLock writeLock() { return writerLock; };
    
    /** Performs all synchronization mechanics */
    final Sync sync;

    /** Creates a new ReentrantReadWriteLock with  the given fairness policy */
    public ReentrantReadWriteLock(boolean fair) {
        sync = fair ? new FairSync() : new NonfairSync();
        readerLock = new ReadLock(this);
        writerLock = new WriteLock(this);
    }
    public ReentrantReadWriteLock() {
        this(false);
    }

    /** Synchronization implementation for ReentrantReadWriteLock. Subclassed into fair and nonfair versions */
    abstract static class Sync extends AbstractQueuedSynchronizer {

        /*
         * Read vs write count extraction constants and functions. Lock state is logically divided into two 
         * unsigned shorts: The lower one representing the exclusive (writer) lock hold count, and the upper 
         * the shared (reader) hold count.
         */

        static final int SHARED_SHIFT   = 16;
        static final int SHARED_UNIT    = (1 << SHARED_SHIFT);
        static final int MAX_COUNT      = (1 << SHARED_SHIFT) - 1;
        static final int EXCLUSIVE_MASK = (1 << SHARED_SHIFT) - 1;

        /** Returns the number of shared holds represented in count  */
        static int sharedCount(int c)    { return c >>> SHARED_SHIFT; }
        /** Returns the number of exclusive holds represented in count  */
        static int exclusiveCount(int c) { return c & EXCLUSIVE_MASK; }

        /** A counter for per-thread read hold counts. Maintained as a ThreadLocal; cached in cachedHoldCounter */
        static final class HoldCounter {
            int count = 0;
            // Use id, not reference, to avoid garbage retention
            final long tid = getThreadId(Thread.currentThread());
        }

        /** ThreadLocal subclass. Easiest to explicitly define for sake of deserialization mechanics */
        static final class ThreadLocalHoldCounter extends ThreadLocal<HoldCounter> {
            public HoldCounter initialValue() {
                return new HoldCounter();
            }
        }

        /**
         * The number of reentrant read locks held by current thread. Initialized only in constructor and readObject.
         * Removed whenever a thread's read hold count drops to 0.
         */
        private transient ThreadLocalHoldCounter readHolds;

        /**
         * The hold count of the last thread to successfully acquire readLock. This saves ThreadLocal lookup 
         * in the common case where the next thread to release is the last one to acquire. This is non-volatile 
         * since it is just used as a heuristic, and would be great for threads to cache.
         * Can outlive the Thread for which it is caching the read hold count, but avoids garbage retention 
         * by not retaining a reference to the Thread.
         * Accessed via a benign data race; relies on the memory model's final field and out-of-thin-air guarantees.
         */
        private transient HoldCounter cachedHoldCounter;

        /**
         * firstReader is the first thread to have acquired the read lock. firstReaderHoldCount is firstReader's 
         * hold count. More precisely, firstReader is the unique thread that last changed the shared count from 
         * 0 to 1, and has not released the read lock since then; null if there is no such thread.
         * Cannot cause garbage retention unless the thread terminated without relinquishing its read locks, 
         * since tryReleaseShared sets it to null.
         * Accessed via a benign data race; relies on the memory model's out-of-thin-air guarantees for references.
         * This allows tracking of read holds for uncontended read locks to be very cheap.
         */
        private transient Thread firstReader = null;
        private transient int firstReaderHoldCount;

        Sync() {
            readHolds = new ThreadLocalHoldCounter();
            setState(getState()); // ensures visibility of readHolds
        }

        /*
         * Acquires and releases use the same code for fair and nonfair locks, but differ in whether/how they 
         * allow barging when queues are non-empty.
         */

        /**
         * Returns true if the current thread, when trying to acquire the read lock, and otherwise eligible to do so, 
         * should block because of policy for overtaking other waiting threads.
         */
        abstract boolean readerShouldBlock();

        /**
         * Returns true if the current thread, when trying to acquire the write lock, and otherwise eligible to do so, 
         * should block because of policy for overtaking other waiting threads.
         */
        abstract boolean writerShouldBlock();

        /*
         * Note that tryRelease and tryAcquire can be called by Conditions. So it is possible that their 
         * arguments contain both read and write holds that are all released during a condition wait and 
         * re-established in tryAcquire.
         */
        protected final boolean tryRelease(int releases) {
            if (!isHeldExclusively())
                throw new IllegalMonitorStateException();
            int nextc = getState() - releases;
            boolean free = exclusiveCount(nextc) == 0;
            if (free)
                setExclusiveOwnerThread(null);
            setState(nextc);
            return free;
        }

        protected final boolean tryAcquire(int acquires) {
            /*
             * Walkthrough:
             * 1. If read count nonzero or write count nonzero and owner is a different thread, fail.
             * 2. If count would saturate, fail. (This can only happen if count is already nonzero.)
             * 3. Otherwise, this thread is eligible for lock if it is either a reentrant acquire or
             *    queue policy allows it. If so, update state and set owner.
             */
            Thread current = Thread.currentThread();
            int c = getState();
            int w = exclusiveCount(c);
            if (c != 0) {
                // (Note: if c != 0 and w == 0 then shared count != 0)
                if (w == 0 || current != getExclusiveOwnerThread())
                    return false;
                if (w + exclusiveCount(acquires) > MAX_COUNT)
                    throw new Error("Maximum lock count exceeded");
                // Reentrant acquire
                setState(c + acquires);
                return true;
            }
            if (writerShouldBlock() || !compareAndSetState(c, c + acquires))
                return false;
            setExclusiveOwnerThread(current);
            return true;
        }

        protected final boolean tryReleaseShared(int unused) {
            Thread current = Thread.currentThread();
            if (firstReader == current) {
                // assert firstReaderHoldCount > 0;
                if (firstReaderHoldCount == 1)
                    firstReader = null;
                else
                    firstReaderHoldCount--;
            } else {
                HoldCounter rh = cachedHoldCounter;
                if (rh == null || rh.tid != getThreadId(current))
                    rh = readHolds.get();
                int count = rh.count;
                if (count <= 1) {
                    readHolds.remove();
                    if (count <= 0)
                        throw unmatchedUnlockException();
                }
                --rh.count;
            }
            for (;;) {
                int c = getState();
                int nextc = c - SHARED_UNIT;
                if (compareAndSetState(c, nextc))
                    // Releasing the read lock has no effect on readers,
                    // but it may allow waiting writers to proceed if
                    // both read and write locks are now free.
                    return nextc == 0;
            }
        }

        private IllegalMonitorStateException unmatchedUnlockException() {
            return new IllegalMonitorStateException("attempt to unlock read lock, not locked by current thread");
        }

        protected final int tryAcquireShared(int unused) {
            /*
             * Walkthrough:
             * 1. If write lock held by another thread, fail.
             * 2. Otherwise, this thread is eligible for lock wrt state, so ask if it should block
             *    because of queue policy. If not, try to grant by CASing state and updating count.
             *    Note that step does not check for reentrant acquires, which is postponed to full version
             *    to avoid having to check hold count in the more typical non-reentrant case.
             * 3. If step 2 fails either because thread apparently not eligible or CAS fails or count
             *    saturated, chain to version with full retry loop.
             */
            Thread current = Thread.currentThread();
            int c = getState();
            if (exclusiveCount(c) != 0 && getExclusiveOwnerThread() != current)
                return -1;
            int r = sharedCount(c);
            if (!readerShouldBlock() && r < MAX_COUNT && compareAndSetState(c, c + SHARED_UNIT)) {
                if (r == 0) {
                    firstReader = current;
                    firstReaderHoldCount = 1;
                } else if (firstReader == current) {
                    firstReaderHoldCount++;
                } else {
                    HoldCounter rh = cachedHoldCounter;
                    if (rh == null || rh.tid != getThreadId(current))
                        cachedHoldCounter = rh = readHolds.get();
                    else if (rh.count == 0)
                        readHolds.set(rh);
                    rh.count++;
                }
                return 1;
            }
            return fullTryAcquireShared(current);
        }

        /**
         * Full version of acquire for reads, that handles CAS misses
         * and reentrant reads not dealt with in tryAcquireShared.
         */
        final int fullTryAcquireShared(Thread current) {
            /*
             * This code is in part redundant with that in tryAcquireShared but is simpler overall by not
             * complicating tryAcquireShared with interactions between retries and lazily reading hold counts.
             */
            HoldCounter rh = null;
            for (;;) {
                int c = getState();
                if (exclusiveCount(c) != 0) {
                    if (getExclusiveOwnerThread() != current)
                        return -1;
                    // else we hold the exclusive lock; blocking here would cause deadlock.
                } else if (readerShouldBlock()) {
                    // Make sure we're not acquiring read lock reentrantly
                    if (firstReader == current) {
                        // assert firstReaderHoldCount > 0;
                    } else {
                        if (rh == null) {
                            rh = cachedHoldCounter;
                            if (rh == null || rh.tid != getThreadId(current)) {
                                rh = readHolds.get();
                                if (rh.count == 0)
                                    readHolds.remove();
                            }
                        }
                        if (rh.count == 0)
                            return -1;
                    }
                }
                if (sharedCount(c) == MAX_COUNT)
                    throw new Error("Maximum lock count exceeded");
                if (compareAndSetState(c, c + SHARED_UNIT)) {
                    if (sharedCount(c) == 0) {
                        firstReader = current;
                        firstReaderHoldCount = 1;
                    } else if (firstReader == current) {
                        firstReaderHoldCount++;
                    } else {
                        if (rh == null)
                            rh = cachedHoldCounter;
                        if (rh == null || rh.tid != getThreadId(current))
                            rh = readHolds.get();
                        else if (rh.count == 0)
                            readHolds.set(rh);
                        rh.count++;
                        cachedHoldCounter = rh; // cache for release
                    }
                    return 1;
                }
            }
        }

        /**
         * Performs tryLock for write, enabling barging in both modes. This is identical in effect to 
         * tryAcquire except for lack of calls to writerShouldBlock.
         */
        final boolean tryWriteLock() {
            Thread current = Thread.currentThread();
            int c = getState();
            if (c != 0) {
                int w = exclusiveCount(c);
                if (w == 0 || current != getExclusiveOwnerThread())
                    return false;
                if (w == MAX_COUNT)
                    throw new Error("Maximum lock count exceeded");
            }
            if (!compareAndSetState(c, c + 1))
                return false;
            setExclusiveOwnerThread(current);
            return true;
        }

        /**
         * Performs tryLock for read, enabling barging in both modes. This is identical in effect to 
         * tryAcquireShared except for lack of calls to readerShouldBlock.
         */
        final boolean tryReadLock() {
            Thread current = Thread.currentThread();
            for (;;) {
                int c = getState();
                if (exclusiveCount(c) != 0 &&
                    getExclusiveOwnerThread() != current)
                    return false;
                int r = sharedCount(c);
                if (r == MAX_COUNT)
                    throw new Error("Maximum lock count exceeded");
                if (compareAndSetState(c, c + SHARED_UNIT)) {
                    if (r == 0) {
                        firstReader = current;
                        firstReaderHoldCount = 1;
                    } else if (firstReader == current) {
                        firstReaderHoldCount++;
                    } else {
                        HoldCounter rh = cachedHoldCounter;
                        if (rh == null || rh.tid != getThreadId(current))
                            cachedHoldCounter = rh = readHolds.get();
                        else if (rh.count == 0)
                            readHolds.set(rh);
                        rh.count++;
                    }
                    return true;
                }
            }
        }

        protected final boolean isHeldExclusively() {
            // While we must in general read state before owner,
            // we don't need to do so to check if current thread is owner
            return getExclusiveOwnerThread() == Thread.currentThread();
        }

        // Methods relayed to outer class

        final ConditionObject newCondition() {
            return new ConditionObject();
        }

        final Thread getOwner() {
            // Must read state before owner to ensure memory consistency
            return ((exclusiveCount(getState()) == 0) ? null : getExclusiveOwnerThread());
        }

        final int getReadLockCount() {
            return sharedCount(getState());
        }

        final boolean isWriteLocked() {
            return exclusiveCount(getState()) != 0;
        }

        final int getWriteHoldCount() {
            return isHeldExclusively() ? exclusiveCount(getState()) : 0;
        }

        final int getReadHoldCount() {
            if (getReadLockCount() == 0)
                return 0;

            Thread current = Thread.currentThread();
            if (firstReader == current)
                return firstReaderHoldCount;

            HoldCounter rh = cachedHoldCounter;
            if (rh != null && rh.tid == getThreadId(current))
                return rh.count;

            int count = readHolds.get().count;
            if (count == 0) readHolds.remove();
            return count;
        }

        final int getCount() { return getState(); }
    }

    /** Nonfair version of Sync */
    static final class NonfairSync extends Sync {
        final boolean writerShouldBlock() {
            return false; // writers can always barge
        }
        final boolean readerShouldBlock() {
            /**
             * As a heuristic to avoid indefinite writer starvation, block if the thread that momentarily 
             * appears to be head of queue, if one exists, is a waiting writer. This is only a probabilistic 
             * effect since a new reader will not block if there is a waiting writer behind other enabled
             * readers that have not yet drained from the queue.
             */
            return apparentlyFirstQueuedIsExclusive();
        }
    }

    /** Fair version of Sync */
    static final class FairSync extends Sync {
        private static final long serialVersionUID = -2274990926593161451L;
        final boolean writerShouldBlock() {
            return hasQueuedPredecessors();
        }
        final boolean readerShouldBlock() {
            return hasQueuedPredecessors();
        }
    }

    /** The lock returned by method ReentrantReadWriteLock.readLock() */
    public static class ReadLock implements Lock, java.io.Serializable {
        private final Sync sync;

        /** Constructor for use by subclasses */
        protected ReadLock(ReentrantReadWriteLock lock) {
            sync = lock.sync;
        }

        /**
         * Acquires the read lock if the write lock is not held by another thread and returns immediately.
         * If the write lock is held by another thread then the current thread becomes disabled for thread 
         * scheduling purposes and lies dormant until the read lock has been acquired.
         */
        public void lock() {
            sync.acquireShared(1);
        }

        /**
         * Acquires the read lock unless the current thread is interrupted. Acquires the read lock if the 
         * write lock is not held by another thread and returns immediately. If the write lock is held by another 
         * thread then the current thread becomes disabled for thread scheduling purposes and lies dormant until 
         * The read lock is acquired by the current thread or Some other thread interrupts the current thread.
         *
         * If the current thread has its interrupted status set on entry to this method or is interrupted while
         * acquiring the read lock, then InterruptedException is thrown and the current thread's interrupted status 
         * is cleared.
         *
         * In this implementation, as this method is an explicit interruption point, preference is given to 
         * responding to the interrupt over normal or reentrant acquisition of the lock.
         */
        public void lockInterruptibly() throws InterruptedException {
            sync.acquireSharedInterruptibly(1);
        }

        /**
         * Acquires the read lock if the write lock is not held by another thread and returns immediately with true. 
         * Even when this lock has been set to use a fair ordering policy, a call to tryLock() will immediately 
         * acquire the read lock if it is available, whether or not other threads are currently waiting for the 
         * read lock. This 'barging' behavior can be useful in certain circumstances, even though it breaks fairness. 
         * If you want to honor the fairness setting for this lock, then use tryLock(long, TimeUnit) which is 
         * almost equivalent (it also detects interruption).
         *
         * If the write lock is held by another thread then this method will return immediately with false.
         */
        public boolean tryLock() {
            return sync.tryReadLock();
        }

        /**
         * Acquires the read lock if the write lock is not held by another thread and returns immediately with true. 
         * If this lock has been set to use a fair ordering policy then an available lock will not be acquired 
         * if any other threads are waiting for the lock. This is in contrast to the tryLock() method. If you want 
         * a timed tryLock that does permit barging on a fair lock then combine the timed and un-timed forms together:
         * if (lock.tryLock() || lock.tryLock(timeout, unit)).
         *
         * If the write lock is held by another thread then the current thread becomes disabled for thread scheduling
         * purposes and lies dormant until the read lock is acquired by the current thread or some other thread 
         * interrupts the current thread or the specified waiting time elapses.
         *
         * If the current thread has its interrupted status set on entry to this method or is interrupted while
         * acquiring the read lock, then InterruptedException is thrown and the current thread's interrupted status 
         * is cleared.
         *
         * If the specified waiting time elapses then false is returned. If the time is less than or equal to zero, 
         * the method will not wait at all.
         *
         * In this implementation, as this method is an explicit interruption point, preference is given to 
         * responding to the interrupt over normal or reentrant acquisition of the lock, and over reporting the 
         * elapse of the waiting time.
         */
        public boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException {
            return sync.tryAcquireSharedNanos(1, unit.toNanos(timeout));
        }

        /**
         * Attempts to release this lock. If the number of readers is now zero then the lock
         * is made available for write lock attempts.
         */
        public void unlock() {
            sync.releaseShared(1);
        }

        /** Throws UnsupportedOperationException because ReadLocks do not support conditions */
        public Condition newCondition() {
            throw new UnsupportedOperationException();
        }
    }

    /** The lock returned by method ReentrantReadWriteLock.writeLock */
    public static class WriteLock implements Lock, java.io.Serializable {
        private final Sync sync;

        /** Constructor for use by subclasses */
        protected WriteLock(ReentrantReadWriteLock lock) {
            sync = lock.sync;
        }

        /**
         * Acquires the write lock if neither the read nor write lock are held by another thread and returns 
         * immediately, setting the write lock hold count to one. If the current thread already holds the write 
         * lock then the hold count is incremented by one and the method returns immediately.
         *
         * If the lock is held by another thread then the current thread becomes disabled for thread scheduling 
         * purposes and lies dormant until the write lock has been acquired, at which time the write lock hold 
         * count is set to one.
         */
        public void lock() {
            sync.acquire(1);
        }

        /**
         * Acquires the write lock unless the current thread is interrupted. Acquires the write lock if neither 
         * the read nor write lock are held by another thread and returns immediately, setting the write lock 
         * hold count to one. If the current thread already holds this lock then the hold count is incremented 
         * by one and the method returns immediately.
         *
         * If the lock is held by another thread then the current thread becomes disabled for thread scheduling 
         * purposes and lies dormant until the write lock is acquired by the current thread or some other thread 
         * interrupts the current thread.
         *
         * If the write lock is acquired by the current thread then the lock hold count is set to one. If the 
         * current thread has its interrupted status set on entry to this method or is interrupted while acquiring 
         * the write lock, then InterruptedException is thrown and the current thread's interrupted status is cleared.
         *
         * In this implementation, as this method is an explicit interruption point, preference is given to 
         * responding to the interrupt over normal or reentrant acquisition of the lock.
         */
        public void lockInterruptibly() throws InterruptedException {
            sync.acquireInterruptibly(1);
        }

        /**
         * Acquires the write lock if neither the read nor write lock are held by another thread and returns 
         * immediately with true, setting the write lock hold count to one. Even when this lock has been set 
         * to use a fair ordering policy, a call to tryLock() will immediately acquire the lock if it is available, 
         * whether or not other threads are currently waiting for the write lock. This 'barging' behavior can be 
         * useful in certain circumstances, even though it breaks fairness. If you want to honor the fairness 
         * setting for this lock, then use tryLock(long, TimeUnit) which is almost equivalent 
         * (it also detects interruption).
         *
         * If the current thread already holds this lock then the hold count is incremented by one and the 
         * method returns true. If the lock is held by another thread then this method will return immediately 
         * with the value false
         */
        public boolean tryLock( ) {
            return sync.tryWriteLock();
        }

        /**
         * Acquires the write lock if neither the read nor write lock are held by another thread and returns 
         * immediately with the value true, setting the write lock hold count to one. If this lock has been
         * set to use a fair ordering policy then an available lock will not be acquired if any other threads 
         * are waiting for the write lock. This is in contrast to the tryLock() method. 
         * If the current thread already holds this lock then the hold count is incremented by one and the 
         * method returns true.
         *
         * If the lock is held by another thread then the current thread becomes disabled for thread scheduling 
         * purposes and lies dormant until the write lock is acquired by the current thread or some other thread 
         * interrupts the current thread or the specified waiting time elapses
         *
         * If the write lock is acquired then true is returned and the write lock hold count is set to one.
         *
         * If the current thread has its interrupted status set on entry to this method or is interrupted while
         * acquiring the write lock, then InterruptedException is thrown and the current thread's interrupted status 
         * is cleared.
         *
         * If the specified waiting time elapses then false is returned. If the time is less than or equal to zero, 
         * the method will not wait at all.
         *
         * In this implementation, as this method is an explicit interruption point, preference is given to 
         * responding to the interrupt over normal or reentrant acquisition of the lock, and over reporting the 
         * elapse of the waiting time.
         */
        public boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException {
            return sync.tryAcquireNanos(1, unit.toNanos(timeout));
        }

        /**
         * Attempts to release this lock. If the current thread is the holder of this lock then the hold count 
         * is decremented. If the hold count is now zero then the lock is released. If the current thread is 
         * not the holder of this lock then IllegalMonitorStateException is thrown.
         */
        public void unlock() {
            sync.release(1);
        }

        /**
         * Returns a Condition instance for use with this Lock instance. The returned Condition instance supports 
         * the same usages as do the Object monitor methods (Object.wait(), Object.notify(), and Object.notifyAll) 
         * when used with the built-in monitor lock.
         *
         * If this write lock is not held when any Condition method is called then an IllegalMonitorStateException
         * is thrown. (Read locks are held independently of write locks, so are not checked or affected. However 
         * it is essentially always an error to invoke a condition waiting method when the current thread has 
         * also acquired read locks, since other threads that could unblock it will not be able to acquire the write
         * lock.)
         *
         * When the condition Condition.await() methods are called the write lock is released and, before they 
         * return, the write lock is reacquired and the lock hold count restored to what it was when the method 
         * was called.
         *
         * If a thread is interrupted while waiting then the wait will terminate, an InterruptedException will 
         * be thrown, and the thread's interrupted status will be cleared.
         *
         * Waiting threads are signalled in FIFO order. The ordering of lock reacquisition for threads returning
         * from waiting methods is the same as for threads initially acquiring the lock, which is in the default 
         * case not specified, but for fair locks favors those threads that have been waiting the longest.
         */
        public Condition newCondition() {
            return sync.newCondition();
        }

        /**
         * Queries if this write lock is held by the current thread. Identical in effect to 
         * ReentrantReadWriteLock.isWriteLockedByCurrentThread()
         */
        public boolean isHeldByCurrentThread() {
            return sync.isHeldExclusively();
        }

        /**
         * Queries the number of holds on this write lock by the current thread. A thread has a hold on a lock 
         * for each lock action that is not matched by an unlock action. Identical in effect to 
         * ReentrantReadWriteLock.getWriteHoldCount().
         */
        public int getHoldCount() {
            return sync.getWriteHoldCount();
        }
    }

    // Instrumentation and status

    /** Returns true if this lock has fairness set true */
    public final boolean isFair() {
        return sync instanceof FairSync;
    }

    /**
     * Returns the thread that currently owns the write lock, or null if not owned. When this method is called by a
     * thread that is not the owner, the return value reflects a best-effort approximation of current lock status. 
     * For example, the owner may be momentarily null even if there are threads trying to acquire the lock but have 
     * not yet done so. This method is designed to facilitate construction of subclasses that provide more 
     * extensive lock monitoring facilities.
     */
    protected Thread getOwner() {
        return sync.getOwner();
    }

    /**
     * Queries the number of read locks held for this lock. This method is designed for use in monitoring 
     * system state, not for synchronization control.
     */
    public int getReadLockCount() {
        return sync.getReadLockCount();
    }

    /**
     * Queries if the write lock is held by any thread. This method is designed for use in monitoring 
     * system state, not for synchronization control.
     */
    public boolean isWriteLocked() {
        return sync.isWriteLocked();
    }

    /** Queries if the write lock is held by the current thread */
    public boolean isWriteLockedByCurrentThread() {
        return sync.isHeldExclusively();
    }

    /**
     * Queries the number of reentrant write holds on this lock by the current thread. A writer thread has 
     * a hold on a lock for each lock action that is not matched by an unlock action.
     */
    public int getWriteHoldCount() {
        return sync.getWriteHoldCount();
    }

    /**
     * Queries the number of reentrant read holds on this lock by the current thread. A reader thread has 
     * a hold on a lock for each lock action that is not matched by an unlock action.
     */
    public int getReadHoldCount() {
        return sync.getReadHoldCount();
    }

    /**
     * Returns a collection containing threads that may be waiting to acquire the write/read lock. Because the actual 
     * set of threads may change dynamically while constructing this result, the returned collection is only a 
     * best-effort estimate. The elements of the returned collection are in no particular order. This method is
     * designed to facilitate construction of subclasses that provide more extensive lock monitoring facilities.
     */
    protected Collection<Thread> getQueuedWriterThreads() {
        return sync.getExclusiveQueuedThreads();
    }
    protected Collection<Thread> getQueuedReaderThreads() {
        return sync.getSharedQueuedThreads();
    }

    /**
     * Queries whether any threads are waiting to acquire the read or write lock. Note that because cancellations 
     * may occur at any time, a true return does not guarantee that any other thread will ever acquire a lock. 
     * This method is designed primarily for use in monitoring of the system state.
     */
    public final boolean hasQueuedThreads() {
        return sync.hasQueuedThreads();
    }

    /**
     * Queries whether the given thread is waiting to acquire either the read or write lock. Note that because 
     * cancellations may occur at any time, a true return does not guarantee that this thread will ever acquire 
     * a lock. This method is designed primarily for use in monitoring of the system state.
     */
    public final boolean hasQueuedThread(Thread thread) {
        return sync.isQueued(thread);
    }

    /**
     * Returns an estimate of the number of threads waiting to acquire either the read or write lock. The value is 
     * only an estimate because the number of threads may change dynamically while this method traverses internal data 
     * structures. This method is designed for use in monitoring of the system state, not for synchronization control.
     */
    public final int getQueueLength() {
        return sync.getQueueLength();
    }

    /**
     * Returns a collection containing threads that may be waiting to acquire either the read or write lock. 
     * Because the actual set of threads may change dynamically while constructing this result, the returned 
     * collection is only a best-effort estimate. The elements of the returned collection are in no particular
     * order. This method is designed to facilitate construction of subclasses that provide more extensive 
     * monitoring facilities.
     */
    protected Collection<Thread> getQueuedThreads() {
        return sync.getQueuedThreads();
    }

    /**
     * Queries whether any threads are waiting on the given condition associated with the write lock. Note that 
     * because timeouts and interrupts may occur at any time, a true return does not guarantee that a future signal
     * will awaken any threads. This method is designed primarily for use in monitoring of the system state.
     */
    public boolean hasWaiters(Condition condition) {
        if (condition == null)
            throw new NullPointerException();
        if (!(condition instanceof AbstractQueuedSynchronizer.ConditionObject))
            throw new IllegalArgumentException("not owner");
        return sync.hasWaiters((AbstractQueuedSynchronizer.ConditionObject)condition);
    }

    /**
     * Returns an estimate of the number of threads waiting on the given condition associated with the write lock. 
     * Note that because timeouts and interrupts may occur at any time, the estimate serves only as an upper bound 
     * on the actual number of waiters. This method is designed for use in monitoring of the system state, 
     * not for synchronization control.
     */
    public int getWaitQueueLength(Condition condition) {
        if (condition == null)
            throw new NullPointerException();
        if (!(condition instanceof AbstractQueuedSynchronizer.ConditionObject))
            throw new IllegalArgumentException("not owner");
        return sync.getWaitQueueLength((AbstractQueuedSynchronizer.ConditionObject)condition);
    }

    /**
     * Returns a collection containing those threads that may be waiting on the given condition associated with 
     * the write lock. Because the actual set of threads may change dynamically while constructing this result, 
     * the returned collection is only a best-effort estimate. The elements of the returned collection are in 
     * no particular order. This method is designed to facilitate construction of subclasses that provide more
     * extensive condition monitoring facilities.
     */
    protected Collection<Thread> getWaitingThreads(Condition condition) {
        if (condition == null)
            throw new NullPointerException();
        if (!(condition instanceof AbstractQueuedSynchronizer.ConditionObject))
            throw new IllegalArgumentException("not owner");
        return sync.getWaitingThreads((AbstractQueuedSynchronizer.ConditionObject)condition);
    }

    /**
     * Returns the thread id for the given thread. We must access this directly rather than via method 
     * Thread.getId() because getId() is not final, and has been known to be overridden in ways that do not 
     * preserve unique mappings.
     */
    static final long getThreadId(Thread thread) {
        return UNSAFE.getLongVolatile(thread, TID_OFFSET);
    }

    // Unsafe mechanics
    private static final sun.misc.Unsafe UNSAFE;
    private static final long TID_OFFSET;
    static {
        try {
            UNSAFE = sun.misc.Unsafe.getUnsafe();
            Class<?> tk = Thread.class;
            TID_OFFSET = UNSAFE.objectFieldOffset(tk.getDeclaredField("tid"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }
}
```