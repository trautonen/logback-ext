package org.eluder.logback.ext.sns.appender;

import ch.qos.logback.classic.AsyncAppender;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.encoder.Encoder;

import java.nio.charset.Charset;

public class AsyncSnsAppender extends AsyncAppender {

    private final SnsAppender appender;

    public AsyncSnsAppender() {
        this(new SnsAppender());
    }

    protected AsyncSnsAppender(SnsAppender appender) {
        appender.setAsyncParent(true);
        addAppender(appender);
        this.appender = appender;
    }

    public void setAccessKey(String accessKey) {
        appender.setAccessKey(accessKey);
    }

    public void setSecretKey(String secretKey) {
        appender.setSecretKey(secretKey);
    }

    public void setMaxPayloadSize(int maxPayloadSize) {
        appender.setMaxPayloadSize(maxPayloadSize);
    }

    public void setRegion(String region) {
        appender.setRegion(region);
    }

    public void setTopic(String topic) {
        appender.setTopic(topic);
    }

    public void setSubject(String subject) {
        appender.setSubject(subject);
    }

    public final void setThreadPoolSize(int threadPoolSize) {
        appender.setThreadPoolSize(threadPoolSize);
    }

    @Override
    public final void setMaxFlushTime(int maxFlushTime) {
        appender.setMaxFlushTime(maxFlushTime);
        // add an extra 100 millis to wait for the internal event queue handling
        super.setMaxFlushTime(maxFlushTime + 100);
    }

    public void setCharset(Charset charset) {
        appender.setCharset(charset);
    }

    public void setEncoder(Encoder<ILoggingEvent> encoder) {
        appender.setEncoder(encoder);
    }

    public void setLayout(Layout<ILoggingEvent> layout) {
        appender.setLayout(layout);
    }

    public void setBinary(boolean binary) {
        appender.setBinary(binary);
    }

    @Override
    public void setName(String name) {
        appender.setName(name);
        super.setName(name);
    }

    @Override
    public void setContext(Context context) {
        appender.setContext(context);
        super.setContext(context);
    }

    @Override
    public void start() {
        appender.start();
        super.start();
    }
}
