package com.jr.test.concurrent.practice;

public class ThreadInterrupt {

    public static void main(String[] args) throws InterruptedException {

        Thread thread = new Thread(() -> {
            while (true) {
                // 在线程run方法中需要显式处理线程中断，否则即使线程被中断，程序也不会有任何响应
                if (Thread.currentThread().isInterrupted()) {
                    System.out.println("thread has been interrupted");
                    break;
                }
                Thread.yield();
            }
        });

        thread.start();
        Thread.sleep(1000L);
        thread.interrupt();
        System.out.println("I have had thread interrupt status set");
    }

}
