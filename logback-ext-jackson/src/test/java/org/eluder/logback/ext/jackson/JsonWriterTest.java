package org.eluder.logback.ext.jackson;

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