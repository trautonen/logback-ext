package org.eluder.logback.ext.sqs.appender;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.amazonaws.handlers.AsyncHandler;
import com.amazonaws.services.sqs.AmazonSQSAsyncClient;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageResult;
import org.eluder.logback.ext.aws.core.AbstractAwsEncodingStringAppender;
import org.eluder.logback.ext.aws.core.AppenderExecutors;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;

public class SqsAppender extends AbstractAwsEncodingStringAppender<ILoggingEvent> {

    private String queueUrl;
    private int threadPoolSize = AppenderExecutors.DEFAULT_THREAD_POOL_SIZE;
    private int maxFlushTime = AppenderExecutors.DEFAULT_MAX_FLUSH_TIME;

    private AmazonSQSAsyncClient sqs;

    public final void setQueueUrl(String queueUrl) {
        this.queueUrl = queueUrl;
    }

    public final void setThreadPoolSize(int threadPoolSize) {
        this.threadPoolSize = threadPoolSize;
    }

    public final void setMaxFlushTime(int maxFlushTime) {
        this.maxFlushTime = maxFlushTime;
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
                Executors.newFixedThreadPool(threadPoolSize)
        );
        sqs.setEndpoint(getEndpoint());
    }

    @Override
    protected void doStop() {
        if (sqs != null) {
            AppenderExecutors.shutdown(this, sqs.getExecutorService(), maxFlushTime);
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
        final CountDownLatch latch = new CountDownLatch(1);
        sqs.sendMessageAsync(request, new AsyncHandler<SendMessageRequest, SendMessageResult>() {
            @Override
            public void onError(Exception exception) {
                addWarn(format("Appender '%s' failed to send logging event '%s' to SQS queue '%s'", getName(), event, queueUrl), exception);
                latch.countDown();
            }

            @Override
            public void onSuccess(SendMessageRequest request, SendMessageResult result) {
                latch.countDown();
            }
        });
        try {
            latch.await(maxFlushTime, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            addWarn(format("Appender '%s' was interrupted, a logging message might have been lost or shutdown was initiated", getName()));
            Thread.currentThread().interrupt();
        }
    }

}
