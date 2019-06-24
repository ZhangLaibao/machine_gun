package com.jr.test.tkij.conc.sync;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by PengXianglong on 2018/7/13.
 */
public class AtomicitySynchronization implements Runnable {

    private AtomicInteger i = new AtomicInteger(0);

    public int getValue() {// should be synchronized
        return i.get();
    }

    private void evenIncre() {
        i.addAndGet(2);
    }

    @Override
    public void run() {
        while (true)
            evenIncre();
    }

    public static void main(String[] args) {
        ExecutorService pool = Executors.newCachedThreadPool();
        AtomicitySynchronization task = new AtomicitySynchronization();
        pool.execute(task);

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                System.out.println("Aborting");
                System.exit(0);
            }
        }, 5000);

        while (true) {
            int val = task.getValue();
            if (val % 2 != 0) {
                System.out.println(val);
                System.exit(0);
            }
        }
    }
}
