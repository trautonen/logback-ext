package org.eluder.logback.ext.sns.appender;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.amazonaws.handlers.AsyncHandler;
import com.amazonaws.regions.RegionUtils;
import com.amazonaws.services.sns.AmazonSNSAsyncClient;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;
import org.eluder.logback.ext.aws.core.AbstractAwsEncodingStringAppender;
import org.eluder.logback.ext.core.ContextAwareExecutorService;

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
    protected void doInit() {
        sns = new AmazonSNSAsyncClient(getCredentials(), new ContextAwareExecutorService(this)) {
            @Override
            public void shutdown() {
                // we don't want to shutdown the logback managed executorservice
                client.shutdown();
            }
        };
        sns.setRegion(RegionUtils.getRegion(region));
    }

    @Override
    protected void doClose() {
        if (sns != null) {
            sns.shutdown();
            sns = null;
        }
    }

    @Override
    protected void handle(final String event) throws Exception {
        PublishRequest request = new PublishRequest(topic, event, subject);
        sns.publishAsync(request, new AsyncHandler<PublishRequest, PublishResult>() {
            @Override
            public void onError(Exception exception) {
                addWarn(format("Appender '%s' failed to send logging event '%s' to SNS topic '%s'", getName(), event, topic), exception);
            }

            @Override
            public void onSuccess(PublishRequest request, PublishResult publishResult) {
                // noop
            }
        });
    }
}
