package org.eluder.logback.ext.sns.appender;

import ch.qos.logback.classic.spi.ILoggingEvent;
import org.eluder.logback.ext.aws.core.AbstractAwsEncodingStringAppender;

public class SnsAppender extends AbstractAwsEncodingStringAppender<ILoggingEvent> {

    @Override
    protected void doInit() {

    }

    @Override
    protected void doClose() {

    }

    @Override
    protected void handle(String event) throws Exception {

    }
}
