package org.eluder.logback.ext.core;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import com.google.common.io.BaseEncoding;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.locks.ReentrantLock;

import static java.lang.String.format;

public abstract class EncodingStringAppender<E> extends UnsynchronizedAppenderBase<E> {
    
    protected final ReentrantLock lock = new ReentrantLock(true);

    private Charset charset = Charset.forName("UTF-8");
    private boolean binary;
    private Encoder<E> encoder;

    public final void setCharset(Charset charset) {
        if (encoder instanceof LayoutWrappingEncoder) {
            ((LayoutWrappingEncoder) encoder).setCharset(charset);
        } else if (encoder instanceof CharacterEncoder) {
            ((CharacterEncoder<?>) encoder).setCharset(charset);
        }
        this.charset = charset;
    }

    public final void setBinary(boolean binary) {
        this.binary = binary;
    }
    
    public final void setEncoder(Encoder<E> encoder) {
        this.encoder = encoder;
        setContext(context);
        setCharset(charset);
    }

    public final void setLayout(Layout<E> layout) {
        LayoutWrappingEncoder<E> enc = new LayoutWrappingEncoder<E>();
        enc.setLayout(layout);
        setEncoder(enc);
    }

    @Override
    public void setContext(Context context) {
        if (encoder != null) {
            encoder.setContext(context);
        }
        super.setContext(context);
    }

    protected final Charset getCharset() {
        return charset;
    }

    protected final boolean isBinary() {
        return binary;
    }

    protected final Encoder<E> getEncoder() {
        return encoder;
    }

    @Override
    public void start() {
        if (encoder == null) {
            addError(format("Encoder not set for appender '%s'", getName()));
            return;
        }
        lock.lock();
        try {
            encoder.start();
            super.start();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void stop() {
        lock.lock();
        try {
            super.stop();
            encoder.stop();
        } finally {
            lock.unlock();
        }
    }

    @Override
    protected void append(E event) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        encode(event, stream);
        doHandle(event, convert(stream.toByteArray()));
    }

    private void encode(E event, ByteArrayOutputStream stream) {
        lock.lock();
        try {
            encoderInit(stream);
            try {
                doEncode(event);
            } finally {
                encoderClose();
            }
        } finally {
            lock.unlock();
        }
    }

    protected abstract void handle(E event, String encoded) throws Exception;
    
    protected String convert(byte[] payload) {
        if (binary) {
            return BaseEncoding.base64().encode(payload);
        } else {
            return new String(payload, charset);
        }
    }

    protected void doHandle(E event, String encoded) {
        try {
            if (encoded != null && !encoded.isEmpty()) {
                handle(event, encoded);
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
        } catch (Exception ex) {
            this.started = false;
            addError(format("Failed to close encoder for appender '%s'", getName()), ex);
        }
    }
}
