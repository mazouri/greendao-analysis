package com.mazouri.fork.greendao.async;

import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.mazouri.fork.greendao.DaoException;
import com.mazouri.fork.greendao.DaoLog;
import com.mazouri.fork.greendao.query.Query;

import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * 封装了异步操作的线程池以及相关的事件的回调
 *
 *
 * Created by wangdong on 16-1-18.
 */
public class AsyncOperationExecutor implements Runnable, Handler.Callback{
    //类型是ExecutorService，也就是Java中的线程池
    private static ExecutorService executorService = Executors.newCachedThreadPool();

    //类型是BlockingQueue队列，类型强制为AsyncOperation。实际上也就是异步操作的存储队列
    private final BlockingQueue<AsyncOperation> queue;

    private volatile boolean executorRunning; //当前的线程池是否处于执行任务的状态

    private volatile int maxOperationCountToMerge;  //整数类型，表示的是等待合并的最大的异步操作的对象的数量

    //listener与listenerMainThread均是异步操作的观察者的回调，只是listenerMainThread是在主线程中回调
    private volatile AsyncOperationListener listener;
    private volatile AsyncOperationListener listenerMainThread;
    private volatile int waitForMergeMillis;  //等待合并的时间

    private int countOperationsEnqueued;    //异步操作进入队列的数目
    private int countOperationsCompleted;   //异步操作已经完成的数目

    private Handler handlerMainThread;  //主线程中处理的Handler
    private int lastSequenceNumber; //上一次的异步操作的序号

    /**
     * 在构造函数中，
     *      创建BlockingQueue的队列。
     *      初始化允许最大允许合并的异步操作的数目是50，
     *      允许最大合并超时的时间是50ms
     */
    AsyncOperationExecutor() {
        queue = new LinkedBlockingQueue<AsyncOperation>();
        maxOperationCountToMerge = 50;
        waitForMergeMillis = 50;
    }

    /**
     * 参数是异步操作AsyncOperation，
     * 在函数内部将当前的AsyncOperation进入队列。
     * 同时如果当前的线程池不是处于运行的状态，开启线程池的运行的状态
     * @param operation
     */
    public void enqueue(AsyncOperation operation) {
        synchronized (this) {
            operation.sequenceNumber = ++lastSequenceNumber;
            queue.add(operation);
            countOperationsEnqueued++;
            if (!executorRunning) {
                executorRunning = true;
                executorService.execute(this);
            }
        }
    }

    //成员变量的setter、getter方法
    public int getMaxOperationCountToMerge() {
        return maxOperationCountToMerge;
    }

    public void setMaxOperationCountToMerge(int maxOperationCountToMerge) {
        this.maxOperationCountToMerge = maxOperationCountToMerge;
    }

    public int getWaitForMergeMillis() {
        return waitForMergeMillis;
    }

    public void setWaitForMergeMillis(int waitForMergeMillis) {
        this.waitForMergeMillis = waitForMergeMillis;
    }

    public AsyncOperationListener getListener() {
        return listener;
    }

    public void setListener(AsyncOperationListener listener) {
        this.listener = listener;
    }

    public AsyncOperationListener getListenerMainThread() {
        return listenerMainThread;
    }

    public void setListenerMainThread(AsyncOperationListener listenerMainThread) {
        this.listenerMainThread = listenerMainThread;
    }

    public synchronized boolean isCompleted() {
        return countOperationsEnqueued == countOperationsCompleted;
    }

    /**
     * Waits until all enqueued operations are complete. If the thread gets interrupted, any
     * {@link InterruptedException} will be rethrown as a {@link DaoException}.
     */
    public synchronized void waitForCompletion() {
        while (!isCompleted()) {
            try {
                wait();
            } catch (InterruptedException e) {
                throw new DaoException("Interrupted while waiting for all operations to complete", e);
            }
        }
    }

    /**
     * Waits until all enqueued operations are complete, but at most the given amount of milliseconds. If the thread
     * gets interrupted, any {@link InterruptedException} will be rethrown as a {@link DaoException}.
     *
     * @return true if operations completed in the given time frame.
     */
    public synchronized boolean waitForCompletion(int maxMillis) {
        if (!isCompleted()) {
            try {
                wait(maxMillis);
            } catch (InterruptedException e) {
                throw new DaoException("Interrupted while waiting for all operations to complete", e);
            }
        }
        return isCompleted();
    }

    @Override
    public boolean handleMessage(Message msg) {
        AsyncOperationListener listenerToCall = listenerMainThread;
        if (listenerToCall != null) {
            listenerToCall.onAsyncOperationCompleted((AsyncOperation) msg.obj);
        }
        return false;
    }

    /**
     * 在while循环中，从队列中逐一的去除异步操作的对象AsyncOperation。
     * 如果两次在队列中取出的对象都是空指针，就在逻辑上将当前的线程池停止运行。
     * 当取出了异步操作的对象，判断当前的操作是否允许合并事务操作处理，如果允许，
     * (等待50ms，等待另外一次异步操作的合并，因为一次事务是昂贵的，而判断是否
     * 支持合并的条件是两个异步操作是都支持合并的同时所隶属的数据库是同一个数据库)
     */
    @Override
    public void run() {
        try {
            try {
                while (true) {
                    AsyncOperation operation = queue.poll(1, TimeUnit.SECONDS);
                    if (operation == null) {
                        synchronized (this) {
                            // Check again, this time in synchronized to be in sync with enqueue(AsyncOperation)
                            operation = queue.poll();
                            if (operation == null) {
                                // set flag while still inside synchronized
                                executorRunning = false;
                                return;
                            }
                        }
                    }
                    if (operation.isMergeTx()) {
                        // Wait some ms for another operation to merge because a TX is expensive
                        AsyncOperation operation2 = queue.poll(waitForMergeMillis, TimeUnit.MILLISECONDS);
                        if (operation2 != null) {
                            if (operation.isMergeableWith(operation2)) {
                                mergeTxAndExecute(operation, operation2);
                            } else {
                                // Cannot merge, execute both
                                executeOperationAndPostCompleted(operation);
                                executeOperationAndPostCompleted(operation2);
                            }
                            continue;
                        }
                    }
                    executeOperationAndPostCompleted(operation);
                }
            } catch (InterruptedException e) {
                DaoLog.w(Thread.currentThread().getName() + " was interruppted", e);
            }
        } finally {
            executorRunning = false;
        }
    }

    /**
     * Also checks for other operations in the queue that can be merged into the transaction.
     *
     * 两个异步操作合并事务并且进行执行
     */
    private void mergeTxAndExecute(AsyncOperation operation1, AsyncOperation operation2) {
        ArrayList<AsyncOperation> mergedOps = new ArrayList<AsyncOperation>();
        mergedOps.add(operation1);
        mergedOps.add(operation2);

        SQLiteDatabase db = operation1.getDatabase();
        db.beginTransaction();
        boolean success = false;
        try {
            for (int i = 0; i < mergedOps.size(); i++) {
                AsyncOperation operation = mergedOps.get(i);    //从合并的列表中取出异步操作的对象
                executeOperation(operation);    //执行异步操作的对象
                if (operation.isFailed()) {     //如果当前的操作失败，回滚
                    // Operation may still have changed the DB, roll back everything
                    break;
                }
                if (i == mergedOps.size() - 1) {    //如果当前的对象已经是数组中的最后一个异步执行的任务
                    AsyncOperation peekedOp = queue.peek(); //再从全局的队列中检查，当前是否有新的任务进入
                    if (i < maxOperationCountToMerge && operation.isMergeableWith(peekedOp)) {  //如果当前合并处理的最大的操作数目还没有达到，并且当前的异步操作与新取出来的操作是能够合并的
                        AsyncOperation removedOp = queue.remove();
                        if (removedOp != peekedOp) {    //同时,为了避免peek与remove取出的对象不是同一个对象,需要做异步检查
                            // Paranoia check, should not occur unless threading is broken
                            throw new DaoException("Internal error: peeked op did not match removed op");
                        }
                        mergedOps.add(removedOp);
                    } else {
                        // No more ops in the queue to merge, finish it 如果代码走到这里(没有要合并的操作了)，结束当前的事务
                        db.setTransactionSuccessful();
                        success = true;
                        break;
                    }
                }
            }
        } finally {
            try {
                db.endTransaction();
            } catch (RuntimeException e) {
                DaoLog.i("Async transaction could not be ended, success so far was: " + success, e);
                success = false;
            }
        }
        if (success) {
            int mergedCount = mergedOps.size();
            for (AsyncOperation asyncOperation : mergedOps) {
                asyncOperation.mergedOperationsCount = mergedCount;
                handleOperationCompleted(asyncOperation);
            }
        } else {
            DaoLog.i("Reverted merged transaction because one of the operations failed. Executing operations one by " +
                    "one instead...");
            for (AsyncOperation asyncOperation : mergedOps) {
                asyncOperation.reset();
                executeOperationAndPostCompleted(asyncOperation);
            }
        }
    }

    private void handleOperationCompleted(AsyncOperation operation) {
        operation.setCompleted();

        AsyncOperationListener listenerToCall = listener;
        if (listenerToCall != null) {
            listenerToCall.onAsyncOperationCompleted(operation);
        }
        if (listenerMainThread != null) {
            if (handlerMainThread == null) {
                handlerMainThread = new Handler(Looper.getMainLooper(), this);
            }
            Message msg = handlerMainThread.obtainMessage(1, operation);
            handlerMainThread.sendMessage(msg);
        }
        synchronized (this) {
            countOperationsCompleted++;
            if (countOperationsCompleted == countOperationsEnqueued) {
                notifyAll();
            }
        }
    }

    private void executeOperationAndPostCompleted(AsyncOperation operation) {
        executeOperation(operation);
        handleOperationCompleted(operation);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void executeOperation(AsyncOperation operation) {
        operation.timeStarted = System.currentTimeMillis();
        try {
            switch (operation.type) {
                case Delete:
                    operation.dao.delete(operation.parameter);
                    break;
                case DeleteInTxIterable:
                    operation.dao.deleteInTx((Iterable<Object>) operation.parameter);
                    break;
                case DeleteInTxArray:
                    operation.dao.deleteInTx((Object[]) operation.parameter);
                    break;
                case Insert:
                    operation.dao.insert(operation.parameter);
                    break;
                case InsertInTxIterable:
                    operation.dao.insertInTx((Iterable<Object>) operation.parameter);
                    break;
                case InsertInTxArray:
                    operation.dao.insertInTx((Object[]) operation.parameter);
                    break;
                case InsertOrReplace:
                    operation.dao.insertOrReplace(operation.parameter);
                    break;
                case InsertOrReplaceInTxIterable:
                    operation.dao.insertOrReplaceInTx((Iterable<Object>) operation.parameter);
                    break;
                case InsertOrReplaceInTxArray:
                    operation.dao.insertOrReplaceInTx((Object[]) operation.parameter);
                    break;
                case Update:
                    operation.dao.update(operation.parameter);
                    break;
                case UpdateInTxIterable:
                    operation.dao.updateInTx((Iterable<Object>) operation.parameter);
                    break;
                case UpdateInTxArray:
                    operation.dao.updateInTx((Object[]) operation.parameter);
                    break;
                case TransactionRunnable:
                    executeTransactionRunnable(operation);
                    break;
                case TransactionCallable:
                    executeTransactionCallable(operation);
                    break;
                case QueryList:
                    operation.result = ((Query) operation.parameter).forCurrentThread().list();
                    break;
                case QueryUnique:
                    operation.result = ((Query) operation.parameter).forCurrentThread().unique();
                    break;
                case DeleteByKey:
                    operation.dao.deleteByKey(operation.parameter);
                    break;
                case DeleteAll:
                    operation.dao.deleteAll();
                    break;
                case Load:
                    operation.result = operation.dao.load(operation.parameter);
                    break;
                case LoadAll:
                    operation.result = operation.dao.loadAll();
                    break;
                case Count:
                    operation.result = operation.dao.count();
                    break;
                case Refresh:
                    operation.dao.refresh(operation.parameter);
                    break;
                default:
                    throw new DaoException("Unsupported operation: " + operation.type);
            }
        } catch (Throwable th) {
            operation.throwable = th;
        }
        operation.timeCompleted = System.currentTimeMillis();
        // Do not set it to completed here because it might be a merged TX
    }

    private void executeTransactionRunnable(AsyncOperation operation) {
        SQLiteDatabase db = operation.getDatabase();
        db.beginTransaction();
        try {
            ((Runnable) operation.parameter).run();
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @SuppressWarnings("unchecked")
    private void executeTransactionCallable(AsyncOperation operation) throws Exception {
        SQLiteDatabase db = operation.getDatabase();
        db.beginTransaction();
        try {
            operation.result = ((Callable<Object>) operation.parameter).call();
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }
}
