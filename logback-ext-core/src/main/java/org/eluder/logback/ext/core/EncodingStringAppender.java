package org.eluder.logback.ext.core;

import static java.lang.String.format;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.locks.ReentrantLock;

import ch.qos.logback.core.Layout;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import com.google.common.io.BaseEncoding;

public abstract class EncodingStringAppender<E> extends UnsynchronizedAppenderBase<E> {
    
    protected final ReentrantLock lock = new ReentrantLock(true);

    private Charset charset = Charset.forName("UTF-8");
    private boolean binary;
    private Encoder<E> encoder;

    public final void setCharset(Charset charset) {
        this.charset = charset;
        if (this.encoder instanceof LayoutWrappingEncoder) {
            ((LayoutWrappingEncoder) this.encoder).setCharset(charset);
        }
    }

    public final void setBinary(boolean binary) {
        this.binary = binary;
    }
    
    public final void setEncoder(Encoder<E> encoder) {
        this.encoder = encoder;
    }

    public final void setLayout(Layout<E> layout) {
        LayoutWrappingEncoder<E> enc = new LayoutWrappingEncoder<E>();
        enc.setLayout(layout);
        enc.setContext(context);
        enc.setCharset(charset);
        this.encoder = enc;
    }

    protected final Charset getCharset() {
        return charset;
    }

    protected final boolean isBinary() {
        return binary;
    }

    @Override
    public void start() {
        if (encoder == null) {
            addError(format("Encoder not set for appender '%s'", getName()));
            return;
        }
        super.start();
    }

    @Override
    public void stop() {
        lock.lock();
        try {
            super.stop();
            encoderClose();
        } finally {
            lock.unlock();
        }
    }

    @Override
    protected void append(E event) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        lock.lock();
        try {
            encoderInit(stream);
            doEncode(event);
            doHandle(convert(stream.toByteArray()));
        } finally {
            lock.unlock();
        }
    }
    
    protected abstract void handle(String event) throws Exception;
    
    protected String convert(byte[] payload) {
        if (binary) {
            return BaseEncoding.base64().encode(payload);
        } else {
            return new String(payload, charset);
        }
    }
    
    protected void doHandle(String event) {
        try {
            if (event != null && !event.isEmpty()) {
                handle(event);
            }
        } catch (Exception ex) {
            this.started = false;
            addError(format("Failed to handle logging event for '%s'", getName()), ex);
        }
    }
    
    protected void doEncode(E event) {
        try {
            encoder.doEncode(event);
        } catch (IOException ex) {
            this.started = false;
            addError(format("Failed to encode logging event for appender '%s'", getName()), ex);
        }
    }
    
    protected void encoderInit(ByteArrayOutputStream stream) {
        try {
            encoder.init(stream);
        } catch (IOException ex) {
            this.started = false;
            addError(format("Failed to initialize encoder for appender '%s'", getName()), ex);
        }
    }
    
    protected void encoderClose() {
        try {
            encoder.close();
        } catch (IOException ex) {
            this.started = false;
            addError(format("Failed to close encoder for appender '%s'", getName()), ex);
        }
    }
}
