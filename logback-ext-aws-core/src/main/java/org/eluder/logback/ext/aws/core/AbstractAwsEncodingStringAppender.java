package org.eluder.logback.ext.aws.core;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import org.eluder.logback.ext.core.AppenderExecutors;
import org.eluder.logback.ext.core.EncodingStringAppender;

import static java.lang.String.format;

public abstract class AbstractAwsEncodingStringAppender<E> extends EncodingStringAppender<E> implements AWSCredentials {

    protected final AwsSupport awsSupport;

    private String accessKey;
    private String secretKey;
    private int maxPayloadSize = 256;
    private boolean asyncParent = false;
    private int threadPoolSize = AppenderExecutors.DEFAULT_THREAD_POOL_SIZE;
    private int maxFlushTime = AppenderExecutors.DEFAULT_MAX_FLUSH_TIME;

    protected AbstractAwsEncodingStringAppender() {
        this(new AwsSupport());
    }

    protected AbstractAwsEncodingStringAppender(AwsSupport awsSupport) {
        this.awsSupport = awsSupport;
    }

    public final void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    @Override
    public final String getAWSAccessKeyId() {
        return accessKey;
    }

    public final void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    @Override
    public final String getAWSSecretKey() {
        return secretKey;
    }

    public final void setMaxPayloadSize(int maxPayloadSize) {
        this.maxPayloadSize = maxPayloadSize;
    }

    protected final int getMaxPayloadSize() {
        return maxPayloadSize;
    }

    public final void setAsyncParent(boolean asyncParent) {
        this.asyncParent = asyncParent;
    }

    protected final boolean isAsyncParent() {
        return asyncParent;
    }

    public final void setThreadPoolSize(int threadPoolSize) {
        this.threadPoolSize = threadPoolSize;
    }

    protected final int getThreadPoolSize() {
        return threadPoolSize;
    }

    public final void setMaxFlushTime(int maxFlushTime) {
        this.maxFlushTime = maxFlushTime;
    }

    protected final int getMaxFlushTime() {
        return maxFlushTime;
    }

    @Override
    public void start() {
        lock.lock();
        try {
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
