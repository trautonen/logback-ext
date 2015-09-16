package org.eluder.logback.ext.cloudwatch.appender;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import com.amazonaws.regions.RegionUtils;
import com.amazonaws.services.logs.AWSLogsClient;
import com.amazonaws.services.logs.model.DataAlreadyAcceptedException;
import com.amazonaws.services.logs.model.InputLogEvent;
import com.amazonaws.services.logs.model.InvalidSequenceTokenException;
import com.amazonaws.services.logs.model.PutLogEventsRequest;
import com.amazonaws.services.logs.model.PutLogEventsResult;
import com.amazonaws.util.StringUtils;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import org.eluder.logback.ext.aws.core.AbstractAwsEncodingStringAppender;
import org.eluder.logback.ext.aws.core.AwsSupport;
import org.eluder.logback.ext.core.StringPayloadConverter;

import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;

public class CloudWatchAppender extends AbstractAwsEncodingStringAppender<String> {

    private static final int DEFAULT_MAX_BATCH_SIZE = 500;
    private static final int DEFAULT_MAX_BATCH_TIME = 1000;

    private String region;
    private String logGroup;
    private String logStream;
    private int maxBatchSize = DEFAULT_MAX_BATCH_SIZE;
    private long maxBatchTime = DEFAULT_MAX_BATCH_TIME;

    private AWSLogsClient logs;

    private LinkedBlockingQueue<InputLogEvent> queue;
    private Worker worker;

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

    public final void setMaxBatchSize(int maxBatchSize) {
        this.maxBatchSize = maxBatchSize;
    }

    public final void setMaxBatchTime(long maxBatchTime) {
        this.maxBatchTime = maxBatchTime;
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
        queue = new LinkedBlockingQueue<InputLogEvent>();
        worker = new Worker(this);
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
    protected void handle(final ILoggingEvent event, final String encoded) throws Exception {
        InputLogEvent ile = new InputLogEvent().withTimestamp(event.getTimeStamp()).withMessage(encoded);
        if (!queue.offer(ile)) {
            addWarn("Logging event discarded because queue was full");
        }
    }

    private static class Worker extends Thread {

        private static final Comparator<InputLogEvent> COMPARATOR = new Comparator<InputLogEvent>() {
            @Override
            public int compare(InputLogEvent o1, InputLogEvent o2) {
                if (o1.getTimestamp() < o2.getTimestamp()) {
                    return -1;
                } else if (o1.getTimestamp() > o2.getTimestamp()) {
                    return 1;
                } else {
                    return 0;
                }
            }
        };

        private final CloudWatchAppender parent;

        private String token = null;
        private volatile boolean started = false;

        public Worker(CloudWatchAppender parent) {
            this.parent = parent;
        }

        @Override
        public void run() {
            started = true;
            while (started) {
                SortedSet<InputLogEvent> events = new TreeSet<InputLogEvent>(COMPARATOR);
                try {
                    Queues.drain(parent.queue, events, parent.maxBatchSize, parent.maxBatchTime, TimeUnit.MILLISECONDS);
                    handle(ImmutableList.copyOf(events));
                } catch (InterruptedException ex) {
                    handle(ImmutableList.copyOf(events));
                }
            }

            SortedSet<InputLogEvent> remaining = new TreeSet<InputLogEvent>(COMPARATOR);
            parent.queue.drainTo(remaining);
            for (List<InputLogEvent> batch : Lists.partition(ImmutableList.copyOf(remaining), parent.maxBatchSize)) {
                handle(batch);
            }

        }

        public void stopGracefully() {
            started = false;
        }

        private void handle(List<InputLogEvent> events) {
            if (!events.isEmpty()) {
                PutLogEventsRequest request = new PutLogEventsRequest(parent.logGroup, parent.logStream, events);
                try {
                    try {
                        putEvents(request);
                    } catch (DataAlreadyAcceptedException ex) {
                        putEvents(request);
                    } catch (InvalidSequenceTokenException ex) {
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
