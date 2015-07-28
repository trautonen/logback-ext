package org.eluder.logback.ext.aws.core;

import ch.qos.logback.core.spi.ContextAware;
import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.handlers.AsyncHandler;

import java.util.concurrent.CountDownLatch;

public class LoggingEventHandler<REQUEST extends AmazonWebServiceRequest, RESULT> implements AsyncHandler<REQUEST, RESULT> {

    private final ContextAware contextAware;
    private final CountDownLatch latch;
    private final String errorMessage;

    public LoggingEventHandler(ContextAware contextAware, CountDownLatch latch, String errorMessage) {
        this.contextAware = contextAware;
        this.latch = latch;
        this.errorMessage = errorMessage;
    }

    @Override
    public void onError(Exception exception) {
        contextAware.addWarn(errorMessage, exception);
        latch.countDown();
    }

    @Override
    public void onSuccess(REQUEST request, RESULT result) {
        latch.countDown();
    }
}
