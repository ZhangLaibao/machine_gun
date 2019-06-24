package com.jr.test.concurrent.practice;

import java.util.concurrent.TimeUnit;

public class InterruptedReview {

    public static void main(String[] args) {
        Thread sleepThread = new Thread(new SleepRunner(), "sleep thread");
        sleepThread.setDaemon(true);

        Thread busiThread = new Thread(new BusyRunner(), "busy thread");
        busiThread.setDaemon(true);

        sleepThread.start();
        busiThread.start();

        sleepSeconds(5);

        sleepThread.interrupt();
        busiThread.interrupt();

        System.out.println("SleepThread interrupted is " + sleepThread.isInterrupted());
        System.out.println("BusyThread interrupted is " + busiThread.isInterrupted());

        sleepSeconds(5);

    }

    static class SleepRunner implements Runnable {
        @Override
        public void run() {
            while (true) sleepSeconds(1);
        }
    }

    static class BusyRunner implements Runnable {
        @Override
        public void run() {
            while (true) {
            }
        }
    }

    static void sleepSeconds(int seconds) {
        try {
            TimeUnit.SECONDS.sleep(seconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
