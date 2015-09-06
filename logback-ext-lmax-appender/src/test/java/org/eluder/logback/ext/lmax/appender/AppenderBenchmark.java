package org.eluder.logback.ext.lmax.appender;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import ch.qos.logback.core.spi.AppenderAttachable;
import org.openjdk.jmh.annotations.*;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@State(Scope.Benchmark)
public abstract class AppenderBenchmark<T extends Appender<ILoggingEvent> & AppenderAttachable<ILoggingEvent>> {

    private static final int EVENTS = 32000;
    private static final LoggerContext CONTEXT = (LoggerContext) LoggerFactory.getILoggerFactory();

    private static final AtomicInteger idx = new AtomicInteger(0);
    private static final CountDownLatch[] latches = new CountDownLatch[99999];


    @Param({ "1" })
    public int consumers;

    private EmulatingAppender controller;
    private T appender;

    @State(Scope.Thread)
    public static class ThreadContext {
        LoggingEvent event;
        int index;

        @Setup(Level.Trial)
        public void setupContext() {
            index = idx.getAndIncrement();
            event = new LoggingEvent(
                    "org.eluder.logback.ext.lmax.appender.AppenderBenchmark",
                    CONTEXT.getLogger(Logger.ROOT_LOGGER_NAME), ch.qos.logback.classic.Level.INFO, "" + index, null, null
            );
        }

        @Setup(Level.Invocation)
        public void setupLatch() {
            latches[index] = new CountDownLatch(EVENTS);
        }

        public void await() throws InterruptedException {
            latches[index].await();
        }
    }

    @Setup(Level.Trial)
    public void setupAppender() {
        controller = new EmulatingAppender(CONTEXT);
        appender = createAppender(consumers);
        appender.setContext(CONTEXT);
        appender.addAppender(controller);

        controller.start();
        appender.start();
    }

    @TearDown(Level.Trial)
    public void tearDownAppender() {
        appender.stop();
        controller.stop();
    }

    protected abstract T createAppender(int consumers);

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    @OperationsPerInvocation(EVENTS)
    public void throughput(ThreadContext context) throws Exception {
        append(context);
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    @OperationsPerInvocation(EVENTS)
    public void latency(ThreadContext context) throws Exception {
        append(context);
    }

    private void append(ThreadContext context) throws Exception {
        for (int i = 0; i < EVENTS; i++) {
            appender.doAppend(context.event);
        }
        context.await();
    }

    public static class EmulatingAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {

        public EmulatingAppender(LoggerContext context) {
            setContext(context);
            setName("emulator");
        }

        @Override
        protected void append(ILoggingEvent eventObject) {
            int index = Integer.parseInt(eventObject.getMessage());
            latches[index].countDown();
        }
    }
}
