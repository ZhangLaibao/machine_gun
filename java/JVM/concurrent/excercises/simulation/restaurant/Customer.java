package com.jr.test.tkij.conc.simulation.restaurant;

import java.util.concurrent.SynchronousQueue;

/**
 * Created by PengXianglong on 2018/8/8.
 */
public class Customer implements Runnable {

    private static int counter = 0;
    private final int id = counter++;
    private final WaitPerson waitPerson;

    private SynchronousQueue<Plate> placeSetting = new SynchronousQueue<>();

    public Customer(WaitPerson waitPerson) {
        this.waitPerson = waitPerson;
    }

    public void deliver(Plate plate) throws InterruptedException {
        placeSetting.put(plate);
    }

    @Override
    public void run() {

    }
}
