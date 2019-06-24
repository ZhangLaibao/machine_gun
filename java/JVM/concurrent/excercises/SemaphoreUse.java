package com.jr.test.concurrent.practice;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

public class SemaphoreUse {

    public static void main(String[] args) {

        Semaphore semaphore = new Semaphore(3);
        ExecutorService threadPool = Executors.newFixedThreadPool(10);
        for (int i = 0; i < 10; i++) {
            threadPool.submit(() -> {
                try {
                    semaphore.acquire();
                    System.out.println(Thread.currentThread().getName() + " : acquried the semaphore");

                    Thread.sleep(1000L);

                    semaphore.release();
                    System.out.println(Thread.currentThread().getName() + " : released the semaphore");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }
        threadPool.shutdown();
    }
}
