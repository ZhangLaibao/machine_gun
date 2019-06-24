package com.jr.test.tkij.conc;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * Created by PengXianglong on 2018/7/12.
 */
public class DaemonThreadFactorySample implements ThreadFactory {
    @Override
    public Thread newThread(Runnable r) {
        Thread t = new Thread(r);
        t.setDaemon(true);
        return t;
    }
}

class DaemonFromFactory implements Runnable {
    public static void main(String[] args) throws InterruptedException {
        ExecutorService threadPool = Executors.newCachedThreadPool(new DaemonThreadFactorySample());
        for (int i = 0; i < 10; i++)
            threadPool.execute(new DaemonFromFactory());

        System.out.println("All daemon threads have started");
        TimeUnit.MILLISECONDS.sleep(1000);
    }

    @Override
    public void run() {
        try {
            while (true) {
                TimeUnit.MILLISECONDS.sleep(100);
                System.out.println(Thread.currentThread() + " " + this);
            }
        } catch (InterruptedException e) {
            System.out.println("Thread interrupted for " + e.getMessage());
        }
    }
}
