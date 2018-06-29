我们来看JDK1.8中Runnable和Callable接口的源码和注释：
### Runnable
```java
package java.lang;

/**
 * The Runnable interface should be implemented by any
 * class whose instances are intended to be executed by a thread. The
 * class must define a method of no arguments called run.
 * This interface is designed to provide a common protocol for objects that
 * wish to execute code while they are active. For example,
 * Runnable is implemented by class Thread.
 * Being active simply means that a thread has been started and has not
 * yet been stopped.
 * In addition, Runnable provides the means for a class to be
 * active while not subclassing Thread. A class that implements
 * Runnable can run without subclassing Thread
 * by instantiating a Thread instance and passing itself in
 * as the target.  In most cases, the Runnable interface should
 * be used if you are only planning to override the run()
 * method and no other Thread methods.
 * This is important because classes should not be subclassed
 * unless the programmer intends on modifying or enhancing the fundamental
 * behavior of the class.
 *
 * @since   JDK1.0
 */
@FunctionalInterface
public interface Runnable {
    /**
     * When an object implementing interface Runnable is used
     * to create a thread, starting the thread causes the object's
     * run method to be called in that separately executing thread.
     * The general contract of the method run is that it may
     * take any action whatsoever.
     */
    public abstract void run();
}
```
### Callable
```java
package java.util.concurrent;

/**
 * A task that returns a result and may throw an exception.
 * Implementors define a single method with no arguments called call
 * The Callable interface is similar to java.lang.Runnable, 
 * in that both are designed for classes whose
 * instances are potentially executed by another thread.  A
 * Runnable, however, does not return a result and cannot
 * throw a checked exception.
 * The Executors class contains utility methods to
 * convert from other common forms to Callable classes.
 * @since 1.5
 */
@FunctionalInterface
public interface Callable<V> {
    /**
     * Computes a result, or throws an exception if unable to do so.
     *
     * @return computed result
     * @throws Exception if unable to compute a result
     */
    V call() throws Exception;
}
```
通过源码中的注释我们可以很清楚的总结出如下结论，这两个接口都是用来新启动一个线程执行默认方法中的代码，不同之处在于：
Runnable接口的run()方法没有返回值，也未声明抛出异常，所以我们也无法获取其执行的结果或者错误信息；
但是Callable接口的call()方法有一个泛型化的返回值，并会抛出异常，我们可以从中获取新线程执行任务的结果信息。
下面我们示例一些代码片段，解释一些使用和编码时的要点：
```java
    // 我们可以通过Thread类包装Runnable接口的实现来启动新线程并执行任务
    // 但是在实际生产环境中不要显式创建线程，推荐使用线程池
    // 使用线程池的好处是减少在创建和销毁线程上所花的时间以及系统资源的开销，解决资源不足的问题。
    // 如果不使用线程池，有可能造成系统创建大量同类线程而导致消耗完内存或者“过度切换”的问题。
     void notRecommended() {
        System.out.println(Thread.currentThread().getName());
        new Thread(() -> System.out.println(Thread.currentThread().getName())).start();
    }
    // 推荐的用法
    // 1.我们通过guava提供的工具类来创建线程池，并提交我们的任务执行
    void recommended1() {
        ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("demo-pool-%d").build();
        ExecutorService singleThreadPool = new ThreadPoolExecutor(1, 1, 0L,
                TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(1024), threadFactory, new ThreadPoolExecutor.AbortPolicy());
        singleThreadPool.execute(() -> System.out.println(Thread.currentThread().getName()));
        singleThreadPool.shutdown();
    }
    // 2.我们通过apache-commons-lang3提供的API来创建线程池
    void recommended2() {
        ScheduledExecutorService executorService = new ScheduledThreadPoolExecutor(1,
                new BasicThreadFactory.Builder().namingPattern("example-schedule-pool-%d").daemon(true).build());
        executorService.execute(() -> System.out.println(Thread.currentThread().getName()));
        executorService.shutdown();
    }
    // 3.spring配置 例如我们在applicationContext.xml文件中做如下配置
        <bean id="threadPoolTaskExecutorOperate"
              class="org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor">
            <!-- 线程池维护线程的最少数量 -->
            <property name="corePoolSize" value="10"/>
            <!-- 线程池维护线程的最大数量 -->
            <property name="maxPoolSize" value="100"/>
            <!-- 队列最大长度 >=mainExecutor.maxSize -->
            <property name="queueCapacity" value="6000"/>
            <!-- 线程池维护线程所允许的空闲时间 -->
            <property name="keepAliveSeconds" value="600"/>
        </bean> 
    // 在我们的代码中就可以通过@Autowired注解引用此线程池使用了
```
```java
    // 我们使用了JDK原生的Executors工具类来创建线程池，并将我们的Callable实例提交执行
    // 实际生产环境中我们仍然不建议使用Executors创建线程池，
    // newSingleThreadExecutor/newFixedThreadPool 会使请求在处理队列中堆积，造成大量系统资源占用甚至OOM
    // newCachedThreadPool/newScheduledThreadPool 会使新建线程数量不受控制的增大，造成大量系统资源占用甚至OOM
    void notRecommended() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Date> future = executor.submit(() -> new Date());
    }
```
### Future/FutureTask
在上面的例子中我们可以看到，通过ExecutorService执行的任务可以返回Future<V>类型的返回值，代表执行结果。
```java
package java.util.concurrent;

/**
 * A Future represents the result of an asynchronous
 * computation.  Methods are provided to check if the computation is
 * complete, to wait for its completion, and to retrieve the result of
 * the computation.  The result can only be retrieved using method
 * get when the computation has completed, blocking if
 * necessary until it is ready.  Cancellation is performed by the
 * cancel method.  Additional methods are provided to
 * determine if the task completed normally or was cancelled. Once a
 * computation has completed, the computation cannot be cancelled.
 * If you would like to use a Future for the sake
 * of cancellability but not provide a usable result, you can
 * declare types of the form Future<?> and
 * return null as a result of the underlying task.
 * 
 * Sample Usage (Note that the following classes are all made-up.)
 * 
 * interface ArchiveSearcher { String search(String target); }
 * class App {
 *   ExecutorService executor = ...
 *   ArchiveSearcher searcher = ...
 *   void showSearch(final String target)
 *       throws InterruptedException {
 *     Future<String> future
 *       = executor.submit(new Callable<String>() {
 *         public String call() {
 *             return searcher.search(target);
 *         }});
 *     displayOtherThings(); // do other things while searching
 *     try {
 *       displayText(future.get()); // use future
 *     } catch (ExecutionException ex) { cleanup(); return; }
 *   }
 * } 
 *
 * The FutureTask class is an implementation of Future that
 * implements Runnable, and so may be executed by an Executor.
 * For example, the above construction with submit could be replaced by:
 * 
 * FutureTask<String> future =
 *   new FutureTask<String>(new Callable<String>() {
 *     public String call() {
 *       return searcher.search(target);
 *   }});
 * executor.execute(future);
 *
 * Memory consistency effects: Actions taken by the asynchronous computation
 * happen-before actions following the corresponding Future.get() in another thread.
 *
 * @since 1.5
 */
public interface Future<V> {

    /**
     * Attempts to cancel execution of this task. This attempt will
     * fail if the task has already completed, has already been cancelled,
     * or could not be cancelled for some other reason. If successful,
     * and this task has not started when  cancel is called,
     * this task should never run. If the task has already started,
     * then the mayInterruptIfRunning parameter determines
     * whether the thread executing this task should be interrupted in
     * an attempt to stop the task.
     *
     * After this method returns, subsequent calls to isDone will
     * always return true. Subsequent calls to isCancelled 
     * will always return true if this method returned true .
     *
     * mayInterruptIfRunning: true if the thread executing this
     * task should be interrupted; otherwise, in-progress tasks are allowed
     * to complete
     * return false if the task could not be cancelled,
     * typically because it has already completed normally; true otherwise
     */
    boolean cancel(boolean mayInterruptIfRunning);

    /**
     * Returns true if this task was cancelled before it completed normally.
     */
    boolean isCancelled();

    /**
     * Returns true if this task completed.
     *
     * Completion may be due to normal termination, an exception, or
     * cancellation -- in all of these cases, this method will return true.
     */
    boolean isDone();

    /**
     * Waits if necessary for the computation to complete, and then
     * retrieves its result.
     * 
     * throws CancellationException if the computation was cancelled
     * throws ExecutionException if the computation threw an exception
     * throws InterruptedException if the current thread was interrupted while waiting
     */
    V get() throws InterruptedException, ExecutionException;

    /**
     * Waits if necessary for at most the given time for the computation
     * to complete, and then retrieves its result, if available.
     */
    V get(long timeout, TimeUnit unit)
        throws InterruptedException, ExecutionException, TimeoutException;
}
```
在Future的官方文档里提到了它的默认实现FutureTask，我们截取其中关键代码：
```java
package java.util.concurrent;
import java.util.concurrent.locks.LockSupport;

/**
 * A cancellable asynchronous computation. This class provides a base
 * implementation of Future, with methods to start and cancel
 * a computation, query to see if the computation is complete, and
 * retrieve the result of the computation. The result can only be
 * retrieved when the computation has completed; the get
 * methods will block if the computation has not yet completed. Once
 * the computation has completed, the computation cannot be restarted
 * or cancelled (unless the computation is invoked using runAndReset).
 *
 * A FutureTask can be used to wrap a Callable} or Runnable} object. 
 * Because FutureTask implements Runnable, a FutureTask can be 
 * submitted to an Executor for execution.
 *
 * In addition to serving as a standalone class, this class provides
 * protected functionality that may be useful when creating
 * customized task classes.
 *
 * @since 1.5
 */
public class FutureTask<V> implements RunnableFuture<V> {
    ...
    private Callable<V> callable;
    
    /**
     * Creates a FutureTask that will, upon running, execute the given Callable .
     */
    public FutureTask(Callable<V> callable) {
        if (callable == null)
            throw new NullPointerException();
        this.callable = callable;
        this.state = NEW;       // ensure visibility of callable
    }

    /**
     * Creates a FutureTask that will, upon running, execute the
     * given Runnable, and arrange that get will return the
     * given result on successful completion.
     */
    public FutureTask(Runnable runnable, V result) {
        this.callable = Executors.callable(runnable, result);
        this.state = NEW;       // ensure visibility of callable
    }
    ...
}

```
```java
public interface RunnableFuture<V> extends Runnable, Future<V> {
    /**
     * Sets this Future to the result of its computation
     * unless it has been cancelled.
     */
    void run();
}
```
我们可以认为FutureTask是Runnable/Callable/Future的集大成者，它实现了Runnable和Future接口，又通过
注入callable属性兼容了Callable<V>接口，通过其构造函数我们可以注入Callable<V>实例，或者Runnable实例。
Runnable实例最终会被如下代码处理：
```java
    static final class RunnableAdapter<T> implements Callable<T> {
        final Runnable task;
        final T result;
        RunnableAdapter(Runnable task, T result) {
            this.task = task;
            this.result = result;
        }
        public T call() {
            task.run();
            return result;
        }
    }
```
通过RunnableAdapter实现了将Runnable接口像Callable一样的处理。