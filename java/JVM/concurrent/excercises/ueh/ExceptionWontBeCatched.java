package com.jr.test.tkij.conc.ueh;

/**
 * Created by PengXianglong on 2018/7/13.
 */
public class ExceptionWontBeCatched {

    public static void main(String[] args) {

        try {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    int i = 1 / 0;
                }
            }).start();
        } catch (Exception e) {
            System.out.println("catched exception : " + e);
        }

    }
}
