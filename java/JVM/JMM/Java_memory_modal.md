### 一.运行时数据区域
JVM的内存模型(JMM)是指Java虚拟机在执行Java程序的过程中，将其管理的内存划分成的体系机构，这一结构在不同的虚拟机实现产品
中会有很大差异，但是他们都遵循Java虚拟机规范。
关于JVM和JMM的关系，我们可以看这张图片来帮助理解，这张图片很清楚的表达了JVM和JMM的体系结构和各部分之间的交互：   
![JVM-JMM](https://github.com/ZhangLaibao/machine_gun/blob/master/images/JVM-JMM.png)    
我们所说的JMM主要针对图中的RUNTIME DATA AREA部分。其中黄色部分是被所有线程共享的区域，
我们所讨论的线程之间数据同步即针对这一部分的数据，白色部分是线程隔离的。    
    
    注：本文运行环境 
    java version "1.8.0_121" Java(TM) SE 
    Runtime Environment (build 1.8.0_121-b13) 
    Java HotSpot(TM) 64-Bit Server VM (build 25.121-b13, mixed mode)
    
#### 1.Program counter register - 程序计数器
    程序计数器是一块较小的内存空间，它的作用可以看作是当前线程所执行的字节码的行号指示器。在虚拟机的概念模型里字节码解释器
    工作时就是通过改变这个计数器的值来选取下一条需要执行的字节码指令，分支、循环、跳转、异常处理、线程恢复等基础功能都需要
    依赖这个计数器来完成。--《深入理解Java虚拟机》
我们可以通过一段简单的Java代码来看一下在运行时程序计数器到底存储了些什么数据    
```java
public class ProgramCounterRegisterDemonstrate {

    public static void main(String[] args) {
        int i = 10;
        int j = 20;
        int k = i + j;
    }

}
```
我们使用反汇编命令javap -c来分析这个java类编译得到的.class文件，可以达到如下的结果：    
```java
public class com.jr.test.OperandStackExample {
  public com.jr.test.OperandStackExample();
    Code:
       0: aload_0
       1: invokespecial #1                  // Method java/lang/Object."<init>":()V
       4: return

  public static void main(java.lang.String[]);
    Code:
       0: bipush        10
       2: istore_1
       3: bipush        20
       5: istore_2
       6: iload_1
       7: iload_2
       8: iadd
       9: istore_3
      10: return
}
```

在运行时，程序计数器中存储的就是这些行号 - 字节码指令的偏移地址，JVM启动并执行main方法时，主线程诞生，相应的其程序计数器也初始化
完毕。我们知道，线程是CPU的最小调度单元，Java虚拟机中的多线程是通过分配处理器的执行时间来实现的，在某一个时间点，一个处理器
(或多核处理器的一个核心)只能执行某一个线程的某一条指令，而这条指令就是通过程序计数器中存储的这个行号找到的。在程序运行过程中，
程序计数器中改变的只是字节码指令的地址值，所以不需要额外的空间，也就不会发生内存溢出的情况，这也使得程序计数器成为JVM规范中
唯一一块没有定义OutOfMemoryError的区域。多线程环境下，每一个线程都要记录本线程的指令执行位置，所以这一区域必须要线程隔离。    
JVM的native方法大多是通过C实现并未编译的，所以当运行本地方法时，这一区域的置为undefined。另外，JVM的线程在HotSpot虚拟机
实现中是和操作系统的线程一一映射的，在执行native方法的时候，线程由操作系统直接调度。

#### 2.JAVA STACK - Java虚拟机栈
与程序计数器类似的，因为Java虚拟机栈记载每个线程执行的方法调用链的信息，所以也是线程私有的，其生命周期与对应的线程相同。
虚拟机栈也是遵循后进先出(LIFO)的栈结构，每一个方法被调用的时候，虚拟机都会创建一个栈帧(Stack Frame)并push到其所在线程
的虚拟机栈的栈顶，这个栈帧存储了局部变量表(Local variable arry)、操作栈(Operand stack)、动态链接、
运行时常量引用(Reference to runtime constant pool for class of the current thread)、方法出口(Return value)等信息，
当方法执行完毕正常返回或者抛出异常时，这个栈帧就会被pop掉，除了压栈和弹栈，虚拟机栈并不会被直接操作。   
**局部变量表** 由数组实现，存储了编译期可知的各种基本数据类型、对象引用(reference)和retrurnAddress类型。对于实例方法，
索引为0的元素表示这个方法所属的类的实例(this)，从1开始，首先存放的是传给该方法的参数，在参数后面保存的是方法的局部变量；
对于类方法，索引从0开始。在Java语言中定义的64位长度的long和double类型的数据会占用2个局部变量空间(slot)，其余的数据类型占用1个。
局部变量表所需的内存空间在编译期间即可确定。在JVM规范中，如果一个线程请求的栈的深度大于虚拟机所允许的深度，
将会抛出StackOverflowError；如果虚拟机栈允许动态扩展，则当扩展内从用于放置新的栈时所申请的内存空间无法得到满足，
会抛出OutOfMemoryError。我们可以很简单的制造一个StackOverflowError，只需一个循环调用，当然其他方法，比如递归调用，
也可能引发这一错误：
```java
// run with VM Args: -Xss108k(It's the least size of JVM stack in JDK8) 
public class StackOverflowErrorExample {

    public static void main(String[] args) {
        main(new String[]{});
    }
    
}
```
**操作数栈**操作数栈和局部变量表很类似，也是由数组实现的，数据的存储方式也是一样的，不同的是，它不能通过数组的索引访问，
而是通过标准的栈操作来访问。不同于程序计数器，Java虚拟机没有寄存器，程序计数器也无法被程序指令直接访问。
Java虚拟机的指令是从操作数栈中而不是从寄存器中取得操作数的，因此它的运行方式是基于栈的而不是基于寄存器的。    
参考我们说明程序计数器时反汇编的例子，通过下图直观的展示在程序运行过程中局部变量表和操作数栈的动态：
![local_variable_operand_stack](https://github.com/ZhangLaibao/machine_gun/blob/master/images/local_variable_operand_stack.png)
   
#### 3.NATIVE METHOD STACK - 本地方法栈
本地方法栈可以理解为与虚拟机栈相同，只不过它是为本地方法调用服务的。Java虚拟机规范对于这部分的使用方式、数据结构和实现语言
并没有强制规定，所以不同虚拟机产品实现方式自由。HotSpot虚拟机甚至把虚拟机栈和本地方法栈合二为一。例如：
**JIT()**
**逃逸分析(Escape Analysis)**
逃逸分析是目前Java虚拟机中比较前沿的优化技术。这是一种可以有效减少Java程序中同步负载和内存堆分配压力的跨函数全局数据流分析算法。
通过逃逸分析，Java Hotspot编译器能够分析出一个新的对象的引用的使用范围从而决定是否要将这个对象分配到堆上。
逃逸分析的基本行为就是分析对象动态作用域：当一个对象在方法中被定义后，它可能被外部方法所引用，例如作为调用参数传递到其他地方中，
称为方法逃逸。例如：
```java
    public static StringBuffer plus(String d1, String d2){
        StringBuffer res = new StringBuffer();
        res.append(d1);
        res.append(d2);
        return res;
    }

```
StringBuffer对象res是一个方法内部变量，上述方法中直接将res返回，这样这个StringBuffer对象就会被其他的方法访问到，
它的作用域就不只是在本方法内部，虽然它是一个局部变量，但是逃逸到了方法外部。甚至还有可能被外部线程访问到，
譬如赋值给类变量或可以在其他线程中访问的实例变量，称为线程逃逸。
如果我们将以上的例子写成这样：
```java
    public static String plus(String d1, String d2){
        StringBuffer res = new StringBuffer();
        res.append(d1);
        res.append(d2);
        return res.toString();
    }

```
在这个例子里，StringBuffer对象res的作用域仅限于本方法，不会发生逃逸的情况。
逃逸分析是通过DoEscapeAnalysis参数控制的，例如
    
    -XX:+DoEscapeAnalysis - 开启
    -XX:-DoEscapeAnalysis - 关闭 

从jdk 1.7开始已经默认开始逃逸分析，使用逃逸分析中对内存分配进行优化主要体现在以下三点：
一、同步省略。如果一个对象只能被一个线程被访问到，那么对于这个对象的操作可以不考虑同步。
二、将堆分配转化为栈分配。如果一个对象在子程序中被分配，要使指向该对象的指针永远不会逃逸，对象可能是栈分配的候选，而不是堆分配。
我们可以通过一个例子来直观的演示栈上分配：
```java



```
三、分离对象或标量替换。有的对象可能不需要作为一个连续的内存结构存在也可以被访问到，那么对象的部分（或全部）可以不存储在内存，
而是存储在CPU寄存器中。
#### 4.JAVA HEAP - Java虚拟机堆
    The heap is the runtime data area from which memory for all class instances and arrays is allocated.
虚拟机规范规定：所有对象实例以及数组都必须要在堆上分配。虽然随着JIT编译技术的发展与逃逸分析技术的逐渐成熟，
栈上分配、标量替换等优化技术会打破这一必须，但是Java Heap唯一目的仍然是存放对象的实例。堆由线程共享，
所以我们在并发编程中谈到的线程安全问题也主要针对堆内存中的数据。堆内存在虚拟机启动时就会被创建，
是虚拟机所管理的内存中体量最大的一块，GC也主要针对这一块区域。

从GC角度来说，堆还可以被分为新生代和老年代，新生代又被分为Eden Space/From Survivor/To Survivor等，
我们会在学习GC的时候来学习更详细的知识；从内存分配的角度来说，这一线程共享的区域又可以划分出多个线程私有的分配缓冲区。
虚拟机规范只规定了这一区域必须是逻辑上连续的，所以在物理上，堆空间可以是不连续的。在实现上，既可以固定堆的大小，
也可以通过虚拟机参数设置扩展。如果堆空间不足以完成实例分配，也不能通过扩展获取足够的空间时，就会抛出OutOfMemoryError。    
通过限制一个比较小的堆空间，然后在上面分配一些大对象，我么可以制造出这种OOM：
```java
// run with VM Args: -Xms20M -Xmx20M -Xmn10M -XX:+PrintGCDetails -XX:SurvivorRatio=8 
/**
* we specify the same value of Xms and Xmx to forbid the JVM to auto expend the heap space
*/
public class HeapOOMExample {

    public static void main(String[] args) {

        List<Date> list = new ArrayList<>();
        while(true){
            list.add(new Date());
        }

    }

}
```
下面是我们在IDEA中运行上面的例子时，在控制台打印出来的信息：
```java
[GC (Allocation Failure) [PSYoungGen: 7844K->1017K(9216K)] 7844K->5105K(19456K), 0.0258327 secs] [Times: user=0.08 sys=0.00, real=0.03 secs] 
[GC (Allocation Failure) [PSYoungGen: 8070K->1000K(9216K)] 12158K->10638K(19456K), 0.0166318 secs] [Times: user=0.06 sys=0.00, real=0.02 secs] 
[Full GC (Ergonomics) [PSYoungGen: 1000K->487K(9216K)] [ParOldGen: 9638K->10024K(10240K)] 10638K->10512K(19456K), [Metaspace: 3547K->3547K(1056768K)], 0.1908355 secs] [Times: user=0.33 sys=0.02, real=0.19 secs] 
[Full GC (Ergonomics) [PSYoungGen: 7056K->6818K(9216K)] [ParOldGen: 10024K->8617K(10240K)] 17080K->15435K(19456K), [Metaspace: 3547K->3547K(1056768K)], 0.1788845 secs] [Times: user=0.45 sys=0.00, real=0.18 secs] 
[Full GC (Allocation Failure) Exception in thread "main" [PSYoungGen: 6818K->6818K(9216K)] [ParOldGen: 8617K->8599K(10240K)] 15435K->15417K(19456K), [Metaspace: 3547K->3547K(1056768K)], 0.1148374 secs] [Times: user=0.56 sys=0.00, real=0.12 secs] 
java.lang.OutOfMemoryError: Java heap space
	at java.util.Arrays.copyOf(Arrays.java:3210)
	at java.util.Arrays.copyOf(Arrays.java:3181)
	at java.util.ArrayList.grow(ArrayList.java:261)
	at java.util.ArrayList.ensureExplicitCapacity(ArrayList.java:235)
	at java.util.ArrayList.ensureCapacityInternal(ArrayList.java:227)
	at java.util.ArrayList.add(ArrayList.java:458)
	at com.jr.test.HeapOOMExample.main(HeapOOMExample.java:16)
Heap
 PSYoungGen      total 9216K, used 7058K [0x00000000ff600000, 0x0000000100000000, 0x0000000100000000)
  eden space 8192K, 86% used [0x00000000ff600000,0x00000000ffce4990,0x00000000ffe00000)
  from space 1024K, 0% used [0x00000000fff00000,0x00000000fff00000,0x0000000100000000)
  to   space 1024K, 0% used [0x00000000ffe00000,0x00000000ffe00000,0x00000000fff00000)
 ParOldGen       total 10240K, used 8599K [0x00000000fec00000, 0x00000000ff600000, 0x00000000ff600000)
  object space 10240K, 83% used [0x00000000fec00000,0x00000000ff465cc0,0x00000000ff600000)
 Metaspace       used 3579K, capacity 4536K, committed 4864K, reserved 1056768K
  class space    used 395K, capacity 428K, committed 512K, reserved 1048576K
```
我们可以看出，在抛出错误之前，虚拟机为了给新创建的对象分配内存空间经过了几次GC，但最终因为内存不足抛出错误。

#### 5.METHOD AREA - 方法区
方法区存储的是虚拟机加载的类信息(如类名、访问修饰符、字段方法注解等)、常量、静态变量、即时编译器编译的代码等数据。
在虚拟机规范中，方法区是堆的一个逻辑部分，但是为了区分，方法区又被称为Non-Heap。在HotSpot虚拟机实现中，
这一区域被称作永久代(Permanent Generation)。在JDK8及以后的版本中，这一区域改为由Native Memory实现，所以这一称呼也将成为历史。    
虚拟机规范对此区域的限制也很宽松，除了可以像堆那样不需要连续的内存地址之外，其大小也可以选择固定或者动态扩展，并且可以选择不
实现GC。因为针对此区域的GC内容主要是常量回收和类型卸载，鉴于这些操作都难有明显的内存回收效果。当方法区无法满足内存需求时，也会
抛出OutOfMemoryError。    
我们知道，运行时的字符串是存储在方法区的常量池中的，所以我们可以通过向常量池里写入大量字符串来制造常量池的OOM：
```java
// run with VM Args: -XX:PermSize=10M -XX:MaxPermSize=10M
// run in JDK7 or earlier, or you will see:
// Java HotSpot(TM) 64-Bit Server VM warning: ignoring option PermSize=10M; support was removed in 8.0
// Java HotSpot(TM) 64-Bit Server VM warning: ignoring option MaxPermSize=10M; support was removed in 8.0
public class ConstantPoolOOMExample {

    public static void main(String[] args) {

        int i = 0;
        while (true) {
            String.valueOf(i).intern();
        }
    }

}
```
在Java企业开发中用到的很多框架技术，如Spring/Hibernate/Mybatis等，都会使用动态代理技术来增强被代理类，无论是JDK原生的动态代理
还是CGLIB代理，都会产生大量代理类进入方法区，另外使用JSP技术时也很容易导致方法区的OOM。
#### 6.直接内存
直接内存就是操作系统的物理内存，这一部分并不归属于虚拟机运行时数据区，但是这一部分内存和JVM内存是密不可分的，比如在NIO中，
Channel和Buffer就是通过直接调用Native函数直接分配内存，然后通过虚拟机堆内存的DirectByteBuffer对象引用以提高性能，
避免在Java堆内存和Native堆内存之间copy数据的开销。
##### RFERENCES
1.http://blog.jamesdbloom.com/JVMInternals.html    
2.https://blog.csdn.net/aigoogle/article/details/38757771    
3.深入理解Java虚拟机(JVM高级特性与最佳实践)-周志明





































