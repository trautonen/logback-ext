package org.eluder.logback.ext.core;

import com.google.common.io.BaseEncoding;

import java.nio.charset.Charset;

public class StringPayloadConverter implements PayloadConverter<String> {

    private final Charset charset;
    private final boolean binary;

    public StringPayloadConverter(Charset charset, boolean binary) {
        this.charset = charset;
        this.binary = binary;
    }

    @Override
    public String convert(byte[] payload) {
        if (payload == null) {
            return null;
        } else if (binary) {
            return BaseEncoding.base64().encode(payload);
        } else {
            return new String(payload, charset);
        }
    }
}
