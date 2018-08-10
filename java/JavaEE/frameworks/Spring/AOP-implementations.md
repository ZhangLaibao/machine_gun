### 提纲
    Aspect-oriented programming is a way of modularizing crosscutting concerns much like 
    object-oriented programming is a way of modularizing common concerns.   
**静态代理**
    编译期将增强功能代码注入被代理方法，例如：AspectJ   
**动态代理** 
    运行期为目标生成代理对象，例如：JDK动态代理，使用反射机制，要求被代理的目标类必须实现一个接口；CGLIB为被代理对象生成子类增强其功能。
    
#### AspectJ简介
官方的定义：AspectJ is a seamless aspect-oriented extension to Java.对于此类编译时通过注入字节码实现增强的静态代理类库，
使用场景已经逐渐被动态代理替代，所以我们重点介绍AspectJ引入并别Spring沿用的概念做介绍。对于AspectJ来说，
它对java的扩展主要体现在引入或者抽象出了如下的一些概念：
###### Join Point - 连接点
join point 模型是AOP引入的最核心的概念，他提供了动态定义横切关注点结构的基础框架。我们可以简单的将join point理解为
程序执行过程中的某些点。AspectJ提供了多种形式的join point，单就方法调用连接点来说，当一个对象收到方法调用时被触发，
其中包含了构成一次方法调用的所有信息，在到达这个连接点所需要的所有参数值计算完毕时开始，包括这个方法的正常返回或者抛出异常。
**每一次**方法调用都是一个不同的join point，即使来自同一个调用表达式。

###### Pointcuts - 切入点
切入点就是程序执行流程中的**某些**join point。接入点在选取到join point的同时还可以获取到程序执行的上下文信息。
这些上下文信息包括一些参数值等，可以用在Advice的定义中。。

###### Advice
切入点只是用来选择程序执行过程中的一些join point，advice定义了到达切入点时的行为 - Advice brings together a pointcut 
(to pick out join points) and a body of code (to run at each of those join points).
AspectJ定义了不同形式的Advice:
1. **Before advice** runs as a join point is reached, before the program proceeds with the join point. 
2. **After advice** on a particular join point runs after the program proceeds with that join point.    
    Because Java programs can leave a join point 'normally' or by throwing an exception,    
    there are three kinds of after advice: 
    1. after returning, 
    2. after throwing, 
    3. plain after (which runs after returning or throwing, like Java's finally). 
3. **Around advice** on a join point runs as the join point is reached, and has explicit control over 
whether the program proceeds with the join point. 

###### Inter-type declarations
Inter-type declarations in AspectJ are declarations that cut across classes and their hierarchies. 
They may declare members that cut across multiple classes, or change the inheritance relationship between classes.

###### Aspects - 切面
Aspect把pointcut/advice/inter-type declarations组装在一起，形成一个切面的实现。
 
#### Special Thanks
http://www.importnew.com/24305.html   
https://www.ibm.com/developerworks/cn/java/j-lo-springaopcglib/   
https://blog.csdn.net/dreamrealised/article/details/12885739   
https://blog.csdn.net/innost/article/details/49387395