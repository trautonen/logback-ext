## CloudWatch Appender

Appender that submits log events to CloudWatch Logs service. By default all appenders work in
synchronous manner, except this. CloudWatch Logs is intended to accept batches of log events and
due to the requirement of sequence token for batches this appender uses an internal queue similar
to `ch.qos.logback.classic.AsyncAppender` and batches submits to CloudWatch logs according to
appender configuration.


### Maven

```xml
<dependency>
    <groupId>org.eluder.logback</groupId>
    <artifactId>logback-ext-cloudwatch-appender</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```


### Configuration

Create the following appender configuration in `logback.xml` and replace the required properties
`region`, `logGroup` and `logStream` with desired values and set an `encoder` that serializes
the logging events.

By default the appender automatically creates the log group and stream. To restrict IAM policy
actions more, use `skipCreate` property as `true` and create the group and stream beforehand.

```xml
<appender name="CLOUDWATCH" class="org.eluder.logback.ext.cloudwatch.appender.CloudWatchAppender">
    <region>eu-west-1</region>
    <logGroup>logzgroup</logGroup>
    <logStream>logzstream</logStream>
    <encoder class="org.eluder.logback.ext.jackson.JacksonEncoder">
        <timeStampFormat>yyyy-MM-dd'T'HH:mm:ss.SSS</timeStampFormat>
    </encoder>
</appender>
```

Complete list of the appender properties.

| Property | Type | Description |
| -------- | ---- | ----------- |
| `region` | *string* | AWS region. |
| `logGroup` | *string* | Log group name. |
| `logStream` | *string* | Log stream name. |
| `maxBatchSize` | *integer* | **Default: 512**<br>Maximum number of log events in single submit batch. |
| `maxBatchTime` | *ingeger* | **Default: 1000**<br>Maximum time in milliseconds to collect log events to submit batch. |
| `internalQueueSize` | *integer* | **Default: 8192**<br>Size of the internal log event queue. |
| `skipCreate` | *boolean* | **Default: false**<br>Skip queue and stream creationg. Requires less IAM policy actions. |
| `charset` | *charset* | **Default: UTF-8**<br>Charset for the log event encoder. |
| `binary` | *boolean* | **Default: false**<br>Encoded data is binary and must be Base64 encoded. |
| `encoder` | *encoder* | Logback encoder to serialize the logging events. |
| `accessKey` | *string* | IAM access key. |
| `secretKey` | *string* | IAM secret key. |
| `maxPayloadSize` | *integer* | **Default: 256**<br>Maximum log event payload size in kilobytes. |
| `maxFlushTime` | *integer* | **Default: 3000**<br>Maximum wait time in milliseconds to wait if internal queue is full and time to wait for the remaining queue to flush events on appender stop. |


### Required IAM policy

Policy required to create the log group and stream on demand.

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "logs:CreateLogGroup",
        "logs:CreateLogStream",
        "logs:DescribeLogGroups",
        "logs:DescribeLogStreams",
        "logs:PutLogEvents"
    ],
      "Resource": [
        "arn:aws:logs:*:*:*"
    ]
  }
 ]
}
```

More restrictive policy for log event submits only.

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "logs:PutLogEvents"
    ],
      "Resource": [
        "arn:aws:logs:eu-west-1:*:log-group:logzgroup:log-stream:logzstream"
    ]
  }
 ]
}
```
