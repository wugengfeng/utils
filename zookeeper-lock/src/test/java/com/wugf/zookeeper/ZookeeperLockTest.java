package com.wugf.zookeeper;

import com.wugf.zookeeper.lock.ZookeeperLock;

/**
 * @program: myProject
 * @author: wgf
 * @create: 2018-11-17 15:39
 * @description:
 **/
public class ZookeeperLockTest implements Runnable {

    private ZookeeperLock zookeeperLock;
    private int flag;

    public ZookeeperLockTest(ZookeeperLock zookeeperLock, int flag) {
        this.zookeeperLock = zookeeperLock;
        this.flag = flag;
    }

    @Override
    public void run() {
        zookeeperLock.lock();

        try {
            System.out.println("-----------------------------------");
            System.out.println("线程 " + Thread.currentThread().getName() + " 获取锁");
            System.out.println("-----------------------------------");
            Thread.sleep(5 * 1000l);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            zookeeperLock.unLock();
        }
    }


    public static void main(String[] args) {
        for (int i=1; i<=5; i++) {
            new Thread(new ZookeeperLockTest(new ZookeeperLock("47.106.207.16:2181", "lock"), i)).start();
        }
    }
}
