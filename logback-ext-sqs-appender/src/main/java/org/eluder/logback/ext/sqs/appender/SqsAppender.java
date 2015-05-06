package org.eluder.logback.ext.sqs.appender;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.amazonaws.handlers.AsyncHandler;
import com.amazonaws.services.sqs.AmazonSQSAsyncClient;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageResult;
import org.eluder.logback.ext.aws.core.AbstractAwsEncodingStringAppender;

import java.net.URI;
import java.net.URISyntaxException;

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
    protected void doInit() {
        sqs = new AmazonSQSAsyncClient(getCredentials(), getContext().getExecutorService());
        sqs.setEndpoint(getEndpoint());
    }

    @Override
    protected void doClose() {
        if (sqs != null) {
            shutdownExecutor(sqs.getExecutorService());
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
    protected void handle(final String event) throws Exception {
        SendMessageRequest request = new SendMessageRequest(queueUrl, event);
        sqs.sendMessageAsync(request, new AsyncHandler<SendMessageRequest, SendMessageResult>() {
            @Override
            public void onError(Exception exception) {
                addWarn(format("Appender '%s' failed to send logging event '%s' to SQS queue '%s'", getName(), event, queueUrl), exception);
            }

            @Override
            public void onSuccess(SendMessageRequest request, SendMessageResult result) {
                // noop
            }
        });
    }

}
