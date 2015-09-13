package org.eluder.logback.ext.kinesis.appender;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.amazonaws.regions.RegionUtils;
import com.amazonaws.services.kinesis.AmazonKinesisAsyncClient;
import com.amazonaws.services.kinesis.model.PutRecordRequest;
import com.amazonaws.services.kinesis.model.PutRecordResult;
import org.eluder.logback.ext.aws.core.AbstractAwsEncodingStringAppender;
import org.eluder.logback.ext.aws.core.LoggingEventHandler;
import org.eluder.logback.ext.core.AppenderExecutors;

import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

public class KinesisAppender extends AbstractAwsEncodingStringAppender {

    private String region;
    private String stream;

    private AmazonKinesisAsyncClient kinesis;

    public final void setRegion(String region) {
        this.region = region;
    }

    public final void setStream(String stream) {
        this.stream = stream;
    }

    @Override
    protected void doStart() {
        kinesis = new AmazonKinesisAsyncClient(
                getCredentials(),
                getClientConfiguration(),
                AppenderExecutors.newExecutor(this, getThreadPoolSize())
        );
        kinesis.setRegion(RegionUtils.getRegion(region));
    }

    @Override
    protected void doStop() {
        if (kinesis != null) {
            AppenderExecutors.shutdown(this, kinesis.getExecutorService(), getMaxFlushTime());
            kinesis.shutdown();
            kinesis = null;
        }
    }

    @Override
    protected void handle(ILoggingEvent event, String encoded) throws Exception {
        ByteBuffer buffer = ByteBuffer.wrap(encoded.getBytes(getCharset()));
        PutRecordRequest request = new PutRecordRequest()
                .withPartitionKey(getPartitionKey(event, encoded))
                .withStreamName(stream)
                .withData(buffer);
        CountDownLatch latch = new CountDownLatch(isAsyncParent() ? 0 : 1);
        kinesis.putRecordAsync(request, new LoggingEventHandler<PutRecordRequest, PutRecordResult>(this, latch, "foo"));
        AppenderExecutors.awaitLatch(this, latch, getMaxFlushTime());
    }

    protected String getPartitionKey(ILoggingEvent event, String encoded) {
        return UUID.randomUUID().toString();
    }
}
