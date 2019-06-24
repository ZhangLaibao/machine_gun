package com.jr.test.concurrent.practice;

public class StartThread {

    public static void main(String[] args) {
        new ExtendsThread("extends thread").start();
        new Thread(new ImplementRunnable(), "implements thread").start();
    }

    // 继承Thread类
    private static class ExtendsThread extends Thread {

        // 最好提供可读性强的线程名称方便查找日志
        ExtendsThread(String name) {
            super(name);
        }

        @Override
        public void run() {
            System.out.println("Extends java.lang.Thread to create a new Thread");
        }
    }

    // 实现Runnable接口
    private static class ImplementRunnable implements Runnable {

        @Override
        public void run() {
            System.out.println("Implements java.lang.Runnable to create a new Thread");
        }
    }
}
