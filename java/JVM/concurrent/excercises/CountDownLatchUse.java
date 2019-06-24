package com.jr.test.concurrent.practice;

import java.util.concurrent.CountDownLatch;

public class CountDownLatchUse {

    public static void main(String[] args) throws InterruptedException {

        int count = 10;
        CountDownLatch latch = new CountDownLatch(count);
        for (int i = 0; i < count; i++)
            new CountDownThread(latch).start();

        System.out.println("waiting for sub work end");
        latch.await();
        System.out.println("sub work ended");

    }

    private static class CountDownThread extends Thread {

        private static CountDownLatch latch;

        public CountDownThread(CountDownLatch latch) {
            CountDownThread.latch = latch;
        }

        @Override
        public void run() {
            latch.countDown();
            System.out.println("do sth.");

            try {
                Thread.sleep(1000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}
