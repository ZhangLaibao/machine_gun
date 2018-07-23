```java
/**
 * Lock implementations provide more extensive locking operations than can be obtained using synchronized methods
 * and statements. They allow more flexible structuring, may have quite different properties, and may support 
 * multiple associated Condition objects.
 *
 * A lock is a tool for controlling access to a shared resource by multiple threads. Commonly, a lock provides 
 * exclusive access to a shared resource: only one thread at a time can acquire the lock and all access to the 
 * shared resource requires that the lock be acquired first. However, some locks may allow concurrent access to
 * a shared resource, such as the read lock of a ReadWriteLock.
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
 * the automatic release of locks that occurs with synchronized methods and statements. In most cases, the following 
 * idiom should be used:
 *
 * Lock l = ...;
 * l.lock();
 * try {
 *   // access the resource protected by this lock
 * } finally {
 *   l.unlock();
 * }
 *
 * When locking and unlocking occur in different scopes, care must be taken to ensure that all code that is executed 
 * while the lock is held is protected by try-finally or try-catch to ensure that the lock is released when necessary.
 *
 * Lock implementations provide additional functionality over the use of synchronized methods and statements by
 * providing a non-blocking attempt to acquire a lock (tryLock()), an attempt to acquire the lock that can be
 * interrupted (lockInterruptibly(), and an attempt to acquire the lock that can timeout (tryLock(long, TimeUnit)).
 *
 * A Lock class can also provide behavior and semantics that is quite different from that of the implicit monitor 
 * lock, such as guaranteed ordering, non-reentrant usage, or deadlock detection. If an implementation provides such 
 * specialized semantics then the implementation must document those semantics.
 *
 * Note that Lock instances are just normal objects and can themselves be used as the target in a synchronized 
 * statement. Acquiring the monitor lock of a Lock instance has no specified relationship with invoking any of the 
 * lock() methods of that instance. It is recommended that to avoid confusion you never use Lock instances in this 
 * way, except within their own implementation.
 *
 * Except where noted, passing a null value for any parameter will result in a NullPointerException being thrown.
 *
 * Memory Synchronization
 * All Lock implementations  must enforce the same memory synchronization semantics as provided by the built-in 
 * monitor lock, as described in The Java Language Specification (17.4 Memory Model):
 * A successful lock operation has the same memory synchronization effects as a successful Lock action.
 * A successful unlock operation has the same memory synchronization effects as a successful Unlock action.
 * Unsuccessful locking and unlocking operations, and reentrant locking/unlocking operations, do not require any 
 * memory synchronization effects.
 *
 * Implementation Considerations
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
     *
     * Implementation Considerations
     * Lock implementation may be able to detect erroneous use of the lock, such as an invocation that would cause 
     * deadlock, and may throw an (unchecked) exception in such circumstances. The circumstances and the exception 
     * type must be documented by that Lock implementation.
     */
    void lock();

    /**
     * Acquires the lock unless the current thread is interrupted. Acquires the lock if it is available and 
     * returns immediately. If the lock is not available then the current thread becomes disabled for thread 
     * scheduling purposes and lies dormant until one of two things happens:
     * 1.The lock is acquired by the current thread; 
     * 2.Some other thread interrupts the current thread, and interruption of lock acquisition is supported.
     *
     * If the current thread has its interrupted status set on entry to this method; or is interrupted while 
     * acquiring the lock, and interruption of lock acquisition is supported, then InterruptedException is thrown 
     * and the current thread's interrupted status is cleared.
     *
     * Implementation Considerations
     * The ability to interrupt a lock acquisition in some implementations may not be possible, and if possible 
     * may be an expensive operation. The programmer should be aware that this may be the case. An implementation 
     * should document when this is the case.
     *
     * An implementation can favor responding to an interrupt over normal method return.
     *
     * A  implementation may be able to detect erroneous use of the lock, such as an invocation that would
     * cause deadlock, and may throw an (unchecked) exception in such circumstances. The circumstances and the 
     * exception type must be documented by that Lock implementation.
     */
    void lockInterruptibly() throws InterruptedException;

    /**
     * Acquires the lock only if it is free at the time of invocation.
     *
     * Acquires the lock if it is available and returns immediately with the value true. If the lock is not 
     * available then this method will return immediately with the value false.
     *
     * A typical usage idiom for this method would be:
     * Lock lock = ...;
     * if (lock.tryLock()) {
     *   try {
     *     // manipulate protected state
     *   } finally {
     *     lock.unlock();
     *   }
     * } else {
     *   // perform alternative actions
     * }
     * This usage ensures that the lock is unlocked if it was acquired, and doesn't try to unlock if the lock 
     * was not acquired.
     */
    boolean tryLock();

    /**
     * Acquires the lock if it is free within the given waiting time and the current thread has not been interrupted.
     *
     * If the lock is available this method returns immediately with the value true. If the lock is not available 
     * then the current thread becomes disabled for thread scheduling purposes and lies dormant until one of three 
     * things happens:
     * 1.The lock is acquired by the current thread; 
     * 2.Some other thread interrupts the current thread, and interruption of lock acquisition is supported; 
     * 3.The specified waiting time elapses
     *
     * If the lock is acquired then the value true is returned. If the current thread has its interrupted status 
     * set on entry to this method; or is interrupted while acquiring the lock, and interruption of lock acquisition 
     * is supported, then InterruptedException is thrown and the current thread's interrupted status is cleared.
     *
     * If the specified waiting time elapses then the value false is returned. If the time is less than or equal 
     * to zero, the method will not wait at all.
     */
    boolean tryLock(long time, TimeUnit unit) throws InterruptedException;

    /**
     * Releases the lock.
     *
     * Implementation Considerations
     * A Lock implementation will usually impose restrictions on which thread can release a lock (typically only the
     * holder of the lock can release it) and may throw an (unchecked) exception if the restriction is violated.
     * Any restrictions and the exception type must be documented by that Lock implementation.
     */
    void unlock();

    /**
     * Returns a new Condition instance that is bound to this Lock instance. Before waiting on the condition the 
     * lock must be held by the current thread. A call to Condition.await() will atomically release the lock before 
     * waiting and re-acquire the lock before the wait returns.
     *
     * Implementation Considerations 
     * The exact operation of the Conditioninstance depends on the Lock implementation and must be documented by that
     * implementation.
     */
    Condition newCondition();
}
```

```java
/**
 * Basic thread blocking primitives for creating locks and other synchronization classes.
 *
 * This class associates, with each thread that uses it, a permit (in the sense of the java.util.concurrent.Semaphore 
 * class). A call to park will return immediately if the permit is available, consuming it in the process; otherwise
 * it may block. A call to unpark makes the permit available, if it was not already available. (Unlike with Semaphores
 * though, permits do not accumulate. There is at most one.)
 *
 * Methods park()and unpark() provide efficient means of blocking and unblocking threads that do not encounter the
 * problems that cause the deprecated methods Thread.suspend() and Thread.resume() to be unusable for such purposes: 
 * Races between one thread invoking park() and another thread trying to unpark() it will preserve liveness, due to 
 * the permit. Additionally, park() will return if the caller's thread was interrupted, and timeout versions are 
 * supported. The park() method may also return at any other time, for "no reason", so in general must be invoked 
 * within a loop that rechecks conditions upon return. In this sense park() serves as an optimization of a "busy wait" 
 * that does not waste as much time spinning, but must be paired with an unpark() to be effective.
 *
 * The three forms of park() each also support a blocker object parameter. This object is recorded while the thread 
 * is blocked to permit monitoring and diagnostic tools to identify the reasons that threads are blocked. (Such tools 
 * may access blockers using method getBlocker(Thread). The use of these forms rather than the original forms without 
 * this parameter is strongly encouraged. The normal argument to supply as a blocker within a lock implementation 
 * is "this".
 *
 * These methods are designed to be used as tools for creating higher-level synchronization utilities, and are not 
 * in themselves useful for most concurrency control applications. The park() method is designed for use only in 
 * constructions of the form:
 *
 * while (!canProceed()) { ... LockSupport.park(this); }}
 *
 * where neither canProceed() nor any other actions prior to the call to park entail locking or blocking. Because 
 * only one permit is associated with each thread, any intermediary uses of park() could interfere with its 
 * intended effects.
 *
 * Here is a sketch of a first-in-first-out non-reentrant lock class:
 * class FIFOMutex {
 *   private final AtomicBoolean locked = new AtomicBoolean(false);
 *   private final Queue<Thread> waiters = new ConcurrentLinkedQueue<Thread>();
 *
 *   public void lock() {
 *     boolean wasInterrupted = false;
 *     Thread current = Thread.currentThread();
 *     waiters.add(current);
 *
 *     // Block while not first in queue or cannot acquire lock
 *     while (waiters.peek() != current || !locked.compareAndSet(false, true)) {
 *       LockSupport.park(this);
 *       if (Thread.interrupted()) // ignore interrupts while waiting
 *         wasInterrupted = true;
 *     }
 *
 *     waiters.remove();
 *     if (wasInterrupted)          // reassert interrupt status on exit
 *       current.interrupt();
 *   }
 *
 *   public void unlock() {
 *     locked.set(false);
 *     LockSupport.unpark(waiters.peek());
 *   }
 * }
 */
public class LockSupport {

    private static void setBlocker(Thread t, Object arg) {
        // Even though volatile, hotspot doesn't need a write barrier here.
        UNSAFE.putObject(t, parkBlockerOffset, arg);
    }

    /**
     * Makes available the permit for the given thread, if it was not already available. If the thread was blocked on
     * park() then it will unblock. Otherwise, its next call to park() is guaranteed not to block. This operation
     * is not guaranteed to have any effect at all if the given thread has not been started.
     */
    public static void unpark(Thread thread) {
        if (thread != null)
            UNSAFE.unpark(thread);
    }

    /**
     * Disables the current thread for thread scheduling purposes unless the permit is available.
     *
     * If the permit is available then it is consumed and the call returns immediately; otherwise the current thread 
     * becomes disabled for thread scheduling purposes and lies dormant until one of three things happens:
     * 1.Some other thread invokes unpark() with the current thread as the target; 
     * 2.Some other thread interrupts the current thread; 
     * 3.The call spuriously (that is, for no reason) returns.
     *
     * This method does not report which of these caused the method to return. Callers should re-check the conditions 
     * which caused the thread to park in the first place. Callers may also determine, for example, the interrupt 
     * status of the thread upon return.
     */
    public static void park() {
        UNSAFE.park(false, 0L);
    }
    
    public static void parkNanos(long nanos) {
        if (nanos > 0)
            UNSAFE.park(false, nanos);
    }
    
    public static void parkUntil(long deadline) {
        UNSAFE.park(true, deadline);
    }
    
    public static void park(Object blocker) {
        Thread t = Thread.currentThread();
        setBlocker(t, blocker);
        UNSAFE.park(false, 0L);
        setBlocker(t, null);
    }

    public static void parkNanos(Object blocker, long nanos) {
        if (nanos > 0) {
            Thread t = Thread.currentThread();
            setBlocker(t, blocker);
            UNSAFE.park(false, nanos);
            setBlocker(t, null);
        }
    }

    public static void parkUntil(Object blocker, long deadline) {
        Thread t = Thread.currentThread();
        setBlocker(t, blocker);
        UNSAFE.park(true, deadline);
        setBlocker(t, null);
    }

    /**
     * Returns the blocker object supplied to the most recent invocation of a park method that has not yet unblocked, 
     * or null if not blocked. The value returned is just a momentary snapshot -- the thread may have since unblocked 
     * or blocked on a different blocker object.
     */
    public static Object getBlocker(Thread t) {
        if (t == null)
            throw new NullPointerException();
        return UNSAFE.getObjectVolatile(t, parkBlockerOffset);
    }

    /**
     * Returns the pseudo-randomly initialized or updated secondary seed. Copied from ThreadLocalRandom due to 
     * package access restrictions.
     */
    static final int nextSecondarySeed() {
        int r;
        Thread t = Thread.currentThread();
        if ((r = UNSAFE.getInt(t, SECONDARY)) != 0) {
            r ^= r << 13;   // xorshift
            r ^= r >>> 17;
            r ^= r << 5;
        }
        else if ((r = java.util.concurrent.ThreadLocalRandom.current().nextInt()) == 0)
            r = 1; // avoid zero
        UNSAFE.putInt(t, SECONDARY, r);
        return r;
    }

    // Hotspot implementation via intrinsics API
    private static final sun.misc.Unsafe UNSAFE;
    private static final long parkBlockerOffset;
    private static final long SEED;
    private static final long PROBE;
    private static final long SECONDARY;
    static {
        try {
            UNSAFE = sun.misc.Unsafe.getUnsafe();
            Class<?> tk = Thread.class;
            parkBlockerOffset = UNSAFE.objectFieldOffset
                (tk.getDeclaredField("parkBlocker"));
            SEED = UNSAFE.objectFieldOffset
                (tk.getDeclaredField("threadLocalRandomSeed"));
            PROBE = UNSAFE.objectFieldOffset
                (tk.getDeclaredField("threadLocalRandomProbe"));
            SECONDARY = UNSAFE.objectFieldOffset
                (tk.getDeclaredField("threadLocalRandomSecondarySeed"));
        } catch (Exception ex) { throw new Error(ex); }
    }
}
```