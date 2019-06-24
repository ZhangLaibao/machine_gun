package com.jr.test.desing.pattern.singleton;

/**
 * 使用静态内部类，由JVM维护线程安全性和懒加载特性
 * (内部类只有在使用的时候才会被JVM加载，从而初始化内部变量)
 */
public class InnerClass {

    private static class SingletonHolder {
        public static final InnerClass instance = new InnerClass();
    }

    public InnerClass getInstance() {
        return SingletonHolder.instance;
    }

    private InnerClass() {

    }

}
