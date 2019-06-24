package com.jr.test.desing.pattern.singleton;

/**
 * 饿汉式，懒加载，但是线程不安全
 */
public class Lazy {

    private static Lazy instance = null;

    private Lazy() {

    }

    public static Lazy getInstance() {
        if (null == instance)
            instance = new Lazy();

        return instance;
    }

}
