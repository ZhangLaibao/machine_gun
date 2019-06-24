package com.jr.test.tkij.conc.simulation.restaurant;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by PengXianglong on 2018/8/8.
 */
public class Test {

    public static void main(String[] args) {
        ExecutorService exec = Executors.newCachedThreadPool();
        Restaurant restaurant = new Restaurant(exec, 5,2);
        exec.execute(restaurant);

    }
}
