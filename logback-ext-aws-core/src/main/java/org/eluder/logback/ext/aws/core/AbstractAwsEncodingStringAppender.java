package org.eluder.logback.ext.aws.core;

/*
 * #[license]
 * logback-ext-aws-core
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

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.filter.Filter;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import org.eluder.logback.ext.core.AppenderExecutors;
import org.eluder.logback.ext.core.EncodingStringAppender;

import static java.lang.String.format;

public abstract class AbstractAwsEncodingStringAppender extends EncodingStringAppender<ILoggingEvent> implements AWSCredentials {

    protected final AwsSupport awsSupport;
    protected final Filter<ILoggingEvent> sdkLoggingFilter;

    private String accessKey;
    private String secretKey;
    private int maxPayloadSize = 256;
    private boolean asyncParent = false;
    private int threadPoolSize = AppenderExecutors.DEFAULT_THREAD_POOL_SIZE;
    private int maxFlushTime = AppenderExecutors.DEFAULT_MAX_FLUSH_TIME;

    protected AbstractAwsEncodingStringAppender() {
        this(new AwsSupport(), new InternalSdkLoggingFilter());
    }

    protected AbstractAwsEncodingStringAppender(AwsSupport awsSupport, Filter<ILoggingEvent> sdkLoggingFilter) {
        this.awsSupport = awsSupport;
        this.sdkLoggingFilter = sdkLoggingFilter;
        addFilter(sdkLoggingFilter);
    }

    public final void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public final void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public final void setMaxPayloadSize(int maxPayloadSize) {
        this.maxPayloadSize = maxPayloadSize;
    }

    public final void setAsyncParent(boolean asyncParent) {
        this.asyncParent = asyncParent;
    }

    public final void setThreadPoolSize(int threadPoolSize) {
        this.threadPoolSize = threadPoolSize;
    }

    public final void setMaxFlushTime(int maxFlushTime) {
        this.maxFlushTime = maxFlushTime;
    }

    @Override
    public void setContext(Context context) {
        sdkLoggingFilter.setContext(context);
        super.setContext(context);
    }

    @Override
    public final String getAWSAccessKeyId() {
        return accessKey;
    }

    @Override
    public final String getAWSSecretKey() {
        return secretKey;
    }

    protected final int getMaxPayloadSize() {
        return maxPayloadSize;
    }

    protected final boolean isAsyncParent() {
        return asyncParent;
    }

    protected final int getThreadPoolSize() {
        return threadPoolSize;
    }

    protected final int getMaxFlushTime() {
        return maxFlushTime;
    }

    @Override
    public void start() {
        lock.lock();
        try {
            sdkLoggingFilter.start();
            doStart();
            super.start();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void stop() {
        lock.lock();
        try {
            super.stop();
            doStop();
            sdkLoggingFilter.stop();
        } finally {
            lock.unlock();
        }
    }

    protected abstract void doStart();

    protected abstract void doStop();

    protected AWSCredentialsProvider getCredentials() {
        return awsSupport.getCredentials(this);
    }

    protected ClientConfiguration getClientConfiguration() {
        return awsSupport.getClientConfiguration();
    }

    @Override
    protected String convert(byte[] payload) {
        String converted = super.convert(payload);
        if (converted.getBytes().length > (maxPayloadSize * 1024)) {
            addWarn(format("Logging event '%s' exceeds the maximum size of %dkB", converted, maxPayloadSize));
            return null;
        } else {
            return converted;
        }
    }

}
