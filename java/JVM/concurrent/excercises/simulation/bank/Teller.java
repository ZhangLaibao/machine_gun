package com.jr.test.tkij.conc.simulation.bank;

import java.util.concurrent.TimeUnit;

/**
 * Created by PengXianglong on 2018/8/8.
 */
public class Teller implements Runnable, Comparable<Teller> {

    private static int counter = 0;
    private final int id = counter++;

    private int customerServed = 0;
    private CustomerLine customers;
    private boolean servingCustomerLine = true;

    public Teller(CustomerLine customers) {
        this.customers = customers;
    }

    @Override
    public int compareTo(Teller o) {
        return customerServed < o.customerServed ? -1 : (customerServed == o.customerServed ? 0 : 1);
    }

    @Override
    public void run() {
        try {
            while (!Thread.interrupted()) {
                Customer take = customers.take();
                TimeUnit.MILLISECONDS.sleep(take.getServiceTime());

                synchronized (this) {
                    customerServed++;
                    while (!servingCustomerLine) wait();
                }
            }
        } catch (InterruptedException e) {
            System.out.println(this + "interrupted");
        }
        System.out.println(this + "terminating");
    }

    public synchronized void doSomethingElse() {
        customerServed = 0;
        servingCustomerLine = false;
    }

    public synchronized void serveCustomerLine() {
        assert !servingCustomerLine : "already serving: " + this;
        servingCustomerLine = true;
        notifyAll();
    }

    @Override
    public String toString() {
        return "Teller " + id + " ";
    }

    public String shortString() {
        return "T" + id;
    }


}
