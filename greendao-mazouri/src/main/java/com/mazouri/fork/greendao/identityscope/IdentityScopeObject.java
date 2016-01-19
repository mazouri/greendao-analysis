package com.mazouri.fork.greendao.identityscope;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by wangdong on 16-1-18.
 */
public class IdentityScopeObject<K, T> implements IdentityScope<K, T> {

    private final HashMap<K, Reference<T>> map;
    private final ReentrantLock lock;

    public IdentityScopeObject() {
        map = new HashMap<K, Reference<T>>();
        lock = new ReentrantLock();
    }

    @Override
    public T get(K key) {
        Reference<T> ref;
        lock.lock();
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

    @Override
    public T getNoLock(K key) {
        Reference<T> ref = map.get(key);
        if (ref != null) {
            return ref.get();
        } else {
            return null;
        }
    }

    @Override
    public void put(K key, T entity) {
        lock.lock();
        try {
            map.put(key, new WeakReference<T>(entity));
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void putNoLock(K key, T entity) {
        map.put(key, new WeakReference<T>(entity));
    }

    @Override
    public boolean detach(K key, T entity) {
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
    public void remove(K key) {
        lock.lock();
        try {
            map.remove(key);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void remove(Iterable< K> keys) {
        lock.lock();
        try {
            for (K key : keys) {
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
        // HashMap does not allow
    }
}
