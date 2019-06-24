package com.jr.test.concurrent.practice;

public class SleepInterrupt {

    public static void main(String[] args) throws InterruptedException {

        Thread thread = new Thread(() -> {
            while (true) {
                // 在线程run方法中需要显式处理线程中断，否则即使线程被中断，程序也不会有任何响应
                if (Thread.currentThread().isInterrupted()) {
                    System.out.println("thread has been interrupted");
                    break;
                }

                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException e) {
                    System.out.println("Sleep has been interrupted");
                    // 在Thread.sleep()过程中被中断，会清除当前线程的中断标记
                    // 由于在run方法中使用此中断标记作为退出条件，故需要重置此中断标记
                    Thread.currentThread().interrupt();
                }
                Thread.yield();
            }
        });

        thread.start();
        Thread.sleep(500L);
        thread.interrupt();
        System.out.println("I have had thread interrupt status set");
    }

}
