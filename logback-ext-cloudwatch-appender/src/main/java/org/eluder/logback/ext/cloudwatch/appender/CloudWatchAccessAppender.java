package org.eluder.logback.ext.cloudwatch.appender;

import ch.qos.logback.access.spi.IAccessEvent;
import ch.qos.logback.core.filter.Filter;
import org.eluder.logback.ext.aws.core.AwsSupport;
import org.eluder.logback.ext.core.CommonEventAttributes;

public class CloudWatchAccessAppender extends AbstractCloudWatchAppender<IAccessEvent> {

    public CloudWatchAccessAppender() {
        super();
    }

    protected CloudWatchAccessAppender(AwsSupport awsSupport, Filter<IAccessEvent> sdkLoggingFilter) {
        super(awsSupport, sdkLoggingFilter);
    }

    @Override
    protected CommonEventAttributes applyCommonEventAttributes(final IAccessEvent event) {
        return new CommonEventAttributes() {
            @Override
            public String getThreadName() {
                return event.getThreadName();
            }

            @Override
            public long getTimeStamp() {
                return event.getTimeStamp();
            }
        };
    }
}
