package com.jr.test.tkij.conc;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by PengXianglong on 2018/7/12.
 */
public class PrioritySample implements Runnable {

    private int countDown = 5;

    private volatile double d;

    private int priority;

    public PrioritySample(int priority) {
        this.priority = priority;
    }

    @Override
    public String toString() {
        return Thread.currentThread() + " : " + countDown;
    }

    @Override
    public void run() {
        Thread.currentThread().setPriority(priority);
        while (true) {
            for (int i = 0; i < 1000000; i++) {
                d += (Math.PI + Math.E) / (double) i;
                if (i % 1000 == 0) {
                    Thread.yield();
                }
            }
            System.out.println(this);
            if (--countDown == 0) return;
        }
    }

    public static void main(String[] args) {
        ExecutorService threadPool = Executors.newCachedThreadPool();
        for (int i = 0; i < 5; i++)
            threadPool.execute(new PrioritySample(Thread.MIN_PRIORITY));

        threadPool.execute(new PrioritySample(Thread.MAX_PRIORITY));
        threadPool.shutdown();
    }
}
