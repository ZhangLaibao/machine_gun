package com.jr.test.concurrent.practice;

import java.util.ArrayList;

public class ArrayListUnsynced {

    public static void main(String[] args) throws InterruptedException {
        Thread thread1 = new AddToArrayListThread();
        Thread thread2 = new AddToArrayListThread();

        thread1.start();
        thread2.start();

        thread1.join();
        thread2.join();

        System.out.println(AddToArrayListThread.list.size());
    }

    private static class AddToArrayListThread extends Thread {

        static ArrayList<Integer> list = new ArrayList<>();

        @Override
        public void run() {
            for (int i = 0; i < 1000000; i++) list.add(i);
        }
    }
}
