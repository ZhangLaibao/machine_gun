package com.jr.test.tkij.conc.joins;

/**
 * Created by PengXianglong on 2018/7/13.
 */
public class Joining {

    public static void main(String[] args) {

        SleeperThread
                sleepy = new SleeperThread("sleepy", 1500),
                grumpy = new SleeperThread("grumpy", 1000);

        JoinerThread
                dopey = new JoinerThread("dopey", sleepy),
                doc = new JoinerThread("doc", grumpy);

        grumpy.interrupt();
    }
}

class SleeperThread extends Thread {

    private int duration;

    public SleeperThread(String name, int sleepTime) {
        super(name);
        duration = sleepTime;
        start();
    }

    @Override
    public void run() {
        try {
            sleep(duration);
        } catch (InterruptedException e) {
            System.out.println(getName() + " was interrupted. " + "isInterrupted(): " + isInterrupted());
            return;
        }

        System.out.println(getName() + " has awakened");
    }
}

class JoinerThread extends Thread {

    private SleeperThread sleeper;

    public JoinerThread(String name, SleeperThread sleeper) {
        super(name);
        this.sleeper = sleeper;
        start();
    }

    @Override
    public void run() {
        try {
            sleeper.join();
        } catch (InterruptedException e) {
            System.out.println("printed");
        }

        System.out.println(getName() + " join completed");
    }
}
