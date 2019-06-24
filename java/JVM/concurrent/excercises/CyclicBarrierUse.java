package com.jr.test.concurrent.practice;

import java.util.Random;
import java.util.concurrent.CyclicBarrier;

public class CyclicBarrierUse {

    public static void main(String[] args) throws InterruptedException {

        int count = 10;
        CyclicBarrier barrier = new CyclicBarrier(count);

        for (int i = 0; i < count; i++) {
            new WorkerThread(barrier).start();
        }
    }


    private static class WorkerThread extends Thread {

        private static Random random = new Random();
        private CyclicBarrier barrier;

        public WorkerThread(CyclicBarrier barrier) {
            this.barrier = barrier;
        }

        @Override
        public void run() {

            try {
                barrier.await();
            } catch (Exception e) {
                e.printStackTrace();
            }

            work(1);

            try {
                barrier.await();
            } catch (Exception e) {
                e.printStackTrace();
            }

            work(2);

            try {
                barrier.await();
            } catch (Exception e) {
                e.printStackTrace();
            }

            work(3);
        }

        private void work(int num) {
            int i = random.nextInt(1000);
            try {
                Thread.sleep(i);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println(Thread.currentThread().getName()
                    + " do work " + num
                    + " and cost : " + i + " milis");
        }
    }

}
