package com.mazouri.fork.greendao.async;

import com.mazouri.fork.greendao.DaoException;

/**
 * 异步操作的异常类的封装
 * Used here: {@link AsyncOperation#getResult()}.
 *
 * Created by wangdong on 16-1-18.
 */
public class AsyncDaoException extends DaoException {

    /**
     * 成员变量：failedOperation,类型是AsyncOperation，也就是说当Dao操作sql的时候，
     *          抛出异常，需要维护是哪一个异步操作抛出的异常。
     * 继承关系：继承 DaoException，DaoException本质上是运行时的异常。
     * 扩展方法：getFailedOperation，向外界暴露了AsyncOperation，知道是哪一步出现了运行时的异常。
     */
    private static final long serialVersionUID = 5872157552005102382L;

    private final AsyncOperation failedOperation;

    public AsyncDaoException(AsyncOperation failedOperation, Throwable cause) {
        super(cause);
        this.failedOperation = failedOperation;
    }

    public AsyncOperation getFailedOperation() {
        return failedOperation;
    }

}
