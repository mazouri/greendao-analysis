package com.mazouri.fork.greendao.async;

/**
 * Listener being called after completion of {@link AsyncOperation}.
 *
 * Created by wangdong on 16-1-18.
 */
public interface AsyncOperationListener {

    /**
     * AsyncOperation操作完成以后回调
     *
     * Note, that the operation may not have been successful, check
     * {@link AsyncOperation#isFailed()} and/or {@link AsyncOperation#getThrowable()} for error situations.
     */
    void onAsyncOperationCompleted(AsyncOperation operation);
}
