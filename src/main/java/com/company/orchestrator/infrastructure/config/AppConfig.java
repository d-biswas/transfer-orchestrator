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
import org.apache.hc.client5.http.impl.DefaultHttpRequestRetryStrategy;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.TimeValue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

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
    public RestTemplate restTemplate() {
        PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setMaxConnTotal(httpClientProperties.getMaxTotalConnection())
                .setMaxConnPerRoute(httpClientProperties.getMaxConnectionPerRoute())
                .build();

        // Automatically evict expired or idle connections
        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .evictExpiredConnections()
                .evictIdleConnections(TimeValue.ofSeconds(30))
                .setRetryStrategy(new DefaultHttpRequestRetryStrategy(3, TimeValue.ofSeconds(2)))
                .build();

        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
        requestFactory.setConnectTimeout(httpClientProperties.getConnectTimeout());
        requestFactory.setReadTimeout(httpClientProperties.getReadTimeout());
        return new RestTemplate(requestFactory);
    }
}
