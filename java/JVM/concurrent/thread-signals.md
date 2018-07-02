JDK1.5之后引入的java.util.concurrent包为我们提供了一些多线程之间调度和通信的信号量，用来增强最初Thread类的原始功能。
常用的有Semaphore/CountdownLatch/CyclicBarrier/Phaser等。下面我们来阅读一下JDK中这些工具的源码并整理一下官方文档，
做一些使用场景的示例。
### Semaphore - 信号量
```java
package java.util.concurrent;
import java.util.Collection;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

/**
 * A counting semaphore. Conceptually, a semaphore maintains a set of permits. Each acquire() blocks if necessary 
 * until a permit is available, and then takes it. Each release() adds a permit, potentially releasing a blocking 
 * acquirer. However, no actual permit objects are used; the Semaphore just keeps a count of the number available 
 * and acts accordingly. Semaphores are often used to restrict the number of threads than can access some 
 * (physical or logical) resource. 
 * 
 * 从概念上讲，Semaphore维护了一个许可的集合. acquire方法会阻塞直到有一个许可可用. release方法会在集合里添加一个许可,
 * 潜在地会释放一个请求者.Semaphore只是维护了这个可用许可的数量并做相应的处理, 并没有使用其他对象.通常Semaphore用来限制
 * 访问物理或逻辑资源的线程数量,例如下面的例子使用Semaphore限制对池化资源的访问：
 * 
 * class Pool {
 *   private static final int MAX_AVAILABLE = 100;
 *   private final Semaphore available = new Semaphore(MAX_AVAILABLE, true);
 *
 *   public Object getItem() throws InterruptedException {
 *     available.acquire();
 *     return getNextAvailableItem();
 *   }
 *
 *   public void putItem(Object x) {
 *     if (markAsUnused(x))
 *       available.release();
 *   }
 *
 *   // Not a particularly efficient data structure; just for demo
 *   protected Object[] items = ... whatever kinds of items being managed
 *   protected boolean[] used = new boolean[MAX_AVAILABLE];
 *
 *   protected synchronized Object getNextAvailableItem() {
 *     for (int i = 0; i < MAX_AVAILABLE; ++i) {
 *       if (!used[i]) {
 *          used[i] = true;
 *          return items[i];
 *       }
 *     }
 *     return null; // not reached
 *   }
 *
 *   protected synchronized boolean markAsUnused(Object item) {
 *     for (int i = 0; i < MAX_AVAILABLE; ++i) {
 *       if (item == items[i]) {
 *          if (used[i]) {
 *            used[i] = false;
 *            return true;
 *          } else
 *            return false;
 *       }
 *     }
 *     return false;
 *   }
 * }
 *
 * Before obtaining an item each thread must acquire a permit from the semaphore, guaranteeing that an item 
 * is available for use. When the thread has finished with the item it is returned back to the pool and 
 * a permit is returned to the semaphore, allowing another thread to acquire that item. 
 * Note that no synchronization lock is held when acquire is called as that would prevent an item from 
 * being returned to the pool. The semaphore encapsulates the synchronization needed to restrict access to the pool, 
 * separately from any synchronization needed to maintain the consistency of the pool itself.
 * 在获取池中的一个元素的时候,每个线程都必须先从Semaphore中获取一个许可,以保证这个元素可用.当这个线程使用完这个元素并将其
 * 退还到池中的时候,这个许可同时也被退还给Semaphore,以使其他线程可以获取到许可.需要注意的是,acquire方法的调用是
 * 不会获取同步锁的,因为这样会导致元素无法返还给池.信号量只封装了限制对池的访问所需的同步,与维护池本身一致性所需的同步是分开的.
 * 
 * A semaphore initialized to one, and which is used such that it only has at most one permit available, 
 * can serve as a mutual exclusion lock.  This is more commonly known as a binary semaphore, because it only 
 * has two states: one permit available, or zero permits available.  When used in this way, the binary semaphore 
 * has the property (unlike many java.util.concurrent.locks.Lock implementations), that the lock can be released by a
 * thread other than the owner (as semaphores have no notion of ownership).  This can be useful in some 
 * specialized contexts, such as deadlock recovery.
 * 只持有1个许可的Semaphore可以作为互斥锁使用.因为它只有两种状态,我们也可以称之为二元信号量.由于Semaphore本身没有
 * 持有者的概念,所以当在这种场景下使用时,锁可以被其他线程释放.在某些情况,比如死锁恢复时,这就会很有用.
 * .
 * The constructor for this class optionally accepts a fairness parameter. When set false, this class makes no
 * guarantees about the order in which threads acquire permits. In particular, barging is permitted, that is, a thread
 * invoking acquire can be allocated a permit ahead of a thread that has been waiting - logically the 
 * new thread places itself at the head of the queue of waiting threads. When fairness is set true, the
 * semaphore guarantees that threads invoking any of the acquire methods are selected to obtain permits in the order 
 * in which their invocation of those methods was processed (first-in-first-out; FIFO). Note that FIFO ordering 
 * necessarily applies to specific internal points of execution within these methods.  So, it is possible for 
 * one thread to invoke acquire before another, but reach the ordering point after the other, and similarly 
 * upon return from the method. Also note that the untimed tryAcquire methods do not honor the fairness setting, 
 * but will take any permits that are available.
 * Generally, semaphores used to control resource access should be initialized as fair, to ensure that no thread 
 * is starved out from accessing a resource. When using semaphores for other kinds of synchronization control,
 * the throughput advantages of non-fair ordering often outweigh fairness considerations.
 * Semaphore的构造函数可以接受一个boolean类型的参数来指定不同线程获取信号量的公平性.传入true时,采用公平策略,也就是说
 * 多个调用acquire()的线程会按照FIFO的顺序获取信号量,而传入false时,采用非公平策略,一个线程先于其他线程调用acquire()并不能
 * 保证他能先于其他线程获取信号量.一般来说,当semaphore用于控制资源访问时,推荐使用公平策略,防止线程饥饿,在其他场景下,
 * 吞吐量优先的非公平策略性能更高.
 *
 * This class also provides convenience methods to acquire(int) and release(int) multiple permits at a time.  
 * Beware of the increased risk of indefinite postponement when these methods are used without fairness set true.
 *
 * Memory consistency effects: Actions in a thread prior to calling a "release" method such as release() 
 * happen-before actions following a successful "acquire" method such as acquire() in another thread.
 *
 * @since 1.5
  
 */
public class Semaphore implements java.io.Serializable {
    // ...
    private final Sync sync;

    /**
     * Synchronization implementation for semaphore.  Uses AQS state to represent permits. 
     * Subclassed into fair and nonfair versions.
     */
    abstract static class Sync extends AbstractQueuedSynchronizer {
        private static final long serialVersionUID = 1192457210091910933L;

        Sync(int permits) {
            setState(permits);
        }

        final int getPermits() {
            return getState();
        }

        final int nonfairTryAcquireShared(int acquires) {
            for (;;) {
                int available = getState();
                int remaining = available - acquires;
                if (remaining < 0 ||
                    compareAndSetState(available, remaining))
                    return remaining;
            }
        }

        protected final boolean tryReleaseShared(int releases) {
            for (;;) {
                int current = getState();
                int next = current + releases;
                if (next < current) // overflow
                    throw new Error("Maximum permit count exceeded");
                if (compareAndSetState(current, next))
                    return true;
            }
        }

        final void reducePermits(int reductions) {
            for (;;) {
                int current = getState();
                int next = current - reductions;
                if (next > current) // underflow
                    throw new Error("Permit count underflow");
                if (compareAndSetState(current, next))
                    return;
            }
        }

        final int drainPermits() {
            for (;;) {
                int current = getState();
                if (current == 0 || compareAndSetState(current, 0))
                    return current;
            }
        }
    }

    /**
     * NonFair version
     */
    static final class NonfairSync extends Sync {
        private static final long serialVersionUID = -2694183684443567898L;

        NonfairSync(int permits) {
            super(permits);
        }

        protected int tryAcquireShared(int acquires) {
            return nonfairTryAcquireShared(acquires);
        }
    }

    /**
     * Fair version
     */
    static final class FairSync extends Sync {
        private static final long serialVersionUID = 2014338818796000944L;

        FairSync(int permits) {
            super(permits);
        }

        protected int tryAcquireShared(int acquires) {
            for (;;) {
                if (hasQueuedPredecessors())
                    return -1;
                int available = getState();
                int remaining = available - acquires;
                if (remaining < 0 ||
                    compareAndSetState(available, remaining))
                    return remaining;
            }
        }
    }

    /**
     * Creates a Semaphore with the given number of permits and nonfair fairness setting. 
     * This value may be negative, in which case releases must occur before any acquires will be granted.
     */
    public Semaphore(int permits) {
        sync = new NonfairSync(permits);
    }

    /**
     * Creates a Semaphore with the given number of permits and the given fairness setting. 
     */
    public Semaphore(int permits, boolean fair) {
        sync = fair ? new FairSync(permits) : new NonfairSync(permits);
    }

    /**
     * Acquires a permit from this semaphore, blocking until one is available, or the thread is interrupted:
     * if one is available and returns immediately, reducing the number of available permits by one; if no permit 
     * is available then the current thread becomes disabled for thread scheduling purposes and lies dormant until
     * one of two things happens:
     * Some other thread invokes the release method for this semaphore and the current thread is 
     * next to be assigned a permit; or Some other thread interrupts the current thread.
     * If the current thread: has its interrupted status set on entry to this method; or is interrupted while waiting
     * for a permit, then InterruptedException is thrown and the current thread's interrupted status is cleared.
     * throws InterruptedException if the current thread is interrupted
     */
    public void acquire() throws InterruptedException {
        sync.acquireSharedInterruptibly(1);
    }

    /**
     * Acquires a permit from this semaphore, blocking until one is available, if one is available and 
     * returns immediately, reducing the number of available permits by one.
     * If no permit is available then the current thread becomes disabled for thread scheduling purposes 
     * and lies dormant until some other thread invokes the release} method for this semaphore and 
     * the current thread is next to be assigned a permit.
     * If the current thread is interrupted while waiting for a permit then it will continue to wait, but the
     * time at which the thread is assigned a permit may change compared to the time it would have received 
     * the permit had no interruption occurred.  When the thread does return from this method its interrupt
     * status will be set.
     */
    public void acquireUninterruptibly() {
        sync.acquireShared(1);
    }

    /**
     * Acquires a permit from this semaphore, only if one is available at the time of invocation, if one is 
     * available and returns immediately with the value true, reducing the number of available permits by one.
     * If no permit is available then this method will return immediately with the value false.
     * Even when this semaphore has been set to use a fair ordering policy, a call to tryAcquire() will immediately 
     * acquire a permit if one is available, whether or not  other threads are currently waiting.
     * This barging behavior can be useful in certain circumstances, even though it breaks fairness. 
     * If you want to honor the fairness setting, then use tryAcquire(long, TimeUnit)
     */
    public boolean tryAcquire() {
        return sync.nonfairTryAcquireShared(1) >= 0;
    }

    /**
     * Acquires a permit from this semaphore, if one becomes available within the given waiting time and the 
     * current thread has not been interrupted, if one is available and returns immediately, with the value true,
     * reducing the number of available permits by one, if no permit is available then the current thread becomes
     * disabled for thread scheduling purposes and lies dormant until one of three things happens:
     * 1.Some other thread invokes the release() method for this semaphore and the current thread 
     * is next to be assigned a permit;
     * 2.Some other thread interrupts the current thread; 
     * 3.The specified waiting time elapses.
     * If a permit is acquired then the value true is returned.
     * If the current thread has its interrupted status set on entry to this method; or is interrupted while waiting
     * to acquire a permit, then InterruptedException is thrown and the current thread's interrupted status is cleared.
     * If the specified waiting time elapses then the value false is returned.  
     * If the time is less than or equal to zero, the method will not wait at all.
     */
    public boolean tryAcquire(long timeout, TimeUnit unit)
        throws InterruptedException {
        return sync.tryAcquireSharedNanos(1, unit.toNanos(timeout));
    }

    /**
     * Releases a permit, returning it to the semaphore, increasing the number of available permits by one. 
     * If any threads are trying to acquire a permit, then one is selected and given the permit that was just released.
     * That thread is (re)enabled for thread scheduling purposes.
     * There is no requirement that a thread that releases a permit must have acquired that permit by calling acquire().
     * Correct usage of a semaphore is established by programming convention in the application.
     */
    public void release() {
        sync.releaseShared(1);
    }

    public void acquire(int permits) throws InterruptedException {
        if (permits < 0) throw new IllegalArgumentException();
        sync.acquireSharedInterruptibly(permits);
    }

    public void acquireUninterruptibly(int permits) {
        if (permits < 0) throw new IllegalArgumentException();
        sync.acquireShared(permits);
    }

    public boolean tryAcquire(int permits) {
        if (permits < 0) throw new IllegalArgumentException();
        return sync.nonfairTryAcquireShared(permits) >= 0;
    }

    public boolean tryAcquire(int permits, long timeout, TimeUnit unit)
        throws InterruptedException {
        if (permits < 0) throw new IllegalArgumentException();
        return sync.tryAcquireSharedNanos(permits, unit.toNanos(timeout));
    }

    public void release(int permits) {
        if (permits < 0) throw new IllegalArgumentException();
        sync.releaseShared(permits);
    }

    /**
     * Returns the current number of permits available in this semaphore.
     * This method is typically used for debugging and testing purposes.
     */
    public int availablePermits() {
        return sync.getPermits();
    }

    /**
     * Acquires and returns all permits that are immediately available.
     */
    public int drainPermits() {
        return sync.drainPermits();
    }

    /**
     * Shrinks the number of available permits by the indicated reduction. This method can be useful 
     * in subclasses that use semaphores to track resources that become unavailable. This method differs from
     * acquire in that it does not block waiting for permits to become available.
     */
    protected void reducePermits(int reduction) {
        if (reduction < 0) throw new IllegalArgumentException();
        sync.reducePermits(reduction);
    }

    /**
     * Returns true if this semaphore has fairness set true.
     */
    public boolean isFair() {
        return sync instanceof FairSync;
    }

    /**
     * Queries whether any threads are waiting to acquire. Note that because cancellations may occur at any time, 
     * a true return does not guarantee that any other thread will ever acquire.  This method is designed primarily 
     * for use in monitoring of the system state.
     */
    public final boolean hasQueuedThreads() {
        return sync.hasQueuedThreads();
    }

    /**
     * Returns an estimate of the number of threads waiting to acquire. The value is only an estimate because 
     * the number of threads may change dynamically while this method traverses internal data structures.  
     * This method is designed for use in monitoring of the system state, not for synchronization control.
     */
    public final int getQueueLength() {
        return sync.getQueueLength();
    }

    /**
     * Returns a collection containing threads that may be waiting to acquire. Because the actual set of threads
     * may change dynamically while constructing this result, the returned collection is only a best-effort estimate.  
     * The elements of the returned collection are in no particular order.  This method is designed to facilitate
     * construction of subclasses that provide more extensive monitoring facilities.
     */
    protected Collection<Thread> getQueuedThreads() {
        return sync.getQueuedThreads();
    }
}
```
下面我们模拟这样的场景，公司只有三台打印机，但是现在有十个人需要打印文件，那么我们就可以使用Semaphore来做打印任务的调度:
```java
public class PrintQueue {

    // 信号量
    private Semaphore semaphore;

    // 打印机状态
    private boolean[] printerStats;

    /*
     * semaphore提供的acquire()/release()方法当然是线程安全的,
     * 但是这些线程之间共享的数据不归semaphore管辖,需要单独处理加锁
     */
    private Lock lock;

    public PrintQueue() {
        this.semaphore = new Semaphore(3);
        this.lock = new ReentrantLock();
        this.printerStats = new boolean[3];
        for (int i = 0; i < printerStats.length; i++) {
            printerStats[i] = true;
        }
    }

    public void print(Object doc) {
        try {
            semaphore.acquire();// will block
            int printerNo = getPrinter();
            long elapse = (long) (Math.random() * 10);

            // print simulation
            TimeUnit.SECONDS.sleep(elapse);
            System.out.printf("%s: PrintQueue: Printing a Job in Printer %d during %d seconds\n",
                    Thread.currentThread().getName(), printerNo, elapse);

            printerStats[printerNo] = true;
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            semaphore.release();
        }
    }

    private int getPrinter() {
        int ret = -1;
        lock.lock();
        for (int i = 0; i < printerStats.length; i++) {
            if (printerStats[i]) {
                printerStats[i] = false;
                ret = i;
                break;
            }
        }
        lock.unlock();
        return ret;
    }
}

class PrintJob implements Runnable {

    private PrintQueue queue;

    public PrintJob(PrintQueue queue) {
        this.queue = queue;
    }

    @Override
    public void run() {
        System.out.printf("%s: Going to print a job\n", Thread.currentThread().getName());
        queue.print(new Object());
        System.out.printf("%s: The document has been printed\n", Thread.currentThread().getName());
    }
}

class Test {

    public static void main(String[] args) {

        PrintQueue queue = new PrintQueue();
        for (int i = 0; i < 10; i++)
            new Thread(new PrintJob(queue)).start();

    }
}
```

### CountDownLatch
```java
package java.util.concurrent;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

/**
 * A synchronization aid that allows one or more threads to wait until a set of operations being performed 
 * in other threads completes.
 * A CountDownLatch is initialized with a given count. The await() methods block until the current count reaches
 * zero due to invocations of the countDown() method, after which all waiting threads are released and any subsequent 
 * invocations of await() return immediately. This is a one-shot phenomenon -- the count cannot be reset. 
 * If you need a version that resets the count, consider using a CyclicBarrier.
 * A CountDownLatch is a versatile synchronization tool and can be used for a number of purposes. A CountDownLatch
 * initialized with a count of one serves as a simple on/off latch, or gate: all threads invoking await() wait 
 * at the gate until it is opened by a thread invoking countDown(). A CountDownLatch initialized to N can be used 
 * to make one thread wait until N threads have completed some action, or some action has been completed N times.
 * A useful property of a CountDownLatch is that it doesn't require that threads calling countDown wait for
 * the count to reach zero before proceeding, it simply prevents any thread from proceeding past an await() until all
 * threads could pass.
 * Sample usage: Here is a pair of classes in which a group of worker threads use two countdown latches:
 * The first is a start signal that prevents any worker from proceeding until the driver is ready for them to proceed;
 * The second is a completion signal that allows the driver to wait until all workers have completed.
 * 
 * class Driver { // ...
 *   void main() throws InterruptedException {
 *     CountDownLatch startSignal = new CountDownLatch(1);
 *     CountDownLatch doneSignal = new CountDownLatch(N);
 *
 *     for (int i = 0; i < N; ++i) // create and start threads
 *       new Thread(new Worker(startSignal, doneSignal)).start();
 *
 *     doSomethingElse();            // don't let run yet
 *     startSignal.countDown();      // let all threads proceed
 *     doSomethingElse();
 *     doneSignal.await();           // wait for all to finish
 *   }
 * }
 *
 * class Worker implements Runnable {
 *   private final CountDownLatch startSignal;
 *   private final CountDownLatch doneSignal;
 *   Worker(CountDownLatch startSignal, CountDownLatch doneSignal) {
 *     this.startSignal = startSignal;
 *     this.doneSignal = doneSignal;
 *   }
 *   public void run() {
 *     try {
 *       startSignal.await();
 *       doWork();
 *       doneSignal.countDown();
 *     } catch (InterruptedException ex) {} // return;
 *   }
 *
 *   void doWork() { ... }
 * } 
 * Another typical usage would be to divide a problem into N parts, describe each part with a Runnable 
 * that executes that portion and counts down on the latch, and queue all the Runnables to an Executor. 
 * When all sub-parts are complete, the coordinating thread will be able to pass through await. 
 * (When threads must repeatedly count down in this way, instead use a CyclicBarrier.)
 * 
 * class Driver2 { // ...
 *   void main() throws InterruptedException {
 *     CountDownLatch doneSignal = new CountDownLatch(N);
 *     Executor e = ...
 *
 *     for (int i = 0; i < N; ++i) // create and start threads
 *       e.execute(new WorkerRunnable(doneSignal, i));
 *
 *     doneSignal.await();           // wait for all to finish
 *   }
 * }
 *
 * class WorkerRunnable implements Runnable {
 *   private final CountDownLatch doneSignal;
 *   private final int i;
 *   WorkerRunnable(CountDownLatch doneSignal, int i) {
 *     this.doneSignal = doneSignal;
 *     this.i = i;
 *   }
 *   public void run() {
 *     try {
 *       doWork(i);
 *       doneSignal.countDown();
 *     } catch (InterruptedException ex) {} // return;
 *   }
 *
 *   void doWork() { ... }
 * } 
 * Memory consistency effects: Until the count reaches zero, actions in a thread prior to calling countDown()
 * happen-before actions following a successful return from a corresponding await() in another thread.
 *
 * @since 1.5
 */
public class CountDownLatch {
    /**
     * Synchronization control For CountDownLatch. Uses AQS state to represent count.
     */
    private static final class Sync extends AbstractQueuedSynchronizer {
        private static final long serialVersionUID = 4982264981922014374L;

        Sync(int count) {
            setState(count);
        }

        int getCount() {
            return getState();
        }

        protected int tryAcquireShared(int acquires) {
            return (getState() == 0) ? 1 : -1;
        }

        protected boolean tryReleaseShared(int releases) {
            // Decrement count; signal when transition to zero
            for (;;) {
                int c = getState();
                if (c == 0)
                    return false;
                int nextc = c-1;
                if (compareAndSetState(c, nextc))
                    return nextc == 0;
            }
        }
    }

    private final Sync sync;

    /**
     * Constructs a CountDownLatch initialized with the given count.
     */
    public CountDownLatch(int count) {
        if (count < 0) throw new IllegalArgumentException("count < 0");
        this.sync = new Sync(count);
    }

    /**
     * Causes the current thread to wait until the latch has counted down to zero, unless the thread is interrupted.
     * If the current count is zero then this method returns immediately. If the current count is greater than zero 
     * then the current thread becomes disabled for thread scheduling purposes and lies dormant until 
     * one of two things happen:
     * 1.The count reaches zero due to invocations of the countDown() method;
     * 2.Some other thread interrupts the current thread.
     * If the current thread: has its interrupted status set on entry to this method; or is interrupted while waiting,
     * then InterruptedException is thrown and the current thread's interrupted status is cleared.
     */
    public void await() throws InterruptedException {
        sync.acquireSharedInterruptibly(1);
    }

    /**
     * Causes the current thread to wait until the latch has counted down to zero, unless the thread is interrupted,
     * or the specified waiting time elapses.
     * If the current count is zero then this method returns immediately with the value true.
     * If the current count is greater than zero then the current thread becomes disabled for thread scheduling 
     * purposes and lies dormant until one of three things happen:
     * 1.The count reaches zero due to invocations of the countDown() method;
     * 2.Some other thread interrupts the current thread;
     * 3.The specified waiting time elapses.
     * If the count reaches zero then the method returns with the value true.
     * If the current thread: has its interrupted status set on entry to this method; or is interrupted while waiting,
     * then InterruptedException is thrown and the current thread's interrupted status is cleared.
     * If the specified waiting time elapses then the value false is returned.  
     * If the time is less than or equal to zero, the method will not wait at all.
     */
    public boolean await(long timeout, TimeUnit unit)
        throws InterruptedException {
        return sync.tryAcquireSharedNanos(1, unit.toNanos(timeout));
    }

    /**
     * Decrements the count of the latch, releasing all waiting threads if the count reaches zero.
     * If the current count is greater than zero then it is decremented.
     * If the new count is zero then all waiting threads are re-enabled for thread scheduling purposes.
     * If the current count equals zero then nothing happens.
     */
    public void countDown() {
        sync.releaseShared(1);
    }

    /**
     * Returns the current count. This method is typically used for debugging and testing purposes.
     */
    public long getCount() {
        return sync.getCount();
    }

    // ...
}

```
我们使用CountdownLatch来实现这样一个场景，一个视频会议，需要10个与会人到齐之后开始
```java
public class VideoConference implements Runnable {

    private CountDownLatch latch;

    public VideoConference(int num) {
        this.latch = new CountDownLatch(num);
    }

    // 我们使用同步方法并非是考虑线程安全，知识为了保证同一个线程的两行打印语句输出在一起
    public synchronized void arrive(Participant par) {
        System.out.printf("%s has arrived.\n", par.getName());
        latch.countDown();//调用countDown()方法，使内部计数器减1
        System.out.printf("VideoConference: Waiting for %d participants.\n", latch.getCount());
    }

    @Override
    public void run() {
        try {
            latch.await();// 阻塞直到latch内部计数为0
            System.out.printf("VideoConference: All the participants have come\n");
            System.out.printf("VideoConference: Let's start...\n");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

class Participant implements Runnable {

    private String name;

    private VideoConference conference;

    public Participant(VideoConference conference, String name) {
        this.conference = conference;
        this.name = name;
    }

    @Override
    public void run() {
        Long duration = (long) (Math.random() * 10);
        try {
            TimeUnit.SECONDS.sleep(duration);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        conference.arrive(this);
    }
    
}

class Test {

    public static void main(String[] args) {

        VideoConference conference = new VideoConference(10);
        new Thread(conference).start();

        for (int i = 0; i < 10; i++)
            new Thread(new Participant(conference, "man - " + i)).start();

    }

}
```
### CyclicBarrier
```java
package java.util.concurrent;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A synchronization aid that allows a set of threads to all wait for each other to reach a common barrier point.  
 * CyclicBarriers are useful in programs involving a fixed sized party of threads that must occasionally 
 * wait for each other. The barrier is called cyclic because it can be re-used after the waiting threads
 * are released.
 * A CyclicBarrier supports an optional Runnable command that is run once per barrier point, after the last thread 
 * in the party arrives, but before any threads are released.
 * This barrier action is useful for updating shared-state before any of the parties continue.
 * Sample usage:  Here is an example of using a barrier in a parallel decomposition design:
 * 
 * class Solver {
 *   final int N;
 *   final float[][] data;
 *   final CyclicBarrier barrier;
 *
 *   class Worker implements Runnable {
 *     int myRow;
 *     Worker(int row) { myRow = row; }
 *     public void run() {
 *       while (!done()) {
 *         processRow(myRow);
 *
 *         try {
 *           barrier.await();
 *         } catch (InterruptedException ex) {
 *           return;
 *         } catch (BrokenBarrierException ex) {
 *           return;
 *         }
 *       }
 *     }
 *   }
 *
 *   public Solver(float[][] matrix) {
 *     data = matrix;
 *     N = matrix.length;
 *     Runnable barrierAction =
 *       new Runnable() { public void run() { mergeRows(...); }};
 *     barrier = new CyclicBarrier(N, barrierAction);
 *
 *     List<Thread> threads = new ArrayList<Thread>(N);
 *     for (int i = 0; i < N; i++) {
 *       Thread thread = new Thread(new Worker(i));
 *       threads.add(thread);
 *       thread.start();
 *     }
 *
 *     // wait until done
 *     for (Thread thread : threads)
 *       thread.join();
 *   }
 * } 
 *
 * Here, each worker thread processes a row of the matrix then waits at the barrier until all rows have been processed. 
 * When all rows are processed the supplied Runnable barrier action is executed and merges the rows. If the merger
 * determines that a solution has been found then done() will return true and each worker will terminate.
 * If the barrier action does not rely on the parties being suspended when it is executed, then any of the threads 
 * in the party could execute that action when it is released. To facilitate this, each invocation of await()
 * returns the arrival index of that thread at the barrier.
 * You can then choose which thread should execute the barrier action, for example:
 * if (barrier.await() == 0) {
 *   // log the completion of this iteration
 * }
 * The CyclicBarrier uses an all-or-none breakage model for failed synchronization attempts: If a thread leaves 
 * a barrier point prematurely because of interruption, failure, or timeout, all other threads waiting at that 
 * barrier point will also leave abnormally via BrokenBarrierException (or  InterruptedException if they too were 
 * interrupted at about the same time).
 * Memory consistency effects: Actions in a thread prior to calling await() happen-before actions that are 
 * part of the barrier action, which in turn happen-before actions following a successful return from the
 * corresponding await() in other threads.
 *
 * @since 1.5
 */
public class CyclicBarrier {
    /**
     * Each use of the barrier is represented as a generation instance. The generation changes whenever 
     * the barrier is tripped, or is reset. There can be many generations associated with threads using the barrier 
     * - due to the non-deterministic way the lock may be allocated to waiting threads - but only one of these
     * can be active at a time (the one to which count applies) and all the rest are either broken or tripped.
     * There need not be an active generation if there has been a break but no subsequent reset.
     */
    private static class Generation {
        boolean broken = false;
    }

    /** The lock for guarding barrier entry */
    private final ReentrantLock lock = new ReentrantLock();
    /** Condition to wait on until tripped */
    private final Condition trip = lock.newCondition();
    /** The number of parties */
    private final int parties;
    /* The command to run when tripped */
    private final Runnable barrierCommand;
    /** The current generation */
    private Generation generation = new Generation();

    /**
     * Number of parties still waiting. Counts down from parties to 0 on each generation.
     * It is reset to parties on each new generation or when broken.
     */
    private int count;

    /**
     * Updates state on barrier trip and wakes up everyone. Called only while holding lock.
     */
    private void nextGeneration() {
        // signal completion of last generation
        trip.signalAll();
        // set up next generation
        count = parties;
        generation = new Generation();
    }

    /**
     * Sets current barrier generation as broken and wakes up everyone. Called only while holding lock.
     */
    private void breakBarrier() {
        generation.broken = true;
        count = parties;
        trip.signalAll();
    }

    /**
     * Main barrier code, covering the various policies.
     */
    private int dowait(boolean timed, long nanos)
        throws InterruptedException, BrokenBarrierException,
               TimeoutException {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            final Generation g = generation;

            if (g.broken)
                throw new BrokenBarrierException();

            if (Thread.interrupted()) {
                breakBarrier();
                throw new InterruptedException();
            }

            int index = --count;
            if (index == 0) {  // tripped
                boolean ranAction = false;
                try {
                    final Runnable command = barrierCommand;
                    if (command != null)
                        command.run();
                    ranAction = true;
                    nextGeneration();
                    return 0;
                } finally {
                    if (!ranAction)
                        breakBarrier();
                }
            }

            // loop until tripped, broken, interrupted, or timed out
            for (;;) {
                try {
                    if (!timed)
                        trip.await();
                    else if (nanos > 0L)
                        nanos = trip.awaitNanos(nanos);
                } catch (InterruptedException ie) {
                    if (g == generation && ! g.broken) {
                        breakBarrier();
                        throw ie;
                    } else {
                        // We're about to finish waiting even if we had not been interrupted, 
                        // so this interrupt is deemed to "belong" to subsequent execution.
                        Thread.currentThread().interrupt();
                    }
                }

                if (g.broken)
                    throw new BrokenBarrierException();

                if (g != generation)
                    return index;

                if (timed && nanos <= 0L) {
                    breakBarrier();
                    throw new TimeoutException();
                }
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Creates a new CyclicBarrier that will trip when the given number of parties (threads) are waiting upon it, 
     * and which will execute the given barrier action when the barrier is tripped, performed by the last thread 
     * entering the barrier.
     */
    public CyclicBarrier(int parties, Runnable barrierAction) {
        if (parties <= 0) throw new IllegalArgumentException();
        this.parties = parties;
        this.count = parties;
        this.barrierCommand = barrierAction;
    }

    /**
     * Creates a new CyclicBarrier that will trip when the given number of parties (threads) are waiting upon it, 
     * and does not perform a predefined action when the barrier is tripped.
     */
    public CyclicBarrier(int parties) {
        this(parties, null);
    }

    /**
     * Returns the number of parties required to trip this barrier.
     */
    public int getParties() {
        return parties;
    }

    /**
     * Waits until all getParties() have invoked await on this barrier. If the current thread is not 
     * the last to arrive then it is disabled for thread scheduling purposes and lies dormant until
     * one of the following things happens:
     * 1.The last thread arrives;
     * 2.Some other thread interrupts the current thread;
     * 3.Some other thread interrupts one of the other waiting threads; or Some other thread times out while 
     * waiting for barrier; or Some other thread invokes reset() on this barrier.
     * If the current thread: has its interrupted status set on entry to this method; or is interrupted while waiting
     * then InterruptedException is thrown and the current thread's interrupted status is cleared.
     * If the barrier is reset while any thread is waiting, or if the barrier is broken when await is invoked,
     * or while any thread is waiting, then BrokenBarrierException is thrown.
     * If any thread is interrupted while waiting, then all other waiting threads will throw BrokenBarrierException 
     * and the barrier is placed in the broken state.
     * If the current thread is the last thread to arrive, and a non-null barrier action was supplied in the 
     * constructor, then the current thread runs the action before allowing the other threads to continue.
     * If an exception occurs during the barrier action then that exception will be propagated in the current thread 
     * and the barrier is placed in the broken state.
     */
    public int await() throws InterruptedException, BrokenBarrierException {
        try {
            return dowait(false, 0L);
        } catch (TimeoutException toe) {
            throw new Error(toe); // cannot happen
        }
    }

    /**
     * Waits until all parties have invoked await on this barrier, or the specified waiting time elapses.
     * If the current thread is not the last to arrive then it is disabled for thread scheduling purposes and 
     * lies dormant until one of the following things happens:
     * 1.The last thread arrives;
     * 2.The specified timeout elapses;
     * 3.Some other thread interrupts the current thread; or Some other thread interrupts one of the other 
     * waiting threads; or Some other thread times out while waiting for barrier; or Some other thread invokes reset()
     * on this barrier.
     * If the current thread: has its interrupted status set on entry to this method; or is interrupted while waiting
     * then InterruptedException is thrown and the current thread's interrupted status is cleared.
     * If the specified waiting time elapses then TimeoutException is thrown. If the time is less than or 
     * equal to zero, the method will not wait at all.
     * If the barrier is reset while any thread is waiting, or if the barrier is broken when await is invoked, 
     * or while any thread is waiting, then BrokenBarrierException is thrown.
     * If any thread is interrupted while waiting, then all other waiting threads will throw BrokenBarrierException 
     * and the barrier is placed in the broken state.
     * If the current thread is the last thread to arrive, and a non-null barrier action was supplied in the 
     * constructor, then the current thread runs the action before allowing the other threads to continue.
     * If an exception occurs during the barrier action then that exception will be propagated in the current 
     * thread and the barrier is placed in the broken state.
     */
    public int await(long timeout, TimeUnit unit)
        throws InterruptedException,
               BrokenBarrierException,
               TimeoutException {
        return dowait(true, unit.toNanos(timeout));
    }

    /**
     * Queries if this barrier is in a broken state, if one or more parties broke out of this barrier due to
     * interruption or timeout since construction or the last reset, or a barrier action failed due to an exception,
     * return true; false otherwise.
     */
    public boolean isBroken() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return generation.broken;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Resets the barrier to its initial state.  If any parties are currently waiting at the barrier, 
     * they will return with a BrokenBarrierException. Note that resets after a breakage has occurred for 
     * other reasons can be complicated to carry out; threads need to re-synchronize in some other way,
     * and choose one to perform the reset. It may be preferable to instead create a new barrier for subsequent use.
     */
    public void reset() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            breakBarrier();   // break the current generation
            nextGeneration(); // start a new generation
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns the number of parties currently waiting at the barrier. 
     * This method is primarily useful for debugging and assertions.
     */
    public int getNumberWaiting() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return parties - count;
        } finally {
            lock.unlock();
        }
    }
}
```
### Phaser
```java
package java.util.concurrent;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

/**
 * A reusable synchronization barrier, similar in functionality to CyclicBarrier and CountDownLatch 
 * but supporting more flexible usage.
 * Registration. Unlike the case for other barriers, the number of parties registered to synchronize on a phaser
 * may vary over time. Tasks may be registered at any time (using methods register(), bulkRegister(), or forms of
 * constructors establishing initial numbers of parties), and optionally deregistered upon any arrival 
 * (using arriveAndDeregister).  As is the case with most basic synchronization constructs, 
 * registration and deregistration affect only internal counts; they do not establish any further internal
 * bookkeeping, so tasks cannot query whether they are registered. (However, you can introduce such bookkeeping
 * by subclassing this class.)
 * Synchronization. Like a CyclicBarrier, a Phaser may be repeatedly awaited. Method arriveAndAwaitAdvance()
 * has effect analogous to CyclicBarrier.await. Each generation of a phaser has an associated phase number. 
 * The phase number starts at zero, and advances when all parties arrive at the phaser, wrapping around to zero 
 * after reaching Integer.MAX_VALUE. The use of phase numbers enables independent control of actions upon 
 * arrival at a phaser and upon awaiting others, via two kinds of methods that may be invoked by any
 * registered party:
 *  1. Arrival. Methods arrive() and arriveAndDeregister() record arrival. These methods do not block, but 
 *  return an associated arrival phase number; that is, the phase number of the phaser to which the arrival applied. 
 *  When the final party for a given phase arrives, an optional action is performed and the phase advances.  
 *  These actions are performed by the party triggering a phase advance, and are arranged by overriding method 
 *  onAdvance(int, int), which also controls termination. Overriding this method is similar to, but more
 *  flexible than, providing a barrier action to a CyclicBarrier.
 *  2. Waiting. Method awaitAdvance() requires an argument indicating an arrival phase number, and returns when 
 *  the phaser advances to (or is already at) a different phase. Unlike similar constructions using CyclicBarrier, 
 *  method awaitAdvance continues to wait even if the waiting thread is interrupted. Interruptible and timeout
 *  versions are also available, but exceptions encountered while tasks wait interruptibly or with timeout 
 *  do not change the state of the phaser. If necessary, you can perform any associated recovery within handlers 
 *  of those exceptions, often after invoking forceTermination.  Phasers may also be used by tasks executing in a 
 *  ForkJoinPool, which will ensure sufficient parallelism to execute tasks when others are blocked waiting for 
 *  a phase to advance.
 * Termination. A phaser may enter a termination state, that may be checked using method isTerminated(). Upon
 * termination, all synchronization methods immediately return without waiting for advance, as indicated by 
 * a negative return value. Similarly, attempts to register upon termination have no effect. Termination is triggered 
 * when an invocation of onAdvance() returns true. The default implementation returns true if a deregistration 
 * has caused the number of registered parties to become zero. As illustrated below, when phasers control actions 
 * with a fixed number of iterations, it is often convenient to override this method to cause termination when 
 * the current phase number reaches a threshold. Method forceTermination() is also available to abruptly release 
 * waiting threads and allow them to terminate.
 * Tiering. Phasers may be tiered (i.e., constructed in tree structures) to reduce contention. Phasers with large 
 * numbers of parties that would otherwise experience heavy synchronization contention costs may instead 
 * be set up so that groups of sub-phasers share a common parent. This may greatly increase throughput even though 
 * it incurs greater per-operation overhead.
 * In a tree of tiered phasers, registration and deregistration of child phasers with their parent are managed 
 * automatically. Whenever the number of registered parties of a child phaser becomes non-zero (as established in the
 * Phaser(Phaser,int) constructor, register(), or bulkRegister()), the child phaser is registered with its parent.  
 * Whenever the number of registered parties becomes zero as the result of an invocation of arriveAndDeregister, 
 * the child phaser is deregistered from its parent.
 * Monitoring. While synchronization methods may be invoked only by registered parties, the current state of a phaser 
 * may be monitored by any caller. At any given moment there are getRegisteredParties() parties in total, of which 
 * getArrivedParties() have arrived at the current phase (getPhase()). When the remaining(getUnarrivedParties())
 * parties arrive, the phase advances.  The values returned by these methods may reflect transient states and 
 * so are not in general useful for synchronization control.
 *
 * Sample usages:
 * A Phaser may be used instead of a CountDownLatch to control a one-shot action serving a variable number of parties.
 * The typical idiom is for the method setting this up to first register, then start the actions, then deregister, 
 * as in:
 * void runTasks(List<Runnable> tasks) {
 *   final Phaser phaser = new Phaser(1); // "1" to register self
 *   // create and start threads
 *   for (final Runnable task : tasks) {
 *     phaser.register();
 *     new Thread() {
 *       public void run() {
 *         phaser.arriveAndAwaitAdvance(); // await all creation
 *         task.run();
 *       }
 *     }.start();
 *   }
 *
 *   // allow threads to start and deregister self
 *   phaser.arriveAndDeregister();
 * }
 * 
 * One way to cause a set of threads to repeatedly perform actions for a given number of iterations is to override
 * onAdvance():
 * void startTasks(List<Runnable> tasks, final int iterations) {
 *   final Phaser phaser = new Phaser() {
 *     protected boolean onAdvance(int phase, int registeredParties) {
 *       return phase >= iterations || registeredParties == 0;
 *     }
 *   };
 *   phaser.register();
 *   for (final Runnable task : tasks) {
 *     phaser.register();
 *     new Thread() {
 *       public void run() {
 *         do {
 *           task.run();
 *           phaser.arriveAndAwaitAdvance();
 *         } while (!phaser.isTerminated());
 *       }
 *     }.start();
 *   }
 *   phaser.arriveAndDeregister(); // deregister self, don't wait
 * } 
 *
 * If the main task must later await termination, it may re-register and then execute a similar loop:
 *   // ...
 *   phaser.register();
 *   while (!phaser.isTerminated())
 *     phaser.arriveAndAwaitAdvance();
 *
 * Related constructions may be used to await particular phase numbers in contexts where you are sure that 
 * the phase will never wrap around Integer.MAX_VALUE. For example:
 * void awaitPhase(Phaser phaser, int phase) {
 *   int p = phaser.register(); // assumes caller not already registered
 *   while (p < phase) {
 *     if (phaser.isTerminated())
 *       // ... deal with unexpected termination
 *     else
 *       p = phaser.arriveAndAwaitAdvance();
 *   }
 *   phaser.arriveAndDeregister();
 * } 
 * 
 * To create a set of n tasks using a tree of phasers, you could use code of the following form, 
 * assuming a Task class with a constructor accepting a Phaser that it registers with upon construction. 
 * After invocation of build(new Task[n], 0, n, new Phaser()), these tasks could then be started, for example by
 * submitting to a pool:
 * void build(Task[] tasks, int lo, int hi, Phaser ph) {
 *   if (hi - lo > TASKS_PER_PHASER) {
 *     for (int i = lo; i < hi; i += TASKS_PER_PHASER) {
 *       int j = Math.min(i + TASKS_PER_PHASER, hi);
 *       build(tasks, i, j, new Phaser(ph));
 *     }
 *   } else {
 *     for (int i = lo; i < hi; ++i)
 *       tasks[i] = new Task(ph);
 *       // assumes new Task(ph) performs ph.register()
 *   }
 * }
 *
 * The best value of TASKS_PER_PHASER depends mainly on expected synchronization rates. A value as low as four may
 * be appropriate for extremely small per-phase task bodies (thus high rates), or up to hundreds for extremely 
 * large ones.
 * Implementation notes: This implementation restricts the maximum number of parties to 65535. Attempts to register 
 * additional parties result in  IllegalStateException. However, you can and should create tiered phasers to 
 * accommodate arbitrarily large sets of participants.
 *
 * @since 1.7
 */
public class Phaser {

    /**
     * Primary state representation, holding four bit-fields:
     *
     * unarrived  -- the number of parties yet to hit barrier (bits  0-15)
     * parties    -- the number of parties to wait            (bits 16-31)
     * phase      -- the generation of the barrier            (bits 32-62)
     * terminated -- set if barrier is terminated             (bit  63 / sign)
     *
     * Except that a phaser with no registered parties is distinguished by the otherwise illegal state of having zero
     * parties and one unarrived parties (encoded as EMPTY below).
     *
     * To efficiently maintain atomicity, these values are packed into a single (atomic) long. 
     * Good performance relies on keeping state decoding and encoding simple, and keeping race windows short.
     *
     * All state updates are performed via CAS except initial registration of a sub-phaser (i.e., one with a non-null
     * parent). In this (relatively rare) case, we use built-in synchronization to lock while first registering 
     * with its parent.
     *
     * The phase of a subphaser is allowed to lag that of its ancestors until it is actually accessed -- see method
     * reconcileState.
     */
    private volatile long state;

    private static final int  MAX_PARTIES     = 0xffff;
    private static final int  MAX_PHASE       = Integer.MAX_VALUE;
    private static final int  PARTIES_SHIFT   = 16;
    private static final int  PHASE_SHIFT     = 32;
    private static final int  UNARRIVED_MASK  = 0xffff;      // to mask ints
    private static final long PARTIES_MASK    = 0xffff0000L; // to mask longs
    private static final long COUNTS_MASK     = 0xffffffffL;
    private static final long TERMINATION_BIT = 1L << 63;

    // some special values
    private static final int  ONE_ARRIVAL     = 1;
    private static final int  ONE_PARTY       = 1 << PARTIES_SHIFT;
    private static final int  ONE_DEREGISTER  = ONE_ARRIVAL|ONE_PARTY;
    private static final int  EMPTY           = 1;

    // The following unpacking methods are usually manually inlined

    private static int unarrivedOf(long s) {
        int counts = (int)s;
        return (counts == EMPTY) ? 0 : (counts & UNARRIVED_MASK);
    }

    private static int partiesOf(long s) {
        return (int)s >>> PARTIES_SHIFT;
    }

    private static int phaseOf(long s) {
        return (int)(s >>> PHASE_SHIFT);
    }

    private static int arrivedOf(long s) {
        int counts = (int)s;
        return (counts == EMPTY) ? 0 :
            (counts >>> PARTIES_SHIFT) - (counts & UNARRIVED_MASK);
    }

    /**
     * The parent of this phaser, or null if none
     */
    private final Phaser parent;

    /**
     * The root of phaser tree. Equals this if not in a tree.
     */
    private final Phaser root;

    /**
     * Heads of Treiber stacks for waiting threads. To eliminate contention when releasing some threads 
     * while adding others, we use two of them, alternating across even and odd phases.
     * Subphasers share queues with root to speed up releases.
     */
    private final AtomicReference<QNode> evenQ;
    private final AtomicReference<QNode> oddQ;

    private AtomicReference<QNode> queueFor(int phase) {
        return ((phase & 1) == 0) ? evenQ : oddQ;
    }

    /**
     * Returns message string for bounds exceptions on arrival.
     */
    private String badArrive(long s) {
        return "Attempted arrival of unregistered party for " +
            stateToString(s);
    }

    /**
     * Returns message string for bounds exceptions on registration.
     */
    private String badRegister(long s) {
        return "Attempt to register more than " +
            MAX_PARTIES + " parties for " + stateToString(s);
    }

    /**
     * Main implementation for methods arrive and arriveAndDeregister. Manually tuned to speed up and minimize 
     * race windows for the common case of just decrementing unarrived field. 
     * ONE_ARRIVAL for arrive, ONE_DEREGISTER for arriveAndDeregister
     */
    private int doArrive(int adjust) {
        final Phaser root = this.root;
        for (;;) {
            long s = (root == this) ? state : reconcileState();
            int phase = (int)(s >>> PHASE_SHIFT);
            if (phase < 0)
                return phase;
            int counts = (int)s;
            int unarrived = (counts == EMPTY) ? 0 : (counts & UNARRIVED_MASK);
            if (unarrived <= 0)
                throw new IllegalStateException(badArrive(s));
            if (UNSAFE.compareAndSwapLong(this, stateOffset, s, s-=adjust)) {
                if (unarrived == 1) {
                    long n = s & PARTIES_MASK;  // base of next state
                    int nextUnarrived = (int)n >>> PARTIES_SHIFT;
                    if (root == this) {
                        if (onAdvance(phase, nextUnarrived))
                            n |= TERMINATION_BIT;
                        else if (nextUnarrived == 0)
                            n |= EMPTY;
                        else
                            n |= nextUnarrived;
                        int nextPhase = (phase + 1) & MAX_PHASE;
                        n |= (long)nextPhase << PHASE_SHIFT;
                        UNSAFE.compareAndSwapLong(this, stateOffset, s, n);
                        releaseWaiters(phase);
                    }
                    else if (nextUnarrived == 0) { // propagate deregistration
                        phase = parent.doArrive(ONE_DEREGISTER);
                        UNSAFE.compareAndSwapLong(this, stateOffset,
                                                  s, s | EMPTY);
                    }
                    else
                        phase = parent.doArrive(ONE_ARRIVAL);
                }
                return phase;
            }
        }
    }

    /**
     * Implementation of register, bulkRegister, the number to add to both parties and
     * unarrived fields must be greater than zero.
     */
    private int doRegister(int registrations) {
        // adjustment to state
        long adjust = ((long)registrations << PARTIES_SHIFT) | registrations;
        final Phaser parent = this.parent;
        int phase;
        for (;;) {
            long s = (parent == null) ? state : reconcileState();
            int counts = (int)s;
            int parties = counts >>> PARTIES_SHIFT;
            int unarrived = counts & UNARRIVED_MASK;
            if (registrations > MAX_PARTIES - parties)
                throw new IllegalStateException(badRegister(s));
            phase = (int)(s >>> PHASE_SHIFT);
            if (phase < 0)
                break;
            if (counts != EMPTY) {                  // not 1st registration
                if (parent == null || reconcileState() == s) {
                    if (unarrived == 0)             // wait out advance
                        root.internalAwaitAdvance(phase, null);
                    else if (UNSAFE.compareAndSwapLong(this, stateOffset,
                                                       s, s + adjust))
                        break;
                }
            }
            else if (parent == null) {              // 1st root registration
                long next = ((long)phase << PHASE_SHIFT) | adjust;
                if (UNSAFE.compareAndSwapLong(this, stateOffset, s, next))
                    break;
            }
            else {
                synchronized (this) {               // 1st sub registration
                    if (state == s) {               // recheck under lock
                        phase = parent.doRegister(1);
                        if (phase < 0)
                            break;
                        // finish registration whenever parent registration
                        // succeeded, even when racing with termination,
                        // since these are part of the same "transaction".
                        while (!UNSAFE.compareAndSwapLong
                               (this, stateOffset, s,
                                ((long)phase << PHASE_SHIFT) | adjust)) {
                            s = state;
                            phase = (int)(root.state >>> PHASE_SHIFT);
                            // assert (int)s == EMPTY;
                        }
                        break;
                    }
                }
            }
        }
        return phase;
    }

    /**
     * Resolves lagged phase propagation from root if necessary. Reconciliation normally occurs when root 
     * has advanced but subphasers have not yet done so, in which case they must finish their own advance 
     * by setting unarrived to parties (or if parties is zero, resetting to unregistered EMPTY state).
     */
    private long reconcileState() {
        final Phaser root = this.root;
        long s = state;
        if (root != this) {
            int phase, p;
            // CAS to root phase with current parties, tripping unarrived
            while ((phase = (int)(root.state >>> PHASE_SHIFT)) !=
                   (int)(s >>> PHASE_SHIFT) &&
                   !UNSAFE.compareAndSwapLong
                   (this, stateOffset, s,
                    s = (((long)phase << PHASE_SHIFT) |
                         ((phase < 0) ? (s & COUNTS_MASK) :
                          (((p = (int)s >>> PARTIES_SHIFT) == 0) ? EMPTY :
                           ((s & PARTIES_MASK) | p))))))
                s = state;
        }
        return s;
    }

    /**
     * Creates a new phaser with no initially registered parties, no parent, and initial phase number 0. 
     * Any thread using this phaser will need to first register for it.
     */
    public Phaser() {
        this(null, 0);
    }

    /**
     * Creates a new phaser with the given number of registered unarrived parties, no parent, and initial phase number 0
     */
    public Phaser(int parties) {
        this(null, parties);
    }

    /**
     * Equivalent to Phaser(parent, 0) 
     */
    public Phaser(Phaser parent) {
        this(parent, 0);
    }

    /**
     * Creates a new phaser with the given parent and number of registered unarrived parties.  
     * When the given parent is non-null and the given number of parties is greater than zero, this
     * child phaser is registered with its parent.
     */
    public Phaser(Phaser parent, int parties) {
        if (parties >>> PARTIES_SHIFT != 0)
            throw new IllegalArgumentException("Illegal number of parties");
        int phase = 0;
        this.parent = parent;
        if (parent != null) {
            final Phaser root = parent.root;
            this.root = root;
            this.evenQ = root.evenQ;
            this.oddQ = root.oddQ;
            if (parties != 0)
                phase = parent.doRegister(1);
        }
        else {
            this.root = this;
            this.evenQ = new AtomicReference<QNode>();
            this.oddQ = new AtomicReference<QNode>();
        }
        this.state = (parties == 0) ? (long)EMPTY :
            ((long)phase << PHASE_SHIFT) |
            ((long)parties << PARTIES_SHIFT) |
            ((long)parties);
    }

    /**
     * Adds a new unarrived party to this phaser.  If an ongoing invocation of onAdvance() is in progress, this method
     * may await its completion before returning.  If this phaser has a parent, and this phaser previously 
     * had no registered parties, this child phaser is also registered with its parent. If this phaser is terminated, 
     * the attempt to register has no effect, and a negative value is returned.
     */
    public int register() {
        return doRegister(1);
    }

    /**
     * Adds the given number of new unarrived parties to this phaser. If an ongoing invocation of onAdvance()
     * is in progress, this method may await its completion before returning.  If this phaser has a parent, 
     * and the given number of parties is greater than zero, and this phaser previously had no registered
     * parties, this child phaser is also registered with its parent. If this phaser is terminated, 
     * the attempt to register has no effect, and a negative value is returned.
     */
    public int bulkRegister(int parties) {
        if (parties < 0)
            throw new IllegalArgumentException();
        if (parties == 0)
            return getPhase();
        return doRegister(parties);
    }

    /**
     * Arrives at this phaser, without waiting for others to arrive. It is a usage error for an unregistered party 
     * to invoke this method.  However, this error may result in an IllegalStateException only upon some subsequent 
     * operation on this phaser, if ever.
     */
    public int arrive() {
        return doArrive(ONE_ARRIVAL);
    }

    /**
     * Arrives at this phaser and deregisters from it without waiting for others to arrive. Deregistration 
     * reduces the number of parties required to advance in future phases.  If this phaser has a parent, 
     * and deregistration causes this phaser to have zero parties, this phaser is also deregistered from its parent.
     * It is a usage error for an unregistered party to invoke this method.  However, this error may result in an 
     * IllegalStateException only upon some subsequent operation on this phaser, if ever.
     */
    public int arriveAndDeregister() {
        return doArrive(ONE_DEREGISTER);
    }

    /**
     * Arrives at this phaser and awaits others. Equivalent in effect to awaitAdvance(arrive()). If you need to 
     * await with interruption or timeout, you can arrange this with an analogous construction using one of 
     * the other forms of the awaitAdvance method. If instead you need to deregister upon arrival, use 
     * awaitAdvance(arriveAndDeregister()).
     * It is a usage error for an unregistered party to invoke this method.  However, this error may result in an 
     * IllegalStateException only upon some subsequent operation on this phaser, if ever.
     */
    public int arriveAndAwaitAdvance() {
        // Specialization of doArrive+awaitAdvance eliminating some reads/paths
        final Phaser root = this.root;
        for (;;) {
            long s = (root == this) ? state : reconcileState();
            int phase = (int)(s >>> PHASE_SHIFT);
            if (phase < 0)
                return phase;
            int counts = (int)s;
            int unarrived = (counts == EMPTY) ? 0 : (counts & UNARRIVED_MASK);
            if (unarrived <= 0)
                throw new IllegalStateException(badArrive(s));
            if (UNSAFE.compareAndSwapLong(this, stateOffset, s,
                                          s -= ONE_ARRIVAL)) {
                if (unarrived > 1)
                    return root.internalAwaitAdvance(phase, null);
                if (root != this)
                    return parent.arriveAndAwaitAdvance();
                long n = s & PARTIES_MASK;  // base of next state
                int nextUnarrived = (int)n >>> PARTIES_SHIFT;
                if (onAdvance(phase, nextUnarrived))
                    n |= TERMINATION_BIT;
                else if (nextUnarrived == 0)
                    n |= EMPTY;
                else
                    n |= nextUnarrived;
                int nextPhase = (phase + 1) & MAX_PHASE;
                n |= (long)nextPhase << PHASE_SHIFT;
                if (!UNSAFE.compareAndSwapLong(this, stateOffset, s, n))
                    return (int)(state >>> PHASE_SHIFT); // terminated
                releaseWaiters(phase);
                return nextPhase;
            }
        }
    }

    /**
     * Awaits the phase of this phaser to advance from the given phase value, returning immediately 
     * if the current phase is not equal to the given phase value or this phaser is terminated.
     */
    public int awaitAdvance(int phase) {
        final Phaser root = this.root;
        long s = (root == this) ? state : reconcileState();
        int p = (int)(s >>> PHASE_SHIFT);
        if (phase < 0)
            return phase;
        if (p == phase)
            return root.internalAwaitAdvance(phase, null);
        return p;
    }

    /**
     * Awaits the phase of this phaser to advance from the given phase value, throwing InterruptedException 
     * if interrupted while waiting, or returning immediately if the current phase is not equal to the 
     * given phase value or this phaser is terminated.
     */
    public int awaitAdvanceInterruptibly(int phase)
        throws InterruptedException {
        final Phaser root = this.root;
        long s = (root == this) ? state : reconcileState();
        int p = (int)(s >>> PHASE_SHIFT);
        if (phase < 0)
            return phase;
        if (p == phase) {
            QNode node = new QNode(this, phase, true, false, 0L);
            p = root.internalAwaitAdvance(phase, node);
            if (node.wasInterrupted)
                throw new InterruptedException();
        }
        return p;
    }

    /**
     * Awaits the phase of this phaser to advance from the given phase value or the given timeout to elapse, 
     * throwing InterruptedException if interrupted while waiting, or returning immediately 
     * if the current phase is not equal to the  given phase value or this phaser is terminated.
     */
    public int awaitAdvanceInterruptibly(int phase, long timeout, TimeUnit unit)
        throws InterruptedException, TimeoutException {
        long nanos = unit.toNanos(timeout);
        final Phaser root = this.root;
        long s = (root == this) ? state : reconcileState();
        int p = (int)(s >>> PHASE_SHIFT);
        if (phase < 0)
            return phase;
        if (p == phase) {
            QNode node = new QNode(this, phase, true, true, nanos);
            p = root.internalAwaitAdvance(phase, node);
            if (node.wasInterrupted)
                throw new InterruptedException();
            else if (p == phase)
                throw new TimeoutException();
        }
        return p;
    }

    /**
     * Forces this phaser to enter termination state.  Counts of registered parties are unaffected.
     * If this phaser is a member of a tiered set of phasers, then all of the phasers in the set are terminated.
     * If this phaser is already terminated, this method has no effect.This method may be useful for
     * coordinating recovery after one or more tasks encounter unexpected exceptions.
     */
    public void forceTermination() {
        // Only need to change root state
        final Phaser root = this.root;
        long s;
        while ((s = root.state) >= 0) {
            if (UNSAFE.compareAndSwapLong(root, stateOffset,
                                          s, s | TERMINATION_BIT)) {
                // signal all threads
                releaseWaiters(0); // Waiters on evenQ
                releaseWaiters(1); // Waiters on oddQ
                return;
            }
        }
    }

    /**
     * Returns the current phase number. The maximum phase number is Integer.MAX_VALUE, after which it restarts at
     * zero. Upon termination, the phase number is negative, in which case the prevailing phase prior to termination
     * may be obtained via getPhase() + Integer.MIN_VALUE.
     */
    public final int getPhase() {
        return (int)(root.state >>> PHASE_SHIFT);
    }

    /**
     * Returns the number of parties registered at this phaser.
     */
    public int getRegisteredParties() {
        return partiesOf(state);
    }

    /**
     * Returns the number of registered parties that have arrived at the current phase of this phaser. 
     * If this phaser has terminated, the returned value is meaningless and arbitrary.
     */
    public int getArrivedParties() {
        return arrivedOf(reconcileState());
    }

    /**
     * Returns the number of registered parties that have not yet arrived at the current phase of this phaser. 
     * If this phaser has terminated, the returned value is meaningless and arbitrary.
     */
    public int getUnarrivedParties() {
        return unarrivedOf(reconcileState());
    }

    /**
     * Returns the parent of this phaser, or null if none
     */
    public Phaser getParent() {
        return parent;
    }

    /**
     * Returns the root ancestor of this phaser, which is the same as this phaser if it has no parent.
     */
    public Phaser getRoot() {
        return root;
    }

    /**
     * Returns true if this phaser has been terminated.
     */
    public boolean isTerminated() {
        return root.state < 0L;
    }

    /**
     * Overridable method to perform an action upon impending phase advance, and to control termination. 
     * This method is invoked upon arrival of the party advancing this phaser (when all other waiting parties 
     * are dormant).  If this method returns true, this phaser will be set to a final termination state
     * upon advance, and subsequent calls to isTerminated() will return true. Any (unchecked) Exception or Error 
     * thrown by an invocation of this method is propagated to the party  attempting to advance this phaser, 
     * in which case no advance occurs.
     * The arguments to this method provide the state of the phaser prevailing for the current transition.
     * The effects of invoking arrival, registration, and waiting methods on this phaser from within onAdvance 
     * are unspecified and should not be relied on.
     * If this phaser is a member of a tiered set of phasers, then onAdvance() is invoked only for its root phaser 
     * on each advance.
     * To support the most common use cases, the default implementation of this method returns true when the
     * number of registered parties has become zero as the result of a party invoking arriveAndDeregister.
     * You can disable this behavior, thus enabling continuation upon future registrations, by overriding 
     * this method to always return false:
     * 
     * Phaser phaser = new Phaser() {
     *   protected boolean onAdvance(int phase, int parties) { return false; }
     * }
     */
    protected boolean onAdvance(int phase, int registeredParties) {
        return registeredParties == 0;
    }

    /**
     * Returns a string identifying this phaser, as well as its state.
     */
    public String toString() {
        return stateToString(reconcileState());
    }

    /**
     * Implementation of toString and string-based error messages
     */
    private String stateToString(long s) {
        return super.toString() +
            "[phase = " + phaseOf(s) +
            " parties = " + partiesOf(s) +
            " arrived = " + arrivedOf(s) + "]";
    }

    // Waiting mechanics

    /**
     * Removes and signals threads from queue for phase.
     */
    private void releaseWaiters(int phase) {
        QNode q;   // first element of queue
        Thread t;  // its thread
        AtomicReference<QNode> head = (phase & 1) == 0 ? evenQ : oddQ;
        while ((q = head.get()) != null &&
               q.phase != (int)(root.state >>> PHASE_SHIFT)) {
            if (head.compareAndSet(q, q.next) &&
                (t = q.thread) != null) {
                q.thread = null;
                LockSupport.unpark(t);
            }
        }
    }

    /**
     * Variant of releaseWaiters that additionally tries to remove any nodes no longer waiting for advance 
     * due to timeout or interrupt. Currently, nodes are removed only if they are at head of queue, 
     * which suffices to reduce memory footprint in  most usages.
     */
    private int abortWait(int phase) {
        AtomicReference<QNode> head = (phase & 1) == 0 ? evenQ : oddQ;
        for (;;) {
            Thread t;
            QNode q = head.get();
            int p = (int)(root.state >>> PHASE_SHIFT);
            if (q == null || ((t = q.thread) != null && q.phase == p))
                return p;
            if (head.compareAndSet(q, q.next) && t != null) {
                q.thread = null;
                LockSupport.unpark(t);
            }
        }
    }

    /** The number of CPUs, for spin control */
    private static final int NCPU = Runtime.getRuntime().availableProcessors();

    /**
     * The number of times to spin before blocking while waiting for advance, per arrival while waiting. 
     * On multiprocessors, fully blocking and waking up a large number of threads all at once is usually a very 
     * slow process, so we use rechargeable spins to avoid it when threads regularly arrive: When a thread in
     * internalAwaitAdvance notices another arrival before blocking, and there appear to be enough CPUs available, 
     * it spins SPINS_PER_ARRIVAL more times before blocking. The value trades off good-citizenship vs 
     * big unnecessary slowdowns.
     */
    static final int SPINS_PER_ARRIVAL = (NCPU < 2) ? 1 : 1 << 8;

    /**
     * Possibly blocks and waits for phase to advance unless aborted. Call only on root phaser.
     */
    private int internalAwaitAdvance(int phase, QNode node) {
        // assert root == this;
        releaseWaiters(phase-1);          // ensure old queue clean
        boolean queued = false;           // true when node is enqueued
        int lastUnarrived = 0;            // to increase spins upon change
        int spins = SPINS_PER_ARRIVAL;
        long s;
        int p;
        while ((p = (int)((s = state) >>> PHASE_SHIFT)) == phase) {
            if (node == null) {           // spinning in noninterruptible mode
                int unarrived = (int)s & UNARRIVED_MASK;
                if (unarrived != lastUnarrived &&
                    (lastUnarrived = unarrived) < NCPU)
                    spins += SPINS_PER_ARRIVAL;
                boolean interrupted = Thread.interrupted();
                if (interrupted || --spins < 0) { // need node to record intr
                    node = new QNode(this, phase, false, false, 0L);
                    node.wasInterrupted = interrupted;
                }
            }
            else if (node.isReleasable()) // done or aborted
                break;
            else if (!queued) {           // push onto queue
                AtomicReference<QNode> head = (phase & 1) == 0 ? evenQ : oddQ;
                QNode q = node.next = head.get();
                if ((q == null || q.phase == phase) &&
                    (int)(state >>> PHASE_SHIFT) == phase) // avoid stale enq
                    queued = head.compareAndSet(q, node);
            }
            else {
                try {
                    ForkJoinPool.managedBlock(node);
                } catch (InterruptedException ie) {
                    node.wasInterrupted = true;
                }
            }
        }

        if (node != null) {
            if (node.thread != null)
                node.thread = null;       // avoid need for unpark()
            if (node.wasInterrupted && !node.interruptible)
                Thread.currentThread().interrupt();
            if (p == phase && (p = (int)(state >>> PHASE_SHIFT)) == phase)
                return abortWait(phase); // possibly clean up on abort
        }
        releaseWaiters(phase);
        return p;
    }

    /**
     * Wait nodes for Treiber stack representing wait queue
     */
    static final class QNode implements ForkJoinPool.ManagedBlocker {
        final Phaser phaser;
        final int phase;
        final boolean interruptible;
        final boolean timed;
        boolean wasInterrupted;
        long nanos;
        final long deadline;
        volatile Thread thread; // nulled to cancel wait
        QNode next;

        QNode(Phaser phaser, int phase, boolean interruptible,
              boolean timed, long nanos) {
            this.phaser = phaser;
            this.phase = phase;
            this.interruptible = interruptible;
            this.nanos = nanos;
            this.timed = timed;
            this.deadline = timed ? System.nanoTime() + nanos : 0L;
            thread = Thread.currentThread();
        }

        public boolean isReleasable() {
            if (thread == null)
                return true;
            if (phaser.getPhase() != phase) {
                thread = null;
                return true;
            }
            if (Thread.interrupted())
                wasInterrupted = true;
            if (wasInterrupted && interruptible) {
                thread = null;
                return true;
            }
            if (timed) {
                if (nanos > 0L) {
                    nanos = deadline - System.nanoTime();
                }
                if (nanos <= 0L) {
                    thread = null;
                    return true;
                }
            }
            return false;
        }

        public boolean block() {
            if (isReleasable())
                return true;
            else if (!timed)
                LockSupport.park(this);
            else if (nanos > 0L)
                LockSupport.parkNanos(this, nanos);
            return isReleasable();
        }
    }

    // Unsafe mechanics
    private static final sun.misc.Unsafe UNSAFE;
    private static final long stateOffset;
    static {
        try {
            UNSAFE = sun.misc.Unsafe.getUnsafe();
            Class<?> k = Phaser.class;
            stateOffset = UNSAFE.objectFieldOffset
                (k.getDeclaredField("state"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }
}
```

Special Thanks:
https://www.cnblogs.com/uodut/p/6830939.html