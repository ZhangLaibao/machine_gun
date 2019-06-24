package com.jr.test.concurrent.practice;

import java.util.concurrent.TimeUnit;

/**
 * ·避免一个线程同时获取多个锁。
 * ·避免一个线程在锁内同时占用多个资源，尽量保证每个锁只占用一个资源。
 * ·尝试使用定时锁，使用lock.tryLock（timeout）来替代使用内部锁机制。
 * ·对于数据库锁，加锁和解锁必须在一个数据库连接里，否则会出现解锁失败的情况。
 */
public class AnotherDeadLock {

    public static void main(String[] args) {
        Object lock1 = new Object(), lock2 = new Object();
        new Thread(() -> {
            synchronized (lock1) {
                try {
                    TimeUnit.MILLISECONDS.sleep(1000L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                synchronized (lock2) {

                }
            }
        }).start();
        new Thread(() -> {
            synchronized (lock2) {
                try {
                    TimeUnit.MILLISECONDS.sleep(800L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                synchronized (lock1) {

                }
            }
        }).start();
    }

}
