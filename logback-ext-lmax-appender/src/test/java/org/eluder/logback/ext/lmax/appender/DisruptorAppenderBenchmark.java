package org.eluder.logback.ext.lmax.appender;

public class DisruptorAppenderBenchmark extends AppenderBenchmark<LoggingEventDisruptorAppender> {

    @Override
    protected LoggingEventDisruptorAppender createAppender(int consumers) {
        LoggingEventDisruptorAppender appender = new LoggingEventDisruptorAppender();
        appender.setName("disruptor");
        appender.setBufferSize(1048576);
        appender.setThreadPoolSize(consumers);
        return appender;
    }
}
