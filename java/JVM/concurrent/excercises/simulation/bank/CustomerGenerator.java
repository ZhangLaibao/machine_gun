package com.jr.test.tkij.conc.simulation.bank;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Created by PengXianglong on 2018/8/8.
 */
public class CustomerGenerator implements Runnable {

    private CustomerLine customers;
    private static Random rand = new Random(47);

    public CustomerGenerator(CustomerLine customers) {
        this.customers = customers;
    }

    @Override
    public void run() {
        try {
            while (!Thread.interrupted()) {
                TimeUnit.MILLISECONDS.sleep(rand.nextInt(300));
                customers.put(new Customer(rand.nextInt(1000)));
            }
        } catch (InterruptedException e) {
            System.out.println("CustomerGenerator interrupted");
        }
        System.out.println("CustomerGenerator terminating");
    }
}
