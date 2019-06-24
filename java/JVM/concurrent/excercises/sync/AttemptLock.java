package com.jr.test.tkij.conc.sync;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by PengXianglong on 2018/7/13.
 */
public class AttemptLock {

    public static void main(String[] args) {

        final AttemptLock attemptLock = new AttemptLock();
        attemptLock.untimed();
        attemptLock.timed();

        new Thread() {

            {
                setDaemon(true);
            }

            @Override
            public void run() {
                attemptLock.lock.lock();
                System.out.println("Acquired");
            }
        }.start();

        Thread.yield();

        attemptLock.untimed();
        attemptLock.timed();

    }

    private ReentrantLock lock = new ReentrantLock();

    public void untimed() {
        boolean captured = lock.tryLock();
        try {
            System.out.println("tryLock(): " + captured);
        } finally {
            if (captured) lock.unlock();
        }
    }

    public void timed() {
        boolean captured = false;
        try {
            captured = lock.tryLock(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        try {
            System.out.println("tryLock(2, TimeUnits.SECONDS): " + captured);
        } finally {
            if (captured)
                lock.unlock();
        }
    }

}
