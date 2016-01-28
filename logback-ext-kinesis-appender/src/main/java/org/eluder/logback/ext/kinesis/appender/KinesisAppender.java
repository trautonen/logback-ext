package org.eluder.logback.ext.kinesis.appender;

import static java.lang.String.format;

import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import org.eluder.logback.ext.aws.core.AbstractAwsEncodingStringAppender;
import org.eluder.logback.ext.aws.core.LoggingEventHandler;
import org.eluder.logback.ext.core.AppenderExecutors;
import org.eluder.logback.ext.core.ByteArrayPayloadConverter;

import com.amazonaws.regions.RegionUtils;
import com.amazonaws.services.kinesis.AmazonKinesisAsyncClient;
import com.amazonaws.services.kinesis.model.PutRecordRequest;
import com.amazonaws.services.kinesis.model.PutRecordResult;
import com.amazonaws.util.StringUtils;

public class KinesisAppender<E> extends AbstractAwsEncodingStringAppender<E, byte[]> {

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
    public void start() {
        if (RegionUtils.getRegion(region) == null) {
            addError(format("Region not set or invalid for appender '%s'", getName()));
            return;
        }
        if (StringUtils.isNullOrEmpty(stream)) {
            addError(format("Stream not set for appender '%s", getName()));
            return;
        }
        setConverter(new ByteArrayPayloadConverter());
        super.start();
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
    protected void handle(E event, byte[] encoded) throws Exception {
        ByteBuffer buffer = ByteBuffer.wrap(encoded);
        PutRecordRequest request = new PutRecordRequest()
                .withPartitionKey(getPartitionKey(event))
                .withStreamName(stream)
                .withData(buffer);
        String errorMessage = format("Appender '%s' failed to send logging event '%s' to Kinesis stream '%s'", getName(), event, stream);
        CountDownLatch latch = new CountDownLatch(isAsyncParent() ? 0 : 1);
        kinesis.putRecordAsync(request, new LoggingEventHandler<PutRecordRequest, PutRecordResult>(this, latch, errorMessage));
        AppenderExecutors.awaitLatch(this, latch, getMaxFlushTime());
    }

    protected String getPartitionKey(E event) {
        return UUID.randomUUID().toString();
    }
}
