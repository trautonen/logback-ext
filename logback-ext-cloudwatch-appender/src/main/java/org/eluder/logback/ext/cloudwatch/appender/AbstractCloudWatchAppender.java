package org.eluder.logback.ext.cloudwatch.appender;

import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.DeferredProcessingAware;
import com.amazonaws.regions.RegionUtils;
import com.amazonaws.services.logs.AWSLogsClient;
import com.amazonaws.services.logs.model.*;
import com.amazonaws.util.StringUtils;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.collect.Queues;
import org.eluder.logback.ext.aws.core.AbstractAwsEncodingStringAppender;
import org.eluder.logback.ext.aws.core.AwsSupport;
import org.eluder.logback.ext.core.CommonEventAttributes;
import org.eluder.logback.ext.core.StringPayloadConverter;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;

public abstract class AbstractCloudWatchAppender<E extends DeferredProcessingAware> extends AbstractAwsEncodingStringAppender<E, String> {

    private static final int DEFAULT_MAX_BATCH_SIZE = 512;
    private static final int DEFAULT_MAX_BATCH_TIME = 1000;
    private static final int DEFAULT_INTERNAL_QUEUE_SIZE = 8192;
    private static final boolean DEFAULT_SKIP_CREATE = false;

    private String region;
    private String logGroup;
    private String logStream;
    private int maxBatchSize = DEFAULT_MAX_BATCH_SIZE;
    private long maxBatchTime = DEFAULT_MAX_BATCH_TIME;
    private int internalQueueSize = DEFAULT_INTERNAL_QUEUE_SIZE;
    private boolean skipCreate = DEFAULT_SKIP_CREATE;

    private AWSLogsClient logs;

    private LinkedBlockingQueue<InputLogEvent> queue;
    private Worker<E> worker;

    public AbstractCloudWatchAppender() {
        super();
    }

    protected AbstractCloudWatchAppender(AwsSupport awsSupport, Filter<E> sdkLoggingFilter) {
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

    public final void setMaxBatchSize(int maxBatchSize) {
        this.maxBatchSize = maxBatchSize;
    }

    public final void setMaxBatchTime(long maxBatchTime) {
        this.maxBatchTime = maxBatchTime;
    }

    public final void setInternalQueueSize(int internalQueueSize) {
        this.internalQueueSize = internalQueueSize;
    }

    public final void setSkipCreate(boolean skipCreate) {
        this.skipCreate = skipCreate;
    }

    @Override
    public void start() {
        if (RegionUtils.getRegion(region) == null) {
            addError(format("Region not set or invalid for appender '%s'", getName()));
            return;
        }
        if (StringUtils.isNullOrEmpty(logGroup)) {
            addError(format("Log group name not set for appender '%s'", getName()));
            return;
        }
        if (StringUtils.isNullOrEmpty(logStream)) {
            addError(format("Log stream name not set for appender '%s'", getName()));
            return;
        }
        setConverter(new StringPayloadConverter(getCharset(), isBinary()));
        super.start();
    }

    @Override
    protected void doStart() {
        logs = new AWSLogsClient(
                getCredentials(),
                getClientConfiguration()
        );
        logs.setRegion(RegionUtils.getRegion(region));
        if (!skipCreate) {
            if (!logGroupExists(logGroup)) {
                createLogGroup(logGroup);
            }
            if (!logStreamExists(logGroup, logStream)) {
                createLogStream(logGroup, logStream);
            }
        }
        queue = new LinkedBlockingQueue<>(internalQueueSize);
        worker = new Worker<>(this);
        worker.setName(format("%s-worker", getName()));
        worker.setDaemon(true);
        worker.start();
    }

    @Override
    protected void doStop() {
        if (worker != null) {
            worker.stopGracefully();
            try {
                worker.join(getMaxFlushTime());
                if (worker.isAlive()) {
                    addWarn(format("Max queue flush timeout (%d ms) exceeded, approximately %d queued events were possibly discarded",
                                   getMaxFlushTime(), queue.size()));
                }
            } catch (InterruptedException ex) {
                addError(format("Stopping was interrupted, approximately %d queued events may be discarded",
                                queue.size()), ex);
            }
            worker = null;
        }
        if (queue != null) {
            queue.clear();
            queue = null;
        }
        if (logs != null) {
            logs.shutdown();
            logs = null;
        }
    }

    @Override
    protected void handle(final E event, final String encoded) throws Exception {
        CommonEventAttributes attributes = applyCommonEventAttributes(event);
        InputLogEvent ile = new InputLogEvent().withTimestamp(attributes.getTimeStamp()).withMessage(encoded);
        if (!queue.offer(ile, getMaxFlushTime(), TimeUnit.MILLISECONDS)) {
            addWarn(format("No space available in internal queue after %d ms waiting, logging event was discarded",
                           getMaxFlushTime()));
        }
    }

    protected abstract CommonEventAttributes applyCommonEventAttributes(final E event);

    protected boolean logGroupExists(String logGroup) {
        DescribeLogGroupsRequest request = new DescribeLogGroupsRequest().withLogGroupNamePrefix(logGroup);
        DescribeLogGroupsResult result = logs.describeLogGroups(request);
        for (LogGroup group : result.getLogGroups()) {
            if (logGroup.equals(group.getLogGroupName())) {
                return true;
            }
        }
        return false;
    }

    protected void createLogGroup(String logGroup) {
        CreateLogGroupRequest request = new CreateLogGroupRequest(logGroup);
        logs.createLogGroup(request);
        addInfo(format("Successfully created log group '%s'", logGroup));
    }

    protected boolean logStreamExists(String logGroup, String logStream) {
        DescribeLogStreamsRequest request = new DescribeLogStreamsRequest().withLogGroupName(logGroup).withLogStreamNamePrefix(logStream);
        DescribeLogStreamsResult result = logs.describeLogStreams(request);
        for (LogStream stream : result.getLogStreams()) {
            if (logStream.equals(stream.getLogStreamName())) {
                return true;
            }
        }
        return false;
    }

    protected void createLogStream(String logGroup, String logStream) {
        CreateLogStreamRequest request = new CreateLogStreamRequest(logGroup, logStream);
        logs.createLogStream(request);
        addInfo(format("Successfully created log stream '%s' for group '%s'", logStream, logGroup));
    }

    private static class Worker<P extends DeferredProcessingAware> extends Thread {

        private static final Ordering<InputLogEvent> ORDERING = new Ordering<InputLogEvent>() {
            @Override
            public int compare(InputLogEvent left, InputLogEvent right) {
                if (left.getTimestamp() < right.getTimestamp()) {
                    return -1;
                } else if (left.getTimestamp() > right.getTimestamp()) {
                    return 1;
                } else {
                    return 0;
                }
            }
        };

        private final AbstractCloudWatchAppender<P> parent;

        private String token = null;
        private volatile boolean started = false;

        Worker(AbstractCloudWatchAppender<P> parent) {
            this.parent = parent;
        }

        @Override
        public void run() {
            started = true;
            while (started) {
                List<InputLogEvent> events = new LinkedList<>();
                try {
                    Queues.drain(parent.queue, events, parent.maxBatchSize, parent.maxBatchTime, TimeUnit.MILLISECONDS);
                    handle(events);
                } catch (InterruptedException ex) {
                    handle(events);
                }
            }

            List<InputLogEvent> remaining = new LinkedList<>();
            parent.queue.drainTo(remaining);
            for (List<InputLogEvent> batch : Lists.partition(remaining, parent.maxBatchSize)) {
                handle(batch);
            }

        }

        public void stopGracefully() {
            started = false;
        }

        private void handle(List<InputLogEvent> events) {
            if (!events.isEmpty()) {
                List<InputLogEvent> sorted = ORDERING.immutableSortedCopy(events);
                PutLogEventsRequest request = new PutLogEventsRequest(parent.logGroup, parent.logStream, sorted);
                try {
                    try {
                        putEvents(request);
                    } catch (DataAlreadyAcceptedException | InvalidSequenceTokenException ex) {
                        putEvents(request);
                    }
                } catch (Exception ex) {
                    parent.addError(format("Failed to handle %d events", events.size()), ex);
                }
            }
        }

        private void putEvents(PutLogEventsRequest request) {
            try {
                PutLogEventsResult result = parent.logs.putLogEvents(request.withSequenceToken(token));
                token = result.getNextSequenceToken();
            } catch (DataAlreadyAcceptedException ex) {
                token = ex.getExpectedSequenceToken();
                throw ex;
            } catch (InvalidSequenceTokenException ex) {
                token = ex.getExpectedSequenceToken();
                throw ex;
            }
        }
    }
}
