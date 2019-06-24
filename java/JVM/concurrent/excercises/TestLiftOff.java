package com.jr.test.tkij.conc;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by PengXianglong on 2018/7/12.
 */
public class TestLiftOff {

    public static void main(String[] args) {
        int threadNUm = Runtime.getRuntime().availableProcessors() + 1;

//        for (int i = 0; i < threadNUm; i++) {
//            new Thread(new LiftOff()).start();
//        }

        ExecutorService threadPool = Executors.newCachedThreadPool();
        for (int i = 0; i < threadNUm; i++)
            threadPool.execute(new LiftOff());
        threadPool.shutdown();

//        ExecutorService threadPool = Executors.newSingleThreadExecutor();
//        for (int i = 0; i < threadNUm; i++)
//            threadPool.execute(new LiftOff());
//        threadPool.shutdown();
    }

}

class LiftOff implements Runnable {

    protected int countDown = 10;

    private static int tasks = 0;

    private final int id = tasks++;

    public LiftOff() {
    }

    public LiftOff(int countDown) {
        this.countDown = countDown;
    }

    public String status() {
        return "#" + id + "(" + (countDown > 0 ? countDown : "LiftOff!") + "),";
    }

    @Override
    public void run() {
        while (countDown-- > 0) {
            System.out.print(status());
//            Thread.yield();
            try {// JAVA SE5 new style of sleep
                TimeUnit.MILLISECONDS.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}