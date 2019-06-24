package com.jr.test.tkij.conc.simulation.bank;

/**
 * Created by PengXianglong on 2018/8/8.
 */
public class Customer {

    private final int serviceTime;

    public Customer(int serviceTime) {
        this.serviceTime = serviceTime;
    }

    public int getServiceTime() {
        return serviceTime;
    }

    @Override
    public String toString() {
        return "[" + serviceTime + "]";
    }
}
