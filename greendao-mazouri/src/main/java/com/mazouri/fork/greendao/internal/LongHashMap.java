package com.mazouri.fork.greendao.internal;

import com.mazouri.fork.greendao.DaoLog;

import java.util.Arrays;

/**
 * An minimalistic hash map optimized for long keys.
 *
 * 针对键为Long类型而设计的简约的HashMap
 *
 * 记住原理就行，HashMap的原理是数组与链表的结合再加上合理的Hash算法
 *
 * Created by wangdong on 16-1-18.
 *
 * @param <T>
 *            The class to store.
 */
public class LongHashMap<T> {

    final static class Entry<T> {
        final long key;
        T value;
        Entry<T> next;

        Entry(long key, T value, Entry<T> next) {
            this.key = key;
            this.value = value;
            this.next = next;
        }
    }

    private Entry<T>[] table;
    private int capacity;
    private int threshold;
    private int size;

    public LongHashMap() {
        this(16);
    }

    @SuppressWarnings("unchecked")
    public LongHashMap(int capacity) {
        this.capacity = capacity;
        this.threshold = capacity * 4 / 3;
        this.table = new Entry[capacity];
    }

    public boolean containsKey(long key) {
        final int index = ((((int) (key >>> 32)) ^ ((int) (key))) & 0x7fffffff) % capacity;

        for (Entry<T> entry = table[index]; entry != null; entry = entry.next) {
            if (entry.key == key) {
                return true;
            }
        }
        return false;
    }

    public T get(long key) {
        final int index = ((((int) (key >>> 32)) ^ ((int) (key))) & 0x7fffffff) % capacity;
        for (Entry<T> entry = table[index]; entry != null; entry = entry.next) {
            if (entry.key == key) {
                return entry.value;
            }
        }
        return null;
    }

    public T put(long key, T value) {
        final int index = ((((int) (key >>> 32)) ^ ((int) (key))) & 0x7fffffff) % capacity;
        final Entry<T> entryOriginal = table[index];
        for (Entry<T> entry = entryOriginal; entry != null; entry = entry.next) {
            if (entry.key == key) {
                T oldValue = entry.value;
                entry.value = value;
                return oldValue;
            }
        }
        table[index] = new Entry<T>(key, value, entryOriginal);
        size++;
        if (size > threshold) {
            setCapacity(2 * capacity);
        }
        return null;
    }

    public T remove(long key) {
        int index = ((((int) (key >>> 32)) ^ ((int) (key))) & 0x7fffffff) % capacity;
        Entry<T> previous = null;
        Entry<T> entry = table[index];
        while (entry != null) {
            Entry<T> next = entry.next;
            if (entry.key == key) {
                if (previous == null) {
                    table[index] = next;
                } else {
                    previous.next = next;
                }
                size--;
                return entry.value;
            }
            previous = entry;
            entry = next;
        }
        return null;
    }

    public void clear() {
        size = 0;
        Arrays.fill(table, null);
    }

    public int size() {
        return size;
    }

    public void setCapacity(int newCapacity) {
        @SuppressWarnings("unchecked")
        Entry<T>[] newTable = new Entry[newCapacity];
        int length = table.length;
        for (int i = 0; i < length; i++) {
            Entry<T> entry = table[i];
            while (entry != null) {
                long key = entry.key;
                int index = ((((int) (key >>> 32)) ^ ((int) (key))) & 0x7fffffff) % newCapacity;

                Entry<T> originalNext = entry.next;
                entry.next = newTable[index];
                newTable[index] = entry;
                entry = originalNext;
            }
        }
        table = newTable;
        capacity = newCapacity;
        threshold = newCapacity * 4 / 3;
    }

    /** Target load: 0,6 */
    public void reserveRoom(int entryCount) {
        setCapacity(entryCount * 5 / 3);
    }

    public void logStats() {
        int collisions = 0;
        for (Entry<T> entry : table) {
            while (entry != null && entry.next != null) {
                collisions++;
                entry = entry.next;
            }
        }
        DaoLog.d("load: " + ((float) size) / capacity + ", size: " + size + ", capa: " + capacity + ", collisions: "
                + collisions + ", collision ratio: " + ((float) collisions) / size);
    }

}
