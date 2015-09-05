package org.eluder.logback.ext.lmax.appender;

import ch.qos.logback.classic.AsyncAppender;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;

public class AsyncAppenderBenchmark extends AppenderBenchmark {

    @Override
    protected Appender<ILoggingEvent> createAppender(int consumers) {
        AsyncAppender appender = new AsyncAppender();
        appender.setName("async");
        appender.setQueueSize(262144);
        return appender;
    }
}
