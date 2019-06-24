package com.jr.test.concurrent.practice;

import java.util.HashMap;

public class HashMapUnsynced {

    public static void main(String[] args) throws InterruptedException {
        for (int i = 0; i < 100; i++) {
            Thread thread1 = new AddToHashMapThread();
            Thread thread2 = new AddToHashMapThread();

            thread1.start();
            thread2.start();

            thread1.join();
            thread2.join();

            System.out.println(AddToHashMapThread.map.size());
        }
    }

    private static class AddToHashMapThread extends Thread {

        static HashMap<Integer, String> map = new HashMap<>();

        @Override
        public void run() {
            for (int i = 0; i < 1000000; i++) map.put(i, String.valueOf(i));
        }
    }
}
