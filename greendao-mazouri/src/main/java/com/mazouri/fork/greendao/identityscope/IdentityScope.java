package com.mazouri.fork.greendao.identityscope;

/**
 * Common interface for a identity scopes needed internally by greenDAO. Identity scopes let greenDAO re-use Java
 * objects.
 *
 * 被GreenDao内部需要的实体对象作用域的一般性的接口。作用域的限定让GreenDao重复使用Java对象
 *
 * Created by wangdong on 16-1-18.
 *
 * @param <K>
 *            Key
 * @param <T>
 *            Entity
 */
public interface IdentityScope<K, T> {

    T get(K key);   //通过键key，获取对应的数值

    void put(K key, T entity);  //存储键值对

    T getNoLock(K key); //通过键key，不加锁获取对应的数值

    void putNoLock(K key, T entity);    //不加锁的存储键值对

    boolean detach(K key, T entity);    //将指定的键值对拆开、分离

    void remove(K key); //参数是Key，移除指定键所对应的数值

    void remove(Iterable<K> key);   //参数是实现了Iterable

    void clear();

    void lock();    //加锁

    void unlock();  //不加锁

    void reserveRoom(int count);    //保留指定数目的存储空间
}
