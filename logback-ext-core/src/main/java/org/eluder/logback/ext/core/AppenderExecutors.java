package org.eluder.logback.ext.core;

import ch.qos.logback.core.Appender;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.String.format;

public class AppenderExecutors {

    public static final int DEFAULT_THREAD_POOL_SIZE = 20;
    public static final int DEFAULT_MAX_FLUSH_TIME = 3000; // milliseconds

    public static ExecutorService newExecutor(Appender<?> appender, int threadPoolSize) {
        final String name = appender.getName();
        return Executors.newFixedThreadPool(threadPoolSize, new ThreadFactory() {

            private final AtomicInteger idx = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName(name + "-" + idx.getAndIncrement());
                thread.setDaemon(true);
                return thread;
            }
        });
    }

    public static void shutdown(Appender<?> appender, ExecutorService executor, long waitMillis) {
        executor.shutdown();
        boolean completed = awaitTermination(appender, executor, waitMillis);
        if (!completed) {
            appender.addWarn(format("Executor for %s did not shut down in %d milliseconds, " +
                            "logging events might have been discarded",
                    appender.getName(), waitMillis));
        }
    }

    public static void awaitLatch(Appender<?> appender, CountDownLatch latch, long waitMillis) {
        if (latch.getCount() > 0) {
            try {
                boolean completed = latch.await(waitMillis, TimeUnit.MILLISECONDS);
                if (!completed) {
                    appender.addWarn(format("Appender '%s' did not complete sending event in %d milliseconds, " +
                                    "the event might have been lost",
                            appender.getName(), waitMillis));
                }
            } catch (InterruptedException ex) {
                appender.addWarn(format("Appender '%s' was interrupted, " +
                                "a logging event might have been lost or shutdown was initiated",
                        appender.getName()));
                Thread.currentThread().interrupt();
            }
        }
    }

    private static boolean awaitTermination(Appender<?> appender, ExecutorService executor, long waitMillis) {
        long started = System.currentTimeMillis();
        try {
            return executor.awaitTermination(waitMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ie1) {
            // the worker loop is stopped by interrupt, but the remaining queue should still be handled
            long waited = System.currentTimeMillis() - started;
            if (waited < waitMillis) {
                try {
                    return executor.awaitTermination(waitMillis - waited, TimeUnit.MILLISECONDS);
                } catch (InterruptedException ie2) {
                    appender.addError(format("Shut down of executor for %s was interrupted",
                            appender.getName()));
                }
            }
            Thread.currentThread().interrupt();
        }
        return false;
    }

    private AppenderExecutors() {
    }
}
