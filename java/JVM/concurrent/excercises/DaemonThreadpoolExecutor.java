package com.jr.test.tkij.conc;

import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by PengXianglong on 2018/7/12.
 */
public class DaemonThreadpoolExecutor extends ThreadPoolExecutor {

    public DaemonThreadpoolExecutor() {
        super(0, 100, 100L,
                TimeUnit.SECONDS, new SynchronousQueue<Runnable>(), new DaemonThreadFactorySample());
    }
}

class Daemon implements Runnable {

    private Thread[] ts = new Thread[10];

    @Override
    public void run() {
        for (int i = 0; i < ts.length; i++) {
            ts[i] = new Thread(new DaemonSpawn());
            ts[i].start();
            System.out.println("DaemonSpawn " + i + " started. ");
        }

        for (int i = 0; i < ts.length; i++)
            System.out.println("t[" + i + "].isDaemon = " + ts[i].isDaemon() + ". ");

        while (true)
            Thread.yield();

    }
}

class DaemonSpawn implements Runnable {
    @Override
    public void run() {
        while (true)
            Thread.yield();
    }
}

class Daemons{
    public static void main(String[] args) throws InterruptedException {
        Thread th = new Thread(new Daemon());
        th.setDaemon(true);
        th.start();

        System.out.println("th.isDaemon() = " + th.isDaemon() + ". ");
        TimeUnit.SECONDS.sleep(1);
    }
}