package com.mazouri.fork.greendao.async;

import android.database.sqlite.SQLiteDatabase;

import com.mazouri.fork.greendao.AbstractDao;
import com.mazouri.fork.greendao.DaoException;

/**
 * An operation that will be enqueued for asynchronous execution.
 *
 * 异步操作,排队进入队列,等待执行
 *
 * @see AsyncSession
 *
 * Created by wangdong on 16-1-18.
 */
public class AsyncOperation {

    //定义枚举类型，实际上对应sql中操作的类型
    public static enum OperationType {
        Insert, InsertInTxIterable, InsertInTxArray, //
        InsertOrReplace, InsertOrReplaceInTxIterable, InsertOrReplaceInTxArray, //
        Update, UpdateInTxIterable, UpdateInTxArray, //
        Delete, DeleteInTxIterable, DeleteInTxArray, //
        DeleteByKey, DeleteAll, //
        TransactionRunnable, TransactionCallable, //
        QueryList, QueryUnique, //
        Load, LoadAll, //
        Count, Refresh
    }

    public static final int FLAG_MERGE_TX = 1;  //标志当前是否合并事务，默认是1

    /** TODO unused, just an idea */
    public static final int FLAG_STOP_QUEUE_ON_EXCEPTION = 1 << 1;
    public static final int FLAG_TRACK_CREATOR_STACKTRACE = 1 << 2;

    final OperationType type;   //所属的类型是@see OperationType，实际上也就是sql的操作的类型
    final AbstractDao<Object, Object> dao;  //所属的类型是AbstractDao-->所有的Dao的抽象类，实现增删改查实体对象的操作
    private final SQLiteDatabase database;
    /** Entity, Iterable<Entity>, Entity[], or Runnable. */
    final Object parameter;
    final int flags;

    volatile long timeStarted;  //异步操作完成的开始时间
    volatile long timeCompleted;     //异步操作完成的结束时间
    private volatile boolean completed;  //主要是线程的等待与挂起的时候将会用到。每一次检查当前的异步操作是否是执行完毕
    volatile Throwable throwable;
    final Exception creatorStacktrace;
    volatile Object result;
    volatile int mergedOperationsCount; //合并操作的事务的数量

    int sequenceNumber; //当前的异步操作的序号

    @SuppressWarnings("unchecked")
    /**
     * Either supply dao or database (set other to null).
     *
     * 在这个构造函数中，参数包括
     *      异步操作的类型、
     *      Dao的抽象类、
     *      SqliteDataBase的参数、
     *      相关的参数param、
     *      最后一个参数标志当前是否允许合并操作事务
     */
    AsyncOperation(OperationType type, AbstractDao<?, ?> dao, SQLiteDatabase database, Object parameter, int flags) {
        this.type = type;
        this.flags = flags;
        this.dao = (AbstractDao<Object, Object>) dao;
        this.database = database;
        this.parameter = parameter;
        creatorStacktrace = (flags & FLAG_TRACK_CREATOR_STACKTRACE) != 0 ? new Exception("AsyncOperation was created here") : null;
    }

    //成员变量的setter、getter方法
    public Throwable getThrowable() {
        return throwable;
    }

    public void setThrowable(Throwable throwable) {
        this.throwable = throwable;
    }

    public OperationType getType() {
        return type;
    }

    public Object getParameter() {
        return parameter;
    }

    /**
     * getResult()以及waitForCompletion()的作用是获取异步执行的操作的结果，如果当前的异步操作没有结束，进入锁等待
     *
     * The operation's result after it has completed. Waits until a result is available.
     *
     * @return The operation's result or null if the operation type does not produce any result.
     * @throws {@link AsyncDaoException} if the operation produced an exception
     * @see #waitForCompletion()
     */
    public synchronized Object getResult() {
        if (!completed) {
            waitForCompletion();
        }
        if (throwable != null) {
            throw new AsyncDaoException(this, throwable);
        }
        return result;
    }

    /** @return true if this operation may be merged with others into a single database transaction. */
    public boolean isMergeTx() {
        return (flags & FLAG_MERGE_TX) != 0;
    }

    SQLiteDatabase getDatabase() {
        return database != null ? database : dao.getDatabase();
    }

    /**
     * @return true if this operation is mergeable with the given operation. Checks for null, {@link #FLAG_MERGE_TX},
     * and if the database instances match.
     */
    boolean isMergeableWith(AsyncOperation other) {
        return other != null && isMergeTx() && other.isMergeTx() && getDatabase() == other.getDatabase();
    }

    public long getTimeStarted() {
        return timeStarted;
    }

    public long getTimeCompleted() {
        return timeCompleted;
    }

    public long getDuration() {
        if (timeCompleted == 0) {
            throw new DaoException("This operation did not yet complete");
        } else {
            return timeCompleted - timeStarted;
        }
    }

    public boolean isFailed() {
        return throwable != null;
    }

    public boolean isCompleted() {
        return completed;
    }

    /**
     * Waits until the operation is complete. If the thread gets interrupted, any {@link InterruptedException} will be
     * rethrown as a {@link DaoException}.
     *
     * @return Result if any, see {@link #getResult()}
     */
    public synchronized Object waitForCompletion() {
        while (!completed) {
            try {
                wait();
            } catch (InterruptedException e) {
                throw new DaoException("Interrupted while waiting for operation to complete", e);
            }
        }
        return result;
    }

    /**
     * Waits until the operation is complete, but at most the given amount of milliseconds.If the thread gets
     * interrupted, any {@link InterruptedException} will be rethrown as a {@link DaoException}.
     *
     * @return true if the operation completed in the given time frame.
     */
    public synchronized boolean waitForCompletion(int maxMillis) {
        if (!completed) {
            try {
                wait(maxMillis);
            } catch (InterruptedException e) {
                throw new DaoException("Interrupted while waiting for operation to complete", e);
            }
        }
        return completed;
    }

    /** Called when the operation is done. Notifies any threads waiting for this operation's completion. */
    synchronized void setCompleted() {
        completed = true;
        notifyAll();
    }

    public boolean isCompletedSucessfully() {
        return completed && throwable == null;
    }

    /**
     * If this operation was successfully merged with other operation into a single TX, this will give the count of
     * merged operations. If the operation was not merged, it will be 0.
     */
    public int getMergedOperationsCount() {
        return mergedOperationsCount;
    }

    /**
     * Each operation get a unique sequence number when the operation is enqueued. Can be used for efficiently
     * identifying/mapping operations.
     */
    public int getSequenceNumber() {
        return sequenceNumber;
    }

    /**
     * Reset to prepare another execution run.
     *
     * 将相关的成员变量恢复到最原始的数值
     */
    void reset() {
        timeStarted = 0;
        timeCompleted = 0;
        completed = false;
        throwable = null;
        result = null;
        mergedOperationsCount = 0;
    }

    /**
     * The stacktrace is captured using an exception if {@link #FLAG_TRACK_CREATOR_STACKTRACE} was used (null
     * otherwise).
     */
    public Exception getCreatorStacktrace() {
        return creatorStacktrace;
    }
}
