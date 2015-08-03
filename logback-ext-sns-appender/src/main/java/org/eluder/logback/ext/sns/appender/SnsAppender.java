package org.eluder.logback.ext.sns.appender;

/*
 * #[license]
 * logback-ext-sns-appender
 * %%
 * Copyright (C) 2014 - 2015 Tapio Rautonen
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * %[license]
 */

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import com.amazonaws.regions.RegionUtils;
import com.amazonaws.services.sns.AmazonSNSAsyncClient;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;
import org.eluder.logback.ext.aws.core.AbstractAwsEncodingStringAppender;
import org.eluder.logback.ext.aws.core.AwsSupport;
import org.eluder.logback.ext.core.AppenderExecutors;
import org.eluder.logback.ext.aws.core.LoggingEventHandler;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import static java.lang.String.format;

public class SnsAppender extends AbstractAwsEncodingStringAppender {

    private String region;
    private String topic;
    private String subject;

    private AmazonSNSAsyncClient sns;

    public SnsAppender() {
        super();
    }

    protected SnsAppender(AwsSupport awsSupport, Filter<ILoggingEvent> sdkLoggingFilter) {
        super(awsSupport, sdkLoggingFilter);
    }

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
