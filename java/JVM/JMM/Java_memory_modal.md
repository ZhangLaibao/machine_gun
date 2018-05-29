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
完毕。我们知道，线程是CPU的最小调度单元，Java虚拟机中的多线程是通过分配处理器的执行时间来实现的，在某一个时间点，一个处理器(或多核处理器的一个核心)
只能执行某一个线程的某一条指令，而这条指令就是通过程序计数器中存储的这个行号找到的。在程序运行过程中，程序计数器中改变的只是字节码指令的地址值，
所以不需要额外的空间，也就不会发生内存溢出的情况，这也使得程序计数器成为JVM规范中唯一一块没有定义OutOfMemoryError的区域。
多线程环境下，每一个线程都要记录本线程的指令执行位置，所以这一区域必须要线程隔离。    
JVM的native方法大多是通过C实现并未编译成需要执行的，所以当运行本地方法时，这一区域的置为unefined。另外，JVM的线程在HotSpot虚拟机实现中是和
操作系统的线程一一映射的，在执行native方法的时候，线程由操作系统直接调度。