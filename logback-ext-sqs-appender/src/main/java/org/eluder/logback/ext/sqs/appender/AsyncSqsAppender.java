package org.eluder.logback.ext.sqs.appender;

import ch.qos.logback.classic.AsyncAppender;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.encoder.Encoder;

import java.nio.charset.Charset;

public class AsyncSqsAppender extends AsyncAppender {
    
    private final SqsAppender appender;
    
    public AsyncSqsAppender() {
        SqsAppender appender = new SqsAppender();
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

    public void setQueueUrl(String queueUrl) {
        appender.setQueueUrl(queueUrl);
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
