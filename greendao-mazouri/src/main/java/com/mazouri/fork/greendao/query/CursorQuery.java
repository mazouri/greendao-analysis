package com.mazouri.fork.greendao.query;

import android.database.Cursor;

import com.mazouri.fork.greendao.AbstractDao;

/**
 * A repeatable query returning a raw android.database.Cursor. Note, that using cursors is usually a hassle and
 * greenDAO provides a higher level abstraction using entities (see {@link Query}). This class
 * can nevertheless be useful to work with legacy code that is based on Cursors or CursorLoaders.
 *
 * 返回一个Android中的游标。值得注意的是，游标的使用通常是一件麻烦的事情。
 * GreenDao通过使用实体类提供了一个较高的层次的抽象。这个类与一般性的Cursor的代码搭配使用还是很有用的。
 *
 * Created by wangdong on 16-1-18.
 */
public class CursorQuery<T> extends AbstractQueryWithLimit<T> {

    private final static class QueryData<T2> extends AbstractQueryData<T2, CursorQuery<T2>> {
        private final int limitPosition;
        private final int offsetPosition;

        QueryData(AbstractDao dao, String sql, String[] initialValues, int limitPosition, int offsetPosition) {
            super(dao, sql, initialValues);
            this.limitPosition = limitPosition;
            this.offsetPosition = offsetPosition;
        }

        @Override
        protected CursorQuery<T2> createQuery() {
            return new CursorQuery<T2>(this, dao, sql, initialValues.clone(), limitPosition, offsetPosition);
        }

    }

    /** For internal use by greenDAO only. */
    public static <T2> CursorQuery<T2> internalCreate(AbstractDao<T2, ?> dao, String sql, Object[] initialValues) {
        return create(dao, sql, initialValues, -1, -1);
    }

    static <T2> CursorQuery<T2> create(AbstractDao<T2, ?> dao, String sql, Object[] initialValues, int limitPosition,
                                       int offsetPosition) {
        QueryData<T2> queryData = new QueryData<T2>(dao, sql, toStringArray(initialValues), limitPosition,
                offsetPosition);
        return queryData.forCurrentThread();
    }

    private final QueryData<T> queryData;

    private CursorQuery(QueryData<T> queryData, AbstractDao<T, ?> dao, String sql, String[] initialValues, int limitPosition,
                        int offsetPosition) {
        super(dao, sql, initialValues, limitPosition, offsetPosition);
        this.queryData = queryData;
    }

    public CursorQuery forCurrentThread() {
        return queryData.forCurrentThread(this);
    }

    /** Executes the query and returns a raw android.database.Cursor. Don't forget to close it. */
    public Cursor query() {
        checkThread();
        return dao.getDatabase().rawQuery(sql, parameters);
    }
}
