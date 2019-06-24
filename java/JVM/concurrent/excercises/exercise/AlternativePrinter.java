package com.jr.test.tkij.conc.exercise;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by PengXianglong on 2018/7/16.
 */
public class AlternativePrinter {

    public static void main(String[] args) {
        Printer printer = new Printer();

        PrintTask even = new PrintTask(printer);
        PrintTask odd = new PrintTask(printer);

        new Thread(even, "EVEN THREAD").start();
        new Thread(odd, "ODD THREAD").start();

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                System.exit(0);
            }
        }, 100);
    }

}

class PrintTask implements Runnable {

    private final Printer printer;

    PrintTask(Printer printer) {
        this.printer = printer;
    }

    @Override
    public void run() {

        while (true) {
            synchronized (printer) {
                if (printer.isVacant()) {
                    printer.print();
                    printer.addOne();
                    printer.setVacant(false);
                    printer.notify();
                } else {
                    try {
                        printer.setVacant(true);
                        printer.wait();
                    } catch (InterruptedException e) {
                        // LOGGER.error();
                    }
                }
            }
        }
    }
}

class Printer {

    private int curNum = 0;
    private boolean vacant = true;

    void print() {
        System.out.printf("[%s] prints number : %d \n",
                Thread.currentThread().getName(), curNum);
    }

    boolean isVacant() {
        return vacant;
    }

    void setVacant(boolean vacant) {
        this.vacant = vacant;
    }

    void addOne() {
        curNum++;
    }
}
