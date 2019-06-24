package com.jr.test.concurrent.practice;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadPoolNotes {

    /**
     * 线程池线程数量的确定：
     *      Nthreads = Ncpu * Ucpu * (1 + W/C)
     * 其中：
     *      Ncpu：CPU数量
     *      Ucpu：期望的CPU使用率
     *      W/C：等待时间/计算时间
     */

    //核心线程数量
    private int corePoolSize;
    // 最大线程数量
    private int maximumPoolSize;
    // 空闲线程存活时间，配合单位使用
    private long keepAliveTime;

    // 用于新建现成的线程工厂
    private ThreadFactory threadFactory = null;

    /**
     * 用于存放和缓冲新提交的任务，使用盛放Runnable类型对象的阻塞队列实现，一般支持一下几种：
     * 
     * 同步队列SynchronousQueue：
     * 无容量的阻塞队列，每一个入队操作都需要一个相应的出队操作，同理每一个出队操作都需要一个对应的入队操作。
     * 所以提交的任务不会被真正的保存在队列里，而是"直接"交给线程池执行。当没有空闲线程时会创建新线程直至线程数达到maximumPoolSize。
     * 然后执行拒绝策略。所以使用SynchronousQueue时，如果任务繁忙，需要设置较大的maximumPoolSize，否则容易拒绝任务执行。
     * 
     * 有界队列，如ArrayBlockingQueue:
     * 在创建时需要指定队列的大小。新任务提交到有界队列时，如果线程池线程数量小于corePoolSize，会优先创建新线程，直到达到corePoolSize。
     * 然后新提交的任务优先再队列中缓存，直到队列已满，然后创建新线程直到线程数达到maximumPoolSize。到达maximumPoolSize后再有新任务
     * 提交到已经满了的有界队列时会触发拒绝策略。
     * 使用有界队列时，只有在队列满载的情况下才会将线程数量推到corePoolSize之上。
     * 所以如果不是系统任务十分繁忙，线程池数量会稳定在corePoolSize
     * 
     * 无界队列，如LinkedBlockingQueue:
     * 在任务十分繁忙的时候，无界队列会无限缓存新提交的任务直至系统资源耗尽，而线程池线程数会维持在corePoolSize。
     * 使用无界队列虽然不会造成新任务被拒绝，但在系统繁忙的时候会造成资源耗尽。
     * 
     * 优先级队列，如PriorityBlockingQueue:
     * 上述有界队列或者无界队列都是按照先进先出的顺序将任务提交给线程池执行，但是如PriorityBlockingQueue，提供了对优先级逻辑的支持
     * PriorityBlockingQueue也是一个无界队列，可以确保系统性能的同时保证优先级高的任务优先被执行。
     */
    private BlockingQueue<Runnable> queue = null;

    /**
     * 拒绝策略，即队列满之后如何处理新任务，JDK内置如下几种策略：
     *
     * AbortPolicy：直接抛出异常，阻止系统正常工作
     * CallerRunsPolicy：使用提交任务的线程自己执行任务，此行为会极大的拖慢系统响应速度
     * DiscardPolicy：默默丢弃，丢弃新提交进来的任务 - 在允许任务丢失的情况下很实用
     * DiscardOldestPolicy：默默丢弃，丢弃最老的任务 - 在允许任务丢失的情况下很实用
     */
    private RejectedExecutionHandler rejectPolicy = null;

    /**
     * 创建线程池的核心构造方法
     */
    public ThreadPoolExecutor executor = new ThreadPoolExecutor(corePoolSize, maximumPoolSize,
            keepAliveTime, TimeUnit.MILLISECONDS, queue, threadFactory, rejectPolicy);

    /**
     * 将corePoolSize和maximumPoolSize设置为相同的值，使用LinkedBlockingQueue
     * 在系统繁忙的时候会造成队列的无限制增长，造成内存耗尽。
     */
    ExecutorService fixed = Executors.newFixedThreadPool(corePoolSize);

    /**
     * 即size=1的fixedThreadPool
     */
    ExecutorService single= Executors.newSingleThreadExecutor();

    /**
     * corePoolSize=0
     * maximumPoolSize=无穷大
     * 使用SynchronousQueue，在大量任务提交进来时会不断创建新线程造成系统资源耗尽
     */
    ExecutorService cached = Executors.newCachedThreadPool();

}
