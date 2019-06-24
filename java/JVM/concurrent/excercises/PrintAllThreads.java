package com.jr.test.concurrent.practice;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

public class PrintAllThreads {

    /**
     * "Monitor Ctrl-Break" Id=6 RUNNABLE (in native)
     * at java.net.SocketInputStream.socketRead0(Native Method)
     * at java.net.SocketInputStream.socketRead(SocketInputStream.java:116)
     * at java.net.SocketInputStream.read(SocketInputStream.java:171)
     * at java.net.SocketInputStream.read(SocketInputStream.java:141)
     * at sun.nio.cs.StreamDecoder.readBytes(StreamDecoder.java:284)
     * at sun.nio.cs.StreamDecoder.implRead(StreamDecoder.java:326)
     * at sun.nio.cs.StreamDecoder.read(StreamDecoder.java:178)
     * at java.io.InputStreamReader.read(InputStreamReader.java:184)
     * ...
     *
     *
     * "Attach Listener" Id=5 RUNNABLE
     *
     *
     * "Signal Dispatcher" Id=4 RUNNABLE
     *
     *
     * "Finalizer" Id=3 WAITING on java.lang.ref.ReferenceQueue$Lock@3d494fbf
     * at java.lang.Object.wait(Native Method)
     * -  waiting on java.lang.ref.ReferenceQueue$Lock@3d494fbf
     * at java.lang.ref.ReferenceQueue.remove(ReferenceQueue.java:143)
     * at java.lang.ref.ReferenceQueue.remove(ReferenceQueue.java:164)
     * at java.lang.ref.Finalizer$FinalizerThread.run(Finalizer.java:209)
     *
     *
     * "Reference Handler" Id=2 WAITING on java.lang.ref.Reference$Lock@1ddc4ec2
     * at java.lang.Object.wait(Native Method)
     * -  waiting on java.lang.ref.Reference$Lock@1ddc4ec2
     * at java.lang.Object.wait(Object.java:502)
     * at java.lang.ref.Reference.tryHandlePending(Reference.java:191)
     * at java.lang.ref.Reference$ReferenceHandler.run(Reference.java:153)
     *
     *
     * "main" Id=1 RUNNABLE
     * at sun.management.ThreadImpl.dumpThreads0(Native Method)
     * at sun.management.ThreadImpl.dumpAllThreads(ThreadImpl.java:454)
     * at com.jr.test.concurrent.practice.PrintAllThreads.main(PrintAllThreads.java:12)
     *
     */
    public static void main(String[] args) {

        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        ThreadInfo[] threadInfos = threadMXBean.dumpAllThreads(false, false);
        for (ThreadInfo threadInfo : threadInfos)
            System.out.println(threadInfo);
    }

}
