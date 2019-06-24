package com.jr.test.concurrent.practice;

public class ThreadPriority {

    static int i = 0;

    public static void main(String[] args) throws InterruptedException {
        Thread threadHigh = new PriorityThread();
        threadHigh.setPriority(Thread.MAX_PRIORITY);
        Thread threadLow = new PriorityThread();
        threadLow.setPriority(Thread.MIN_PRIORITY);

        threadHigh.start();
        threadLow.start();
    }

    private static class PriorityThread extends Thread {
        @Override
        public void run() {
            while (true) {
                if (i > 1000000) {
                    System.out.printf("Thread of priority %d complete \r\n",
                            Thread.currentThread().getPriority());
                    break;
                }
                i++;
            }
        }
    }
}
