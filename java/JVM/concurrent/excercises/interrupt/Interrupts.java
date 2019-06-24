package com.jr.test.tkij.conc.interrupt;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Created by PengXianglong on 2018/7/16.
 */
public class Interrupts {

    private static ExecutorService pool = Executors.newCachedThreadPool();

    static void test(Runnable r) throws InterruptedException {
        Future<?> future = pool.submit(r);
        TimeUnit.MILLISECONDS.sleep(1000);
        System.out.println("Interrupting " + r.getClass().getName());
        future.cancel(true);
        System.out.println("Interrupt send to " + r.getClass().getName());
    }

    public static void main(String[] args) throws InterruptedException {
        test(new SleepBlocked());
        test(new IOBlocked(System.in));
        test(new SynchronizedBlocked());
        TimeUnit.SECONDS.sleep(3);
        System.out.println("Aborting with System.exit(0)");
        System.exit(0);
    }
}

class SleepBlocked implements Runnable {
    @Override
    public void run() {
        try {
            TimeUnit.SECONDS.sleep(100);
        } catch (InterruptedException e) {
            System.out.println("Sleep interrupted");
        }
        System.out.println("Exiting SleepBlocked.run()");
    }
}

class IOBlocked implements Runnable {
    private InputStream in;

    public IOBlocked(InputStream in) {
        this.in = in;
    }

    @Override
    public void run() {
        System.out.println("Waiting from read:");
        try {
            in.read();
        } catch (IOException e) {
            if (Thread.currentThread().isInterrupted()) {
                System.out.println("Interrupted from blocked I/O");
            } else {
                throw new RuntimeException();
            }
        }
        System.out.println("Exiting IODBlocked.run()");
    }
}

class SynchronizedBlocked implements Runnable {
    public synchronized void func() {
        while (true)
            Thread.yield();
    }

    public SynchronizedBlocked() {
        new Thread() {
            @Override
            public void run() {
                func();
            }
        }.start();
    }

    @Override
    public void run() {
        System.out.println("Trying to call func()");
        func();
        System.out.println("Exiting SynchronizedBlocked.run()");
    }
}
