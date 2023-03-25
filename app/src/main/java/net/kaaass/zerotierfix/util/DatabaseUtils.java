package net.kaaass.zerotierfix.util;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 数据库访问工具类
 */
public class DatabaseUtils {
    public static final Lock readLock;
    public static final ReadWriteLock readWriteLock;
    public static final Lock writeLock;

    static {
        var reentrantReadWriteLock = new ReentrantReadWriteLock();
        readWriteLock = reentrantReadWriteLock;
        writeLock = reentrantReadWriteLock.writeLock();
        readLock = reentrantReadWriteLock.readLock();
    }
}
