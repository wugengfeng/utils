package com.wugf.zookeeper.lock;

import org.apache.commons.lang3.StringUtils;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;

/**
 * @program: myProject
 * @author: wgf
 * @create: 2018-11-16 17:22
 * @description: 基于zookeeper实现的分布式锁
 **/
public class ZookeeperLock implements DistributedLock {

    // 使用ThreadLocal绑定当前线程与其所创建的节点信息
    private ThreadLocal<String> nodeInfo = new ThreadLocal<>();

    // zk客户端
    private ZooKeeper zooKeeper;

    // zk是一个目录结构，这里定义分布式锁的最外层目录
    private static final String ROOT_PATH = "/locks";

    // 当前线程锁的路径
    private String lockPath;

    // zk会话超时时间
    private final static int sessionTimeout = 3000;

    // 节点数据
    private final static byte[] data = new byte[0];

    private final static String SEPARATE = "-";

    /**
     * @param url      zk连接信息
     * @param lockName 分布式锁名
     */
    public ZookeeperLock(String url, String lockName) {

        if (lockName.indexOf(SEPARATE) < 0) {
            lockName += SEPARATE;
        }

        this.lockPath = String.format("%s/%s", ROOT_PATH, lockName);

        try {
            // 用于同步等待zk客户端连接服务端
            CountDownLatch connectedSignal = new CountDownLatch(1);
            zooKeeper = new ZooKeeper(url, sessionTimeout, (WatchedEvent event) -> {

                // 建立连接
                if (event.getState() == Watcher.Event.KeeperState.SyncConnected) {
                    connectedSignal.countDown();
                }
            });

            // 主线程会阻塞等待子线程任务完成
            connectedSignal.await();

            // 是否需要创建根节点
            Stat stat = zooKeeper.exists(ROOT_PATH, false);

            if (stat == null) {
                // 创建持久节点
                zooKeeper.create(ROOT_PATH, data, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void lock() {
        try {
            // 创建临时顺序节点
            String currentThreadNode = zooKeeper.create(lockPath, data, ZooDefs.Ids.OPEN_ACL_UNSAFE,
                    CreateMode.EPHEMERAL_SEQUENTIAL);

            // 取出所有子节点
            List<String> subNodeList = zooKeeper.getChildren(ROOT_PATH, false);
            TreeSet<String> subNodeSet = new TreeSet<>();

            subNodeList.forEach(node -> subNodeSet.add(String.format("%s/%s", ROOT_PATH, node)));

            // 获取最小节点
            String smallNode = subNodeSet.first();

            // 获取当前节点的上一个节点
            String preNode = subNodeSet.lower(currentThreadNode);

            // 如果当前线程节点为最小节点，则表示获取锁
            if (currentThreadNode.equals(smallNode)) {
                nodeInfo.set(currentThreadNode);
                return;
            }

            CountDownLatch waitLockLatch = new CountDownLatch(1);

            // 注册当前节点的上个节点删除事件监听
            Stat stat = zooKeeper.exists(preNode, new LockWatcher(waitLockLatch));

            // 判断比当前节点小的节点是否存在，不存在则获得锁
            if (stat != null) {
                // 阻塞主线程
                waitLockLatch.await();
                nodeInfo.set(currentThreadNode);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.print(Thread.currentThread().getName());
        }
    }

    @Override
    public void unLock() {
        String currentThreadNode = nodeInfo.get();

        try {
            // 判断当前线程是否已上锁（在等待锁时不能解锁）
            if (StringUtils.isNotEmpty(currentThreadNode)) {
                zooKeeper.delete(currentThreadNode, -1);
                nodeInfo.remove();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (KeeperException e) {
            e.printStackTrace();
        }
    }

    /**
     * 等待锁监听事件
     * 实现原理：监听当前线程节点的上一个顺序节点的节点删除事件，
     * 当上一个节点删除后，此监听器会接收来自zk的通知。此事件用于
     * 唤醒主线程获取锁
     */
    class LockWatcher implements Watcher {

        public LockWatcher(CountDownLatch waitLockLatch) {
            this.waitLockLatch = waitLockLatch;
        }

        private CountDownLatch waitLockLatch;

        @Override
        public void process(WatchedEvent event) {

            if (event.getType() == Event.EventType.NodeDeleted) {
                waitLockLatch.countDown();
            }
        }
    }

}
