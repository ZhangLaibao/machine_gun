package com.jr.test.tkij.conc.ueh;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by PengXianglong on 2018/7/13.
 */
public class SetDefaultUncaughtExceptionHandler {

    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler(new LoggedUncaughtExceptionHandler());
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                int i = 1 / 0;
            }
        });
        executor.shutdown();
    }
}
