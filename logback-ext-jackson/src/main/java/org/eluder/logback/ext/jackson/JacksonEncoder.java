package org.eluder.logback.ext.jackson;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.spi.ContextAwareBase;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JacksonEncoder extends ContextAwareBase implements Encoder<ILoggingEvent> {
    
    private final ObjectMapper mapper = new ObjectMapper()
            .configure(JsonGenerator.Feature.ESCAPE_NON_ASCII, true);
    
    private final ThrowableProxyConverter throwableProxyConverter = new ThrowableProxyConverter();
    
    private Charset charset = Charset.forName("UTF-8");
    private FieldNames fieldNames = new FieldNames();
    private DateFormat timeStampFormat;
    
    private boolean started;
    private JsonGenerator generator;

    public void setCharset(Charset charset) {
        this.charset = charset;
    }

    public void setFieldNames(FieldNames fieldNames) {
        this.fieldNames = fieldNames;
    }
    
    public void setTimeStampFormat(String timeStampFormat) {
        this.timeStampFormat = new SimpleDateFormat(timeStampFormat);
    }

    @Override
    public boolean isStarted() {
        return started;
    }

    @Override
    public void start() {
        throwableProxyConverter.start();
        started = true;
    }

    @Override
    public void stop() {
        started = false;
        throwableProxyConverter.stop();
    }
    
    @Override
    public void init(OutputStream os) throws IOException {
        generator = mapper.getFactory()
                .createGenerator(new OutputStreamWriter(os, charset))
                .configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
    }

    @Override
    public void doEncode(ILoggingEvent event) throws IOException {
        generator.writeStartObject();
        writeTimeStamp(generator, event);
        writeLogger(generator, event);
        writeMessage(generator, event);
        writeStackTrace(generator, event);
        writeCallerData(generator, event);
        generator.writeEndObject();
        generator.flush();
    }
    
    private void writeTimeStamp(JsonGenerator generator, ILoggingEvent event) throws IOException {
        if (timeStampFormat != null) {
            String timeStamp = timeStampFormat.format(new Date(event.getTimeStamp()));
            generator.writeStringField(fieldNames.getTimeStamp(), timeStamp);
        } else {
            generator.writeNumberField(fieldNames.getTimeStamp(), event.getTimeStamp());
        }
    }
    
    private void writeLogger(JsonGenerator generator, ILoggingEvent event) throws IOException {
        generator.writeStringField(fieldNames.getLevel(), event.getLevel().toString());
        generator.writeNumberField(fieldNames.getLevelValue(), event.getLevel().toInt());
        generator.writeStringField(fieldNames.getThreadName(), event.getThreadName());
        generator.writeStringField(fieldNames.getLoggerName(), event.getLoggerName());
    }
    
    private void writeMessage(JsonGenerator generator, ILoggingEvent event) throws IOException {
        generator.writeStringField(fieldNames.getMessage(), event.getMessage());
        generator.writeStringField(fieldNames.getFormattedMessage(), event.getFormattedMessage());
    }
    
    private void writeStackTrace(JsonGenerator generator, ILoggingEvent event) throws IOException {
        String stackTrace = throwableProxyConverter.convert(event);
        if (stackTrace != null && !stackTrace.isEmpty()) {
            generator.writeStringField(fieldNames.getStackTrace(), stackTrace);
        }
    }

    private void writeCallerData(JsonGenerator generator, ILoggingEvent event) throws IOException{
        if (event.hasCallerData()) {
            StackTraceElement callerData = event.getCallerData()[0];
            generator.writeObjectFieldStart(fieldNames.getCallerData());
            generator.writeStringField(fieldNames.getCallerClass(), callerData.getClassName());
            generator.writeStringField(fieldNames.getCallerMethod(), callerData.getMethodName());
            generator.writeStringField(fieldNames.getCallerFile(), callerData.getFileName());
            generator.writeNumberField(fieldNames.getCallerLine(), callerData.getLineNumber());
            generator.writeEndObject();
        }
    }
    
    @Override
    public void close() throws IOException {
        if (generator != null) {
            generator.close();
            generator = null;
        }
    }
    
}
