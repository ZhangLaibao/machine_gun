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
系统资源，每个线程在执行任务时任务本身消耗的资源也应该考虑在内。
另外，等待线程处理的任务会被放到等待队列里，当等待的任务堆积过多，如果不能处理队列的大小，那么系统资源也会被耗尽。
3. 线程通讯错误   
JDK原生的线程调度API在使用上难度较高，如果不能合理使用，那么错误在所难免。    
4. 线程泄露    
当线程池中某个线程在执行任务时抛出了RuntimeException或者Error而没有被捕获时，这个线程会退出，那么线程池中的线程资源就少了一个。
当这种情况积累起来时，线程池会慢慢被耗尽。另外一种情况，当线程处理的任务长时间处于阻塞时，这个线程不能被返还给池，
那么相当于池中资源也减少了一个。
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
 * Many Executor implementations impose some sort of limitation on how and when tasks are scheduled. The executor below
 * serializes the submission of tasks to a second executor, illustrating a composite executor.
 * 
 * class SerialExecutor implements Executor {
 *   final Queue<Runnable> tasks = new ArrayDeque<Runnable>();
 *   final Executor executor;
 *   Runnable active;
 *
 *   SerialExecutor(Executor executor) {
 *     this.executor = executor;
 *   }
 *
 *   public synchronized void execute(final Runnable r) {
 *     tasks.offer(new Runnable() {
 *       public void run() {
 *         try {
 *           r.run();
 *         } finally {
 *           scheduleNext();
 *         }
 *       }
 *     });
 *     if (active == null) {
 *       scheduleNext();
 *     }
 *   }
 *
 *   protected synchronized void scheduleNext() {
 *     if ((active = tasks.poll()) != null) {
 *       executor.execute(active);
 *     }
 *   }
 * }
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