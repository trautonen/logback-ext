package org.eluder.logback.ext.aws.core;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

public class InternalSdkLoggingFilter<E> extends Filter<E> {

	private static final String[] EXCLUDED_PACKAGES = { "org.apache.http.", "com.amazonaws." };

	public InternalSdkLoggingFilter() {
		setName("aws-internal-logging-exclude");
	}

	@Override
	public FilterReply decide(E event) {
		if (event instanceof ILoggingEvent) {
			for (String exclude : EXCLUDED_PACKAGES) {
				if (((ILoggingEvent) event).getLoggerName().startsWith(exclude)) {
					return FilterReply.DENY;
				}
			}
		}
		return FilterReply.NEUTRAL;
	}
}
