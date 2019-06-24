package com.jr.test.tkij.conc.communic;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by PengXianglong on 2018/7/19.
 */
public class WaitNotifySample {

    public static void main(String[] args) throws InterruptedException {
        Car car = new Car();
        ExecutorService pool = Executors.newCachedThreadPool();
        pool.execute(new WaxOn(car));
        pool.execute(new WaxOff(car));

        TimeUnit.MILLISECONDS.sleep(5000);
        pool.shutdown();
    }
}

class WaxOn implements Runnable {

    private Car car;

    public WaxOn(Car car) {
        this.car = car;
    }

    @Override
    public void run() {
        try {
            while (!Thread.interrupted()) {
                System.out.println("Wax ON!");
                TimeUnit.MILLISECONDS.sleep(200);
                car.waxed();
                car.waitBuff();
            }
        } catch (InterruptedException e) {
            System.out.println("Exiting via interrupt");
        }
        System.out.println("Ending WaxOn");
    }
}

class WaxOff implements Runnable {

    private Car car;

    public WaxOff(Car car) {
        this.car = car;
    }

    @Override
    public void run() {
        try {
            while (!Thread.interrupted()) {
                car.waitWax();
                System.out.println("Wax OFF!");
                TimeUnit.MILLISECONDS.sleep(200);
                car.buffed();
            }
        } catch (InterruptedException e) {
            System.out.println("Exiting via interrupt");
        }
        System.out.println("Ending WaxOff");
    }
}

class Car {

    private boolean wax = false;

    public synchronized void waxed() {
        wax = true;
        notifyAll();
    }

    public synchronized void buffed() {
        wax = false;
        notifyAll();
    }

    public synchronized void waitWax() throws InterruptedException {
        while (!wax)
            wait();
    }

    public synchronized void waitBuff() throws InterruptedException {
        while (wax)
            wait();

    }

}
