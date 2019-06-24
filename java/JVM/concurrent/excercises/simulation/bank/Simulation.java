package com.jr.test.tkij.conc.simulation.bank;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by PengXianglong on 2018/8/8.
 */
public class Simulation {

    static final int MAX_LINE_SIZE = 50;
    static final int ADJUSTMENT_PERIOD = 1000;

    public static void main(String[] args) throws InterruptedException {
        ExecutorService executorService = Executors.newCachedThreadPool();
        CustomerLine customers = new CustomerLine(MAX_LINE_SIZE);
        executorService.execute(new CustomerGenerator(customers));
        executorService.execute(new TellerManager(executorService, customers, ADJUSTMENT_PERIOD));

        TimeUnit.SECONDS.sleep(50);
        executorService.shutdownNow();
    }

}
