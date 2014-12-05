package org.eluder.logback.ext.sqs.appender;

import static java.lang.String.format;

import java.io.Closeable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSCredentialsProviderChain;
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.auth.SystemPropertiesCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.handlers.AsyncHandler;
import com.amazonaws.internal.StaticCredentialsProvider;
import com.amazonaws.services.sqs.AmazonSQSAsyncClient;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageResult;
import org.eluder.logback.ext.core.EncodingStringAppender;

public class SqsAppender extends EncodingStringAppender<ILoggingEvent> implements Closeable {

    private String accessKey;
    private String secretKey;
    private String queueUrl;
    private int maxMessageSize = 256;

    private AmazonSQSAsyncClient sqs;

    public final void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public final void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public final void setQueueUrl(String queueUrl) {
        this.queueUrl = queueUrl;
    }

    public final void setMaxMessageSize(int maxMessageSize) {
        this.maxMessageSize = maxMessageSize;
    }

    @Override
    public void start() {
        if (queueUrl == null) {
            addError(format("Queue url not set for appender '%s'", getName()));
            return;
        }
        lock.lock();
        try {
            close();
            sqsInit();
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
            if (sqs != null) {
                shutdownExecutor(sqs.getExecutorService());
                sqs.shutdown();
                sqs = null;
            }
        } finally {
            lock.unlock();
        }
    }

    protected void sqsInit() {
        AmazonSQSAsyncClient sqs = new AmazonSQSAsyncClient(
                getCredentials(),
                getContext().getExecutorService()
        );
        sqs.setEndpoint(getEndpoint());
        this.sqs = sqs;
    }
    
    protected AWSCredentialsProvider getCredentials() {
        return new AWSCredentialsProviderChain(
                new EnvironmentVariableCredentialsProvider(),
                new SystemPropertiesCredentialsProvider(),
                new StaticCredentialsProvider(new AppenderCredentials()),
                new ProfileCredentialsProvider(),
                new InstanceProfileCredentialsProvider()
        );
    }
    
    protected String getEndpoint() {
        try {
            return new URI(queueUrl).getHost(); 
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException("Malformed queue url", ex);
        }
    }

    @Override
    protected String convert(byte[] payload) {
        String converted = super.convert(payload);
        if (converted.getBytes().length > (maxMessageSize * 1024)) {
            addWarn(format("Logging event '%s' exceeds the maximum size of %dkB", converted, maxMessageSize));
            return null;
        } else {
            return converted;
        }
    }

    @Override
    protected void handle(final String event) throws Exception {
        SendMessageRequest request = new SendMessageRequest(queueUrl, event);
        sqs.sendMessageAsync(request, new AsyncHandler<SendMessageRequest, SendMessageResult>() {
            @Override
            public void onError(Exception exception) {
                addWarn(format("Appender '%s' failed to send logging event '%s' to SQS", getName(), event), exception);
            }

            @Override
            public void onSuccess(SendMessageRequest request, SendMessageResult result) {
                // noop
            }
        });
    }

    private void shutdownExecutor(ExecutorService executor) {
        try {
            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            addWarn(format("SQS executor shutdown interrupted for appender '%s'", getName()), ex);
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
