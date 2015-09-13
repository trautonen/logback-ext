package org.eluder.logback.ext.cloudwatch.appender;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import com.amazonaws.regions.RegionUtils;
import com.amazonaws.services.logs.AWSLogsAsyncClient;
import com.amazonaws.services.logs.model.DataAlreadyAcceptedException;
import com.amazonaws.services.logs.model.InputLogEvent;
import com.amazonaws.services.logs.model.InvalidSequenceTokenException;
import com.amazonaws.services.logs.model.PutLogEventsRequest;
import com.amazonaws.services.logs.model.PutLogEventsResult;
import com.google.common.collect.ImmutableList;
import org.eluder.logback.ext.aws.core.AbstractAwsEncodingStringAppender;
import org.eluder.logback.ext.aws.core.AwsSupport;
import org.eluder.logback.ext.aws.core.LoggingEventHandler;
import org.eluder.logback.ext.core.AppenderExecutors;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.ReentrantLock;

import static java.lang.String.format;

public class CloudWatchAppender extends AbstractAwsEncodingStringAppender {

    private final ReentrantLock tokenLock = new ReentrantLock(true);

    private String region;
    private String logGroup;
    private String logStream;

    private AWSLogsAsyncClient logs;
    private volatile String token;
    private volatile boolean requiresRehandle;

    public CloudWatchAppender() {
        super();
    }

    protected CloudWatchAppender(AwsSupport awsSupport, Filter<ILoggingEvent> sdkLoggingFilter) {
        super(awsSupport, sdkLoggingFilter);
    }

    public final void setRegion(String region) {
        this.region = region;
    }

    public final void setLogGroup(String logGroup) {
        this.logGroup = logGroup;
    }

    public final void setLogStream(String logStream) {
        this.logStream = logStream;
    }

    @Override
    protected void doStart() {
        logs = new AWSLogsAsyncClient(
                getCredentials(),
                getClientConfiguration(),
                AppenderExecutors.newExecutor(this, getThreadPoolSize())
        );
        logs.setRegion(RegionUtils.getRegion(region));
    }

    @Override
    protected void doStop() {
        if (logs != null) {
            AppenderExecutors.shutdown(this, logs.getExecutorService(), getMaxFlushTime());
            logs.shutdown();
            logs = null;
        }
    }

    @Override
    protected void handle(final ILoggingEvent event, final String encoded) throws Exception {
        tokenLock.lock();
        try {
            InputLogEvent ev = new InputLogEvent().withTimestamp(event.getTimeStamp()).withMessage(encoded);
            final PutLogEventsRequest request = new PutLogEventsRequest(logGroup, logStream, ImmutableList.of(ev)).withSequenceToken(token);
            String errorMessage = format("Appender '%s' failed to send logging event '%s' to CloudWatch logs '%s':'%s'", getName(), event, logGroup, logStream);
            CountDownLatch latch = new CountDownLatch(1);
            logs.putLogEventsAsync(request, new LoggingEventHandler<PutLogEventsRequest, PutLogEventsResult>(this, latch, errorMessage) {
                @Override
                public void onError(Exception exception) {
                    if (exception instanceof DataAlreadyAcceptedException) {
                        updateToken(((DataAlreadyAcceptedException) exception).getExpectedSequenceToken(), true);
                    } else if (exception instanceof InvalidSequenceTokenException) {
                        updateToken(((InvalidSequenceTokenException) exception).getExpectedSequenceToken(), true);
                    } else {
                        CloudWatchAppender.this.requiresRehandle = false;
                        super.onError(exception);
                    }
                }

                @Override
                public void onSuccess(PutLogEventsRequest request, PutLogEventsResult result) {
                    updateToken(result.getNextSequenceToken(), false);
                    super.onSuccess(request, result);
                }

                private void updateToken(String token, boolean requireRehandle) {
                    CloudWatchAppender.this.token = token;
                    CloudWatchAppender.this.requiresRehandle = requireRehandle;
                    latch.countDown();
                }
            });
            AppenderExecutors.awaitLatch(this, latch, getMaxFlushTime());
            if (requiresRehandle) {
                handle(event, encoded);
            }
        } finally {
            tokenLock.unlock();
        }

    }
}
