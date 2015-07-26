package org.eluder.logback.ext.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;

public class JsonWriter implements Closeable {

    private final JsonGenerator generator;

    public JsonWriter(OutputStream os, Charset charset, ObjectMapper mapper) throws IOException {
        generator = mapper.getFactory()
                .createGenerator(new OutputStreamWriter(os, charset))
                .configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
    }

    @Override
    public void close() throws IOException {
        generator.close();
    }

    public void flush() throws IOException {
        generator.flush();
    }

    public ObjectWriter<JsonWriter> writeObject() throws IOException {
        return new ObjectWriter<JsonWriter>(this, true);
    }

    public ArrayWriter<JsonWriter> writeArray() throws IOException {
        return new ArrayWriter<JsonWriter>(this, true);
    }

    public class ObjectWriter<T> {
        private final T parent;
        private final boolean enabled;

        ObjectWriter(T parent, boolean parentEnabled) throws IOException {
            this.parent = parent;
            this.enabled = parentEnabled;
            if (enabled) {
                generator.writeStartObject();
            }
        }

        ObjectWriter(T parent, boolean parentEnabled, String name, boolean active) throws IOException {
            this.parent = parent;
            this.enabled = parentEnabled && active;
            if (enabled) {
                generator.writeObjectFieldStart(name);
            }
        }

        public ObjectWriter<T> writeStringField(String name, String value, boolean active) throws IOException {
            if (enabled && active) {
                generator.writeStringField(name, value);
            }
            return this;
        }

        public ObjectWriter<T> writeNumberField(String name, int value, boolean active) throws IOException {
            if (enabled && active) {
                generator.writeNumberField(name, value);
            }
            return this;
        }

        public ObjectWriter<T> writeNumberField(String name, long value, boolean active) throws IOException {
            if (enabled && active) {
                generator.writeNumberField(name, value);
            }
            return this;
        }

        public ArrayWriter<ObjectWriter<T>> writeArray(String name, boolean active) throws IOException {
            return new ArrayWriter<ObjectWriter<T>>(this, enabled, name, active);
        }

        public ObjectWriter<ObjectWriter<T>> writeObject(String name, boolean active) throws IOException {
            return new ObjectWriter<ObjectWriter<T>>(this, enabled, name, active);
        }

        public T done() throws IOException {
            if (enabled) {
                generator.writeEndObject();
            }
            return parent;
        }
    }

    public class ArrayWriter<T> {
        private final T parent;
        private final boolean enabled;

        ArrayWriter(T parent, boolean parentEnabled) throws IOException {
            this.parent = parent;
            this.enabled = parentEnabled;
            if (enabled) {
                generator.writeStartArray();
            }
        }

        ArrayWriter(T parent, boolean parentEnabled, String name, boolean active) throws IOException {
            this.parent = parent;
            this.enabled = parentEnabled && active;
            if (enabled) {
                generator.writeArrayFieldStart(name);
            }
        }

        public ArrayWriter<T> writeString(String value, boolean active) throws IOException {
            if (enabled && active) {
                generator.writeString(value);
            }
            return this;
        }

        public ArrayWriter<ArrayWriter<T>> writeArray() throws IOException {
            return new ArrayWriter<ArrayWriter<T>>(this, enabled);
        }

        public ObjectWriter<ArrayWriter<T>> writeObject() throws IOException {
            return new ObjectWriter<ArrayWriter<T>>(this, enabled);
        }

        public T done() throws IOException {
            if (enabled) {
                generator.writeEndArray();
            }
            return parent;
        }
    }

}
