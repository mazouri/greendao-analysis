package com.mazouri.fork.greendao.internal;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

/**
 * Helper class to create SQL statements for specific tables (used by greenDAO internally).
 *
 * 针对指定的表创建Sql语句的工具类
 *
 * Created by wangdong on 16-1-18.
 */
public class TableStatements {

    private final SQLiteDatabase db;
    //tablename、allColumns、pkColumns 分别对应的是表的名称、所有的字段的名称的数组、所有的主键的字段的数组
    private final String tablename;
    private final String[] allColumns;
    private final String[] pkColumns;

    //insertStatement、insertOrReplaceStatement、updateStatement、deleteStatement 增删改查的Sql语句的可执行的对象
    private SQLiteStatement insertStatement;
    private SQLiteStatement insertOrReplaceStatement;
    private SQLiteStatement updateStatement;
    private SQLiteStatement deleteStatement;

    //selectAll、selectByKey、selectByRowId、selectKeys 几种不同语句的select的字符串
    private volatile String selectAll;
    private volatile String selectByKey;
    private volatile String selectByRowId;
    private volatile String selectKeys;

    //参数有表名，字段名的数组，主键名的数组
    public TableStatements(SQLiteDatabase db, String tablename, String[] allColumns, String[] pkColumns) {
        this.db = db;
        this.tablename = tablename;
        this.allColumns = allColumns;
        this.pkColumns = pkColumns;
    }

    //获取可执行的插入执行语句的对象
    public SQLiteStatement getInsertStatement() {
        if (insertStatement == null) {
            String sql = SqlUtils.createSqlInsert("INSERT INTO ", tablename, allColumns);
            insertStatement = db.compileStatement(sql);
        }
        return insertStatement;
    }

    //其余的成员方法依旧是创建相关的执行语句的对象，不再进行赘述

    public SQLiteStatement getInsertOrReplaceStatement() {
        if (insertOrReplaceStatement == null) {
            String sql = SqlUtils.createSqlInsert("INSERT OR REPLACE INTO ", tablename, allColumns);
            insertOrReplaceStatement = db.compileStatement(sql);
        }
        return insertOrReplaceStatement;
    }

    public SQLiteStatement getDeleteStatement() {
        if (deleteStatement == null) {
            String sql = SqlUtils.createSqlDelete(tablename, pkColumns);
            deleteStatement = db.compileStatement(sql);
        }
        return deleteStatement;
    }

    public SQLiteStatement getUpdateStatement() {
        if (updateStatement == null) {
            String sql = SqlUtils.createSqlUpdate(tablename, allColumns, pkColumns);
            updateStatement = db.compileStatement(sql);
        }
        return updateStatement;
    }

    /** ends with an space to simplify appending to this string. */
    public String getSelectAll() {
        if (selectAll == null) {
            selectAll = SqlUtils.createSqlSelect(tablename, "T", allColumns, false);
        }
        return selectAll;
    }

    /** ends with an space to simplify appending to this string. */
    public String getSelectKeys() {
        if (selectKeys == null) {
            selectKeys = SqlUtils.createSqlSelect(tablename, "T", pkColumns, false);
        }
        return selectKeys;
    }

    // TODO precompile
    public String getSelectByKey() {
        if (selectByKey == null) {
            StringBuilder builder = new StringBuilder(getSelectAll());
            builder.append("WHERE ");
            SqlUtils.appendColumnsEqValue(builder, "T", pkColumns);
            selectByKey = builder.toString();
        }
        return selectByKey;
    }

    public String getSelectByRowId() {
        if (selectByRowId == null) {
            selectByRowId = getSelectAll() + "WHERE ROWID=?";
        }
        return selectByRowId;
    }
}
