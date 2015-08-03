package org.eluder.logback.ext.dynamodb.appender;

/*
 * #[license]
 * logback-ext-dynamodb-appender
 * %%
 * Copyright (C) 2014 - 2015 Tapio Rautonen
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * %[license]
 */

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
