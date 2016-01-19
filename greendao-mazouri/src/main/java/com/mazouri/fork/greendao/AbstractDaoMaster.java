package com.mazouri.fork.greendao;

import android.database.sqlite.SQLiteDatabase;

import com.mazouri.fork.greendao.identityscope.IdentityScopeType;
import com.mazouri.fork.greendao.internal.DaoConfig;

import java.util.HashMap;
import java.util.Map;

/**
 * The master of dao will guide you: start dao sessions with the master.
 *
 * Created by wangdong on 16-1-18.
 */
public abstract class AbstractDaoMaster {
    protected final SQLiteDatabase db;
    protected final int schemaVersion;
    protected final Map<Class<? extends AbstractDao<?, ?>>, DaoConfig> daoConfigMap;

    public AbstractDaoMaster(SQLiteDatabase db, int schemaVersion) {
        this.db = db;
        this.schemaVersion = schemaVersion;

        daoConfigMap = new HashMap<Class<? extends AbstractDao<?, ?>>, DaoConfig>();
    }

    protected void registerDaoClass(Class<? extends AbstractDao<?, ?>> daoClass) {
        DaoConfig daoConfig = new DaoConfig(db, daoClass);
        daoConfigMap.put(daoClass, daoConfig);
    }

    public int getSchemaVersion() {
        return schemaVersion;
    }

    /** Gets the SQLiteDatabase for custom database access. Not needed for greenDAO entities. */
    public SQLiteDatabase getDatabase() {
        return db;
    }

    public abstract AbstractDaoSession newSession();

    public abstract AbstractDaoSession newSession(IdentityScopeType type);
}
