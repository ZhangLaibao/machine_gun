package com.jr.test.concurrent.practice;

public class SimpleWaitNotify {

    final static Object lock = new Object();

    public static void main(String[] args) {


        new Thread(() -> {
            synchronized (lock) {
                System.out.println(System.currentTimeMillis() + " : waitThread acquired the lock and start");
                try {
                    // wait()与sleep()的不同之处在于
                    // wait会释放锁
                    // sleep不会释放锁
                    lock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                // 这行代码会等到notify()所在线程释放锁才能被执行
                System.out.println(System.currentTimeMillis() + " : waitThread released the lock and stop");
            }
        }).start();

        new Thread(() -> {
            synchronized (lock) {
                System.out.println(System.currentTimeMillis() + " : notifyThread acquired the lock and start");
                lock.notify();
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println(System.currentTimeMillis() + " : notifyThread released the lock and stop");
            }
        }).start();

    }

}
