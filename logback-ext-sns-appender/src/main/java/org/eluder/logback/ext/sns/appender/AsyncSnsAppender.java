package org.eluder.logback.ext.sns.appender;

import ch.qos.logback.classic.AsyncAppender;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.encoder.Encoder;

import java.nio.charset.Charset;

public class AsyncSnsAppender extends AsyncAppender {

    private final SnsAppender appender;

    public AsyncSnsAppender() {
        this.appender = new SnsAppender();
        addAppender(this.appender);
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
    public void start() {
        appender.start();
        super.start();
    }
}
