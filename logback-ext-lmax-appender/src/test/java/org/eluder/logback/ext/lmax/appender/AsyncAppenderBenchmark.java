package org.eluder.logback.ext.lmax.appender;

import ch.qos.logback.classic.AsyncAppender;

public class AsyncAppenderBenchmark extends AppenderBenchmark<AsyncAppender> {

    @Override
    protected AsyncAppender createAppender(int consumers) {
        AsyncAppender appender = new AsyncAppender();
        appender.setName("async");
        appender.setQueueSize(1048576);
        return appender;
    }
}
