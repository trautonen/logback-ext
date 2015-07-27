package org.eluder.logback.ext.sns.appender;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.amazonaws.handlers.AsyncHandler;
import com.amazonaws.regions.RegionUtils;
import com.amazonaws.services.sns.AmazonSNSAsyncClient;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;
import org.eluder.logback.ext.aws.core.AbstractAwsEncodingStringAppender;
import org.eluder.logback.ext.aws.core.AppenderExecutors;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;

public class SnsAppender extends AbstractAwsEncodingStringAppender<ILoggingEvent> {

    private String region;
    private String topic;
    private String subject;
    private int threadPoolSize = AppenderExecutors.DEFAULT_THREAD_POOL_SIZE;
    private int maxFlushTime = AppenderExecutors.DEFAULT_MAX_FLUSH_TIME;

    private AmazonSNSAsyncClient sns;

    public void setRegion(String region) {
        this.region = region;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public final void setThreadPoolSize(int threadPoolSize) {
        this.threadPoolSize = threadPoolSize;
    }

    public final void setMaxFlushTime(int maxFlushTime) {
        this.maxFlushTime = maxFlushTime;
    }

    @Override
    protected void doStart() {
        sns = new AmazonSNSAsyncClient(
                getCredentials(),
                getClientConfiguration(),
                Executors.newFixedThreadPool(threadPoolSize)
        );
        sns.setRegion(RegionUtils.getRegion(region));
    }

    @Override
    protected void doStop() {
        if (sns != null) {
            AppenderExecutors.shutdown(this, sns.getExecutorService(), maxFlushTime);
            sns.shutdown();
            sns = null;
        }
    }

    @Override
    protected void handle(final ILoggingEvent event, final String encoded) throws Exception {
        PublishRequest request = new PublishRequest(topic, encoded, subject);
        final CountDownLatch latch = new CountDownLatch(1);
        sns.publishAsync(request, new AsyncHandler<PublishRequest, PublishResult>() {
            @Override
            public void onError(Exception exception) {
                addWarn(format("Appender '%s' failed to send logging event '%s' to SNS topic '%s'", getName(), event, topic), exception);
                latch.countDown();
            }

            @Override
            public void onSuccess(PublishRequest request, PublishResult publishResult) {
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
