package com.jr.test.jvm;

/**
 * Created by PengXianglong on 2018/5/29.
 */
public class ProgramCounterRegisterDemonstrate {

    private int id;

    private String name;

    public void method() {
        System.out.println(this.id + ":" + this.name);
    }

    public static void main(String[] args) {
        ProgramCounterRegisterDemonstrate  demonstrate = new ProgramCounterRegisterDemonstrate();
        demonstrate.method();
    }

}
