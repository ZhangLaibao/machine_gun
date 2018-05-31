### 一.运行时数据区域
JVM的内存模型(JMM)是指Java虚拟机在执行Java程序的过程中，将其管理的内存划分成的体系机构，这一结构在不同的虚拟机实现产品
中会有很大差异，但是他们都遵循Java虚拟机规范。
关于JVM和JMM的关系，我们可以看这张图片来帮助理解，这张图片很清楚的表达了JVM和JMM的体系结构和各部分之间的交互：   
![JVM-JMM](https://github.com/ZhangLaibao/machine_gun/blob/master/images/JVM-JMM.png)    
我们所说的JMM主要针对图中的RUNTIME DATA AREA部分。其中黄色部分是被所有线程共享的区域，我们所讨论的线程之间数据同步即针对这一部分的数据，
白色部分是线程隔离的。
#### 1.Program counter register - 程序计数器
    程序计数器是一块较小的内存空间，它的作用可以看作是当前线程所执行的字节码的行号指示器。在虚拟机的概念模型里字节码解释器
    工作时就是通过改变这个计数器的值来选取下一条需要执行的字节码指令，分支、循环、跳转、异常处理、线程恢复等基础功能都需要
    依赖这个计数器来完成。--《深入理解Java虚拟机》
我们可以通过一段简单的Java代码来看一下在运行时程序计数器到底存储了些什么数据    
```java
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
```
我们使用反汇编命令javap -c来分析这个java类编译得到的.class文件，可以达到如下的结果：    
```java
public class com.jr.test.ProgramCounterRegisterDemonstrate {
  public com.jr.test.ProgramCounterRegisterDemonstrate();
    Code:
       0: aload_0
       1: invokespecial #1                  // Method java/lang/Object."<init>":()V
       4: return

  public void method();
    Code:
       0: getstatic     #2                  // Field java/lang/System.out:Ljava/io/PrintStream;
       3: new           #3                  // class java/lang/StringBuilder
       6: dup
       7: invokespecial #4                  // Method java/lang/StringBuilder."<init>":()V
      10: aload_0
      11: getfield      #5                  // Field id:I
      14: invokevirtual #6                  // Method java/lang/StringBuilder.append:(I)Ljava/lang/StringBuilder;
      17: ldc           #7                  // String :
      19: invokevirtual #8                  // Method java/lang/StringBuilder.append:(Ljava/lang/String;)Ljava/lang/StringBuilder;
      22: aload_0
      23: getfield      #9                  // Field name:Ljava/lang/String;
      26: invokevirtual #8                  // Method java/lang/StringBuilder.append:(Ljava/lang/String;)Ljava/lang/StringBuilder;
      29: invokevirtual #10                 // Method java/lang/StringBuilder.toString:()Ljava/lang/String;
      32: invokevirtual #11                 // Method java/io/PrintStream.println:(Ljava/lang/String;)V
      35: return

  public static void main(java.lang.String[]);
    Code:
       0: new           #12                 // class com/jr/test/ProgramCounterRegisterDemonstrate
       3: dup
       4: invokespecial #13                 // Method "<init>":()V
       7: astore_1
       8: aload_1
       9: invokevirtual #14                 // Method method:()V
      12: return
}
```

在运行时，程序计数器中存储的就是这些行号 - 字节码指令的偏移地址，JVM启动并执行main方法时，主线程诞生，相应的其程序计数器也初始化
完毕。我们知道，线程是CPU的最小调度单元，Java虚拟机中的多线程是通过分配处理器的执行时间来实现的，在某一个时间点，一个处理器
(或多核处理器的一个核心)只能执行某一个线程的某一条指令，而这条指令就是通过程序计数器中存储的这个行号找到的。在程序运行过程中，
程序计数器中改变的只是字节码指令的地址值，所以不需要额外的空间，也就不会发生内存溢出的情况，这也使得程序计数器成为JVM规范中
唯一一块没有定义OutOfMemoryError的区域。多线程环境下，每一个线程都要记录本线程的指令执行位置，所以这一区域必须要线程隔离。    
JVM的native方法大多是通过C实现并未编译的，所以当运行本地方法时，这一区域的置为undefined。另外，JVM的线程在HotSpot虚拟机实现中是和
操作系统的线程一一映射的，在执行native方法的时候，线程由操作系统直接调度。

#### 2.JAVA STACK - Java虚拟机栈
与程序计数器类似的，因为Java虚拟机栈记载每个线程执行的方法调用链的信息，所以也是线程私有的，其生命周期与对应的线程相同。
虚拟机栈也是遵循后进先出(LIFO)的栈结构，每一个方法被调用的时候，虚拟机都会创建一个栈帧(Stack Frame)并push到其所在线程
的虚拟机栈的栈顶，这个栈帧存储了局部变量表、操作栈、动态链接、运行时常量引用、方法出口等信息，当方法执行完毕正常返回或者
抛出异常时，这个栈帧就会被pop掉，除了压栈和弹栈，虚拟机栈并不会被直接操作。   
**局部变量表** 由数组实现，存储了编译期可知的各种基本数据类型、对象引用和retrurnAddress类型。索引为0的元素表示这个方法所属的
类的实例，从1开始，首先存放的是传给该方法的参数，在参数后面保存的是方法的局部变量。在Java语言中定义的64位长度的long和double类型
的数据会占用2个局部变量空间(slot)，其余的数据类型占用1个。局部变量表所需的内存空间在编译期间即可确定。在JVM规范中，如果一个线程
请求的栈的深度大于虚拟机所允许的深度，将会抛出StackOverflowError；如果虚拟机栈允许动态扩展，则当扩展时所申请的内存空间无法得到
满足时，会抛出OutOfMemoryError。
我们可以很简单的制造一个StackOverflowError，只需一个循环调用，当然其他方法，比如递归调用，也很可能引发这一错误：
```java
// run with VM Args: -Xss108k(It's the least size of JVM stack in JDK8) 
public class StackOverflowErrorExample {

    public static void main(String[] args) {
        main(new String[]{});
    }
    
}
```

#### 3.NATIVE METHOD STACK - 本地方法栈
本地方法栈可以理解为与虚拟机栈相同，只不过它是为本地方法调用服务的。Java虚拟机规范对于这部分的使用方式、数据结构和实现语言并没有强制
规定，所以不同虚拟机产品实现方式自由。HotSpot虚拟机甚至把虚拟机栈和本地方法栈合二为一。

#### 4.JAVA HEAP - Java虚拟机堆
    The heap is the runtime data area from which memory for all class instances and arrays is allocated.
虚拟机规范规定：所有对象实例以及数组都必须要在堆上分配。虽然随着JIT编译技术的发展与逃逸分析技术的逐渐成熟，栈上分配、标量替换等优化技术会
打破这一必须，但是Java Heapd唯一目的仍然是存放对象的实例。堆由线程共享，在虚拟机启动时就会被创建，是虚拟机所管理的内存中体量最大的一块，
GC也主要针对这一块区域。    
从GC角度来说，堆还可以被分为新生代和老年代，新生代又被分为Eden Space/From Survivor/To Survivor等；从内存分配的角度来说，这一线程共享的
区域又可以划分出多个线程私有的分配缓冲区。虚拟机规范只规定了这一区域必须是逻辑上连续的，所以在物理上，堆空间可以是不连续的。在实现上，
既可以固定堆的大小，也可以通过虚拟机参数设置扩展。如果堆空间不足以完成实例分配，也不能通过扩展获取足够的空间时，就会抛出OutOfMemoryError。    
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

##### RFERENCES
1.http://blog.jamesdbloom.com/JVMInternals.html