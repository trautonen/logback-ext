package org.eluder.logback.ext.lmax.appender;

import ch.qos.logback.core.UnsynchronizedAppenderBase;
import ch.qos.logback.core.spi.ContextAware;
import ch.qos.logback.core.spi.DeferredProcessingAware;
import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventTranslatorOneArg;
import com.lmax.disruptor.ExceptionHandler;
import com.lmax.disruptor.LifecycleAware;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.TimeoutException;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.WorkHandler;
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

    private EventFactory<LogEvent<E>> eventFactory = new LogEventFactory<>();
    private EventTranslatorOneArg<LogEvent<E>, E> eventTranslator = new LogEventTranslator<>();
    private ExceptionHandler<LogEvent<E>> exceptionHandler = new LogExceptionHandler<>(this);
    private WorkHandler<LogEvent<E>> workHandler;

    private int threadPoolSize = DEFAULT_THREAD_POOL_SIZE;
    private int bufferSize = DEFAULT_BUFFER_SIZE;
    private int maxFlushTime = AppenderExecutors.DEFAULT_MAX_FLUSH_TIME;
    private ProducerType producerType = ProducerType.MULTI;
    private WaitStrategy waitStrategy = WaitStrategyFactory.DEFAULT_WAIT_STRATEGY;

    private Disruptor<LogEvent<E>> disruptor;
    private ExecutorService executor;

    public final void setEventFactory(EventFactory<LogEvent<E>> eventFactory) {
        this.eventFactory = eventFactory;
    }

    public final void setEventTranslator(EventTranslatorOneArg<LogEvent<E>, E> eventTranslator) {
        this.eventTranslator = eventTranslator;
    }

    public final void setExceptionHandler(ExceptionHandler<LogEvent<E>> exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
    }

    public final void setWorkHandler(WorkHandler<LogEvent<E>> workHandler) {
        this.workHandler = workHandler;
    }

    public final void setThreadPoolSize(int threadPoolSize) {
        this.threadPoolSize = threadPoolSize;
    }

    public final void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    public final void setMaxFlushTime(int maxFlushTime) {
        this.maxFlushTime = maxFlushTime;
    }

    public final void setProducerType(ProducerType producerType) {
        this.producerType = producerType;
    }

    public final void setWaitStrategy(WaitStrategy waitStrategy) {
        this.waitStrategy = waitStrategy;
    }

    public final void setWaitStrategyType(String waitStrategyType) {
        setWaitStrategy(WaitStrategyFactory.createFromType(waitStrategyType));
    }

    @Override
    @SuppressWarnings("unchecked")
    public void start() {
        if (workHandler == null) {
            addError(format("Event handler not set for appender '%s'", getName()));
            return;
        }
        lock.lock();
        try {
            if (isStarted()) {
                return;
            }
            executor = AppenderExecutors.newExecutor(this, threadPoolSize);
            disruptor = new Disruptor<>(
                    eventFactory,
                    bufferSize,
                    executor,
                    producerType,
                    waitStrategy
            );
            disruptor.handleExceptionsWith(exceptionHandler);
            disruptor.handleEventsWithWorkerPool(createWorkers());
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

    @SuppressWarnings("unchecked")
    private WorkHandler<LogEvent<E>>[] createWorkers() {
        WorkHandler<LogEvent<E>> handler = new ClearingWorkHandler<>(workHandler);
        WorkHandler<LogEvent<E>>[] workers = new WorkHandler[threadPoolSize];
        for (int i = 0; i < threadPoolSize; i++) {
            workers[i] = handler;
        }
        return workers;
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
            addWarn(format("Disruptor did not shut down in %d milliseconds, " +
                           "logging events might have been discarded",
                           maxFlushTime));
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
            return new LogEvent<>();
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
            if (ex instanceof InterruptedException) {
                context.addWarn("Disruptor was interrupted while processing event");
            } else {
                context.addError("Failed to process event", ex);
            }
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
    private static class ClearingWorkHandler<E> implements WorkHandler<LogEvent<E>>, LifecycleAware {

        private final WorkHandler<LogEvent<E>> delegate;
        private boolean started = false;

        public ClearingWorkHandler(WorkHandler<LogEvent<E>> delegate) {
            this.delegate = delegate;
        }

        @Override
        public void onEvent(LogEvent<E> event) throws Exception {
            try {
                delegate.onEvent(event);
            } finally {
                event.event = null;
            }
        }

        @Override
        public synchronized void onStart() {
            if (!started) {
                if (delegate instanceof LifecycleAware) {
                    ((LifecycleAware) delegate).onStart();
                }
                started = true;
            }
        }

        @Override
        public synchronized void onShutdown() {
            if (started) {
                started = false;
                if (delegate instanceof LifecycleAware) {
                    ((LifecycleAware) delegate).onShutdown();
                }
            }
        }
    }
}
