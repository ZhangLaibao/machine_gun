### java.util.concurrent.locks.LockSupport
一言以蔽之，LockSupport是用来创建锁和其他同步类的基本线程阻塞原语。这也是官方文档的第一句话。
```java
/**
 * Basic thread blocking primitives for creating locks and other synchronization classes.
 * 
 * This class associates, with each thread that uses it, a permit (in the sense of the java.util.concurrent.Semaphore 
 * class). A call to park will return immediately if the permit is available, consuming it in the process; otherwise
 * it may block. A call to unpark makes the permit available, if it was not already available. (Unlike with Semaphores
 * though, permits do not accumulate. There is at most one.)
 *
 * Methods park() and unpark() provide efficient means of blocking and unblocking threads that do not encounter the
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
    
    /**
     * Returns the blocker object supplied to the most recent invocation of a park method that has not yet unblocked, 
     * or null if not blocked. The value returned is just a momentary snapshot -- the thread may have since unblocked 
     * or blocked on a different blocker object.
     */
    public static Object getBlocker(Thread t) {
        if (t == null) throw new NullPointerException();
        return UNSAFE.getObjectVolatile(t, parkBlockerOffset);
    }
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
关于LockSupport的重点是几种形式的park()方法-void park(), void parkNanos(long), void parkUntil(long)。在JDK1.6之后
对应每一个方法都添加了一个带有blocker参数的重载方法，官方推荐使用这些新的方法，因为在线程阻塞的时候这个blocker会被记录，
可以使用getBlocker()方法获取这个blocker对象，进一步通过监视工具(比如线程dump)和诊断工具确定线程受阻塞的原因。  
通过阅读源码，我们可以清晰地看到，上述这些方法完全是由Unsafe类实现的，关于Unsafe的源码我们另作分析，在此我们需要知道的是，
Java与硬件CPU/内存等的交互是由native代码完成的，通过Java代码我们无法直接操作，而Unsafe类就是为我们提供了这些操作的入口，
通过Unsafe类我们可以直接在Java代码中分配内存/定位对象内存位置并进行无视访问权限的读写/CAS，还有在LockSupport是频繁使用的
线程的挂起和恢复，当然官方并不建议我们在实际开发中使用这个类，因为它会带来一系列的安全和内存管理问题，并且Unsafe系统成本很高。  
另外，getBlocker()方法之所以也由Unsafe实现是因为blocker只有在线程阻塞时才会被记录，此时只能通过直接操作内存来读取blocker
对象，我们可以看到，在静态代码块中维护了parkBlockerOffset变量来记录blocker在内存中相对于起点的偏移量，这个起点在不同的
JVM实现中并不相同，但是通过这个偏移量我们可以找到blocker对象。  
我们来写一段代码，示例通过jstack工具分析blocker对象：
```java
public class ThreadDumpExample {
    public static void main(String[] args) {
        // LockSupport.park(); // --1
        // LockSupport.park(new Object()); // --2
    }
}
```
分别运行1和2处代码，线程阻塞后通过jstack -l <pid> > <dump-file-name>命令得到的dump文件如下：
```
"main" #1 prio=5 os_prio=0 tid=0x000000000236f000 nid=0x1684 waiting on condition [0x00000000027ff000]
   java.lang.Thread.State: WAITING (parking)
	at sun.misc.Unsafe.park(Native Method)
	at java.util.concurrent.locks.LockSupport.park(LockSupport.java:304)
	at ThreadDumpExample.main(ThreadDumpExample.java:9)
========================================================================================================
"main" #1 prio=5 os_prio=0 tid=0x000000000259f000 nid=0x1f7c waiting on condition [0x000000000279f000]
   java.lang.Thread.State: WAITING (parking)
	at sun.misc.Unsafe.park(Native Method)
	- parking to wait for  <0x00000000d5fdec68> (a java.lang.Object) -- 当前线程被Object类型对象0x00000000d5fdec68阻塞
	at java.util.concurrent.locks.LockSupport.park(LockSupport.java:175)
	at ThreadDumpExample.main(ThreadDumpExample.java:9)
```