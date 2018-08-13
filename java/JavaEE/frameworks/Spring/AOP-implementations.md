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

#### JDK动态代理
首先我们来写一个最简单的JDK动态代理的实现，其核心是InvocationHandler和Proxy两个类
```java
interface ITarget {
    void tarMethod();
}

/** 使用JDK的动态代理要求被代理类实现一个接口，代理类面向此接口 */
class Target implements ITarget {
    @Override
    public void tarMethod() {
        System.out.println("In target method");
    }
}

class LogInvocationHandler implements InvocationHandler {

    /** 代理的目标对象 */
    private Object target;

    public LogInvocationHandler(Object target) {
        super();
        this.target = target;
    }

    /** 代理方法 */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        System.out.println("=================Before=================");
        Object result = method.invoke(target, args);
        System.out.println("=================After=================");
        return result;
    }

    /** 获取代理对象 */
    public Object getProxy() {
        return Proxy.newProxyInstance(getClass().getClassLoader(), target.getClass().getInterfaces(), this);
    }
}

public class Test {

    public static void main(String[] args) {
        // 用于保存运行期生成的.class文件
        System.getProperties().put("sun.misc.ProxyGenerator.saveGeneratedFiles", "true");
        
        Target target = new Target();
        LogInvocationHandler invocationHandler = new LogInvocationHandler(target);
        ITarget proxy = (ITarget) invocationHandler.getProxy();
        proxy.tarMethod();
    }
}
```
这个是我们在学习JDK动态代理的时候都会写的最贱的一个例子，但是我们想看一下Proxy类在帮我们生成代理类并创建代理类对象的时候究竟做了
哪些工作。运行上述程序在程序正常输出的同时，我们可以在项目根路径下找到与Target所在的包路径相同的文件夹下找到在运行期生成的代理类的
.class文件，在这个例子中我们拿到的是一个名为$Proxy0.class的文件。
```java
final class $Proxy0 extends Proxy implements ITarget {
    private static Method m1;// equals()
    private static Method m2;// toString()
    private static Method m3;// tarMethod()
    private static Method m0;// hashCode()

    public $Proxy0(InvocationHandler var1) throws  {
        super(var1);
    }

    public final void tarMethod() throws  {
        try {
            super.h.invoke(this, m3, (Object[])null);
        } catch (RuntimeException | Error var2) {
            throw var2;
        } catch (Throwable var3) {
            throw new UndeclaredThrowableException(var3);
        }
    }

    static {
        try {
            m1 = Class.forName("java.lang.Object").getMethod("equals", new Class[]{Class.forName("java.lang.Object")});
            m2 = Class.forName("java.lang.Object").getMethod("toString", new Class[0]);
            m3 = Class.forName("com.jr.test.aop.proxy.jdk.ITarget").getMethod("tarMethod", new Class[0]);
            m0 = Class.forName("java.lang.Object").getMethod("hashCode", new Class[0]);
        } catch (NoSuchMethodException var2) {
            throw new NoSuchMethodError(var2.getMessage());
        } catch (ClassNotFoundException var3) {
            throw new NoClassDefFoundError(var3.getMessage());
        }
    }
    
    // hashCode()/toString()/equals() dispatched to InvocationHandler.invoke()
}
```
通过newProxyInstance()方法获取到的proxy对象实际上是这个$Proxy0类型的，他和Target类型一样实现了ITarget接口，所以这个代理类
还可以被当做目标类被嵌套代理；另外它还继承了Proxy类，但是这个代理类被定义成final的，防止再被继承。在其中我们发现了Object类的三个
基础方法hashCode()/toString()/equals()，并且看到代理类有一个传入InvocationHandler参数的构造器。
下面我们来看一下Proxy.java的源码，分析这个$Proxy0的字节码是如何生成的(我们只抽离出了关键代码)。
```java
// 注意：我们省略了大量代码
public class Proxy implements java.io.Serializable {
    /**
     * Returns an instance of a proxy class for the specified interfaces 
     * that dispatches method invocations to the specified invocation handler.
     */
    @CallerSensitive
    public static Object newProxyInstance(ClassLoader loader, Class<?>[] interfaces, InvocationHandler h) 
            throws IllegalArgumentException {
        final Class<?>[] intfs = interfaces.clone();
        /** Look up or generate the designated proxy class */
        Class<?> cl = getProxyClass0(loader, intfs);

        /** Invoke its constructor with the designated invocation handler */
        try {
            final Constructor<?> cons = cl.getConstructor(constructorParams);
            return cons.newInstance(new Object[]{h});
        } catch (VarianceKindOfException e) {
            // ...
        }
    }

    /** 
     * Generate a proxy class 
     * If the proxy class defined by the given loader implementing the given interfaces exists, this will simply 
     * return the cached copy; otherwise, it will create the proxy class via the ProxyClassFactory
     */
    private static Class<?> getProxyClass0(ClassLoader loader, Class<?>... interfaces) {
        return proxyClassCache.get(loader, interfaces);
    }
}
```
我们可以看到，newProxyInstance实际上只做了通过反射调用代理类class文件的构造器返回代理对象，真正生成代理类字节码的逻辑是由
proxyClassCache来承担的。下面是这个属性和WeakCache类的定义：

    /** a cache of proxy classes */
    private static final WeakCache<ClassLoader, Class<?>[], Class<?>> proxyClassCache 
                         = new WeakCache<>(new KeyFactory(), new ProxyClassFactory());
```java
/**
 * Cache mapping pairs of (key, sub-key) -> value. Keys and values are weakly but sub-keys are strongly referenced. 
 * Keys are passed directly to get() method which also takes aparameter. Sub-keys are calculated from keys and 
 * parameters using the subKeyFactory function passed to the constructor. Values are calculated from keys and 
 * parameters using the valueFactory function passed to the constructor.
 */
final class WeakCache<K, P, V> {
    // ...
    public WeakCache(BiFunction<K, P, ?> subKeyFactory, BiFunction<K, P, V> valueFactory) {
        this.subKeyFactory = Objects.requireNonNull(subKeyFactory);
        this.valueFactory = Objects.requireNonNull(valueFactory);
    }
    // ...
}
```
关于WeakCache的实现与我们所探讨的问题关系不大，它还是作为我们生成的代理类的缓存工具使用，我们来看最终提供创建代理类字节码的
ProxyClassFactory是如何实现的。
```java
/**
 * A factory function that generates, defines and returns the proxy class given
 * the ClassLoader and array of interfaces.
 */
private static final class ProxyClassFactory implements BiFunction<ClassLoader, Class<?>[], Class<?>> {
    // prefix for all proxy class names
    private static final String proxyClassNamePrefix = "$Proxy";
    // next number to use for generation of unique proxy class names
    private static final AtomicLong nextUniqueNumber = new AtomicLong();

    @Override
    public Class<?> apply(ClassLoader loader, Class<?>[] interfaces) {

        // validations:
        Map<Class<?>, Boolean> interfaceSet = new IdentityHashMap<>(interfaces.length);
        for (Class<?> intf : interfaces) {
            /* Verify that the class loader resolves the name of this interface to the same Class object */
            Class<?> interfaceClass;
            try {
                interfaceClass = Class.forName(intf.getName(), false, loader);
            } catch (ClassNotFoundException e) { }
            
            if (interfaceClass != intf) 
                throw new IllegalArgumentException(intf + " is not visible from class loader");
            
            /* Verify that the Class object actually represents an interface */
            if (!interfaceClass.isInterface()) 
                throw new IllegalArgumentException(interfaceClass.getName() + " is not an interface");
            
            /* Verify that this interface is not a duplicate */
            if (interfaceSet.put(interfaceClass, Boolean.TRUE) != null) {
                throw new IllegalArgumentException("repeated interface: " + interfaceClass.getName());
            }
        }

        String proxyPkg = null;     // package to define proxy class in
        int accessFlags = Modifier.PUBLIC | Modifier.FINAL;

        /** Record the package of a non-public proxy interface so that the proxy class will be defined in 
         * the same package. Verify that all non-public proxy interfaces are in the same package */
        for (Class<?> intf : interfaces) {
            int flags = intf.getModifiers();
            if (!Modifier.isPublic(flags)) {
                accessFlags = Modifier.FINAL;
                String name = intf.getName();
                int n = name.lastIndexOf('.');
                String pkg = ((n == -1) ? "" : name.substring(0, n + 1));
                if (proxyPkg == null) 
                    proxyPkg = pkg;
                else if (!pkg.equals(proxyPkg)) 
                    throw new IllegalArgumentException("non-public interfaces from different packages");
            }
        }

        // if no non-public proxy interfaces, use com.sun.proxy package
        if (proxyPkg == null) 
            proxyPkg = ReflectUtil.PROXY_PACKAGE + ".";

        /* Choose a name for the proxy class to generate */
        long num = nextUniqueNumber.getAndIncrement();
        String proxyName = proxyPkg + proxyClassNamePrefix + num;

        /* Generate the specified proxy class */
        byte[] proxyClassFile = ProxyGenerator.generateProxyClass(proxyName, interfaces, accessFlags);
        try {
            return defineClass0(loader, proxyName, proxyClassFile, 0, proxyClassFile.length);
        } catch (ClassFormatError e) {
            /** A ClassFormatError here means that (barring bugs in the proxy class generation code) there was 
             * some other invalid aspect of the arguments supplied to the proxy class creation (such as 
             * virtual machine limitations exceeded) */
            throw new IllegalArgumentException(e.toString());
        }
    }
}
```
apply()方法用于完成参数的校验并计算代理类的包名/类名/权限修饰符等，然后拿着这些参数调用sun.misc.ProxyGenerator来生成字节码。
```java
public class ProxyGenerator {

    public static byte[] generateProxyClass(final String proxyName, Class<?>[] interfaces, int accessFlags) {
        ProxyGenerator proxyGenerator = new ProxyGenerator(proxyName, interfaces, accessFlags);
        final byte[] classFile = proxyGenerator.generateClassFile();
        if(saveGeneratedFiles) {// System.getProperties().put("sun.misc.ProxyGenerator.saveGeneratedFiles", "true");
            // save .class file to local directory
        }
        return classFile;
    }

   private byte[] generateClassFile() {
        // 添Object类的三个基础方法
        // 添加代理接口带来的方法和属性
    }
}
```
#### CGLIB动态代理
CGLIB(Code Generation Library)是一个代码生成类库，它可以在运行时候动态生成某个类的子类。
跟JDK的动态代理不同的是，使用CGLIB不需要目标类实现任何接口，任何类只要没有被定义为final类都可以被代理。例如：
```java
public class Target {
    void tarMethod(){
        System.out.println("Target method");
    }
}
```
代理类需要实现MethodInterceptor接口并实现其中默认的intercept方法，并将advice织入
```java
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import java.lang.reflect.Method;

public class CglibProxy implements MethodInterceptor {
    @Override
    public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
        System.out.println("=================Before=================");
        Object res = proxy.invokeSuper(obj, args);
        System.out.println("=================After=================");
        return res;
    }
}
```
类似于使用JDK动态代理时的getProxy()方法，使用CGLIB时可以定义ProxyFactory通过工厂生产代理类对象。关键是Enhancer.create()方法。
```java
import net.sf.cglib.proxy.Enhancer;

public class ProxyFactory {
    public static Target getInstance(CglibProxy proxy) {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(Target.class);
        enhancer.setCallback(proxy);
        return (Target) enhancer.create();
    }
}
```
```java
public class Test {
    public static void main(String[] args) {
        Target target = ProxyFactory.getInstance(new CglibProxy());
        target.tarMethod();
    }
}
```
#### 比较两种代理方式
+=======+================+======================+==========================+
|       | 对目标类的要求   | advice织入方法        | 字节码生成方式            |
+-------+----------------+----------------------+--------------------------+
| JDK   | 至少实现一个接口 | 实现InvocationHandler | sun.misc.ProxyGenerator |
+-------+----------------+---------+------------+--------------------------+
| CGLIB | 非final        | 实现MethodInterceptor | Enhancer                 |
+=======+================+=========+============+==========================+
#### Special Thanks
http://www.importnew.com/24305.html   
https://www.ibm.com/developerworks/cn/java/j-lo-springaopcglib/   
https://blog.csdn.net/dreamrealised/article/details/12885739   
https://blog.csdn.net/innost/article/details/49387395
