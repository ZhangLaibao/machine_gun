package com.jr.test.tkij.conc.communic;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by PengXianglong on 2018/7/19.
 */
public class NotifyAndNotifyAll {

    public static void main(String[] args) throws InterruptedException {
        ExecutorService pool = Executors.newCachedThreadPool();

        for (int i = 0; i < 5; i++)
            pool.execute(new TaskA());
        pool.execute(new TaskB());

        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            boolean prod = true;

            @Override
            public void run() {
                if (prod) {
                    System.out.print("\nnotify() ");
                    TaskA.blocker.prod();
                    prod = false;
                } else {
                    System.out.print("\nnotifyAll() ");
                    TaskA.blocker.prodAll();
                    prod = true;
                }
            }
        }, 400, 400);

        TimeUnit.SECONDS.sleep(5);
        timer.cancel();
        System.out.println("\nTimer cancelled");
        TimeUnit.MILLISECONDS.sleep(500);
        System.out.println("TaskB.blocker.prodAll() ");
        TaskB.blocker.prodAll();
        TimeUnit.MILLISECONDS.sleep(500);
        System.out.println("\nShutting down");
        pool.shutdownNow();
    }

}

class TaskB implements Runnable {

    static Blocker blocker = new Blocker();

    @Override
    public void run() {
        blocker.waitingCall();
    }
}

class TaskA implements Runnable {

    static Blocker blocker = new Blocker();

    @Override
    public void run() {
        blocker.waitingCall();
    }
}

class Blocker {

    synchronized void waitingCall() {
        try {
            while (!Thread.interrupted()) {
                wait();
                System.out.print(Thread.currentThread() + " ");
            }
        } catch (InterruptedException ignored) {
        }
    }

    synchronized void prod() {
        notify();
    }

    synchronized void prodAll() {
        notifyAll();
    }

}