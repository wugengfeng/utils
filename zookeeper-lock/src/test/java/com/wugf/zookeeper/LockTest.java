package com.wugf.zookeeper;

/**
 * @program: myProject
 * @author: wgf
 * @create: 2018-11-14 18:15
 * @description:
 **/
public class LockTest implements Runnable{

    private LockUtil lockUtil;
    private int flag;

    public LockTest(String config, String lockName, int flag) {
        this.lockUtil = new LockUtil(config, lockName);
        this.flag = flag;
    }

    @Override
    public void run() {
        lockUtil.lock();
        try {
            System.out.println("-----------------------------------");
            System.out.println("线程 " + Thread.currentThread().getName() + " 获取锁");
            System.out.println("-----------------------------------");
            Thread.sleep(5 * 1000l);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lockUtil.unlock();
        }
    }

    public static void main(String[] args) {

        String config = "47.106.207.16:2181";
        String lockName = "lock";
        for (int i=1; i<=5; i++) {
            new Thread(new LockTest(config, lockName, i)).start();
        }
    }
}
