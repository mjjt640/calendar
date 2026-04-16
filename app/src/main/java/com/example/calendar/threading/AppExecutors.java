package com.example.calendar.threading;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AppExecutors {
    private final ExecutorService diskIoExecutor;
    private final ExecutorService computeExecutor;
    private final ExecutorService notificationExecutor;
    private final Executor mainThreadExecutor;

    public AppExecutors() {
        this.diskIoExecutor = Executors.newSingleThreadExecutor();
        this.computeExecutor = Executors.newFixedThreadPool(2);
        this.notificationExecutor = Executors.newSingleThreadExecutor();
        this.mainThreadExecutor = new MainThreadExecutor();
    }

    public ExecutorService getDiskIoExecutor() {
        return diskIoExecutor;
    }

    public ExecutorService getComputeExecutor() {
        return computeExecutor;
    }

    public ExecutorService getNotificationExecutor() {
        return notificationExecutor;
    }

    public Executor getMainThreadExecutor() {
        return mainThreadExecutor;
    }

    private static class MainThreadExecutor implements Executor {
        private final Handler handler = new Handler(Looper.getMainLooper());

        @Override
        public void execute(Runnable command) {
            handler.post(command);
        }
    }
}
