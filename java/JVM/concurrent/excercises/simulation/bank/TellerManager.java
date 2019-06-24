package com.jr.test.tkij.conc.simulation.bank;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created by PengXianglong on 2018/8/8.
 */
public class TellerManager implements Runnable {

    private ExecutorService exec;
    private CustomerLine customers;

    private PriorityBlockingQueue<Teller> workingTellers = new PriorityBlockingQueue<>();
    private Queue<Teller> tellersDoingOthers = new LinkedList<>();

    private int adjustmentPeriod;
    private static Random rand = new Random(47);

    public TellerManager(ExecutorService exec, CustomerLine customers, int adjustmentPeriod) {
        this.exec = exec;
        this.customers = customers;
        this.adjustmentPeriod = adjustmentPeriod;

        Teller teller = new Teller(customers);
        exec.execute(teller);
        workingTellers.add(teller);
    }

    public void adjustTellerNumber() {
        if (customers.size() / workingTellers.size() > 2) {
            if (tellersDoingOthers.size() > 0) {
                Teller teller = tellersDoingOthers.remove();
                teller.serveCustomerLine();
                workingTellers.offer(teller);
                return;
            }
            Teller teller = new Teller(customers);
            exec.execute(teller);
            workingTellers.add(teller);
            return;
        }

        if (workingTellers.size() > 1 && customers.size() / workingTellers.size() < 2) reassignOneTeller();
        if (customers.size() == 0)
            while (workingTellers.size() > 1) reassignOneTeller();
    }

    private void reassignOneTeller() {
        Teller teller = workingTellers.poll();
        teller.doSomethingElse();
        tellersDoingOthers.offer(teller);
    }


    @Override
    public void run() {
        try {
            while (!Thread.interrupted()) {
                TimeUnit.MILLISECONDS.sleep(adjustmentPeriod);
                adjustTellerNumber();
                System.out.println(customers + " { ");
                for (Teller teller : workingTellers)
                    System.out.println(teller.shortString());
                System.out.println(" } ");
            }
        } catch (InterruptedException e) {
            System.out.println(this + "interrupted");
        }
        System.out.println(this + "terminating");
    }

    @Override
    public String toString() {
        return "TellerManager ";
    }
}
