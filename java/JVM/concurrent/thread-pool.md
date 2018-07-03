### 线程池简介
当我们的服务器面临大量并发请求时，为了减少客户端阻塞时间，一个很自然的想法就是我们启动多个线程分别去处理这些请求。
但是我们究竟需要多少个线程呢？如果我们为每一个请求都新建一个线程来处理的话，我们会发现，服务器在创建和销毁线程上花费的时间
和消耗的系统资源要比花在处理实际的用户请求的时间和资源更多。并且在一个JVM里创建太多的线程可能会导致系统由于过度消耗内存而
用完内存或“切换过度”。为了防止资源不足，服务器应用程序需要一些办法来限制任何给定时刻处理的请求数目。这时我们很自然的会想到
准备一些数量的线程，将其放到线程池中，请求到来时从线程池中取线程处理，处理完毕后又将线程归还给线程池。线程池的好处是，
在请求到达时线程已经存在，消除了线程创建所带来的延迟，使应用程序响应更快。通过适当地调整线程池中的线程数目，在请求超出
这些线程的处理能力时，通过等待队列或者拒绝服务处理，可以防止服务器资源不足。
但是真正用于生产环境中的线程池实现确需要考虑更多的问题，一旦不慎我们就会面临以下问题：    
1. 死锁   
多线程程序都会有死锁的风险，比如A线程持有锁1，B线程持有锁2，如果此时A线程需要获取锁2来释放锁1，而B线程恰巧需要获取锁1来释放锁2，
那么一个死锁就形成了。在更复杂的业务场景下，很能会有更复杂的循环引用引发的死锁。线程池引入了另外一种死锁的风险，比如一个线程
在等待另外一个线程的执行结果，但是线程池里却没有空闲的线程来提供这个结果，那么死锁就形成了，当然这种情况会随着其他线程被
返还给线程池而解决。
2. 资源耗尽    
当线程池的大小，或者说线程池中的线程数量得不到合理的的控制时，资源耗尽的情况还是会发生，另外，除了线程池中线程自身所占用的
系统资源，每个线程在执行任务时任务本身消耗的资源也应该考虑在内。线程池的最佳大小取决于可用处理器的数目以及工作队列中的
任务的性质。若在一个具有 N 个处理器的系统上只有一个工作队列，其中全部是计算性质的任务，在线程池具有 N 或 N+1 个线程时一般会
获得最大的 CPU 利用率。对于那些可能需要等待 I/O 完成的任务(例如，从套接字读取 HTTP 请求的任务)，需要让池的大小超过
可用处理器的数目，因为并不是所有线程都一直在工作。通过使用概要分析，您可以估计某个典型请求的等待时间WT与服务时间ST之间的比例。
如果我们将这一比例称之为 WT/ST，那么对于一个具有 N 个处理器的系统，需要设置大约 N*(1+WT/ST) 个线程来保持处理器得到充分利用。
处理器利用率不是调整线程池大小过程中的唯一考虑事项。随着线程池的增长，您可能会碰到调度程序、可用内存方面的限制，
或者其它系统资源方面的限制，例如套接字、打开的文件句柄或数据库连接等的数目。
另外，等待线程处理的任务会被放到等待队列里，当等待的任务堆积过多，如果不能处理队列的大小，那么系统资源也会被耗尽。
3. 线程通讯错误   
JDK原生的线程调度API在使用上难度较高，如果不能合理使用，那么错误在所难免。    
4. 线程泄露    
当线程池中某个线程在执行任务时抛出了RuntimeException或者Error而没有被捕获时，这个线程会退出，那么线程池中的线程资源就少了一个。
当这种情况积累起来时，线程池会慢慢被耗尽。另外一种情况，当线程处理的任务长时间处于阻塞时，这个线程不能被返还给池，
那么相当于池中资源也减少了一个。    

我们从JDK提供的原生API开始学习线程池的实现，本文主要包括以下几部分：   
1. JDK线程池相关API   
2. Guava/Apache commons提供的线程池工具   
3. Tomcat的线程模型   
4. NginX的线程模型   
5. Struts2/SpringMVC等MVC框架的线程模型   
6. Dubbo的线程模型   
7. Netty的线程模型   

#### JDK线程池相关API
##### Executor 接口
Executor接口提供了多线程任务提交的最高抽象，并且仅提供了一个execute方法，我们可以通过源码和官方文档来了解。
```java
package java.util.concurrent;

/**
 * 
 * 官方文档强调的是，Executor的作用是将任务的提交和执行方式解耦，也就是说使用Executor提供的execute()方法
 * 避免直接调用Thread.start()启动新线程。甚至将Runnable的run()方法直接在当前线程执行也没问题。
 * 任务的具体执行方式由接口的实现决定，有可能是在一个新线程中执行，有可能是在线程池中执行，甚至也有可能是在当前线程中执行。
 * ExecutorService提供了更加具有扩展性的功能，ThreadPoolExecutor提供了线程池版本的实现，Executors提供了创建各种线程池的工具。
 * 
 * An object that executes submitted Runnable tasks. This interface provides a way of decoupling task submission 
 * from the mechanics of how each task will be run, including details of thread use, scheduling, etc.  
 * An Executor is normally used instead of explicitly creating threads. For example, rather than invoking
 * new Thread(new(RunnableTask())).start(); for each of a set of tasks, you might use:
 * 
 * Executor executor = anExecutor;
 * executor.execute(new RunnableTask1());
 * executor.execute(new RunnableTask2());
 * 
 * However, the Executor interface does not strictly require that execution be asynchronous. In the simplest case, 
 * an executor can run the submitted task immediately in the caller's thread:
 * 
 * class DirectExecutor implements Executor {
 *   public void execute(Runnable r) {
 *     r.run();
 *   }
 * }
 *
 * More typically, tasks are executed in some thread other than the caller's thread. The executor below 
 * spawns a new thread for each task.
 * 
 * class ThreadPerTaskExecutor implements Executor {
 *   public void execute(Runnable r) {
 *     new Thread(r).start();
 *   }
 * }
 *
 * Many Executor implementations impose some sort of limitation on how and when tasks are scheduled. 
 *
 * The Executor implementations provided in this package implement ExecutorService, which is a more extensive
 * interface. The ThreadPoolExecutor class provides an extensible thread pool implementation. The Executors class
 * provides convenient factory methods for these Executors.
 * Memory consistency effects: Actions in a thread prior to submitting a Runnable object to an Executor 
 * happen-before its execution begins, perhaps in another thread.
 *
 * @since 1.5
 */
public interface Executor {

    /**
     * Executes the given command at some time in the future. The command may execute in a new thread, 
     * in a pooled thread, or in the calling thread, at the discretion of the Executor implementation.
     * throws RejectedExecutionException if this task cannot be accepted for execution
     * throws NullPointerException if command is null
     */
    void execute(Runnable command);
}

```
Executor的功能主要体现在子接口ExecutorService里：
```java
package java.util.concurrent;
import java.util.List;
import java.util.Collection;

/**
 * An Executor that provides methods to manage termination and methods that can produce a Future for tracking 
 * progress of one or more asynchronous tasks.
 *
 * An ExecutorService can be shut down, which will cause it to reject new tasks. Two different methods are provided 
 * for shutting down an ExecutorService. The shutdown() method will allow previously submitted tasks to execute before
 * terminating, while the shutdownNow() method prevents waiting tasks from starting and attempts to stop currently 
 * executing tasks.
 * Upon termination, an executor has no tasks actively executing, no tasks awaiting execution, and no new tasks 
 * can be submitted.  An unused ExecutorService should be shut down to allow reclamation of its resources.
 * 
 * Method submit() extends base method Executor#execute(Runnable) by creating and returning a Future that can be 
 * used to cancel execution and/or wait for completion.
 * Methods invokeAny() and invokeAll() perform the most commonly useful forms of bulk execution, executing a 
 * collection of tasks and then waiting for at least one, or all, to complete. (Class ExecutorCompletionService 
 * can be used to write customized variants of these methods.)
 * 
 * Here is a sketch of a network service in which threads in a thread pool service incoming requests. It uses 
 * the preconfigured Executors#newFixedThreadPool() factory method:
 * 
 * class NetworkService implements Runnable {
 *   private final ServerSocket serverSocket;
 *   private final ExecutorService pool;
 *
 *   public NetworkService(int port, int poolSize)
 *       throws IOException {
 *     serverSocket = new ServerSocket(port);
 *     pool = Executors.newFixedThreadPool(poolSize);
 *   }
 *
 *   public void run() { // run the service
 *     try {
 *       for (;;) {
 *         pool.execute(new Handler(serverSocket.accept()));
 *       }
 *     } catch (IOException ex) {
 *       pool.shutdown();
 *     }
 *   }
 * }
 *
 * class Handler implements Runnable {
 *   private final Socket socket;
 *   Handler(Socket socket) { this.socket = socket; }
 *   public void run() {
 *     // read and service request on socket
 *   }
 * } 
 *
 * The following method shuts down an ExecutorService in two phases, first by calling shutdown to reject 
 * incoming tasks, and then calling shutdownNow, if necessary, to cancel any lingering tasks:
 * 
 * void shutdownAndAwaitTermination(ExecutorService pool) {
 *   pool.shutdown(); // Disable new tasks from being submitted
 *   try {
 *     // Wait a while for existing tasks to terminate
 *     if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
 *       pool.shutdownNow(); // Cancel currently executing tasks
 *       // Wait a while for tasks to respond to being cancelled
 *       if (!pool.awaitTermination(60, TimeUnit.SECONDS))
 *           System.err.println("Pool did not terminate");
 *     }
 *   } catch (InterruptedException ie) {
 *     // (Re-)Cancel if current thread also interrupted
 *     pool.shutdownNow();
 *     // Preserve interrupt status
 *     Thread.currentThread().interrupt();
 *   }
 * }
 *
 * Memory consistency effects: Actions in a thread prior to the submission of a Runnable or Callable task to an
 * ExecutorService happen-before any actions taken by that task, which in turn happen-before the result is retrieved 
 * via Future.get().
 *
 * @since 1.5
 */
public interface ExecutorService extends Executor {

    /**
     * Initiates an orderly shutdown in which previously submitted
     * tasks are executed, but no new tasks will be accepted.
     * Invocation has no additional effect if already shut down.
     *
     * <p>This method does not wait for previously submitted tasks to
     * complete execution.  Use {@link #awaitTermination awaitTermination}
     * to do that.
     *
     * @throws SecurityException if a security manager exists and
     *         shutting down this ExecutorService may manipulate
     *         threads that the caller is not permitted to modify
     *         because it does not hold {@link
     *         java.lang.RuntimePermission}{@code ("modifyThread")},
     *         or the security manager's {@code checkAccess} method
     *         denies access.
     */
    void shutdown();

    /**
     * Attempts to stop all actively executing tasks, halts the
     * processing of waiting tasks, and returns a list of the tasks
     * that were awaiting execution.
     *
     * <p>This method does not wait for actively executing tasks to
     * terminate.  Use {@link #awaitTermination awaitTermination} to
     * do that.
     *
     * <p>There are no guarantees beyond best-effort attempts to stop
     * processing actively executing tasks.  For example, typical
     * implementations will cancel via {@link Thread#interrupt}, so any
     * task that fails to respond to interrupts may never terminate.
     *
     * @return list of tasks that never commenced execution
     * @throws SecurityException if a security manager exists and
     *         shutting down this ExecutorService may manipulate
     *         threads that the caller is not permitted to modify
     *         because it does not hold {@link
     *         java.lang.RuntimePermission}{@code ("modifyThread")},
     *         or the security manager's {@code checkAccess} method
     *         denies access.
     */
    List<Runnable> shutdownNow();

    /**
     * Returns {@code true} if this executor has been shut down.
     *
     * @return {@code true} if this executor has been shut down
     */
    boolean isShutdown();

    /**
     * Returns {@code true} if all tasks have completed following shut down.
     * Note that {@code isTerminated} is never {@code true} unless
     * either {@code shutdown} or {@code shutdownNow} was called first.
     *
     * @return {@code true} if all tasks have completed following shut down
     */
    boolean isTerminated();

    /**
     * Blocks until all tasks have completed execution after a shutdown
     * request, or the timeout occurs, or the current thread is
     * interrupted, whichever happens first.
     *
     * @param timeout the maximum time to wait
     * @param unit the time unit of the timeout argument
     * @return {@code true} if this executor terminated and
     *         {@code false} if the timeout elapsed before termination
     * @throws InterruptedException if interrupted while waiting
     */
    boolean awaitTermination(long timeout, TimeUnit unit)
        throws InterruptedException;

    /**
     * Submits a value-returning task for execution and returns a
     * Future representing the pending results of the task. The
     * Future's {@code get} method will return the task's result upon
     * successful completion.
     *
     * <p>
     * If you would like to immediately block waiting
     * for a task, you can use constructions of the form
     * {@code result = exec.submit(aCallable).get();}
     *
     * <p>Note: The {@link Executors} class includes a set of methods
     * that can convert some other common closure-like objects,
     * for example, {@link java.security.PrivilegedAction} to
     * {@link Callable} form so they can be submitted.
     *
     * @param task the task to submit
     * @param <T> the type of the task's result
     * @return a Future representing pending completion of the task
     * @throws RejectedExecutionException if the task cannot be
     *         scheduled for execution
     * @throws NullPointerException if the task is null
     */
    <T> Future<T> submit(Callable<T> task);

    /**
     * Submits a Runnable task for execution and returns a Future
     * representing that task. The Future's {@code get} method will
     * return the given result upon successful completion.
     *
     * @param task the task to submit
     * @param result the result to return
     * @param <T> the type of the result
     * @return a Future representing pending completion of the task
     * @throws RejectedExecutionException if the task cannot be
     *         scheduled for execution
     * @throws NullPointerException if the task is null
     */
    <T> Future<T> submit(Runnable task, T result);

    /**
     * Submits a Runnable task for execution and returns a Future
     * representing that task. The Future's {@code get} method will
     * return {@code null} upon <em>successful</em> completion.
     *
     * @param task the task to submit
     * @return a Future representing pending completion of the task
     * @throws RejectedExecutionException if the task cannot be
     *         scheduled for execution
     * @throws NullPointerException if the task is null
     */
    Future<?> submit(Runnable task);

    /**
     * Executes the given tasks, returning a list of Futures holding
     * their status and results when all complete.
     * {@link Future#isDone} is {@code true} for each
     * element of the returned list.
     * Note that a <em>completed</em> task could have
     * terminated either normally or by throwing an exception.
     * The results of this method are undefined if the given
     * collection is modified while this operation is in progress.
     *
     * @param tasks the collection of tasks
     * @param <T> the type of the values returned from the tasks
     * @return a list of Futures representing the tasks, in the same
     *         sequential order as produced by the iterator for the
     *         given task list, each of which has completed
     * @throws InterruptedException if interrupted while waiting, in
     *         which case unfinished tasks are cancelled
     * @throws NullPointerException if tasks or any of its elements are {@code null}
     * @throws RejectedExecutionException if any task cannot be
     *         scheduled for execution
     */
    <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
        throws InterruptedException;

    /**
     * Executes the given tasks, returning a list of Futures holding
     * their status and results
     * when all complete or the timeout expires, whichever happens first.
     * {@link Future#isDone} is {@code true} for each
     * element of the returned list.
     * Upon return, tasks that have not completed are cancelled.
     * Note that a <em>completed</em> task could have
     * terminated either normally or by throwing an exception.
     * The results of this method are undefined if the given
     * collection is modified while this operation is in progress.
     *
     * @param tasks the collection of tasks
     * @param timeout the maximum time to wait
     * @param unit the time unit of the timeout argument
     * @param <T> the type of the values returned from the tasks
     * @return a list of Futures representing the tasks, in the same
     *         sequential order as produced by the iterator for the
     *         given task list. If the operation did not time out,
     *         each task will have completed. If it did time out, some
     *         of these tasks will not have completed.
     * @throws InterruptedException if interrupted while waiting, in
     *         which case unfinished tasks are cancelled
     * @throws NullPointerException if tasks, any of its elements, or
     *         unit are {@code null}
     * @throws RejectedExecutionException if any task cannot be scheduled
     *         for execution
     */
    <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks,
                                  long timeout, TimeUnit unit)
        throws InterruptedException;

    /**
     * Executes the given tasks, returning the result
     * of one that has completed successfully (i.e., without throwing
     * an exception), if any do. Upon normal or exceptional return,
     * tasks that have not completed are cancelled.
     * The results of this method are undefined if the given
     * collection is modified while this operation is in progress.
     *
     * @param tasks the collection of tasks
     * @param <T> the type of the values returned from the tasks
     * @return the result returned by one of the tasks
     * @throws InterruptedException if interrupted while waiting
     * @throws NullPointerException if tasks or any element task
     *         subject to execution is {@code null}
     * @throws IllegalArgumentException if tasks is empty
     * @throws ExecutionException if no task successfully completes
     * @throws RejectedExecutionException if tasks cannot be scheduled
     *         for execution
     */
    <T> T invokeAny(Collection<? extends Callable<T>> tasks)
        throws InterruptedException, ExecutionException;

    /**
     * Executes the given tasks, returning the result
     * of one that has completed successfully (i.e., without throwing
     * an exception), if any do before the given timeout elapses.
     * Upon normal or exceptional return, tasks that have not
     * completed are cancelled.
     * The results of this method are undefined if the given
     * collection is modified while this operation is in progress.
     *
     * @param tasks the collection of tasks
     * @param timeout the maximum time to wait
     * @param unit the time unit of the timeout argument
     * @param <T> the type of the values returned from the tasks
     * @return the result returned by one of the tasks
     * @throws InterruptedException if interrupted while waiting
     * @throws NullPointerException if tasks, or unit, or any element
     *         task subject to execution is {@code null}
     * @throws TimeoutException if the given timeout elapses before
     *         any task successfully completes
     * @throws ExecutionException if no task successfully completes
     * @throws RejectedExecutionException if tasks cannot be scheduled
     *         for execution
     */
    <T> T invokeAny(Collection<? extends Callable<T>> tasks,
                    long timeout, TimeUnit unit)
        throws InterruptedException, ExecutionException, TimeoutException;
}

```