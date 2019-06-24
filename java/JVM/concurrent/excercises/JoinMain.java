package com.jr.test.concurrent.practice;

public class JoinMain {

    static int i = 0;

    public static void main(String[] args) throws InterruptedException {
        Thread addThread = new Thread(() -> {
            for (; i < 10000; i++) ;
        }, "extends thread");
        addThread.start();

        // addThread插队了，要等他执行完
        addThread.join();
        System.out.println(i);
    }

}
