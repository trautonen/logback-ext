package org.eluder.logback.ext.sqs.appender;

/*
 * #[license]
 * logback-ext-sqs-appender
 * %%
 * Copyright (C) 2014 - 2015 Tapio Rautonen
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * %[license]
 */

import ch.qos.logback.classic.AsyncAppender;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.encoder.Encoder;

import java.nio.charset.Charset;

public class AsyncSqsAppender extends AsyncAppender {
    
    private final SqsAppender appender;
    
    public AsyncSqsAppender() {
        this(new SqsAppender());
    }

    protected AsyncSqsAppender(SqsAppender appender) {
        appender.setAsyncParent(true);
        addAppender(appender);
        this.appender = appender;
    }

    public void setQueueUrl(String queueUrl) {
        appender.setQueueUrl(queueUrl);
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
