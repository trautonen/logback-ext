package org.eluder.logback.ext.sqs.appender;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.amazonaws.services.sqs.AmazonSQSAsyncClient;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageResult;
import org.eluder.logback.ext.aws.core.AbstractAwsEncodingStringAppender;
import org.eluder.logback.ext.aws.core.LoggingEventHandler;
import org.eluder.logback.ext.core.AppenderExecutors;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import static java.lang.String.format;

public class SqsAppender extends AbstractAwsEncodingStringAppender<ILoggingEvent> {

    private String queueUrl;

    private AmazonSQSAsyncClient sqs;

    public final void setQueueUrl(String queueUrl) {
        this.queueUrl = queueUrl;
    }

    @Override
    public void start() {
        if (queueUrl == null) {
            addError(format("Queue url not set for appender '%s'", getName()));
            return;
        }
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
    protected void handle(final ILoggingEvent event, final String encoded) throws Exception {
        SendMessageRequest request = new SendMessageRequest(queueUrl, encoded);
        String errorMessage = format("Appender '%s' failed to send logging event '%s' to SQS queue '%s'", getName(), event, queueUrl);
        CountDownLatch latch = new CountDownLatch(isAsyncParent() ? 0 : 1);
        sqs.sendMessageAsync(request, new LoggingEventHandler<SendMessageRequest, SendMessageResult>(this, latch, errorMessage));
        AppenderExecutors.awaitLatch(this, latch, getMaxFlushTime());
    }

}
