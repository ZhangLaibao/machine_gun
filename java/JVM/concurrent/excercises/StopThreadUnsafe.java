package com.jr.test.concurrent.practice;

import java.util.concurrent.TimeUnit;

public class StopThreadUnsafe {

    private static User user = new User();

    public static void main(String[] args) throws InterruptedException {

        new ReadThread().start();
        WriteThread writeThread = new WriteThread();
        writeThread.start();
        Thread.sleep(200L);

        // Thread.stop()是一个废弃的方法，不要用这种方式结束线程，极易造成数据不一致
        writeThread.stop();
    }

    private static class WriteThread extends Thread {

        @Override
        public void run() {
            while (true) {
                synchronized (StopThreadUnsafe.class) {
                    long userId = System.currentTimeMillis();
                    user.setId(userId);
                    try {
                        TimeUnit.MILLISECONDS.sleep(100L);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    user.setName(String.valueOf(userId));
                }
            }
        }
    }

    private static class ReadThread extends Thread {
        @Override
        public void run() {
            while (true) {
                synchronized (StopThreadUnsafe.class) {
                   if(user.getId() != Long.parseLong(user.getName())){
                       System.out.println(user);
                       break;
                   }
                }
            }
        }
    }

    private static class User {

        private long id;
        private String name;

        public User() {
            this.id = 0L;
            this.name = "0";
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            sb.append(getClass().getSimpleName());
            sb.append(" [");
            sb.append("id=").append(id);
            sb.append(", name='").append(name);
            sb.append("]");
            return sb.toString();
        }

        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

}
