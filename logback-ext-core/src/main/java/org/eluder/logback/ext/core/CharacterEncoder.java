package org.eluder.logback.ext.core;

import ch.qos.logback.core.encoder.Encoder;

import java.nio.charset.Charset;

public interface CharacterEncoder<E> extends Encoder<E> {

    void setCharset(Charset charset);

    Charset getCharset();

}
