package com.mazouri.fork.greendao.query;

import android.database.sqlite.SQLiteDatabase;

import com.mazouri.fork.greendao.AbstractDao;

/**
 * A repeatable query for deleting entities.<br/>
 * New API note: this is more likely to change.
 * 
 * 这是一个针对删除实体对象可重复使用的查询类
 *
 * Created by wangdong on 16-1-18.
 *
 * @param <T>
 *            The entity class the query will delete from.
 */
public class DeleteQuery<T> extends AbstractQuery<T> {
    private final static class QueryData<T2> extends AbstractQueryData<T2, DeleteQuery<T2>> {

        private QueryData(AbstractDao<T2, ?> dao, String sql, String[] initialValues) {
            super(dao, sql, initialValues);
        }

        @Override
        protected DeleteQuery<T2> createQuery() {
            return new DeleteQuery<T2>(this, dao, sql, initialValues.clone());
        }
    }

    static <T2> DeleteQuery<T2> create(AbstractDao<T2, ?> dao, String sql, Object[] initialValues) {
        QueryData<T2> queryData = new QueryData<T2>(dao, sql, toStringArray(initialValues));
        return queryData.forCurrentThread();
    }

    private final QueryData<T> queryData;

    private DeleteQuery(QueryData<T> queryData, AbstractDao<T, ?> dao, String sql, String[] initialValues) {
        super(dao, sql, initialValues);
        this.queryData = queryData;
    }

    public DeleteQuery<T> forCurrentThread() {
        return queryData.forCurrentThread(this);
    }

    /**
     * Deletes all matching entities without detaching them from the identity scope (aka session/cache). Note that this
     * method may lead to stale entity objects in the session cache. Stale entities may be returned when loaded by their
     * primary key, but not using queries.
     */
    public void executeDeleteWithoutDetachingEntities() {
        checkThread();
        SQLiteDatabase db = dao.getDatabase();
        if (db.isDbLockedByCurrentThread()) {
            dao.getDatabase().execSQL(sql, parameters);
        } else {
            // Do TX to acquire a connection before locking this to avoid deadlocks
            // Locking order as described in AbstractDao
            db.beginTransaction();
            try {
                dao.getDatabase().execSQL(sql, parameters);
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }
    }
}
