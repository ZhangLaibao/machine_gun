package com.jr.test.desing.pattern.singleton;

/**
 * 饱汉式：线程安全，但是无懒加载特性
 */
public class Eager {

    private static Eager instance = new Eager();

    private Eager() { }

    public static Eager getInstance() {
        return instance;
    }
}
