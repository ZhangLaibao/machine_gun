package com.jr.test.tkij.conc.locks;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by PengXianglong on 2018/7/19.
 */
public class InterruptibleBlock {

    public static void main(String[] args) throws InterruptedException {
        Thread th = new Thread(new UseLock());
        th.start();
        TimeUnit.MILLISECONDS.sleep(1000);
        System.out.println("Issuing t.interrupt");
        th.interrupt();
    }
}

class BlockMutex {

    private Lock lock = new ReentrantLock();

    public BlockMutex() {
        lock.lock();
    }

    public void test() {
        try {
            lock.lockInterruptibly();
            System.out.println("Acquired lock in BlockMutex.test()");
        } catch (InterruptedException e) {
            System.out.printf("Thread %s interrupted for %s \n", Thread.currentThread().getName(), e.getMessage());
        }
    }
}

class UseLock implements Runnable {

    private BlockMutex blockMutex = new BlockMutex();

    @Override
    public void run() {
        System.out.println("Waiting for test() in BlockMutex");
        blockMutex.test();
        System.out.println("Broken out of blocked call");
    }
}
