package com.jr.test.tkij.conc.locks;

/**
 * Created by PengXianglong on 2018/7/19.
 */
public class ReenrantSynchronizedLock {

    public static void main(String[] args) {

        final ReenrantSynchronizedLock lock = new ReenrantSynchronizedLock();

        new Thread(new Runnable() {
            @Override
            public void run() {
                lock.func1(10);
            }
        }).start();
    }


    public synchronized void func1(int count) {
        if (count-- > 0) {
            System.out.printf("func1 calls func2 with count %d\n", count);
            func2(count);
        }
    }


    public synchronized void func2(int count) {
        if (count-- > 0) {
            System.out.printf("func2 calls func1 with count %d\n", count);
            func1(count);
        }
    }
}
