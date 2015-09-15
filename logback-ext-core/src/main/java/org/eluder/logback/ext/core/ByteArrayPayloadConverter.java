package org.eluder.logback.ext.core;

public class ByteArrayPayloadConverter implements PayloadConverter<byte[]> {

    @Override
    public byte[] convert(byte[] payload) {
        return payload;
    }
}
