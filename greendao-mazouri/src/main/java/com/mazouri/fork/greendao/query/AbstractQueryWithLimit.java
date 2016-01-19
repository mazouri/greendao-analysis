package com.mazouri.fork.greendao.query;

import com.mazouri.fork.greendao.AbstractDao;

import java.util.Date;

/**
 * Base class for queries returning data (entities or cursor).
 *
 * 继承了AbstractQuery，同时加上了查询的开始位置以及偏移量的限定
 *
 * Created by wangdong on 16-1-18.
 *
 * @param <T> The entity class the query will return results for.
 */
abstract class AbstractQueryWithLimit<T> extends AbstractQuery<T> {

    protected final int limitPosition;
    protected final int offsetPosition;

    protected AbstractQueryWithLimit(AbstractDao<T, ?> dao, String sql, String[] initialValues, int limitPosition,
                                     int offsetPosition) {
        super(dao, sql, initialValues);
        this.limitPosition = limitPosition;
        this.offsetPosition = offsetPosition;
    }

    /**
     * Sets the parameter (0 based) using the position in which it was added during building the query. Note: all
     * standard WHERE parameters come first. After that come the WHERE parameters of joins (if any).
     */
    public void setParameter(int index, Object parameter) {
        if (index >= 0 && (index == limitPosition || index == offsetPosition)) {
            throw new IllegalArgumentException("Illegal parameter index: " + index);
        }
        super.setParameter(index, parameter);
    }

    public void setParameter(int index, Date parameter) {
        Long converted = parameter != null ? parameter.getTime() : null;
        setParameter(index, converted);
    }

    public void setParameter(int index, Boolean parameter) {
        Integer converted = parameter != null ? (parameter ? 1 : 0) : null;
        setParameter(index, converted);
    }

    /**
     * Sets the limit of the maximum number of results returned by this Query. {@link
     * QueryBuilder#limit(int)} must
     * have been called on the QueryBuilder that created this Query object.
     */
    public void setLimit(int limit) {
        checkThread();
        if (limitPosition == -1) {
            throw new IllegalStateException("Limit must be set with QueryBuilder before it can be used here");
        }
        parameters[limitPosition] = Integer.toString(limit);
    }

    /**
     * Sets the offset for results returned by this Query. {@link QueryBuilder#offset(int)} must
     * have been called on
     * the QueryBuilder that created this Query object.
     */
    public void setOffset(int offset) {
        checkThread();
        if (offsetPosition == -1) {
            throw new IllegalStateException("Offset must be set with QueryBuilder before it can be used here");
        }
        parameters[offsetPosition] = Integer.toString(offset);
    }
}
