package com.jr.test.tkij.conc.sync;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by PengXianglong on 2018/7/13.
 */
public class TestSynchronizedBlock {

    static void testApproaches(PairManager pman1, PairManager pman2) {
        ExecutorService pool = Executors.newCachedThreadPool();

        PairManipulator
                pm1 = new PairManipulator(pman1),
                pm2 = new PairManipulator(pman2);

        PairChecker
                pairChecker1 = new PairChecker(pman1),
                pairChecker2 = new PairChecker(pman2);

        pool.execute(pm1);
        pool.execute(pm2);
        pool.execute(pairChecker1);
        pool.execute(pairChecker2);

        try {
            TimeUnit.MILLISECONDS.sleep(500);
        } catch (InterruptedException e) {
            System.out.println("Sleep interrupted");
        }

        System.out.println("pm1 : " + pm1 + "\n" + "pm2 : " + pm2);
        System.exit(0);
    }

    public static void main(String[] args) {
        PairManager
                pman1 = new TotalSynchronization(),
                pman2 = new PartialSynchronization();
        testApproaches(pman1, pman2);
    }

}

class PairChecker implements Runnable {
    private PairManager pm;

    public PairChecker(PairManager pm) {
        this.pm = pm;
    }

    @Override
    public void run() {
        while (true) {
            pm.checkCount.incrementAndGet();
            pm.getP().check();
        }
    }
}

class PairManipulator implements Runnable {

    private PairManager pm;

    public PairManipulator(PairManager pm) {
        this.pm = pm;
    }

    @Override
    public void run() {
        while (true)
            pm.increment();
    }

    @Override
    public String toString() {
        return "Pair: " + pm.getP() + " checkCounter = " + pm.checkCount.get();
    }
}

class PartialSynchronization extends PairManager {

    @Override
    public void increment() {
        Pair temp;
        synchronized (this) {
            p.increX();
            p.increY();
            temp = getP();
        }
        store(temp);
    }
}

class TotalSynchronization extends PairManager {

    @Override
    public synchronized void increment() {
        p.increX();
        p.increY();
        store(p);
    }
}

abstract class PairManager {
    AtomicInteger checkCount = new AtomicInteger(0);
    protected Pair p = new Pair();
    private List<Pair> storage = Collections.synchronizedList(new ArrayList<Pair>());

    public synchronized Pair getP() {
        return new Pair(p.getX(), p.getY());
    }

    protected void store(Pair p) {
        storage.add(p);
        try {
            TimeUnit.MILLISECONDS.sleep(50);
        } catch (InterruptedException ignore) {
        }
    }

    public abstract void increment();
}

class Pair {

    private int x, y;

    public Pair(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public Pair() {
        this(0, 0);
    }

    public int getX() {
        return x;
    }

    public void increX() {
        x++;
    }

    public int getY() {
        return y;
    }

    public void increY() {
        y++;
    }

    @Override
    public String toString() {
        return "(x = " + x + ", " + "y = " + y + ")";
    }

    public void check() {
        if (x != y)
            throw new PairValuesNotEqualException();
    }

    public class PairValuesNotEqualException extends RuntimeException {
        public PairValuesNotEqualException() {
            super("Pair values does not EQUAL" + Pair.this);
        }
    }
}
