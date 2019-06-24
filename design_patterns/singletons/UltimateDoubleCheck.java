package com.jr.test.desing.pattern.singleton;

/**
 * 保证线程安全性
 * 保证懒加载
 */
public class UltimateDoubleCheck {

    /**
     * 对 volatile 变量的写操作，不允许和它之前的读写乱序；
     * 对 volatile 变量的读操作，不允许和它之后的读写乱序
     *
     * 遵循happens-before原则，防止指令重排序
     * 但要付出额外的性能代价
     */
    private static volatile UltimateDoubleCheck instance = null;

    private UltimateDoubleCheck() {

    }

    public static UltimateDoubleCheck getInstance() {
        // 使用内部变量inst取代volatile变量instance
        UltimateDoubleCheck inst = instance;
        if (null == inst)
            synchronized (UltimateDoubleCheck.class) {
                inst = instance;
                if (null == inst){
                    inst = new UltimateDoubleCheck();
                    instance = inst;
                }
            }

        // 实际返回内部变量inst，而不使用volatile变量instance，提高性能
        return inst;
    }

}
