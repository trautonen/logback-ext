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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import org.eluder.logback.ext.core.FieldNames;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;

import static org.junit.Assert.assertNotNull;

public class JacksonEncoderTest {

    private final LoggerContext context = new LoggerContext();
    private final Logger logger = context.getLogger(JacksonEncoderTest.class);
    
    private JacksonEncoder encoder;
    
    @Before
    public void start() {
        encoder = new JacksonEncoder();
        encoder.start();
    }
    
    @After
    public void stop() {
        encoder.stop();
        encoder = null;
    }
    
    @Test
    public void encodeLoggingEvent() throws Exception {
        FieldNames fn = new FieldNames();
        fn.setLevel(FieldNames.IGNORE_NAME);
        encoder.setFieldNames(fn);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        encoder.init(stream);
        encoder.doEncode(createLoggingEvent("Hellö JSON!"));
        String json = new String(stream.toByteArray(), "UTF-8");
        assertNotNull(json);
    }
    
    private ILoggingEvent createLoggingEvent(String message) {
        return new LoggingEvent("", logger, Level.DEBUG, message, new IllegalArgumentException("fobar"), null);
    }
}
