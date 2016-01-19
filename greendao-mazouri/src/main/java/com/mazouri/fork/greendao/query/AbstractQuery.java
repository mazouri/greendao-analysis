package com.mazouri.fork.greendao.query;

import com.mazouri.fork.greendao.AbstractDao;
import com.mazouri.fork.greendao.DaoException;
import com.mazouri.fork.greendao.InternalQueryDaoAccess;

/**
 * Created by wangdong on 16-1-18.
 */
abstract class AbstractQuery<T> {
    protected final AbstractDao<T, ?> dao;
    protected final InternalQueryDaoAccess<T> daoAccess;
    protected final String sql;
    protected final String[] parameters;
    protected final Thread ownerThread;

    protected static String[] toStringArray(Object[] values) {
        int length = values.length;
        String[] strings = new String[length];
        for (int i = 0; i < length; i++) {
            Object object = values[i];
            if (object != null) {
                strings[i] = object.toString();
            } else {
                strings[i] = null;
            }
        }
        return strings;
    }

    protected AbstractQuery(AbstractDao<T, ?> dao, String sql, String[] parameters) {
        this.dao = dao;
        this.daoAccess = new InternalQueryDaoAccess<T>(dao);
        this.sql = sql;
        this.parameters = parameters;
        ownerThread = Thread.currentThread();
    }

    // public void compile() {
    // // TODO implement compile
    // }

    /**
     * Sets the parameter (0 based) using the position in which it was added during building the query.
     */
    public void setParameter(int index, Object parameter) {
        checkThread();
        if (parameter != null) {
            parameters[index] = parameter.toString();
        } else {
            parameters[index] = null;
        }
    }

    protected void checkThread() {
        if (Thread.currentThread() != ownerThread) {
            throw new DaoException(
                    "Method may be called only in owner thread, use forCurrentThread to get an instance for this thread");
        }
    }
}
