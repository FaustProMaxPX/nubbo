package icu.nubbo.handler;

import icu.nubbo.NubboClient;
import icu.nubbo.codec.NubboRequest;
import icu.nubbo.codec.NubboResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.ReentrantLock;

public class NubboFuture implements Future<Object> {

    public static final Logger log = LoggerFactory.getLogger(NubboFuture.class);

    private Sync sync;

    private NubboRequest request;

    private NubboResponse response;

    private long startTime;

    private long responseTimeThreshold = 5000;

    private List<AsyncRPCCallback> pendingCallbacks = new ArrayList<>();

    private ReentrantLock lock = new ReentrantLock();

    public NubboFuture(NubboRequest request) {
        this.sync = new Sync();
        this.request = request;
        this.startTime = System.currentTimeMillis();
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isCancelled() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isDone() {
        return sync.isDone();
    }

    @Override
    public Object get() throws InterruptedException, ExecutionException {
//        尝试获取响应，如果还没有收到，就阻塞在这
        sync.acquire(1);
        if (response != null) {
            return response.getResult();
        }
        return null;
    }

    @Override
    public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        boolean success = sync.tryAcquireNanos(1, unit.toNanos(timeout));
        if (success) {
            if (response != null) {
                return response.getResult();
            } else {
                return null;
            }
        } else {
            log.error("获取结果超时。Request id：" + request.getRequestId()
                    + " Request class name :" + request.getClassName()
                    + " Request method: " + request.getMethodName());
            throw new RuntimeException("获取结果超时。Request id：" + request.getRequestId()
                + " Request class name :" + request.getClassName()
                + " Request method: " + request.getMethodName());
        }
    }

    /*
    * 收到响应后调用，释放同步器
    * */
    public void done(NubboResponse response) {
        this.response = response;
        sync.release(1);
//        收到响应后调用回调函数
        invokeCallbacks();
        long responseTime = System.currentTimeMillis() - startTime;
        if (responseTime > this.responseTimeThreshold) {
            log.warn("请求响应速度过于缓慢，request id: {}", request.getRequestId());
        }
    }

    /*
    * 调用所有的回调操作，期间用锁来保护，避免在执行期间回调列表被修改
    * */
    private void invokeCallbacks() {
        lock.lock();
        try {
            for (AsyncRPCCallback callback : pendingCallbacks) {
                runCallback(callback);
            }
        } finally {
            lock.unlock();
        }
    }

    /*
    * 添加回调函数，如果已经收到响应，直接执行回调，如果没有，加入回调列表
    * 整个操作使用锁来保护，避免这个Future被多个线程操作
    * */
    public NubboFuture addCallback(AsyncRPCCallback callback) {
//        添加新的待执行任务，如果当前Future没有被其他线程操作，就尝试直接执行任务，否则加入等待队列
        lock.lock();
        try {
            if (isDone()) {
                runCallback(callback);
            } else {
                this.pendingCallbacks.add(callback);
            }
        } finally {
            lock.unlock();
        }
        return this;
    }

    private void runCallback(AsyncRPCCallback callback) {
        NubboResponse resp = this.response;
//        用线程池调用回调操作
        NubboClient.submit(() -> {
            if (resp.isSuccess()) {
                callback.success(resp.getResult());
            } else {
                callback.fail(new RuntimeException("响应返回异常", new Throwable(resp.getError())));
            }
        });
    }

    /**
     * 一个同步器，用于跟踪是否接收到RPC响应
     * */
    static class Sync extends AbstractQueuedSynchronizer {

        private final int done = 1;

        private final int pending = 0;

        @Override
        protected boolean tryAcquire(int arg) {
//            如果当前锁的状态是done，代表非占用，可以获取
            return getState() == done;
        }

        @Override
        protected boolean tryRelease(int arg) {
            if (getState() == pending) {
                return compareAndSetState(pending, done);
            } else {
                return true;
            }
        }

        protected boolean isDone() {
            return getState() == done;
        }
    }
}
