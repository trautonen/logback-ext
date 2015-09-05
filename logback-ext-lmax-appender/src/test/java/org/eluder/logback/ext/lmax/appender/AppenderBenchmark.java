package org.eluder.logback.ext.lmax.appender;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import ch.qos.logback.core.spi.AppenderAttachable;
import org.openjdk.jmh.annotations.*;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public abstract class AppenderBenchmark {

    private static final LoggerContext CONTEXT = (LoggerContext) LoggerFactory.getILoggerFactory();
    private static final LoggingEvent EVENT = new LoggingEvent(
            "org.eluder.logback.ext.lmax.appender.LoggingEventDisruptorAppenderBenchmark",
            CONTEXT.getLogger(Logger.ROOT_LOGGER_NAME), Level.INFO, "Hello world", null, null
    );


    @Param({ "16000", "64000", "128000" })
    public int events;

    @Param({ "1", "4", "8" })
    public int producers;

    @Param({ "1" })
    public int consumers;

    private CountDownLatch latch;
    private Appender<ILoggingEvent> appender;
    private List<Thread> threads;

    @Setup(org.openjdk.jmh.annotations.Level.Iteration)
    @SuppressWarnings("unchecked")
    public void init() {
        this.latch = new CountDownLatch(events);
        this.appender = createAppender(consumers);
        this.appender.setContext(CONTEXT);
        ((AppenderAttachable<ILoggingEvent>) this.appender).addAppender(new EmulatingAppender(this.latch, CONTEXT));
        this.appender.start();
        this.threads = createThreads(this.appender);
    }

    @TearDown(org.openjdk.jmh.annotations.Level.Iteration)
    public void clear() {
        appender.stop();
    }

    protected abstract Appender<ILoggingEvent> createAppender(int consumers);

    @Benchmark
    @Warmup(iterations = 3)
    public void benchmarkAppender() throws Exception {
        for (Thread t : threads) {
            t.run();
        }
        for (Thread t : threads) {
            t.join();
        }
        latch.await();
    }

    private List<Thread> createThreads(final Appender<ILoggingEvent> appender) {
        final int iterationsPerProducer = events / producers;
        List<Thread> threads = new ArrayList<Thread>(producers);
        for (int i = 0; i < producers; i++) {
            threads.add(Executors.defaultThreadFactory().newThread(new Runnable() {
                @Override
                public void run() {
                    for (int j = 0; j < iterationsPerProducer; j++) {
                        appender.doAppend(EVENT);
                    }
                }
            }));
        }
        return threads;
    }

    public static class EmulatingAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {

        private final CountDownLatch latch;

        public EmulatingAppender(CountDownLatch latch, LoggerContext context) {
            this.latch = latch;
            setContext(context);
            setName("emulator");
            start();
        }

        @Override
        protected void append(ILoggingEvent eventObject) {
            this.latch.countDown();
        }
    }
}
