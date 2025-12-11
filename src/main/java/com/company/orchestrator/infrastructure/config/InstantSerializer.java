package com.company.orchestrator.infrastructure.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

public class InstantSerializer extends JsonSerializer<Instant> {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT;

    @Override
    public void serialize(Instant value, JsonGenerator generator, SerializerProvider provider) throws IOException {
        if (value != null) {
            generator.writeString(formatter.format(value));
        } else {
            generator.writeNull();
        }
    }
}
