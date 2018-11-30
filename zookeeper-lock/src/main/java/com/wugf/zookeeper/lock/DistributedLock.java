package com.wugf.zookeeper.lock;

/**
 * @program: myProject
 * @author: wgf
 * @create: 2018-11-16 17:39
 * @description: 分布式锁接口
 **/
public interface DistributedLock {

    void lock();

    void unLock();

}
