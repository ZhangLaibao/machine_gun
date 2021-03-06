package com.jr.test.tkij.conc.communic;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by PengXianglong on 2018/7/22.
 */
public class DelayedQueueSample {

    public static void main(String[] args) {
        Random random = new Random(47);
        ExecutorService pool = Executors.newCachedThreadPool();
        DelayQueue<DelayedTask> queue = new DelayQueue<>();

        for (int i = 0; i < 20; i++)
            queue.put(new DelayedTask(random.nextInt(5000)));

        queue.add(new DelayedTask.EndSentinel(5000, pool));
        pool.execute(new DelayedTaskConsumer(queue));
    }

}

class DelayedTaskConsumer implements Runnable {

    private DelayQueue<DelayedTask> q;

    public DelayedTaskConsumer(DelayQueue<DelayedTask> q) {
        this.q = q;
    }

    @Override
    public void run() {
        try {
            while (!Thread.interrupted()) {
                q.take().run();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Finished DelayedTaskConsumer");
    }
}

class DelayedTask implements Runnable, Delayed {

    private static int counter = 0;
    private final int id = counter++;
    private final int delta;
    private final long trigger;

    protected static List<DelayedTask> sequence = new ArrayList<>();

    public DelayedTask(int delayInMilis) {
        this.delta = delayInMilis;
        this.trigger = System.nanoTime() + TimeUnit.NANOSECONDS.convert(delta, TimeUnit.MICROSECONDS);
        sequence.add(this);
    }

    @Override
    public long getDelay(TimeUnit unit) {
        return unit.convert(trigger - System.nanoTime(), TimeUnit.NANOSECONDS);
    }

    @Override
    public int compareTo(Delayed o) {
        DelayedTask that = (DelayedTask) o;
        if (trigger < that.trigger) return -1;
        if (trigger > that.trigger) return 1;
        return 0;
    }

    @Override
    public void run() {
        System.out.println(this + " ");
    }

    @Override
    public String toString() {
        return String.format("[%1$-4d]", delta) + " Task " + id;
    }

    public String summary() {
        return "(" + id + ":" + delta + ")";
    }

    public static class EndSentinel extends DelayedTask {

        private ExecutorService executorService;

        public EndSentinel(int delayInMilis, ExecutorService executorService) {
            super(delayInMilis);
            this.executorService = executorService;
        }

        @Override
        public void run() {
            for (DelayedTask task : sequence)
                System.out.println(task.summary() + " ");
            System.out.println();
            System.out.println(this + " Calling shutdownNow()");
            executorService.shutdownNow();
        }
    }
}
