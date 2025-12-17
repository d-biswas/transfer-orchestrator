package com.company.orchestrator.utils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class InstantSerializer extends JsonSerializer<Instant> {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX").withZone(ZoneOffset.UTC);

    @Override
    public void serialize(Instant value, JsonGenerator generator, SerializerProvider provider) throws IOException {
        if (value != null) {
            generator.writeString(FORMATTER.format(value));
        } else {
            generator.writeNull();
        }
    }
}
