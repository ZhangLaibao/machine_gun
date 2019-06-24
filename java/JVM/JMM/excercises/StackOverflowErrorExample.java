package com.jr.test.jvm;

/**
 * Created by PengXianglong on 2018/5/31.
 */
public class StackOverflowErrorExample {

    public static void main(String[] args) {
        main(new String[]{});
    }

}
