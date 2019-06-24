package com.jr.test.tkij.conc.ueh;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Created by PengXianglong on 2018/7/13.
 */
public class TestUncaughtException {

    public static void main(String[] args) {
        ExecutorService executor = Executors.newSingleThreadExecutor(new ThreadFactoryWithUncaughtExceptionHandler());
        executor.execute(new Runnable() {
            @Override
            public void run() {
                int i = 1 / 0;
            }
        });

        executor.shutdown();
    }

}

class ThreadFactoryWithUncaughtExceptionHandler implements ThreadFactory {

    @Override
    public Thread newThread(Runnable r) {
        Thread t = new Thread(r);
        t.setUncaughtExceptionHandler(new LoggedUncaughtExceptionHandler());
        return t;
    }
}

class LoggedUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {

    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        System.out.println("Thread: " + thread.getName() + " caught exception: " + throwable);
    }
}
