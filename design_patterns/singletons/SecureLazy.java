package com.jr.test.desing.pattern.singleton;

/**
 * double-check - 同时具备线程安全（弱）和懒加载特性
 */
public class SecureLazy {

    private static SecureLazy instance = null;

    private SecureLazy() {

    }

    public static SecureLazy getInstance() {
        if (null == instance)
            synchronized (SecureLazy.class) {
                if (null == instance)
                    instance = new SecureLazy();
            }

        return instance;
    }

}
