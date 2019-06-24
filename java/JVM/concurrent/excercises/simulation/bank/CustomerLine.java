package com.jr.test.tkij.conc.simulation.bank;

import com.jr.test.tkij.conc.simulation.bank.Customer;

import java.util.concurrent.ArrayBlockingQueue;

/**
 * Created by PengXianglong on 2018/8/8.
 */
public class CustomerLine extends ArrayBlockingQueue<Customer> {

    public CustomerLine(int capacity) {
        super(capacity);
    }

    @Override
    public String toString() {
        if (this.size() == 0) return "[Empty]";
        StringBuilder result = new StringBuilder();
        for (Customer customer : this)
            result.append(customer);
        return result.toString();
    }
}
