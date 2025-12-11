package com.company.orchestrator.infrastructure.props;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "spring.kafka")
public class KafkaProperties {

    /** Kafka broker addresses (comma-separated) */
    private String bootstrapServers;

    /** Topic name mappings */
    private Map<String, Topic> topics;

    /** Consumer configuration */
    private Consumer consumer;

    /** Default topic configuration */
    private TopicConfig topicConfig;

    /** Producer configuration */
    private Producer producer;

    /** Admin client configuration */
    private Admin admin;

    @Getter
    @Setter
    public static class TopicConfig {
        /** Number of partitions per topic */
        private Integer partitions;

        /** Replication factor for topics */
        private Short replicationFactor;
    }

    @Getter
    @Setter
    public static class Topic {
        /** Actual Kafka topic name */
        private String name;
    }

    @Getter
    @Setter
    public static class Consumer {
        /** Retry configuration for failed message consumption */
        private Retry retry;
    }

    @Getter
    @Setter
    public static class Retry {
        /** Maximum retry attempts for failed messages */
        private Integer maxAttempts;

        /** Delay between retries in milliseconds */
        private Long backoffDelay;
    }

    @Getter
    @Setter
    public static class Producer {
        /** Acknowledgment level (all, 1, 0) */
        private String acks;

        /** Number of retries for failed sends */
        private Integer retries;

        /** Batch size in bytes */
        private Integer batchSize;

        /** Time to wait before sending batch in milliseconds */
        private Integer lingerMs;

        /** Maximum request size in bytes */
        private Integer maxRequestSize;

        /** Enable idempotent producer */
        private Boolean enableIdempotence;
    }

    @Getter
    @Setter
    public static class Admin {
        /** Fail fast on admin operations */
        private Boolean failFast;

        /** Auto-create topics if missing */
        private Boolean autoCreate;
    }
}