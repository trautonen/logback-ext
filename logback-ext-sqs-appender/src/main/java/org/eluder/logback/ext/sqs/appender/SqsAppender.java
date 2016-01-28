package org.eluder.logback.ext.sqs.appender;

import static java.lang.String.format;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import org.eluder.logback.ext.aws.core.AbstractAwsEncodingStringAppender;
import org.eluder.logback.ext.aws.core.AwsSupport;
import org.eluder.logback.ext.aws.core.LoggingEventHandler;
import org.eluder.logback.ext.core.AppenderExecutors;
import org.eluder.logback.ext.core.StringPayloadConverter;

import com.amazonaws.services.sqs.AmazonSQSAsyncClient;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageResult;

import ch.qos.logback.core.filter.Filter;

public class SqsAppender<E> extends AbstractAwsEncodingStringAppender<E, String> {

    private String queueUrl;

    private AmazonSQSAsyncClient sqs;

    public SqsAppender() {
        super();
    }

    protected SqsAppender(AwsSupport awsSupport, Filter<E> sdkLoggingFilter) {
        super(awsSupport, sdkLoggingFilter);
    }

    public final void setQueueUrl(String queueUrl) {
        this.queueUrl = queueUrl;
    }

    @Override
    public void start() {
        if (queueUrl == null) {
            addError(format("Queue url not set for appender '%s'", getName()));
            return;
        }
        setConverter(new StringPayloadConverter(getCharset(), isBinary()));
        super.start();
    }

    @Override
    protected void doStart() {
        sqs = new AmazonSQSAsyncClient(
                getCredentials(),
                getClientConfiguration(),
                Executors.newFixedThreadPool(getThreadPoolSize())
        );
        sqs.setEndpoint(getEndpoint());
    }

    @Override
    protected void doStop() {
        if (sqs != null) {
            AppenderExecutors.shutdown(this, sqs.getExecutorService(), getMaxFlushTime());
            sqs.shutdown();
            sqs = null;
        }
    }
    
    protected String getEndpoint() {
        try {
            return new URI(queueUrl).getHost(); 
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException("Malformed queue url", ex);
        }
    }

    @Override
    protected void handle(final E event, final String encoded) throws Exception {
        SendMessageRequest request = new SendMessageRequest(queueUrl, encoded);
        String errorMessage = format("Appender '%s' failed to send logging event '%s' to SQS queue '%s'", getName(), event, queueUrl);
        CountDownLatch latch = new CountDownLatch(isAsyncParent() ? 0 : 1);
        sqs.sendMessageAsync(request, new LoggingEventHandler<SendMessageRequest, SendMessageResult>(this, latch, errorMessage));
        AppenderExecutors.awaitLatch(this, latch, getMaxFlushTime());
    }

}
