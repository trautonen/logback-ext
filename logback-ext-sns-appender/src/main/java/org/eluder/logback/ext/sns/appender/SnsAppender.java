package org.eluder.logback.ext.sns.appender;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.amazonaws.regions.RegionUtils;
import com.amazonaws.services.sns.AmazonSNSAsyncClient;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;
import org.eluder.logback.ext.aws.core.AbstractAwsEncodingStringAppender;
import org.eluder.logback.ext.core.AppenderExecutors;
import org.eluder.logback.ext.aws.core.LoggingEventHandler;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import static java.lang.String.format;

public class SnsAppender extends AbstractAwsEncodingStringAppender<ILoggingEvent> {

    private String region;
    private String topic;
    private String subject;

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

    @Override
    protected void doStart() {
        sns = new AmazonSNSAsyncClient(
                getCredentials(),
                getClientConfiguration(),
                Executors.newFixedThreadPool(getThreadPoolSize())
        );
        sns.setRegion(RegionUtils.getRegion(region));
    }

    @Override
    protected void doStop() {
        if (sns != null) {
            AppenderExecutors.shutdown(this, sns.getExecutorService(), getMaxFlushTime());
            sns.shutdown();
            sns = null;
        }
    }

    @Override
    protected void handle(final ILoggingEvent event, final String encoded) throws Exception {
        PublishRequest request = new PublishRequest(topic, encoded, subject);
        String errorMessage = format("Appender '%s' failed to send logging event '%s' to SNS topic '%s'", getName(), event, topic);
        CountDownLatch latch = new CountDownLatch(isAsyncParent() ? 0 : 1);
        sns.publishAsync(request, new LoggingEventHandler<PublishRequest, PublishResult>(this, latch, errorMessage));
        AppenderExecutors.awaitLatch(this, latch, getMaxFlushTime());
    }
}
