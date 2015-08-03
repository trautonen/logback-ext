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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

import static org.assertj.core.api.Assertions.*;


public class JsonWriterTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final Charset charset = Charset.forName("UTF-8");

    @Test
    public void writeObject() throws Exception {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        JsonWriter writer = writer(os);
        writer.writeObject()
                .writeStringField("hello", "world", true)
                .writeNumberField("missing", 1, false)
                .done()
                .close();

        assertThat(os.toString(charset.name())).isEqualTo("{\"hello\":\"world\"}");
    }

    @Test
    public void writeArray() throws Exception {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        JsonWriter writer = writer(os);
        writer.writeArray()
                .writeString("hello", true)
                .writeString("world", false)
                .writeString("again", true)
                .done()
                .close();

        assertThat(os.toString(charset.name())).isEqualTo("[\"hello\",\"again\"]");
    }

    @Test
    public void writeComplexObject() throws Exception {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        JsonWriter writer = writer(os);
        writer.writeObject()
                .writeArray("array", true)
                .writeString("val1", true)
                .writeString("val2", true)
                .done()
                .writeObject("object", true)
                .writeArray("subarray", true)
                .writeObject()
                .writeNumberField("num", 10, true)
                .writeStringField("str", "test", true)
                .done()
                .writeArray()
                .writeString("sub1", true)
                .writeString("sub2", true)
                .done()
                .done()
                .writeStringField("theend", "yes", true)
                .done()
                .done()
                .close();

        assertThat(os.toString(charset.name())).isEqualTo("{\"array\":[\"val1\",\"val2\"],\"object\":{\"subarray\":[{\"num\":10,\"str\":\"test\"},[\"sub1\",\"sub2\"]],\"theend\":\"yes\"}}");
    }

    private JsonWriter writer(OutputStream os) throws IOException {
        return new JsonWriter(os, charset, mapper);
    }

}