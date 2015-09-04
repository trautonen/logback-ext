package org.eluder.logback.ext.lmax;

import ch.qos.logback.core.Appender;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.spi.AppenderAttachable;
import ch.qos.logback.core.spi.AppenderAttachableImpl;
import ch.qos.logback.core.spi.DeferredProcessingAware;
import com.lmax.disruptor.EventHandler;

import java.util.Iterator;

public class DelegatingDisruptorAppender<E extends DeferredProcessingAware> extends DisruptorAppender<E> implements AppenderAttachable<E> {

    private final AppenderAttachableImpl<E> appenders = new AppenderAttachableImpl<E>();

    public DelegatingDisruptorAppender() {
        setEventHandler(new AppenderEventHandler());
    }

    @Override
    public void start() {
        lock.lock();
        try {
            if (isStarted()) {
                return;
            }
            startDelegateAppenders();
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
            stopDelegateAppenders();
        } finally {
            lock.unlock();
        }
    }

    protected void startDelegateAppenders() {
        Iterator<Appender<E>> iter = appenders.iteratorForAppenders();
        while (iter.hasNext()) {
            Appender<E> appender = iter.next();
            if (!appender.isStarted()) {
                appender.start();
            }
        }
    }

    protected void stopDelegateAppenders() {
        Iterator<Appender<E>> iter = appenders.iteratorForAppenders();
        while (iter.hasNext()) {
            Appender<E> appender = iter.next();
            if (appender.isStarted()) {
                appender.stop();
            }
        }
    }

    @Override
    public void addAppender(Appender<E> newAppender) {
        if (getContext() != null && newAppender.getContext() == null) {
            newAppender.setContext(getContext());
        }
        appenders.addAppender(newAppender);
    }

    @Override
    public Iterator<Appender<E>> iteratorForAppenders() {
        return appenders.iteratorForAppenders();
    }

    @Override
    public Appender<E> getAppender(String name) {
        return appenders.getAppender(name);
    }

    @Override
    public boolean isAttached(Appender<E> appender) {
        return appenders.isAttached(appender);
    }

    @Override
    public void detachAndStopAllAppenders() {
        appenders.detachAndStopAllAppenders();
    }

    @Override
    public boolean detachAppender(Appender<E> appender) {
        return appenders.detachAppender(appender);
    }

    @Override
    public boolean detachAppender(String name) {
        return appenders.detachAppender(name);
    }

    @Override
    public void setContext(Context context) {
        Iterator<Appender<E>> iter = appenders.iteratorForAppenders();
        while (iter.hasNext()) {
            Appender<E> appender = iter.next();
            if (appender.getContext() == null) {
                appender.setContext(context);
            }
        }
        super.setContext(context);
    }

    private class AppenderEventHandler implements EventHandler<LogEvent<E>> {
        @Override
        public void onEvent(LogEvent<E> event, long sequence, boolean endOfBatch) throws Exception {
            appenders.appendLoopOnAppenders(event.event);
        }
    }

}
