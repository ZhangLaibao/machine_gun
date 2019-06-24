package com.jr.test.concurrent.practice;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * +--------+-------+-------+
 * |        | read  | write |
 * +--------+-------+-------+
 * | read   | NO    | yes   |
 * +----------------+-------+
 * | write  | yes   | NO    |
 * +----------------+-------+
 *
 */
public class ReadWriteLockUse {

    public static void main(String[] args) {

        Resource resource = new Resource();

        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                resource.read();
            }).start();
        }

        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                resource.write();
            }).start();
        }
    }
}

class Resource {

//    private ReentrantLock lock = new ReentrantLock();
    private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final Lock readLock = readWriteLock.readLock();    //读锁
    private final Lock writeLock = readWriteLock.writeLock();    //写锁

    public void read() {
        try {
//            lock.lock();
            readLock.lock();
            System.out.println(Thread.currentThread().getName() + " : reading ................");
            Thread.sleep(1000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {

            readLock.unlock();
//            lock.unlock();
        }
    }

    public void write() {
        try {
//            lock.lock();
            writeLock.lock();
            System.out.println(Thread.currentThread().getName() + " : writing ................");
            Thread.sleep(1000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
//            lock.unlock();
            writeLock.unlock();
        }
    }

}
