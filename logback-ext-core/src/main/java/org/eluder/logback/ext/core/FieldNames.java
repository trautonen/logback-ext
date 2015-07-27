package org.eluder.logback.ext.core;

public class FieldNames {
    
    public static final String IGNORE_NAME = "[ignore]";
    
    private String timeStamp        = "timeStamp";
    private String level            = "level";
    private String levelValue       = "levelValue";
    private String threadName       = "thread";
    private String loggerName       = "logger";
    private String message          = "message";
    private String formattedMessage = "formattedMessage";
    private String stackTrace       = "stackTrace";
    private String callerData       = "caller";
    private String callerClass          = "class";
    private String callerMethod         = "method";
    private String callerFile           = "file";
    private String callerLine           = "line";
    private String mdc              = "mdc";
    private String marker           = "marker";

    public String getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(String timeStamp) {
        this.timeStamp = timeStamp;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getLevelValue() {
        return levelValue;
    }

    public void setLevelValue(String levelValue) {
        this.levelValue = levelValue;
    }

    public String getThreadName() {
        return threadName;
    }

    public void setThreadName(String threadName) {
        this.threadName = threadName;
    }

    public String getLoggerName() {
        return loggerName;
    }

    public void setLoggerName(String loggerName) {
        this.loggerName = loggerName;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getFormattedMessage() {
        return formattedMessage;
    }

    public void setFormattedMessage(String formattedMessage) {
        this.formattedMessage = formattedMessage;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }

    public String getCallerData() {
        return callerData;
    }

    public void setCallerData(String callerData) {
        this.callerData = callerData;
    }

    public String getCallerClass() {
        return callerClass;
    }

    public void setCallerClass(String callerClass) {
        this.callerClass = callerClass;
    }

    public String getCallerMethod() {
        return callerMethod;
    }

    public void setCallerMethod(String callerMethod) {
        this.callerMethod = callerMethod;
    }

    public String getCallerFile() {
        return callerFile;
    }

    public void setCallerFile(String callerFile) {
        this.callerFile = callerFile;
    }

    public String getCallerLine() {
        return callerLine;
    }

    public void setCallerLine(String callerLine) {
        this.callerLine = callerLine;
    }
    
    public String getMdc() {
        return mdc;
    }

    public void setMdc(String mdc) {
        this.mdc = mdc;
    }

    public String getMarker() {
        return marker;
    }

    public void setMarker(String marker) {
        this.marker = marker;
    }
}
