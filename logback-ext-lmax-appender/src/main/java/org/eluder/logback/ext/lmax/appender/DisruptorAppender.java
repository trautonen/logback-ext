package org.eluder.logback.ext.lmax.appender;

import ch.qos.logback.core.UnsynchronizedAppenderBase;
import ch.qos.logback.core.spi.ContextAware;
import ch.qos.logback.core.spi.DeferredProcessingAware;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.EventTranslatorOneArg;
import com.lmax.disruptor.ExceptionHandler;
import com.lmax.disruptor.LifecycleAware;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.TimeoutException;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import org.eluder.logback.ext.core.AppenderExecutors;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import static java.lang.String.format;

public class DisruptorAppender<E extends DeferredProcessingAware> extends UnsynchronizedAppenderBase<E> {

    protected final ReentrantLock lock = new ReentrantLock(true);

    private static final int SLEEP_ON_DRAIN = 50;
    private static final int DEFAULT_THREAD_POOL_SIZE = 1;
    private static final int DEFAULT_BUFFER_SIZE = 8192;

    private EventFactory<LogEvent<E>> eventFactory = new LogEventFactory<E>();
    private EventTranslatorOneArg<LogEvent<E>, E> eventTranslator = new LogEventTranslator<E>();
    private ExceptionHandler<LogEvent<E>> exceptionHandler = new LogExceptionHandler<E>(this);
    private EventHandler<LogEvent<E>> eventHandler;

    private int threadPoolSize = DEFAULT_THREAD_POOL_SIZE;
    private int bufferSize = DEFAULT_BUFFER_SIZE;
    private int maxFlushTime = AppenderExecutors.DEFAULT_MAX_FLUSH_TIME;
    private ProducerType producerType = ProducerType.MULTI;
    private WaitStrategy waitStrategy = new BlockingWaitStrategy();

    private Disruptor<LogEvent<E>> disruptor;
    private ExecutorService executor;

    public void setEventFactory(EventFactory<LogEvent<E>> eventFactory) {
        this.eventFactory = eventFactory;
    }

    public void setEventTranslator(EventTranslatorOneArg<LogEvent<E>, E> eventTranslator) {
        this.eventTranslator = eventTranslator;
    }

    public void setExceptionHandler(ExceptionHandler<LogEvent<E>> exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
    }

    public void setEventHandler(EventHandler<LogEvent<E>> eventHandler) {
        this.eventHandler = eventHandler;
    }

    public void setThreadPoolSize(int threadPoolSize) {
        this.threadPoolSize = threadPoolSize;
    }

    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    public void setMaxFlushTime(int maxFlushTime) {
        this.maxFlushTime = maxFlushTime;
    }

    public void setProducerType(ProducerType producerType) {
        this.producerType = producerType;
    }

    public void setWaitStrategy(WaitStrategy waitStrategy) {
        this.waitStrategy = waitStrategy;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void start() {
        if (eventHandler == null) {
            addError(format("Event handler not set for appender '%s'", getName()));
            return;
        }
        lock.lock();
        try {
            if (isStarted()) {
                return;
            }
            executor = AppenderExecutors.newExecutor(this, threadPoolSize);
            disruptor = new Disruptor<LogEvent<E>>(
                    eventFactory,
                    bufferSize,
                    executor,
                    producerType,
                    waitStrategy
            );
            disruptor.handleExceptionsWith(exceptionHandler);
            disruptor.handleEventsWith(new ClearingEventHandler<E>(eventHandler));
            disruptor.start();
            super.start();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void stop() {
        lock.lock();
        try {
            if (!isStarted()) {
                return;
            }
            super.stop();
            shutdownDisruptor();
            executor.shutdownNow();
        } finally {
            lock.unlock();
        }
    }

    @Override
    protected void append(E eventObject) {
        prepareForDeferredProcessing(eventObject);
        disruptor.publishEvent(eventTranslator, eventObject);
    }

    protected void prepareForDeferredProcessing(E event) {
        event.prepareForDeferredProcessing();
    }

    private void shutdownDisruptor() {
        // disruptor busy waits while shutting down so this is a workaround
        // for not to hog all the CPU on shutdown
        long until = System.currentTimeMillis() + maxFlushTime;
        while (System.currentTimeMillis() < until && hashBackLog()) {
            try {
                Thread.sleep(SLEEP_ON_DRAIN);
            } catch (InterruptedException ex) {
                // noop
            }
        }
        try {
            disruptor.shutdown(0, TimeUnit.MILLISECONDS);
        } catch (TimeoutException ex) {
            addWarn(format("Disruptor for %s did not shut down in %d milliseconds, " +
                           "logging events might have been discarded",
                           getName(), maxFlushTime));
        }
    }

    private boolean hashBackLog() {
        RingBuffer<LogEvent<E>> buffer = disruptor.getRingBuffer();
        return !buffer.hasAvailableCapacity(buffer.getBufferSize());
    }

    protected static class LogEvent<E> {
        public volatile E event;
    }

    protected static class LogEventFactory<E> implements EventFactory<LogEvent<E>> {
        @Override
        public LogEvent<E> newInstance() {
            return new LogEvent<E>();
        }
    }

    protected static class LogEventTranslator<E> implements EventTranslatorOneArg<LogEvent<E>, E> {
        @Override
        public void translateTo(LogEvent<E> event, long sequence, E arg0) {
            event.event = arg0;
        }
    }

    protected static class LogExceptionHandler<E> implements ExceptionHandler<LogEvent<E>> {

        private final ContextAware context;

        public LogExceptionHandler(ContextAware context) {
            this.context = context;
        }

        @Override
        public void handleEventException(Throwable ex, long sequence, LogEvent<E> event) {
            context.addError("Failed to process event: " + event.event, ex);
        }

        @Override
        public void handleOnStartException(Throwable ex) {
            context.addError("Failed to start disruptor", ex);
        }

        @Override
        public void handleOnShutdownException(Throwable ex) {
            context.addError("Failed to shutdown disruptor", ex);
        }
    }

    /**
     * Clears logback event objects from distruptor event context to allow proper garbage collecting.
     */
    private static class ClearingEventHandler<E> implements EventHandler<LogEvent<E>>, LifecycleAware {

        private final EventHandler<LogEvent<E>> delegate;

        public ClearingEventHandler(EventHandler<LogEvent<E>> delegate) {
            this.delegate = delegate;
        }

        @Override
        public void onEvent(LogEvent<E> event, long sequence, boolean endOfBatch) throws Exception {
            try {
                delegate.onEvent(event, sequence, endOfBatch);
            } finally {
                event.event = null;
            }
        }

        @Override
        public void onStart() {
            if (delegate instanceof LifecycleAware) {
                ((LifecycleAware) delegate).onStart();
            }
        }

        @Override
        public void onShutdown() {
            if (delegate instanceof LifecycleAware) {
                ((LifecycleAware) delegate).onShutdown();
            }
        }
    }
}
