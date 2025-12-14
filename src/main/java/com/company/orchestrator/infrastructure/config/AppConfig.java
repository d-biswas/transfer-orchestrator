package com.company.orchestrator.infrastructure.config;

import com.company.orchestrator.infrastructure.props.HttpClientProperties;
import com.company.orchestrator.infrastructure.utils.InstantDeserializer;
import com.company.orchestrator.infrastructure.utils.InstantSerializer;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import io.netty.channel.ChannelOption;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;
import java.time.Instant;

@Configuration
@RequiredArgsConstructor
public class AppConfig {
    private final HttpClientProperties httpClientProperties;

    @Bean
    public ObjectMapper objectMapper() {
        SimpleModule timeModule = new SimpleModule();
        timeModule.addSerializer(Instant.class, new InstantSerializer());
        timeModule.addDeserializer(Instant.class, new InstantDeserializer());

        return JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .addModule(timeModule)
                .serializationInclusion(JsonInclude.Include.NON_NULL)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
    }

    @Bean
    public WebClient webClient() {
        // Configure connection provider
        ConnectionProvider connectionProvider = ConnectionProvider.builder("webclient-pool")
                .maxConnections(httpClientProperties.getMaxTotalConnection())
                .maxIdleTime(Duration.ofSeconds(30))
                .maxLifeTime(Duration.ofSeconds(60))
                .pendingAcquireTimeout(Duration.ofSeconds(60))
                .evictInBackground(Duration.ofSeconds(120))
                .build();

        // Configure Reactor Netty HttpClient (reactive, non-blocking)
        reactor.netty.http.client.HttpClient httpClient = reactor.netty.http.client.HttpClient.create(connectionProvider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, httpClientProperties.getConnectTimeout())
                .responseTimeout(Duration.ofMillis(httpClientProperties.getReadTimeout()));

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024)) // 16MB buffer
                .build();
    }
}
