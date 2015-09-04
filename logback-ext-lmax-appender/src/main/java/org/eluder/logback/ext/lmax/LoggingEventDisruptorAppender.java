package org.eluder.logback.ext.lmax;

import ch.qos.logback.classic.spi.ILoggingEvent;

public class LoggingEventDisruptorAppender extends DelegatingDisruptorAppender<ILoggingEvent> {

    private boolean includeCallerData = false;

    public boolean isIncludeCallerData() {
        return includeCallerData;
    }

    public void setIncludeCallerData(boolean includeCallerData) {
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
