package com.jr.test.tkij.conc;

import java.util.concurrent.TimeUnit;

/**
 * Created by PengXianglong on 2018/7/12.
 */
public class DaemonThreadDontRunFinally {

    public static void main(String[] args) throws InterruptedException {
        Thread dae = new Thread(new DaemonWithFinally());
        dae.setDaemon(true);
        dae.start();
        TimeUnit.MILLISECONDS.sleep(100L);
    }
}

class DaemonWithFinally implements Runnable {

    @Override
    public void run() {
        System.out.println("Into this Daemon thread");
        try {
            TimeUnit.MILLISECONDS.sleep(1000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            System.out.println("See will this printed");
        }
    }
}