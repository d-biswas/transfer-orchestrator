package com.company.orchestrator.infrastructure.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.Instant;

public class InstantDeserializer extends JsonDeserializer<Instant> {

    @Override
    public Instant deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
        String str = p.getText();
        if (str != null && !str.isEmpty()) {
            return Instant.parse(str);
        }
        return null;
    }
}
