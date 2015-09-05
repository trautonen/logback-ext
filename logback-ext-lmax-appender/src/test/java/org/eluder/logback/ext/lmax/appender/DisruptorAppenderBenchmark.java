package org.eluder.logback.ext.lmax.appender;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;

public class DisruptorAppenderBenchmark extends AppenderBenchmark {

    @Override
    protected Appender<ILoggingEvent> createAppender(int consumers) {
        LoggingEventDisruptorAppender appender = new LoggingEventDisruptorAppender();
        appender.setName("disruptor");
        appender.setBufferSize(262144);
        appender.setThreadPoolSize(consumers);
        return appender;
    }
}
