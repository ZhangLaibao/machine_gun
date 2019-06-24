package com.jr.test.tkij.conc.communic;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created by PengXianglong on 2018/7/22.
 */
public class PriorityBlockingQueueSample {
    public static void main(String[] args) {
        ExecutorService pool = Executors.newCachedThreadPool();
        PriorityBlockingQueue<Runnable> queue = new PriorityBlockingQueue<>();
        pool.execute(new PrioritirizedTaskProducer(queue,pool));
        pool.execute(new PrioritirizedTaskConsumer(queue));
    }
}


class PrioritirizedTaskConsumer implements Runnable {

    private PriorityBlockingQueue<Runnable> q;

    public PrioritirizedTaskConsumer(PriorityBlockingQueue<Runnable> q) {
        this.q = q;
    }

    @Override
    public void run() {
        try {
            while (!Thread.interrupted()) {
                q.take().run();
            }
        } catch (InterruptedException e) {

        }
        System.out.println("Finished PrioritirizedTaskConsumer");
    }
}

class PrioritirizedTaskProducer implements Runnable {

    private Random random = new Random(47);
    private Queue<Runnable> queue;
    private ExecutorService pool;

    public PrioritirizedTaskProducer(Queue<Runnable> queue, ExecutorService pool) {
        this.queue = queue;
        this.pool = pool;
    }

    @Override
    public void run() {
        for (int i = 1; i < 20; i++) {
            queue.add(new PrioritirizedTask(random.nextInt(10)));
            Thread.yield();
        }

        try {
            for (int i = 0; i < 10; i++) {
                TimeUnit.MILLISECONDS.sleep(250);
                queue.add(new PrioritirizedTask(10));
            }
            for (int i = 0; i < 10; i++) {
                queue.add(new PrioritirizedTask(i));
            }

            queue.add(new PrioritirizedTask.EndSentinel(pool));
        } catch (InterruptedException e) {

        }
        System.out.println("Finished PrioritirizedTaskProducer");
    }
}

class PrioritirizedTask implements Runnable, Comparable<PrioritirizedTask> {

    private Random random = new Random(47);
    private static int counter = 0;
    private final int id = counter++;
    private final int priority;

    protected static List<PrioritirizedTask> sequence = new ArrayList<>();

    public PrioritirizedTask(int priority) {
        this.priority = priority;
        sequence.add(this);
    }

    @Override
    public int compareTo(PrioritirizedTask o) {
        return priority < o.priority ? 1 : (priority > o.priority ? -1 : 0);
    }

    @Override
    public void run() {
        try {
            TimeUnit.MILLISECONDS.sleep(random.nextInt(250));
        } catch (InterruptedException e) {

        }
        System.out.println(this);
    }

    @Override
    public String toString() {
        return String.format("[%1$-3d]", priority) + " Task " + id;
    }

    public String summary() {
        return "(" + id + ":" + priority + ")";
    }

    public static class EndSentinel extends PrioritirizedTask {

        private ExecutorService pool;

        public EndSentinel(ExecutorService pool) {
            super(-1);
            this.pool = pool;
        }

        @Override
        public void run() {
            int count = 0;
            for (PrioritirizedTask task : sequence) {
                System.out.println(task.summary());
                if (++count % 5 == 0)
                    System.out.println();
            }

            System.out.println();
            System.out.println(this + " Calling shutdownNow()");
        }
    }
}
