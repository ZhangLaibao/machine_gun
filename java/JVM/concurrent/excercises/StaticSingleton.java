package com.jr.test.concurrent.practice;

public class StaticSingleton {

    public static void main(String[] args) throws InterruptedException {
        StaticSingleton.getInstance();
    }


    private StaticSingleton() {
        // ===============
    }

    public static StaticSingleton getInstance() {
        return SingltonHolder.instance;
    }

    private static class SingltonHolder {
        private static StaticSingleton instance = new StaticSingleton();
    }
}
