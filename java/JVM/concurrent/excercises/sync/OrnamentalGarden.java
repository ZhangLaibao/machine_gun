package com.jr.test.tkij.conc.sync;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by PengXianglong on 2018/7/15.
 */
public class OrnamentalGarden {

    public static void main(String[] args) throws InterruptedException {
        ExecutorService pool = Executors.newCachedThreadPool();
        for (int i = 0; i < 5; i++)
            pool.execute(new Entrance(i));
        TimeUnit.MILLISECONDS.sleep(100);
        Entrance.cancel();
        pool.shutdown();
        if (!pool.awaitTermination(200, TimeUnit.MILLISECONDS))
            System.out.println("Some task was not terminated");
        System.out.println("Total: " + Entrance.getTotalCount());
        System.out.println("Sum of Entrances: " + Entrance.sumEntrances());
    }
}

class Entrance implements Runnable {

    private static Count count = new Count();
    private static List<Entrance> entrances = new ArrayList<>();

    private int number;
    private final int id;

    private static volatile boolean canceled = false;

    public static void cancel() {
        canceled = true;
    }

    public Entrance(Integer i) {
        this.id = i;
        entrances.add(this);
    }

    @Override
    public void run() {
        while (!canceled) {
            synchronized (this) {
                ++number;
            }
            System.out.println(this + " total: " + count.increment());

            try {
                TimeUnit.MILLISECONDS.sleep(100);
            } catch (InterruptedException e) {
                System.out.println("Sleep interrupted");
            }
        }
        System.out.println("Stopping: " + this);
    }

    public synchronized int getValue() {
        return number;
    }

    @Override
    public String toString() {
        return "Entrance: " + id + ": " + getValue();
    }

    public static int getTotalCount() {
        return count.value();
    }


    public static int sumEntrances() {
        int sum = 0;
        for (Entrance entrance : entrances)
            sum += entrance.getValue();
        return sum;
    }
}

class Count {

    private int count = 0;

    private Random rand = new Random(47);

    public synchronized int increment() {
        int temp = count;
        if (rand.nextBoolean())
            Thread.yield();
        return (count = ++temp);
    }

    public synchronized int value() {
        return count;
    }

}
