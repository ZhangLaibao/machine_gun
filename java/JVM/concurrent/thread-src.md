```java
package java.lang;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.security.AccessController;
import java.security.AccessControlContext;
import java.security.PrivilegedAction;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.LockSupport;
import sun.nio.ch.Interruptible;
import sun.reflect.CallerSensitive;
import sun.reflect.Reflection;
import sun.security.util.SecurityConstants;


/**
 * A thread is a thread of execution in a program. The JVM allows an application to have multiple threads of
 * execution running concurrently. Every thread has a priority. Threads with higher priority are executed 
 * in preference to threads with lower priority. Each thread may or may not also be marked as a daemon. 
 * When code running in some thread creates a new Thread object, the new thread has its priority initially 
 * set equal to the priority of the creating thread, and is a daemon thread if and only if the 
 * creating thread is a daemon.
 * 
 * When a JVM starts up, there is usually a single non-daemon thread (which typically calls the method named
 * main of some designated class). The JVM continues to execute threads until either of the following
 * occurs:
 * 1.The exit method of class Runtime has been called and the security manager has permitted the exit operation
 * to take place.
 * 2.All threads that are not daemon threads have died, either by returning from the call to the run method or 
 * by throwing an exception that propagates beyond the run method.
 * 
 * There are two ways to create a new thread of execution. One is to declare a class to be a subclass of Thread. 
 * This subclass should override the run method of class Thread. An instance of the subclass can then be
 * allocated and started. For example, a thread that computes primes larger than a stated value could 
 * be written as follows:
 * 
 *     class PrimeThread extends Thread {
 *         long minPrime;
 *         PrimeThread(long minPrime) {
 *             this.minPrime = minPrime;
 *         }
 *
 *         public void run() {
 *             // compute primes larger than minPrime
 *         }
 *     }
 * 
 * The following code would then create a thread and start it running:
 * 
 *     PrimeThread p = new PrimeThread(143);
 *     p.start();
 * 
 * The other way to create a thread is to declare a class that implements the Runnable interface. That class then
 * implements the run method. An instance of the class can then be allocated, passed as an argument when creating 
 * Thread and started. The same example in this other style looks like the following:
 * 
 *     class PrimeRun implements Runnable {
 *         long minPrime;
 *         PrimeRun(long minPrime) {
 *             this.minPrime = minPrime;
 *         }
 *
 *         public void run() {
 *             // compute primes larger than minPrime
 *         }
 *     }
 * 
 * The following code would then create a thread and start it running:
 * 
 *     PrimeRun p = new PrimeRun(143);
 *     new Thread(p).start();
 * 
 * Every thread has a name for identification purposes. More than one thread may have the same name. 
 * If a name is not specified when a thread is created, a new name is generated for it.
 * Unless otherwise noted, passing a null argument to a constructor or method in this class will cause a
 * NullPointerException to be thrown.
 */
public class Thread implements Runnable {// Thread类也实现了Runnable接口
    /* Make sure registerNatives is the first thing <clinit> does. */
    private static native void registerNatives();
    static {
        registerNatives();
    }

    private volatile String name;
    private int            priority;
    private Thread         threadQ;
    private long           eetop;

    /* Whether or not to single_step this thread. */
    private boolean     single_step;

    /* Whether or not the thread is a daemon thread. */
    private boolean     daemon = false;

    /* JVM state */
    private boolean     stillborn = false;

    /* What will be run. */
    private Runnable target;

    /* The group of this thread */
    private ThreadGroup group;

    /* The context ClassLoader for this thread */
    private ClassLoader contextClassLoader;

    /* The inherited AccessControlContext of this thread */
    private AccessControlContext inheritedAccessControlContext;

    /* For autonumbering anonymous threads. */
    private static int threadInitNumber;
    private static synchronized int nextThreadNum() {
        return threadInitNumber++;
    }

    /* ThreadLocal values pertaining to this thread. This map is maintained by the ThreadLocal class.*/
    ThreadLocal.ThreadLocalMap threadLocals = null;

    /*
     * InheritableThreadLocal values pertaining to this thread. This map is
     * maintained by the InheritableThreadLocal class.
     */
    ThreadLocal.ThreadLocalMap inheritableThreadLocals = null;

    /*
     * The requested stack size for this thread, or 0 if the creator did not specify a stack size. 
     * It is up to the VM to do whatever it likes with this number; some VMs will ignore it.
     */
    private long stackSize;

    /* JVM-private state that persists after native thread termination. */
    private long nativeParkEventPointer;

    /* Thread ID */
    private long tid;

    /* For generating thread ID */
    private static long threadSeqNumber;

    /* Java thread status for tools, initialized to indicate thread 'not yet started' */
    private volatile int threadStatus = 0;


    private static synchronized long nextThreadID() {
        return ++threadSeqNumber;
    }

    /**
     * The argument supplied to the current call to java.util.concurrent.locks.LockSupport.park.
     * Set by (private) java.util.concurrent.locks.LockSupport.setBlocker
     * Accessed using java.util.concurrent.locks.LockSupport.getBlocker
     */
    volatile Object parkBlocker;

    /**
     * The object in which this thread is blocked in an interruptible I/O operation, if any.
     * The blocker's interrupt method should be invoked after setting this thread's interrupt status.
     */
    private volatile Interruptible blocker;
    private final Object blockerLock = new Object();

    /* Set the blocker field; invoked via sun.misc.SharedSecrets from java.nio code */
    void blockedOn(Interruptible b) {
        synchronized (blockerLock) {
            blocker = b;
        }
    }

    // The minimum priority that a thread can have.
    public final static int MIN_PRIORITY = 1;

    // The default priority that is assigned to a thread.
    public final static int NORM_PRIORITY = 5;

    // The maximum priority that a thread can have.
    public final static int MAX_PRIORITY = 10;

    // Returns a reference to the currently executing thread object. 
    public static native Thread currentThread();

    /**
     * A hint to the scheduler that the current thread is willing to yield its current use of a processor. 
     * The scheduler is free to ignore this hint.
     * 
     * Yield is a heuristic attempt to improve relative progression between threads that would otherwise 
     * over-utilise a CPU. Its use should be combined with detailed profiling and benchmarking to ensure that 
     * it actually has the desired effect.
     *
     * It is rarely appropriate to use this method. It may be useful for debugging or testing purposes, 
     * where it may help to reproduce bugs due to race conditions. It may also be useful when designing
     * concurrency control constructs such as the ones in the java.util.concurrent.locks package.
     */
    public static native void yield();

    /**
     * Causes the currently executing thread to sleep (temporarily cease execution) for the specified number of 
     * milliseconds, subject to the precision and accuracy of system timers and schedulers. The thread does not 
     * lose ownership of any monitors.
     */
    public static native void sleep(long millis) throws InterruptedException;

    /**
     * Causes the currently executing thread to sleep (temporarily cease execution) for the specified number of 
     * milliseconds plus the specified number of nanoseconds, subject to the precision and accuracy of system
     * timers and schedulers. The thread does not lose ownership of any monitors.
     */
    public static void sleep(long millis, int nanos) throws InterruptedException {
        if (millis < 0) 
            throw new IllegalArgumentException("timeout value is negative");

        if (nanos < 0 || nanos > 999999) 
            throw new IllegalArgumentException("nanosecond timeout value out of range");

        if (nanos >= 500000 || (nanos != 0 && millis == 0)) 
            millis++;

        sleep(millis);
    }

    /* Initializes a Thread with the current AccessControlContext. */
    private void init(ThreadGroup g, Runnable target, String name, long stackSize) {
        init(g, target, name, stackSize, null);
    }

    private void init(ThreadGroup g, Runnable target, String name, long stackSize, AccessControlContext acc) {
        if (name == null) 
            throw new NullPointerException("name cannot be null");

        this.name = name;

        Thread parent = currentThread();
        SecurityManager security = System.getSecurityManager();
        if (g == null) {
            /* Determine if it's an applet or not */

            /* If there is a security manager, ask the security manager what to do. */
            if (security != null) {
                g = security.getThreadGroup();
            }

            /* If the security doesn't have a strong opinion of the matter use the parent thread group. */
            if (g == null) {
                g = parent.getThreadGroup();
            }
        }

        /* checkAccess regardless of whether or not threadgroup is explicitly passed in. */
        g.checkAccess();

        // Do we have the required permissions?
        if (security != null) {
            if (isCCLOverridden(getClass())) {
                security.checkPermission(SUBCLASS_IMPLEMENTATION_PERMISSION);
            }
        }

        g.addUnstarted();

        this.group = g;
        this.daemon = parent.isDaemon();
        this.priority = parent.getPriority();
        if (security == null || isCCLOverridden(parent.getClass()))
            this.contextClassLoader = parent.getContextClassLoader();
        else
            this.contextClassLoader = parent.contextClassLoader;
        this.inheritedAccessControlContext = acc != null ? acc : AccessController.getContext();
        this.target = target;
        setPriority(priority);
        if (parent.inheritableThreadLocals != null)
            this.inheritableThreadLocals = ThreadLocal.createInheritedMap(parent.inheritableThreadLocals);
        /* Stash the specified stack size in case the VM cares */
        this.stackSize = stackSize;

        /* Set thread ID */
        tid = nextThreadID();
    }

    /**
     * Throws CloneNotSupportedException as a Thread can not be meaningfully cloned. Construct a new Thread instead.
     */
    @Override
    protected Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }

    /**
     * Allocates a new Thread object. This constructor has the same effect as Thread(ThreadGroup,Runnable,String) 
     * Thread(null, null, gname), where gname is a newly generated name. Automatically generated names are 
     * of the form "Thread-"+n, where n is an integer.
     */
    public Thread() {
        init(null, null, "Thread-" + nextThreadNum(), 0);
    }

    public Thread(Runnable target) {
        init(null, target, "Thread-" + nextThreadNum(), 0);
    }

    Thread(Runnable target, AccessControlContext acc) {
        init(null, target, "Thread-" + nextThreadNum(), 0, acc);
    }

    public Thread(ThreadGroup group, Runnable target) {
        init(group, target, "Thread-" + nextThreadNum(), 0);
    }

    public Thread(String name) {
        init(null, null, name, 0);
    }

    public Thread(ThreadGroup group, String name) {
        init(group, null, name, 0);
    }

    public Thread(Runnable target, String name) {
        init(null, target, name, 0);
    }

    public Thread(ThreadGroup group, Runnable target, String name) {
        init(group, target, name, 0);
    }

    /**
     * Allocates a new Thread object so that it has target as its run object, has the specified name as its name,
     * and belongs to the thread group referred to by group, and has the specified stack size 
     *
     * This constructor is identical to Thread(ThreadGroup,Runnable,String) with the exception of the fact that 
     * it allows the thread stack size to be specified. The stack size is the approximate number of bytes of 
     * address space that the virtual machine is to allocate for this thread's stack. The effect of the
     * stackSize parameter is highly platform dependent 
     *
     * On some platforms, specifying a higher value for the stackSize parameter may allow a thread to achieve 
     * greater recursion depth before throwing a StackOverflowError. Similarly, specifying a lower value may 
     * allow a greater number of threads to exist concurrently without throwing an OutOfMemoryError 
     * (or other internal error).  The details of the relationship between the value of the stackSize parameter
     * and the maximum recursion depth and concurrency level are platform-dependent. On some platforms, 
     * the value of the stackSize parameter may have no effect whatsoever. 
     *
     * The virtual machine is free to treat the stackSize parameter as a suggestion. If the specified value is 
     * unreasonably low for the platform, the virtual machine may instead use some platform-specific minimum value; 
     * if the specified value is unreasonably high, the virtual machine may instead use some platform-specific
     * maximum. Likewise, the virtual machine is free to round the specified value up or down as it sees fit 
     * (or to ignore it completely).
     *
     * Specifying a value of zero for the stackSize parameter will cause this constructor to behave exactly like the
     * Thread(ThreadGroup, Runnable, String) constructor.
     *
     * Due to the platform-dependent nature of the behavior of this constructor, extreme care should be exercised 
     * in its use. The thread stack size necessary to perform a given computation will likely vary from one 
     * JRE implementation to another. In light of this variation, careful tuning of the stack size parameter 
     * may be required, and the tuning may need to be repeated for each JRE implementation on which 
     * an application is to run. 
     */
    public Thread(ThreadGroup group, Runnable target, String name, long stackSize) {
        init(group, target, name, stackSize);
    }

    /**
     * Causes this thread to begin execution; the Java Virtual Machine calls the run method of this thread.
     * 
     * The result is that two threads are running concurrently: the current thread (which returns from the call to 
     * the start method) and the other thread (which executes its run method).
     * 
     * It is never legal to start a thread more than once. In particular, a thread may not be restarted once 
     * it has completed execution.
     */
    public synchronized void start() {
        /**
         * This method is not invoked for the main method thread or "system" group threads created/set up by the VM. 
         * Any new functionality added to this method in the future may have to also be added to the VM.
         *
         * A zero status value corresponds to state "NEW".
         */
        if (threadStatus != 0)
            throw new IllegalThreadStateException();

        /* Notify the group that this thread is about to be started so that it can be added to the group's 
         * list of threads and the group's unstarted count can be decremented. */
        group.add(this);

        boolean started = false;
        try {
            start0();
            started = true;
        } finally {
            try {
                if (!started) 
                    group.threadStartFailed(this);
            } catch (Throwable ignore) {
                /* do nothing. If start0 threw a Throwable then it will be passed up the call stack */
            }
        }
    }

    private native void start0();

    /**
     * If this thread was constructed using a separate Runnable run object, then that Runnable object's run 
     * method is called; otherwise, this method does nothing and returns. 
     * Subclasses of Thread should override this method.
     */
    @Override
    public void run() {
        if (target != null)
            target.run();
    }

    // This method is called by the system to give a Thread a chance to clean up before it actually exits.
    private void exit() {
        if (group != null) {
            group.threadTerminated(this);
            group = null;
        }
        /* Aggressively null out all reference fields: see bug 4006245 */
        target = null;
        /* Speed the release of some of these resources */
        threadLocals = null;
        inheritableThreadLocals = null;
        inheritedAccessControlContext = null;
        blocker = null;
        uncaughtExceptionHandler = null;
    }

    /**
     * Interrupts this thread. Unless the current thread is interrupting itself, which is always permitted, 
     * the checkAccess() method of this thread is invoked, which may cause a SecurityException to be thrown.
     *
     * If this thread is blocked in an invocation of the Object.wait(), or of the join(), sleep(long) methods 
     * of this class, then its interrupt status will be cleared and it will receive an InterruptedException.
     *
     * If this thread is blocked in an I/O operation upon an java.nio.channels.InterruptibleChannel then 
     * the channel will be closed, the thread's interrupt status will be set, and the thread will receive a 
     * java.nio.channels.ClosedByInterruptException.
     *
     * If this thread is blocked in a java.nio.channels.Selector then the thread's interrupt status will be set 
     * and it will return immediately from the selection operation, possibly with a non-zero value, just as if 
     * the selector's java.nio.channels.Selector.wakeup() method were invoked.
     *
     * If none of the previous conditions hold then this thread's interrupt status will be set. 
     * Interrupting a thread that is not alive need not have any effect.
     */
    public void interrupt() {
        if (this != Thread.currentThread())
            checkAccess();

        synchronized (blockerLock) {
            Interruptible b = blocker;
            if (b != null) {
                interrupt0();           // Just to set the interrupt flag
                b.interrupt(this);
                return;
            }
        }
        interrupt0();
    }

    /**
     * Tests whether the current thread has been interrupted. The interrupted status of the thread is cleared 
     * by this method. In other words, if this method were to be called twice in succession, the second call 
     * would return false (unless the current thread were interrupted again, after the first call had cleared 
     * its interrupted status and before the second call had examined it).
     *
     * A thread interruption ignored because a thread was not alive at the time of the interrupt will be 
     * reflected by this method returning false.
     */
    public static boolean interrupted() {
        return currentThread().isInterrupted(true);
    }

    /**
     * Tests whether this thread has been interrupted. The interrupted status of the thread is unaffected 
     * by this method. A thread interruption ignored because a thread was not alive at the time of the interrupt 
     * will be reflected by this method returning false.
     */
    public boolean isInterrupted() {
        return isInterrupted(false);
    }

    /**
     * Tests if some Thread has been interrupted. The interrupted state is reset or not based on the value of 
     * ClearInterrupted that is passed.
     */
    private native boolean isInterrupted(boolean ClearInterrupted);

    /**
     * Tests if this thread is alive. A thread is alive if it has been started and has not yet died.
     */
    public final native boolean isAlive();

    /**
     * Changes the priority of this thread.
     *  
     * First the checkAccess method of this thread is called with no arguments. This may result in throwing a
     * SecurityException. Otherwise, the priority of this thread is set to the smaller of the specified newPriority 
     * and the maximum permitted priority of the thread's thread group.
     */
    public final void setPriority(int newPriority) {
        ThreadGroup g;
        checkAccess();
        if (newPriority > MAX_PRIORITY || newPriority < MIN_PRIORITY) {
            throw new IllegalArgumentException();
        }
        if((g = getThreadGroup()) != null) {
            if (newPriority > g.getMaxPriority()) {
                newPriority = g.getMaxPriority();
            }
            setPriority0(priority = newPriority);
        }
    }

    /**
     * Returns this thread's priority.
     */
    public final int getPriority() {
        return priority;
    }

    /**
     * Changes the name of this thread to be equal to the argument name 
     */
    public final synchronized void setName(String name) {
        checkAccess();
        if (name == null) {
            throw new NullPointerException("name cannot be null");
        }

        this.name = name;
        if (threadStatus != 0) {
            setNativeName(name);
        }
    }

    /**
     * Returns this thread's name.
     */
    public final String getName() {
        return name;
    }

    /**
     * Returns the thread group to which this thread belongs. This method returns null if this thread has died
     * (been stopped).
     */
    public final ThreadGroup getThreadGroup() {
        return group;
    }

    /**
     * Returns an estimate of the number of active threads in the current thread's thread group and its subgroups. 
     * Recursively iterates over all subgroups in the current thread's thread group.
     *
     * The value returned is only an estimate because the number of  threads may change dynamically while 
     * this method traverses internal data structures, and might be affected by the presence of certain
     * system threads. This method is intended primarily for debugging and monitoring purposes.
     */
    public static int activeCount() {
        return currentThread().getThreadGroup().activeCount();
    }

    /**
     * Copies into the specified array every active thread in the current thread's thread group and its subgroups. 
     * This method simply invokes the java.lang.ThreadGroup.enumerate(Thread[]) method of the current thread's 
     * thread group.
     *
     * An application might use the activeCount() method to get an estimate of how big the array should be, 
     * however if the array is too short to hold all the threads, the extra threads are silently ignored.
     * If it is critical to obtain every active thread in the current thread's thread group and its subgroups, 
     * the invoker should verify that the returned int value is strictly less than the length of tarray.
     *
     * Due to the inherent race condition in this method, it is recommended that the method only be used for 
     * debugging and monitoring purposes.
     */
    public static int enumerate(Thread tarray[]) {
        return currentThread().getThreadGroup().enumerate(tarray);
    }


    /**
     * Waits at most millis milliseconds for this thread to die. A timeout of 0 means to wait forever.
     *
     * This implementation uses a loop of this.wait calls conditioned on this.isAlive. As a thread terminates 
     * the this.notifyAll method is invoked. It is recommended that applications not use wait(), notify(), or
     * notifyAll() on Thread instances.
     */
    public final synchronized void join(long millis) throws InterruptedException {
        long base = System.currentTimeMillis();
        long now = 0;

        if (millis < 0) 
            throw new IllegalArgumentException("timeout value is negative");

        if (millis == 0) {
            while (isAlive()) 
                wait(0);
        } else {
            while (isAlive()) {
                long delay = millis - now;
                if (delay <= 0) 
                    break;
                wait(delay);
                now = System.currentTimeMillis() - base;
            }
        }
    }

    /**
     * Waits at most millis milliseconds plus nanos nanoseconds for this thread to die.
     */
    public final synchronized void join(long millis, int nanos) throws InterruptedException {

        if (millis < 0)  
            throw new IllegalArgumentException("timeout value is negative");

        if (nanos < 0 || nanos > 999999) 
            throw new IllegalArgumentException("nanosecond timeout value out of range");
 
        if (nanos >= 500000 || (nanos != 0 && millis == 0)) 
            millis++;

        join(millis);
    }

    /**
     * Waits for this thread to die. An invocation of this method behaves in exactly the same
     * way as the invocation
     */
    public final void join() throws InterruptedException {
        join(0);
    }

    /**
     * Prints a stack trace of the current thread to the standard error stream for debugging.
     */
    public static void dumpStack() {
        new Exception("Stack trace").printStackTrace();
    }

    /**
     * Marks this thread as either a daemon thread or a user thread. The JVM exits when the only
     * threads running are all daemon threads.
     */
    public final void setDaemon(boolean on) {
        checkAccess();
        if (isAlive()) 
            throw new IllegalThreadStateException();
 
        daemon = on;
    }

    /**
     * Tests if this thread is a daemon thread.
     */
    public final boolean isDaemon() {
        return daemon;
    }

    /**
     * Determines if the currently running thread has permission to modify this thread.
     * 
     * If there is a security manager, its checkAccess method is called with this thread as its argument. 
     * This may result in throwing a SecurityException.
     */
    public final void checkAccess() {
        SecurityManager security = System.getSecurityManager();
        if (security != null) 
            security.checkAccess(this);
    }


    /**
     * Returns the context ClassLoader for this Thread. The context ClassLoader is provided by the creator 
     * of the thread for use by code running in this thread when loading classes and resources. If not set, 
     * the default is the ClassLoader context of the parent Thread. The context ClassLoader of the
     * primordial thread is typically set to the class loader used to load the application.
     *
     * If a security manager is present, and the invoker's class loader is not null and is not the same as 
     * or an ancestor of the context class loader, then this method invokes the security manager's
     * checkPermission() method with a RuntimePermission("getClassLoader") permission to verify that 
     * retrieval of the context class loader is permitted.
     */
    @CallerSensitive
    public ClassLoader getContextClassLoader() {
        if (contextClassLoader == null)
            return null;
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) 
            ClassLoader.checkClassLoaderPermission(contextClassLoader, Reflection.getCallerClass());
        return contextClassLoader;
    }

    /**
     * Sets the context ClassLoader for this Thread. The context ClassLoader can be set when a thread is created, 
     * and allows the creator of the thread to provide the appropriate class loader, through getContextClassLoader, 
     * to code running in the thread when loading classes and resources.
     *
     * If a security manager is present, its checkPermission() method is invoked with a 
     * RuntimePermission("setContextClassLoader") permission to see if setting the context ClassLoader is permitted.
     */
    public void setContextClassLoader(ClassLoader cl) {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) 
            sm.checkPermission(new RuntimePermission("setContextClassLoader"));
        contextClassLoader = cl;
    }

    // Returns true if and only if the current thread holds the monitor lock on the specified object.
    public static native boolean holdsLock(Object obj);

    private static final StackTraceElement[] EMPTY_STACK_TRACE = new StackTraceElement[0];

    /**
     * Returns an array of stack trace elements representing the stack dump of this thread.  This method will 
     * return a zero-length array if this thread has not started, has started but has not yet been scheduled 
     * to run by the system, or has terminated.
     * If the returned array is of non-zero length then the first element of the array represents the top of 
     * the stack, which is the most recent method invocation in the sequence. The last element of the array
     * represents the bottom of the stack, which is the least recent method invocation in the sequence.
     *
     * If there is a security manager, and this thread is not the current thread, then the security manager's
     * checkPermission method is called with a RuntimePermission("getStackTrace") to see if it's ok to 
     * get the stack trace.
     *
     * Some virtual machines may, under some circumstances, omit one or more stack frames from the stack trace.
     * In the extreme case, a virtual machine that has no stack trace information concerning this thread 
     * is permitted to return a zero-length array from this method.
     */
    public StackTraceElement[] getStackTrace() {
        if (this != Thread.currentThread()) {
            // check for getStackTrace permission
            SecurityManager security = System.getSecurityManager();
            if (security != null) 
                security.checkPermission(SecurityConstants.GET_STACK_TRACE_PERMISSION);
            // optimization so we do not call into the vm for threads that
            // have not yet started or have terminated
            if (!isAlive()) 
                return EMPTY_STACK_TRACE; 
            StackTraceElement[][] stackTraceArray = dumpThreads(new Thread[] {this});
            StackTraceElement[] stackTrace = stackTraceArray[0];
            // a thread that was alive during the previous isAlive call may have
            // since terminated, therefore not having a stacktrace.
            if (stackTrace == null) 
                stackTrace = EMPTY_STACK_TRACE; 
            return stackTrace;
        } else {
            // Don't need JVM help for current thread
            return (new Exception()).getStackTrace();
        }
    }

    /**
     * Returns a map of stack traces for all live threads. The map keys are threads and each map value is 
     * an array of StackTraceElement that represents the stack dump of the corresponding Thread.
     * The returned stack traces are in the format specified for the getStackTrace() method.
     *
     * The threads may be executing while this method is called. The stack trace of each thread only represents 
     * a snapshot and each stack trace may be obtained at different time. A zero-length array will be returned 
     * in the map value if the virtual machine has no stack trace information about a thread.
     */
    public static Map<Thread, StackTraceElement[]> getAllStackTraces() {
        // check for getStackTrace permission
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkPermission(SecurityConstants.GET_STACK_TRACE_PERMISSION);
            security.checkPermission(SecurityConstants.MODIFY_THREADGROUP_PERMISSION);
        }

        // Get a snapshot of the list of all threads
        Thread[] threads = getThreads();
        StackTraceElement[][] traces = dumpThreads(threads);
        Map<Thread, StackTraceElement[]> m = new HashMap<>(threads.length);
        for (int i = 0; i < threads.length; i++) {
            StackTraceElement[] stackTrace = traces[i];
            if (stackTrace != null) {
                m.put(threads[i], stackTrace);
            }
            // else terminated so we don't put it in the map
        }
        return m;
    }


    private static final RuntimePermission SUBCLASS_IMPLEMENTATION_PERMISSION =
                    new RuntimePermission("enableContextClassLoaderOverride");

    /** cache of subclass security audit results */
    /* Replace with ConcurrentReferenceHashMap when/if it appears in a future release */
    private static class Caches {
        /** cache of subclass security audit results */
        static final ConcurrentMap<WeakClassKey,Boolean> subclassAudits = new ConcurrentHashMap<>();

        /** queue for WeakReferences to audited subclasses */
        static final ReferenceQueue<Class<?>> subclassAuditsQueue = new ReferenceQueue<>();
    }

    /**
     * Verifies that this (possibly subclass) instance can be constructed without violating security constraints: 
     * the subclass must not override security-sensitive non-final methods, or else the 
     * "enableContextClassLoaderOverride" RuntimePermission is checked.
     */
    private static boolean isCCLOverridden(Class<?> cl) {
        if (cl == Thread.class)
            return false;

        processQueue(Caches.subclassAuditsQueue, Caches.subclassAudits);
        WeakClassKey key = new WeakClassKey(cl, Caches.subclassAuditsQueue);
        Boolean result = Caches.subclassAudits.get(key);
        if (result == null) {
            result = Boolean.valueOf(auditSubclass(cl));
            Caches.subclassAudits.putIfAbsent(key, result);
        }

        return result.booleanValue();
    }

    /**
     * Performs reflective checks on given subclass to verify that it doesn't override security-sensitive 
     * non-final methods. Returns true if the subclass overrides any of the methods, false otherwise.
     */
    private static boolean auditSubclass(final Class<?> subcl) {
        Boolean result = AccessController.doPrivileged(
            new PrivilegedAction<Boolean>() {
                public Boolean run() {
                    for (Class<?> cl = subcl; cl != Thread.class; cl = cl.getSuperclass()) {
                        try {
                            cl.getDeclaredMethod("getContextClassLoader", new Class<?>[0]);
                            return Boolean.TRUE;
                        } catch (NoSuchMethodException ex) {
                        }
                        try {
                            Class<?>[] params = {ClassLoader.class};
                            cl.getDeclaredMethod("setContextClassLoader", params);
                            return Boolean.TRUE;
                        } catch (NoSuchMethodException ex) {
                        }
                    }
                    return Boolean.FALSE;
                }
            }
        );
        return result.booleanValue();
    }

    private native static StackTraceElement[][] dumpThreads(Thread[] threads);
    private native static Thread[] getThreads();

    /**
     * Returns the identifier of this Thread. The thread ID is a positive long number generated when this thread 
     * was created. The thread ID is unique and remains unchanged during its lifetime.
     * When a thread is terminated, this thread ID may be reused.
     */
    public long getId() {
        return tid;
    }

    /**
     * A thread can be in only one state at a given point in time. These states are virtual machine states 
     * which do not reflect any operating system thread states.
     */
    public enum State {
        /**
         * 新建状态，没啥好说的
         * Thread state for a thread which has not yet started
         */ 
        NEW,

        /**
         * 就绪或者正常运行中
         * 所谓就绪READY是指调用了Thread.start()之后，该线程等待获取CPU时间片时的状态
         * 当获得了时间片开始执行时，线程处于运行RUNNING状态
         * 当然可能会有某种耗时计算/IO的操作/CPU时间片切换导致当前线程等待,这个状态下发生的等待一般是其他系统资源,而不是锁
         * 
         * Thread state for a runnable thread. A thread in the runnable state is executing in the JVM but it may
         * be waiting for other resources from the operating system such as processor.
         */
        RUNNABLE,

        /**
         * 等待锁 - 被阻塞
         * 
         * Thread state for a thread blocked waiting for a monitor lock. A thread in the blocked state is 
         * waiting for a monitor lock to enter a synchronized block/method or reenter a synchronized block/method 
         * after calling Object.wait() 
         */
        BLOCKED,

        /**
         * 线程拥有了某个锁之后,调用了他的wait方法,等待其他线程/锁拥有者调用notify/notifyAll通知该线程继续下一步操作
         * BLOCKED和WATING的区别：一个是在临界点外面等待进入,一个是在临界点里面wait等待别人notify, 
         * 线程调用了join方法join了另外的线程的时候,也会进入WAITING状态,等待被他join的线程执行结束
         * 
         * A thread is in the waiting state due to calling one of the following methods:
         * 1.Object.wait() with no timeout 
         * 2.Thread.join() with no timeout
         * 3.LockSupport.park()
         *
         * A thread in the waiting state is waiting for another thread to perform a particular action.
         * For example, a thread that has called Object.wait() on an object is waiting for another thread to call
         * Object.notify() or Object.notifyAll() on that object. A thread that has called Thread.join() is 
         * waiting for a specified thread to terminate.
         */
        WAITING,

        /**
         * 有时限的WAITING,超时自行返回
         * 
         * Thread state for a waiting thread with a specified waiting time. A thread is in the timed waiting state 
         * due to calling one of the following methods with a specified positive waiting time:
         * 1.Thread.sleep 
         * 2.Object.wait with timeout 
         * 3.Thread.join with timeout 
         * 4.LockSupport.parkNanos 
         * 5.LockSupport.parkUntil 
         */
        TIMED_WAITING,

        /**
         * 执行完毕
         * Thread state for a terminated thread. The thread has completed execution.
         */
        TERMINATED;
```
![thread-status.jpg](https://github.com/ZhangLaibao/machine_gun/blob/master/images/thread-status.jpg)    
```java
    }

    /**
     * Returns the state of this thread.
     * This method is designed for use in monitoring of the system state, not for synchronization control.
     */
    public State getState() {
        // get current thread state
        return sun.misc.VM.toThreadState(threadStatus);
    }

    // Added in JSR-166
    /**
     * Interface for handlers invoked when a Thread abruptly terminates due to an uncaught exception.
     * When a thread is about to terminate due to an uncaught exception the JVM will query the thread for its
     * UncaughtExceptionHandler using getUncaughtExceptionHandler() and will invoke the handler's 
     * uncaughtException() method, passing the thread and the exception as arguments.
     * If a thread has not had its UncaughtExceptionHandler explicitly set, then its ThreadGroup object 
     * acts as its UncaughtExceptionHandler. If the ThreadGroup object has no special requirements for 
     * dealing with the exception, it can forward the invocation to the getDefaultUncaughtExceptionHandler()
     */
    @FunctionalInterface
    public interface UncaughtExceptionHandler {
        /**
         * Method invoked when the given thread terminates due to the given uncaught exception.
         * Any exception thrown by this method will be ignored by the JVM.
         */
        void uncaughtException(Thread t, Throwable e);
    }

    // null unless explicitly set
    private volatile UncaughtExceptionHandler uncaughtExceptionHandler;

    // null unless explicitly set
    private static volatile UncaughtExceptionHandler defaultUncaughtExceptionHandler;

    /**
     * Set the default handler invoked when a thread abruptly terminates due to an uncaught exception, 
     * and no other handler has been defined for that thread.
     *
     * Uncaught exception handling is controlled first by the thread, then by the thread's ThreadGroup object 
     * and finally by the default uncaught exception handler. If the thread does not have an explicit
     * uncaught exception handler set, and the thread's thread group (including parent thread groups) does not 
     * specialize its uncaughtException() method, then the default handler's uncaughtException() method will 
     * be invoked. By setting the default uncaught exception handler, an application can change the way in which 
     * uncaught exceptions are handled (such as logging to a specific device, or file) for those threads that would
     * already accept whatever default behavior the system provided.
     *
     * Note that the default uncaught exception handler should not usually defer to the thread's ThreadGroup object, 
     * as that could cause infinite recursion.
     */
    public static void setDefaultUncaughtExceptionHandler(UncaughtExceptionHandler eh) {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new RuntimePermission("setDefaultUncaughtExceptionHandler"));
        }

         defaultUncaughtExceptionHandler = eh;
     }

    /**
     * Returns the default handler invoked when a thread abruptly terminates due to an uncaught exception. 
     * If the returned value is null, there is no default.
     */
    public static UncaughtExceptionHandler getDefaultUncaughtExceptionHandler(){
        return defaultUncaughtExceptionHandler;
    }

    public UncaughtExceptionHandler getUncaughtExceptionHandler() {
        return uncaughtExceptionHandler != null ? uncaughtExceptionHandler : group;
    }

    /**
     * Set the handler invoked when this thread abruptly terminates due to an uncaught exception.
     * A thread can take full control of how it responds to uncaught exceptions by having its 
     * uncaught exception handler explicitly set. If no such handler is set then the thread's ThreadGroup 
     * object acts as its handler.
     */
    public void setUncaughtExceptionHandler(UncaughtExceptionHandler eh) {
        checkAccess();
        uncaughtExceptionHandler = eh;
    }

    /**
     * Dispatch an uncaught exception to the handler. This method is intended to be called only by the JVM.
     */
    private void dispatchUncaughtException(Throwable e) {
        getUncaughtExceptionHandler().uncaughtException(this, e);
    }

    /**
     * Removes from the specified map any keys that have been enqueued on the specified reference queue.
     */
    static void processQueue(ReferenceQueue<Class<?>> queue, ConcurrentMap<? extends WeakReference<Class<?>>, ?> map) {
        Reference<? extends Class<?>> ref;
        while((ref = queue.poll()) != null) {
            map.remove(ref);
        }
    }

    /**
     *  Weak key for Class objects.
     **/
    static class WeakClassKey extends WeakReference<Class<?>> {
        /**
         * saved value of the referent's identity hash code, to maintain
         * a consistent hash code after the referent has been cleared
         */
        private final int hash;

        /**
         * Create a new WeakClassKey to the given object, registered with a queue.
         */
        WeakClassKey(Class<?> cl, ReferenceQueue<Class<?>> refQueue) {
            super(cl, refQueue);
            hash = System.identityHashCode(cl);
        }

        /**
         * Returns the identity hash code of the original referent.
         */
        @Override
        public int hashCode() {
            return hash;
        }

        /**
         * Returns true if the given object is this identical WeakClassKey instance, or, if this object's referent 
         * has not been cleared, if the given object is another WeakClassKey instance with the identical non-null 
         * referent as this one.
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this)
                return true;

            if (obj instanceof WeakClassKey) {
                Object referent = get();
                return (referent != null) && (referent == ((WeakClassKey) obj).get());
            } else {
                return false;
            }
        }
    }


    // The following three initially uninitialized fields are exclusively
    // managed by class java.util.concurrent.ThreadLocalRandom. These
    // fields are used to build the high-performance PRNGs in the
    // concurrent code, and we can not risk accidental false sharing.
    // Hence, the fields are isolated with @Contended.

    /** The current seed for a ThreadLocalRandom */
    @sun.misc.Contended("tlr")
    long threadLocalRandomSeed;

    /** Probe hash value; nonzero if threadLocalRandomSeed initialized */
    @sun.misc.Contended("tlr")
    int threadLocalRandomProbe;

    /** Secondary seed isolated from public ThreadLocalRandom sequence */
    @sun.misc.Contended("tlr")
    int threadLocalRandomSecondarySeed;

    /* Some private helper methods */
    private native void setPriority0(int newPriority);
    private native void stop0(Object o);
    private native void suspend0();
    private native void resume0();
    private native void interrupt0();
    private native void setNativeName(String name);
}

```