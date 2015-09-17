package org.eluder.logback.ext.lmax.appender;

import ch.qos.logback.classic.spi.ILoggingEvent;

public class LoggingEventDisruptorAppender extends DelegatingDisruptorAppender<ILoggingEvent> {

    private boolean includeCallerData = false;

    public final boolean isIncludeCallerData() {
        return includeCallerData;
    }

    public final void setIncludeCallerData(boolean includeCallerData) {
        this.includeCallerData = includeCallerData;
    }

    @Override
    protected void prepareForDeferredProcessing(ILoggingEvent event) {
        super.prepareForDeferredProcessing(event);
        if (includeCallerData) {
            event.getCallerData();
        }
    }
}
