### java.util.concurrent.locks.LockSupport
LockSupport是用来创建锁和其他同步类的基本线程阻塞原语。
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
 *
 */
public class LockSupport {

    private static void setBlocker(Thread t, Object arg) {
        // Even though volatile, hotspot doesn't need a write barrier here.
        UNSAFE.putObject(t, parkBlockerOffset, arg);
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
    public static void park(Object blocker) {
        Thread t = Thread.currentThread();
        setBlocker(t, blocker);
        UNSAFE.park(false, 0L);
        setBlocker(t, null);
    }    
    
    public static void parkUntil(long deadline) {
        UNSAFE.park(true, deadline);
    }
    public static void parkUntil(Object blocker, long deadline) {
        Thread t = Thread.currentThread();
        setBlocker(t, blocker);
        UNSAFE.park(true, deadline);
        setBlocker(t, null);
    }

    public static void parkNanos(long nanos) {
        if (nanos > 0) UNSAFE.park(false, nanos);
    }
    public static void parkNanos(Object blocker, long nanos) {
        if (nanos > 0) {
            Thread t = Thread.currentThread();
            setBlocker(t, blocker);
            UNSAFE.park(false, nanos);
            setBlocker(t, null);
        }
    }

    /**
     * Makes available the permit for the given thread, if it was not already available. If the thread was blocked on
     * park() then it will unblock. Otherwise, its next call to park() is guaranteed not to block. This operation
     * is not guaranteed to have any effect at all if the given thread has not been started.
     */
    public static void unpark(Thread thread) {
        if (thread != null) UNSAFE.unpark(thread);
    }

    /**
     * Returns the blocker object supplied to the most recent invocation of a park method that has not yet unblocked, 
     * or null if not blocked. The value returned is just a momentary snapshot -- the thread may have since unblocked 
     * or blocked on a different blocker object.
     */
    public static Object getBlocker(Thread t) {
        if (t == null) throw new NullPointerException();
        return UNSAFE.getObjectVolatile(t, parkBlockerOffset);
    }

    // Hotspot implementation via intrinsics API
    private static final sun.misc.Unsafe UNSAFE;
    private static final long parkBlockerOffset;
    // ...
    
    static {
        try {
            UNSAFE = sun.misc.Unsafe.getUnsafe();
            Class<?> tk = Thread.class;
            parkBlockerOffset = UNSAFE.objectFieldOffset(tk.getDeclaredField("parkBlocker"));
            // ...
        } catch (Exception ex) { throw new Error(ex); }
    }
}
```
源代码文档中提供了一个FIFO的非阻塞锁框架的例子：
```java
class FIFOMutex {
    private final AtomicBoolean locked = new AtomicBoolean(false);
    private final Queue<Thread> waiters = new ConcurrentLinkedQueue<Thread>();

    public void lock() {
        boolean wasInterrupted = false;
        Thread current = Thread.currentThread();
        waiters.add(current);

        // Block while not first in queue or cannot acquire lock
        while (waiters.peek() != current || !locked.compareAndSet(false, true)) {
            LockSupport.park(this);
            if (Thread.interrupted()) // ignore interrupts while waiting
                wasInterrupted = true;
        }

        waiters.remove();
        if (wasInterrupted)          // reassert interrupt status on exit
            current.interrupt();
        }

    public void unlock() {
        locked.set(false);
        LockSupport.unpark(waiters.peek());
    }
}
```
关于LockSupport的一个重点是几种形式的park()方法-void park(), void parkNanos(long), void parkUntil(long)。在JDK1.6之后
对应每一个方法都添加了一个带有blocker参数的重载方法，官方推荐使用这些新的方法，因为在线程阻塞的时候这个blocker会被记录，
可以使用getBlocker()方法获取这个blocker对象，进一步通过监视工具(比如线程dump)和诊断工具确定线程受阻塞的原因。