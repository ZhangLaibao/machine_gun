package com.jr.test.jvm;

/**
 * Created by PengXianglong on 2018/5/31.
 */
public class ConstantPoolOOMExample {

    public static void main(String[] args) {

        int i = 0;
        while (true) {
            String.valueOf(i).intern();
        }
    }

}
