package org.eluder.logback.ext.aws.core;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import org.eluder.logback.ext.core.EncodingStringAppender;

import static java.lang.String.format;

public abstract class AbstractAwsEncodingStringAppender<E> extends EncodingStringAppender<E> implements AWSCredentials {

    private final AwsSupport awsSupport = new AwsSupport();

    private String accessKey;
    private String secretKey;
    private int maxPayloadSize = 256;

    public final void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    @Override
    public String getAWSAccessKeyId() {
        return accessKey;
    }

    public final void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    @Override
    public String getAWSSecretKey() {
        return secretKey;
    }

    public void setMaxPayloadSize(int maxPayloadSize) {
        this.maxPayloadSize = maxPayloadSize;
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
