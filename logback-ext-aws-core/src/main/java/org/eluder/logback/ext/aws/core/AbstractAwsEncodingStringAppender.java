package org.eluder.logback.ext.aws.core;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSCredentialsProviderChain;
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.auth.SystemPropertiesCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.internal.StaticCredentialsProvider;
import org.eluder.logback.ext.core.EncodingStringAppender;

import java.io.Closeable;

import static java.lang.String.format;

public abstract class AbstractAwsEncodingStringAppender<E> extends EncodingStringAppender<E> implements Closeable {

    private String accessKey;
    private String secretKey;
    private int maxPayloadSize = 256;

    public final void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public final void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public void setMaxPayloadSize(int maxPayloadSize) {
        this.maxPayloadSize = maxPayloadSize;
    }

    @Override
    public void start() {
        lock.lock();
        try {
            close();
            doInit();
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
            close();
        } finally {
            lock.unlock();
        }

    }

    @Override
    public void close() {
        lock.lock();
        try {
            doClose();
        } finally {
            lock.unlock();
        }
    }

    protected abstract void doInit();

    protected abstract void doClose();

    protected AWSCredentialsProvider getCredentials() {
        return new AWSCredentialsProviderChain(
                new EnvironmentVariableCredentialsProvider(),
                new SystemPropertiesCredentialsProvider(),
                new StaticCredentialsProvider(new AppenderCredentials()),
                new ProfileCredentialsProvider(),
                new InstanceProfileCredentialsProvider()
        );
    }

    protected ClientConfiguration getClientConfiguration() {
        return new ClientConfiguration();
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

    private class AppenderCredentials implements AWSCredentials {
        @Override
        public String getAWSAccessKeyId() {
            return accessKey;
        }

        @Override
        public String getAWSSecretKey() {
            return secretKey;
        }
    }

}
