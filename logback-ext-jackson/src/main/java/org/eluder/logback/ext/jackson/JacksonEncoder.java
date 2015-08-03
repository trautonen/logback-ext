package org.eluder.logback.ext.jackson;

/*
 * #[license]
 * logback-ext-jackson
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

import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.spi.ContextAwareBase;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eluder.logback.ext.core.CharacterEncoder;
import org.eluder.logback.ext.core.FieldNames;
import org.slf4j.Marker;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

public class JacksonEncoder extends ContextAwareBase implements CharacterEncoder<ILoggingEvent> {

    private final ObjectMapper mapper = new ObjectMapper().configure(JsonGenerator.Feature.ESCAPE_NON_ASCII, true);
    private final ThrowableProxyConverter throwableProxyConverter = new ThrowableProxyConverter();

    private Charset charset = Charset.forName("UTF-8");
    private FieldNames fieldNames = new FieldNames();
    private DateFormat timeStampFormat;
    
    private boolean started;
    private JsonWriter writer;

    @Override
    public final void setCharset(Charset charset) {
        this.charset = charset;
    }

    @Override
    public final Charset getCharset() {
        return charset;
    }

    public final void setFieldNames(FieldNames fieldNames) {
        this.fieldNames = fieldNames;
    }

    public final FieldNames getFieldNames() {
        return fieldNames;
    }

    public final void setTimeStampFormat(String timeStampFormat) {
        this.timeStampFormat = new SimpleDateFormat(timeStampFormat);
    }

    public final DateFormat getTimeStampFormat() {
        return timeStampFormat;
    }

    @Override
    public void setContext(Context context) {
        throwableProxyConverter.setContext(context);
        super.setContext(context);
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
    public void close() throws IOException {
        if (writer != null) {
            writer.close();
            writer = null;
        }
    }
    
    @Override
    public void init(OutputStream os) throws IOException {
        writer = new JsonWriter(os, charset, getMapper());
    }

    @Override
    public void doEncode(ILoggingEvent event) throws IOException {
        JsonWriter.ObjectWriter<JsonWriter> ow = writer.writeObject();
        writeTimeStamp(ow, event);
        writeLogger(ow, event);
        writeMessage(ow, event);
        writeStackTrace(ow, event);
        writeCallerData(ow, event);
        writeMarker(ow, event);
        writeMdc(ow, event);
        ow.done().flush();
    }

    protected ObjectMapper getMapper() {
        return mapper;
    }

    protected void writeTimeStamp(JsonWriter.ObjectWriter<JsonWriter> writer, ILoggingEvent event) throws IOException {
        if (timeStampFormat != null) {
            String timeStamp = timeStampFormat.format(new Date(event.getTimeStamp()));
            writer.writeStringField(fieldNames.getTimeStamp(), timeStamp, isActive(fieldNames.getTimeStamp()));
        } else {
            writer.writeNumberField(fieldNames.getTimeStamp(), event.getTimeStamp(), isActive(fieldNames.getTimeStamp()));
        }
    }

    protected void writeLogger(JsonWriter.ObjectWriter<JsonWriter> writer, ILoggingEvent event) throws IOException {
        writer.writeStringField(fieldNames.getLevel(), event.getLevel().toString(), isActive(fieldNames.getLevel()));
        writer.writeNumberField(fieldNames.getLevelValue(), event.getLevel().toInt(), isActive(fieldNames.getLevelValue()));
        writer.writeStringField(fieldNames.getThreadName(), event.getThreadName(), isActive(fieldNames.getThreadName()));
        writer.writeStringField(fieldNames.getLoggerName(), event.getLoggerName(), isActive(fieldNames.getLoggerName()));
    }

    protected void writeMessage(JsonWriter.ObjectWriter<JsonWriter> writer, ILoggingEvent event) throws IOException {
        writer.writeStringField(fieldNames.getMessage(), event.getMessage(), isActive(fieldNames.getMessage()));
        writer.writeStringField(fieldNames.getFormattedMessage(), event.getFormattedMessage(), isActive(fieldNames.getFormattedMessage()));
    }

    protected void writeStackTrace(JsonWriter.ObjectWriter<JsonWriter> writer, ILoggingEvent event) throws IOException {
        String stackTrace = throwableProxyConverter.convert(event);
        if (stackTrace != null && !stackTrace.isEmpty()) {
            writer.writeStringField(fieldNames.getStackTrace(), stackTrace, isActive(fieldNames.getStackTrace()));
        }
    }

    protected void writeCallerData(JsonWriter.ObjectWriter<JsonWriter> writer, ILoggingEvent event) throws IOException {
        if (event.hasCallerData()) {
            StackTraceElement callerData = event.getCallerData()[0];
            JsonWriter.ObjectWriter<JsonWriter.ObjectWriter<JsonWriter>> ow =
                    writer.writeObject(fieldNames.getCallerData(), isActive(fieldNames.getCallerData()));
            ow.writeStringField(fieldNames.getCallerClass(), callerData.getClassName(), isActive(fieldNames.getCallerClass()));
            ow.writeStringField(fieldNames.getCallerMethod(), callerData.getMethodName(), isActive(fieldNames.getCallerMethod()));
            ow.writeStringField(fieldNames.getCallerFile(), callerData.getFileName(), isActive(fieldNames.getCallerFile()));
            ow.writeNumberField(fieldNames.getCallerLine(), callerData.getLineNumber(), isActive(fieldNames.getCallerLine()));
            ow.done();
        }
    }

    protected void writeMarker(JsonWriter.ObjectWriter<JsonWriter> writer, ILoggingEvent event) throws IOException {
        Marker marker = event.getMarker();
        if (marker != null) {
            JsonWriter.ArrayWriter<JsonWriter.ObjectWriter<JsonWriter.ObjectWriter<JsonWriter>>> aw =
                    writer.writeObject(fieldNames.getMarker(), isActive(fieldNames.getMarker())).writeArray(marker.getName(), true);
            Iterator<Marker> markers = marker.iterator();
            while (markers.hasNext()) {
                String name = markers.next().getName();
                aw.writeString(name, name != null && !name.isEmpty());
            }
            aw.done().done();
        }
    }

    protected void writeMdc(JsonWriter.ObjectWriter<JsonWriter> writer, ILoggingEvent event) throws IOException {
        Map<String, String> mdc = event.getMDCPropertyMap();
        if (mdc != null && !mdc.isEmpty()) {
            JsonWriter.ObjectWriter<JsonWriter.ObjectWriter<JsonWriter>> ow =
                    writer.writeObject(fieldNames.getMdc(), isActive(fieldNames.getMdc()));
            for (Map.Entry<String, String> property : mdc.entrySet()) {
                ow.writeStringField(property.getKey(), property.getValue(), true);
            }
            ow.done();
        }
    }

    private boolean isActive(String name) {
        return !FieldNames.IGNORE_NAME.equals(name);
    }
    
}
