package com.company.orchestrator.infrastructure.config;

import com.company.orchestrator.infrastructure.props.CacheProperties;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

@EnableCaching
@Configuration
@RequiredArgsConstructor
public class CacheConfig {
    private static final Long TTL_IN_MINUTES = 10L;
    private static final String CACHE_PREFIX = "cartup:product::v2::";
    private final CacheProperties cacheProperties;

    @Bean
    public RedisCacheManager cacheManager() {
        RedisCacheManager redisCacheManager = RedisCacheManager.builder(lettuceConnectionFactory())
                .cacheDefaults(
                        RedisCacheConfiguration.defaultCacheConfig()
                                .entryTtl(Duration.ofMinutes(TTL_IN_MINUTES))
                                .disableCachingNullValues()
                                .prefixCacheNameWith(CACHE_PREFIX)
                ).build();
        redisCacheManager.setTransactionAware(true);
        return redisCacheManager;
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(
            RedisConnectionFactory connectionFactory,
            ObjectMapper objectMapper) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Create a copy of ObjectMapper with Redis-specific configuration
        ObjectMapper redisObjectMapper = objectMapper.copy();
        redisObjectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        redisObjectMapper.activateDefaultTyping(
                redisObjectMapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL
        );

        // Use String serializer for keys
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        // Use Jackson serializer for values
        GenericJackson2JsonRedisSerializer jackson2JsonRedisSerializer =
                new GenericJackson2JsonRedisSerializer(redisObjectMapper);
        template.setValueSerializer(jackson2JsonRedisSerializer);
        template.setHashValueSerializer(jackson2JsonRedisSerializer);

        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public LettuceClientConfiguration lettuceClientConfiguration() {
        LettuceClientConfiguration.LettuceClientConfigurationBuilder lettuceClientConfigurationBuilder = LettuceClientConfiguration.builder();
        ClusterTopologyRefreshOptions topologyRefreshOptions = ClusterTopologyRefreshOptions.builder()
                .enableAllAdaptiveRefreshTriggers()
                .enablePeriodicRefresh()
                .dynamicRefreshSources(true)
                .build();
        ClusterClientOptions.Builder clusterClientOptionsBuilder = ClusterClientOptions.builder();
        clusterClientOptionsBuilder.autoReconnect(true)
                .topologyRefreshOptions(topologyRefreshOptions)
                .validateClusterNodeMembership(false)
                .build();
        lettuceClientConfigurationBuilder.clientOptions(clusterClientOptionsBuilder.build());
        return lettuceClientConfigurationBuilder.build();
    }

    @Bean
    public LettuceConnectionFactory lettuceConnectionFactory() {
        if (cacheProperties.isClusterModeEnabled()) {
            return new LettuceConnectionFactory(redisClusterConfiguration(), lettuceClientConfiguration());
        } else {
            return new LettuceConnectionFactory(redisStandaloneConfiguration(), lettuceClientConfiguration());
        }
    }

    @Bean
    public RedisClusterConfiguration redisClusterConfiguration() {
        String redisUrl = cacheProperties.getHost() + ":" + cacheProperties.getPort();
        Objects.requireNonNull(redisUrl);
        return new RedisClusterConfiguration(List.of(redisUrl));
    }

    @Bean
    public RedisStandaloneConfiguration redisStandaloneConfiguration() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(cacheProperties.getHost());
        config.setPort(cacheProperties.getPort());
        return config;
    }

}
