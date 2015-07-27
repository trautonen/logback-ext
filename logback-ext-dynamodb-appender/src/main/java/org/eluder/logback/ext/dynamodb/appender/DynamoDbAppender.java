package org.eluder.logback.ext.dynamodb.appender;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.amazonaws.handlers.AsyncHandler;
import com.amazonaws.regions.RegionUtils;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsyncClient;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.PrimaryKey;
import com.amazonaws.services.dynamodbv2.document.internal.InternalUtils;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemResult;
import org.eluder.logback.ext.aws.core.AbstractAwsEncodingStringAppender;
import org.eluder.logback.ext.aws.core.AppenderExecutors;
import org.eluder.logback.ext.jackson.JacksonEncoder;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;

public class DynamoDbAppender extends AbstractAwsEncodingStringAppender<ILoggingEvent> {

    private static final String TIMESTAMP_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

    private String region;
    private String table;
    private String primaryKey = "Id";
    private int threadPoolSize = AppenderExecutors.DEFAULT_THREAD_POOL_SIZE;
    private int maxFlushTime = AppenderExecutors.DEFAULT_MAX_FLUSH_TIME;

    private AmazonDynamoDBAsyncClient dynamoDb;

    public void setRegion(String region) {
        this.region = region;
    }

    public void setTable(String table) {
        this.table = table;
    }

    public void setPrimaryKey(String primaryKey) {
        this.primaryKey = primaryKey;
    }

    public void setThreadPoolSize(int threadPoolSize) {
        this.threadPoolSize = threadPoolSize;
    }

    public void setMaxFlushTime(int maxFlushTime) {
        this.maxFlushTime = maxFlushTime;
    }

    @Override
    public void start() {
        if (getEncoder() == null) {
            JacksonEncoder encoder = new JacksonEncoder();
            encoder.setFieldNames(new CapitalizingFieldNames());
            encoder.setTimeStampFormat(TIMESTAMP_FORMAT);
            setEncoder(encoder);
        }
        super.start();
    }

    @Override
    protected void doStart() {

        dynamoDb = new AmazonDynamoDBAsyncClient(
                getCredentials(),
                getClientConfiguration(),
                AppenderExecutors.newExecutor(this, threadPoolSize)
        );
        dynamoDb.setRegion(RegionUtils.getRegion(region));
    }

    @Override
    protected void doStop() {
        if (dynamoDb != null) {
            AppenderExecutors.shutdown(this, dynamoDb.getExecutorService(), maxFlushTime);
            dynamoDb.shutdown();
            dynamoDb = null;
        }
    }

    @Override
    protected void handle(final ILoggingEvent event, final String encoded) throws Exception {
        Item item = Item.fromJSON(encoded).withPrimaryKey(createEventId(event));
        Map<String, AttributeValue> attributes = InternalUtils.toAttributeValues(item);
        PutItemRequest request = new PutItemRequest(table, attributes);
        final CountDownLatch latch = new CountDownLatch(1);
        dynamoDb.putItemAsync(request, new AsyncHandler<PutItemRequest, PutItemResult>() {
            @Override
            public void onError(Exception exception) {
                addWarn(format("Appender '%s' failed to send logging event '%s' to DynamoDB table '%s'", getName(), event, table), exception);
                latch.countDown();
            }

            @Override
            public void onSuccess(PutItemRequest request, PutItemResult putItemResult) {
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

    protected PrimaryKey createEventId(ILoggingEvent event) {
        return new PrimaryKey(primaryKey, UUID.randomUUID().toString());
    }

}
