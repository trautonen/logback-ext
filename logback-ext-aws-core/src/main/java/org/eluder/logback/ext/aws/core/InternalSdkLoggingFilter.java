package org.eluder.logback.ext.aws.core;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

public class InternalSdkLoggingFilter extends Filter<ILoggingEvent> {

    private static final String[] EXCLUDED_PACKAGES = {
            "org.apache.http.",
            "com.amazonaws."
    };

    public InternalSdkLoggingFilter() {
        setName("aws-internal-logging-exclude");
    }

    @Override
    public FilterReply decide(ILoggingEvent event) {
        for (String exclude : EXCLUDED_PACKAGES) {
            if (event.getLoggerName().startsWith(exclude)) {
                return FilterReply.DENY;
            }
        }
        return FilterReply.NEUTRAL;
    }
}
