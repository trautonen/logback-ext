package org.eluder.logback.ext.dynamodb.appender;

import org.eluder.logback.ext.core.FieldNames;

public class CapitalizingFieldNames extends FieldNames {

    public CapitalizingFieldNames() {
        setTimeStamp(getTimeStamp());
        setLevel(getLevel());
        setLevelValue(getLevelValue());
        setThreadName(getThreadName());
        setLoggerName(getLoggerName());
        setMessage(getMessage());
        setFormattedMessage(getFormattedMessage());
        setStackTrace(getStackTrace());
        setCallerData(getCallerData());
        setCallerClass(getCallerClass());
        setCallerMethod(getCallerMethod());
        setCallerFile(getCallerFile());
        setCallerLine(getCallerLine());
        setMdc(getMdc());
        setMarker(getMarker());
    }

    @Override
    public void setTimeStamp(String timeStamp) {
        super.setTimeStamp(capitalize(timeStamp));
    }

    @Override
    public void setLevel(String level) {
        super.setLevel(capitalize(level));
    }

    @Override
    public void setLevelValue(String levelValue) {
        super.setLevelValue(capitalize(levelValue));
    }

    @Override
    public void setThreadName(String threadName) {
        super.setThreadName(capitalize(threadName));
    }

    @Override
    public void setLoggerName(String loggerName) {
        super.setLoggerName(capitalize(loggerName));
    }

    @Override
    public void setMessage(String message) {
        super.setMessage(capitalize(message));
    }

    @Override
    public void setFormattedMessage(String formattedMessage) {
        super.setFormattedMessage(capitalize(formattedMessage));
    }

    @Override
    public void setStackTrace(String stackTrace) {
        super.setStackTrace(capitalize(stackTrace));
    }

    @Override
    public void setCallerData(String callerData) {
        super.setCallerData(capitalize(callerData));
    }

    @Override
    public void setCallerClass(String callerClass) {
        super.setCallerClass(capitalize(callerClass));
    }

    @Override
    public void setCallerMethod(String callerMethod) {
        super.setCallerMethod(capitalize(callerMethod));
    }

    @Override
    public void setCallerFile(String callerFile) {
        super.setCallerFile(capitalize(callerFile));
    }

    @Override
    public void setCallerLine(String callerLine) {
        super.setCallerLine(capitalize(callerLine));
    }

    @Override
    public void setMdc(String mdc) {
        super.setMdc(capitalize(mdc));
    }

    @Override
    public void setMarker(String marker) {
        super.setMarker(capitalize(marker));
    }

    private String capitalize(String value) {
        if (value == null || value.isEmpty() || FieldNames.IGNORE_NAME.equals(value)) {
            return value;
        } else {
            return value.substring(0, 1).toUpperCase() + value.substring(1);
        }
    }
}
