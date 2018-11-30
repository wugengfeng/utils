package com.wugf.zookeeper;

import java.util.concurrent.CountDownLatch;

/**
 * @program: myProject
 * @author: wgf
 * @create: 2018-11-16 15:46
 * @description:
 **/
public class CountDownLatchTest {

    // 构造函数的数量就是子线程数量
    private static CountDownLatch countDownLatch = new CountDownLatch(5);

    public static void main(String[] args) throws InterruptedException {
        for (int i=1; i <=5; i++) {
            new Thread(new TestThread(countDownLatch)).start();
        }

        // 阻塞主线程，当CountDownLatch的线程数到0时，恢复主线程执行
        countDownLatch.await();
        System.out.println("主线程执行");
    }
}


class TestThread implements Runnable {

    private CountDownLatch countDownLatch;

    public TestThread(CountDownLatch countDownLatch) {
        this.countDownLatch = countDownLatch;
    }

    @Override
    public void run() {
        try {
            System.out.println(Thread.currentThread().getName());
            Thread.sleep(1 * 2000L);

            // 减少countDownLatch子线程数
            countDownLatch.countDown();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}