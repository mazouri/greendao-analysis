package com.mazouri.fork.greendao.query;

import java.io.Closeable;
import java.util.ListIterator;

/**
 * A list iterator that needs to be closed (or the associated list) to free underlying resources like a database cursor.
 * Typically used with LazyList.
 *
 * 这是一个列表的迭代器，需要被关闭，释放底层的资源，比如数据库的游标等
 *
 * Created by wangdong on 16-1-18.
 */
public interface CloseableListIterator<T> extends ListIterator<T>, Closeable {
}
