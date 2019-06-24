package com.jr.test.tkij.conc;

import java.util.concurrent.TimeUnit;

/**
 * Created by PengXianglong on 2018/7/12.
 */
public class DaemonSample implements Runnable {

    public static void main(String[] args) throws InterruptedException {
        for (int i = 0; i < 10; i++) {
            Thread daemon = new Thread(new DaemonSample());
            daemon.setDaemon(true);
            daemon.start();
        }

        System.out.println("All daemon thread starts");
//        TimeUnit.MILLISECONDS.sleep(175);
        TimeUnit.MILLISECONDS.sleep(50);
    }

    @Override
    public void run() {
        try {
            while (true) {
                TimeUnit.MILLISECONDS.sleep(100);
                System.out.println(Thread.currentThread() + " " + this);
            }
        } catch (InterruptedException e) {
            System.out.println("sleep() interrupted for " + e.getMessage());
        }
    }

}
