package com.jr.test.concurrent.practice;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class ReentrantLockVSSynchronized {

    /**
     * ReentrantLock和synchronized对比：
     * 1.性能：差不多，在JDK1.6之前，ReentrantLock性能远远好于synchronized
     * 但是JDK1.6对synchronized做了大量优化，是其性能与ReentrantLock性能接近
     * <p>
     * 2.编码：synchronized的锁边界由java语法中的{}决定，无需显式释放锁
     * ReentrantLock的加锁解锁都需要显式方法调用，灵活性较好，并且解锁方法最好放在finally语句块种，确保锁一定能被释放
     * <p>
     * 3.实现：synchronized内嵌在java语法中，由jvm实现
     * RentrantLock只是标准库提供的一个api
     */
    public static void main(String[] args) throws InterruptedException {
        // 4.ReentrantLock可以响应中断，使用lockInterruptibly()方法
        //        DeadLockThread t1 = new DeadLockThread(true);
        //        DeadLockThread t2 = new DeadLockThread(false);
        //
        //        t1.start();
        //        t2.start();
        //
        //        Thread.sleep(2000L);
        //        t1.interrupt();
        //

        // 5.ReentrantLock可以设置获取锁超时时间，使用tryLock方法
        //        TimedDeadLockThread t1 = new TimedDeadLockThread(true);
        //        TimedDeadLockThread t2 = new TimedDeadLockThread(false);
        //
        //        t1.start();
        //        t2.start();

        // 6.ReentrantLock可以指定公平/非公平策略
        //        IsFairLockThread t1 = new IsFairLockThread(false);
        //        IsFairLockThread t2 = new IsFairLockThread(false);
        //        t1.start();t2.start();

        // 7.ReentrantLock可以搭配Condition使用，类比synchronized与wait(),notify()搭配

        // 8.在虚拟机规范中我们可以查阅到，synchronized是通过monitor指令实现的，
        // synchronized的加锁解锁对应monitorenter和moniterexit指令
        // Reentrantlock通过AQS实现，而AQS底层是cas无锁实现
    }
}

class IsFairLockThread extends Thread {

    private ReentrantLock lock;

    public IsFairLockThread(boolean fair) {
        this.lock = new ReentrantLock(fair);
    }

    @Override
    public void run() {
        while (true) {
            try {
                lock.lock();
                System.out.println(Thread.currentThread().getName() + " : acquired lock");
            } finally {
                lock.unlock();
            }
        }
    }
}

class TimedDeadLockThread extends Thread {

    private static ReentrantLock lock1 = new ReentrantLock();
    private static ReentrantLock lock2 = new ReentrantLock();

    private boolean order;

    public TimedDeadLockThread(boolean order) {
        this.order = order;
    }

    @Override
    public void run() {
        try {
            if (order) {
                /**
                 * 使用tryLock()方法会立即返回，或者等待指定时间，即使在死锁情况下也可以在指定时间返回
                 */
                if (lock1.tryLock(5000, TimeUnit.MILLISECONDS)) {
                    Thread.sleep(1000L);
                    if (lock2.tryLock(5000, TimeUnit.MILLISECONDS)) {
                        System.out.println(Thread.currentThread().getName() + " : Job done");
                        lock2.unlock();
                    }
                    lock1.unlock();
                }
            } else {
                if (lock2.tryLock(5000, TimeUnit.MILLISECONDS)) {
                    Thread.sleep(1000L);
                    if (lock1.tryLock(5000, TimeUnit.MILLISECONDS)) {
                        System.out.println(Thread.currentThread().getName() + " : Job done");
                        lock1.unlock();
                    }
                    lock2.unlock();
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

class DeadLockThread extends Thread {

    private static ReentrantLock lock1 = new ReentrantLock();
    private static ReentrantLock lock2 = new ReentrantLock();

    private boolean order;

    public DeadLockThread(boolean order) {
        this.order = order;
    }

    @Override
    public void run() {
        try {
            /**
             * 在不同线程种对几把锁使用不同的加锁顺序，极易造成死锁
             * 使用jstack [pid]命令可以查看死锁：
             *
             * Found one Java-level deadlock:
             * =============================
             * "Thread-1":
             *   waiting for ownable synchronizer 0x00000000d621d4e0, (a java.util.concurrent.locks.ReentrantLock$NonfairSync),
             *   which is held by "Thread-0"
             * "Thread-0":
             *   waiting for ownable synchronizer 0x00000000d621d510, (a java.util.concurrent.locks.ReentrantLock$NonfairSync),
             *   which is held by "Thread-1"
             *
             * Java stack information for the threads listed above:
             * ===================================================
             * "Thread-1":
             *      at sun.misc.Unsafe.park(Native Method)
             *      - parking to wait for  <0x00000000d621d4e0> (a java.util.concurrent.locks.ReentrantLock$NonfairSync)
             *      at java.util.concurrent.locks.LockSupport.park(LockSupport.java:175)
             *      at java.util.concurrent.locks.AbstractQueuedSynchronizer.parkAndCheckInterrupt(AbstractQueuedSynchronizer.java:836)
             *      at java.util.concurrent.locks.AbstractQueuedSynchronizer.doAcquireInterruptibly(AbstractQueuedSynchronizer.java:897)
             *      at java.util.concurrent.locks.AbstractQueuedSynchronizer.acquireInterruptibly(AbstractQueuedSynchronizer.java:1222)
             *      at java.util.concurrent.locks.ReentrantLock.lockInterruptibly(ReentrantLock.java:335)
             *      at com.jr.test.concurrent.practice.LockThread.run(ReentrantLockVSSynchronized.java:54)
             * "Thread-0":
             *      at sun.misc.Unsafe.park(Native Method)
             *      - parking to wait for  <0x00000000d621d510> (a java.util.concurrent.locks.ReentrantLock$NonfairSync)
             *      at java.util.concurrent.locks.LockSupport.park(LockSupport.java:175)
             *      at java.util.concurrent.locks.AbstractQueuedSynchronizer.parkAndCheckInterrupt(AbstractQueuedSynchronizer.java:836)
             *      at java.util.concurrent.locks.AbstractQueuedSynchronizer.doAcquireInterruptibly(AbstractQueuedSynchronizer.java:897)
             *      at java.util.concurrent.locks.AbstractQueuedSynchronizer.acquireInterruptibly(AbstractQueuedSynchronizer.java:1222)
             *      at java.util.concurrent.locks.ReentrantLock.lockInterruptibly(ReentrantLock.java:335)
             *      at com.jr.test.concurrent.practice.LockThread.run(ReentrantLockVSSynchronized.java:50)
             *
             * Found 1 deadlock.
             */
            if (order) {
                lock1.lockInterruptibly();
                Thread.sleep(1000L);
                lock2.lockInterruptibly();
            } else {
                lock2.lockInterruptibly();
                Thread.sleep(1000L);
                lock1.lockInterruptibly();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            if (lock1.isHeldByCurrentThread()) lock1.unlock();
            if (lock2.isHeldByCurrentThread()) lock2.unlock();
        }
    }
}