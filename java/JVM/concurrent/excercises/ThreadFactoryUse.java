package com.jr.test.concurrent.practice;

import java.util.concurrent.ThreadFactory;

public class ThreadFactoryUse {

    public static void main(String[] args) {
        ThreadFactory threadFactory = (r) -> {
            Thread thread = new Thread(r);
            thread.setDaemon(true);
            return thread;
        };
    }

}
