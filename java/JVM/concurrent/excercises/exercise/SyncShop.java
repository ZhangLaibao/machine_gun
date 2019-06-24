package com.jr.test.tkij.conc.exercise;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Created by PengXianglong on 2018/7/16.
 */
public class SyncShop {

    public static void main(String[] args) {

        SyncShop shop = new SyncShop();
        new Thread(new Producer(shop)).start();
        new Thread(new Producer(shop)).start();
        new Thread(new Consumer(shop)).start();

    }

    private Something[] goods = new Something[20];
    private int index = 0;

    public int getIndex() {
        return index;
    }

    synchronized Something sell() {
        while (index == 0) {
            try {
                this.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        try {
            TimeUnit.MILLISECONDS.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        this.notifyAll();
        return goods[--index];
    }

    synchronized void inStorage(Something good) {

        while (index == goods.length) {
            try {
                this.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        try {
            TimeUnit.MILLISECONDS.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        this.notifyAll();
        goods[index++] = good;
    }

}

class Producer implements Runnable {

    private SyncShop shop;

    public Producer(SyncShop shop) {
        this.shop = shop;
    }

    @Override
    public void run() {
        while (true) {
            Something good = new Something();
            System.out.printf("Producer: [%s] produces: [%s], current index is %d \n",
                    Thread.currentThread().getName(), good.toString(), shop.getIndex());
            shop.inStorage(good);
        }
    }

}

class Consumer implements Runnable {

    private SyncShop shop;

    public Consumer(SyncShop shop) {
        this.shop = shop;
    }

    @Override
    public void run() {
        while (true) {
            System.out.printf("Consumer: [%s] buys: [%s]current index is %d \n",
                    Thread.currentThread().getName(), shop.sell(), shop.getIndex());
        }
    }

}

class Something {

    private static Random random = new Random(47);

    private int id;

    public Something() {
        this.id = random.nextInt();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }


    @Override
    public String toString() {
        return getClass().getSimpleName() + " [id=" + id+"] ";
    }
}
