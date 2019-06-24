package com.jr.test.concurrent.practice;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

// 使用线程池时，如果不做特殊处理，任务异常会被丢弃，给问题定位带来极大的困难
// 使用如下处理可以打印提交任务的线程和任务执行线程的堆栈信息，用于问题定位
public class TraceThreadPoolExecutor extends ThreadPoolExecutor {

    public static void main(String[] args) {
        ThreadPoolExecutor threadPool = new TraceThreadPoolExecutor(
                0, Integer.MAX_VALUE, 0L, TimeUnit.MILLISECONDS, new SynchronousQueue<>());
        for (int i = 0; i < 5; i++) {
            threadPool.execute(new DivTask(i));
        }
    }

    public TraceThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime,
                                   TimeUnit unit, BlockingQueue<Runnable> workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
    }

    @Override
    public void execute(Runnable command) {
        super.execute(wrap(command, clientTrace()));
    }

    @Override
    public Future<?> submit(Runnable task) {
        return super.submit(wrap(task, clientTrace()));
    }

    private Exception clientTrace() {
        return new Exception("Submitter stack trace:");
    }

    private Runnable wrap(final Runnable task, final Exception clientStack) {
        return () -> {
            try {
                task.run();
            } catch (Exception e) {
                clientStack.printStackTrace();
                throw e;
            }
        };
    }

    private static class DivTask implements Runnable {

        int i = 0;

        DivTask(int i) {
            this.i = i;
        }

        @Override
        public void run() {
            double re = 100 / i;
        }
    }
}
