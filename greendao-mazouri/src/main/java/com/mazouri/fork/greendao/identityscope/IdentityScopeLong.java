package com.mazouri.fork.greendao.identityscope;

import com.mazouri.fork.greendao.internal.LongHashMap;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by wangdong on 16-1-18.
 */
public class IdentityScopeLong<T> implements IdentityScope<Long, T> {

    //对应的对象的类型是LongHashMap，也就是GreenDao为存储键是Long类型而优化后的HashMap
    private final LongHashMap<Reference<T>> map;
    private final ReentrantLock lock;   //Java内部的互斥锁

    public IdentityScopeLong() {
        map = new LongHashMap<Reference<T>>();
        lock = new ReentrantLock();
    }

    //成员方法正如IdentityScope接口中的一样，主要分插入与删除在加锁不加锁上的不同的处理

    @Override
    public T get(Long key) {
        return get2(key);
    }

    @Override
    public T getNoLock(Long key) {
        return get2NoLock(key);
    }

    public T get2(long key) {
        lock.lock();
        Reference<T> ref;
        try {
            ref = map.get(key);
        } finally {
            lock.unlock();
        }
        if (ref != null) {
            return ref.get();
        } else {
            return null;
        }
    }

    public T get2NoLock(long key) {
        Reference<T> ref = map.get(key);
        if (ref != null) {
            return ref.get();
        } else {
            return null;
        }
    }

    @Override
    public void put(Long key, T entity) {
        put2(key, entity);
    }

    @Override
    public void putNoLock(Long key, T entity) {
        put2NoLock(key, entity);
    }

    public void put2(long key, T entity) {
        lock.lock();
        try {
            map.put(key, new WeakReference<T>(entity));
        } finally {
            lock.unlock();
        }
    }

    public void put2NoLock(long key, T entity) {
        map.put(key, new WeakReference<T>(entity));
    }

    @Override
    public boolean detach(Long key, T entity) {
        lock.lock();
        try {
            if (get(key) == entity && entity != null) {
                remove(key);
                return true;
            } else {
                return false;
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void remove(Long key) {
        lock.lock();
        try {
            map.remove(key);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void remove(Iterable<Long> keys) {
        lock.lock();
        try {
            for (Long key : keys) {
                map.remove(key);
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void clear() {
        lock.lock();
        try {
            map.clear();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void lock() {
        lock.lock();
    }

    @Override
    public void unlock() {
        lock.unlock();
    }

    @Override
    public void reserveRoom(int count) {
        map.reserveRoom(count);
    }
}
