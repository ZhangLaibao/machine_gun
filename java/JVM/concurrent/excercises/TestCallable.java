package com.jr.test.tkij.conc;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created by PengXianglong on 2018/7/12.
 */
public class TestCallable {

    public static void main(String[] args) {
        int threadNum = Runtime.getRuntime().availableProcessors() + 1;
        ExecutorService threadPool = Executors.newCachedThreadPool();

        List<Future<String>> results = new ArrayList<>();
        for (int i = 0; i < threadNum; i++) {
            results.add(threadPool.submit(new TaskWithResult(i)));
        }

        for (Future<String> result : results) {
            try {
                System.out.println(result.get());
            } catch (InterruptedException e) {
                e.printStackTrace();
                return;
            } catch (ExecutionException e) {
                e.printStackTrace();
            } finally {
                threadPool.shutdown();
            }
        }

    }

}

class TaskWithResult implements Callable<String> {

    private int id;

    public TaskWithResult(int id) {
        this.id = id;
    }

    @Override
    public String call() throws Exception {
        return "result from TaskWithResult : " + id;
    }

}
