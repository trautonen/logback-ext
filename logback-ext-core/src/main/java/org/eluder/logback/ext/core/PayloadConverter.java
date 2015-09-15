package org.eluder.logback.ext.core;

public interface PayloadConverter<P> {

    P convert(byte[] payload);

}
