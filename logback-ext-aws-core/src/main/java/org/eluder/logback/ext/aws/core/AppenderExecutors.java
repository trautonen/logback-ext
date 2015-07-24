package org.eluder.logback.ext.aws.core;

import ch.qos.logback.core.Appender;
import ch.qos.logback.core.AsyncAppenderBase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;

public class AppenderExecutors {

    public static final int DEFAULT_THREAD_POOL_SIZE = 20;
    public static final int DEFAULT_MAX_FLUSH_TIME = AsyncAppenderBase.DEFAULT_MAX_FLUSH_TIME; // milliseconds

    public static void shutdown(Appender<?> appender, ExecutorService executor, long waitMillis) {
        executor.shutdown();
        boolean completed = awaitTermination(appender, executor, waitMillis);
        if (!completed) {
            appender.addWarn(format("Executor for %s did not shut down in %d milliseconds, " +
                            "logging events might have been discarded",
                    appender.getName(), waitMillis));
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
