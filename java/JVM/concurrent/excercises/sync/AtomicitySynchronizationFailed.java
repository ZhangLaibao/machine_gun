package com.jr.test.tkij.conc.sync;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by PengXianglong on 2018/7/13.
 */
public class AtomicitySynchronizationFailed implements Runnable {

    private volatile int i = 0;

//    public int getValue() {
    public synchronized int getValue() {// should be synchronized
        return i;
    }

    private synchronized void evenIncre() {
        i++;
        i++;
    }

    @Override
    public void run() {
        while (true)
            evenIncre();
    }

    public static void main(String[] args) {
        ExecutorService pool = Executors.newCachedThreadPool();
        AtomicitySynchronizationFailed task = new AtomicitySynchronizationFailed();
        pool.execute(task);
        while (true) {
            int val = task.getValue();
            if (val % 2 != 0) {
                System.out.println(val);
                System.exit(0);
            }
        }
    }
}
