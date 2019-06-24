package com.jr.test.concurrent.practice;

/**
 * JDK1.7中，大神Doug Lea大神LinkedTransferQueue的相关代码如下
 * 在LinkedTransferQueue的内部定义了头尾节点，都是volatile变量，而它们的类型是PaddedAtomicReference
 * 在PaddedAtomicReference构造函数中我们看到，除了对结点的引用之外，还声明了15个Object空对象
 * 这是因为在JVM中一个对象的引用占用4个字节，处理器的缓存读写是以缓存行为单位的，流行的大多数处理器的缓存行(cahe line)大小为64字节
 * 也就是说一个缓存行中可以缓存16个对象的引用，而这些对象中一旦有一个被volatile修饰，那么整个缓存行都会因为这一个volatile变量的写
 * 而失效，这反而降低了缓存的性能，由于队列的头为节点需要被频繁读写，所以使用空对象填充满整个缓存行可以优化性能。
 * (不过这种追加字节的方式在Java 7下可能不生效，因为Java 7变得更加智慧，它会淘汰或重新排列无用字段，需要使用其他追加字节的方式。)
 */
public class VolatileOptimization {

//    /** 队列中的头部节点 */
//    private transient final PaddedAtomicReference<QNode> head;
//    /** 队列中的尾部节点 */
//    private transient final PaddedAtomicReference<QNode> tail;
//
//    static final class PaddedAtomicReference<T> extends AtomicReference T> {
//        // 使用很多4个字节的引用追加到64个字节
//        Object p0, p1, p2, p3, p4, p5, p6, p7, p8, p9, pa, pb, pc, pd, pe;
//        PaddedAtomicReference(T r) {
//            super(r);
//        }
//    }
//    public class AtomicReference <V> implements java.io.Serializable {
//        private volatile V value;
//        // 省略其他代码
//    ｝
}
