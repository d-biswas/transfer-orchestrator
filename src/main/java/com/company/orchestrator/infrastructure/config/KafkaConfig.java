package com.company.orchestrator.infrastructure.config;

import com.company.orchestrator.infrastructure.props.KafkaProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class KafkaConfig {

    private final KafkaProperties props;

    private static final String TRUSTED_PACKAGES = "com.company.orchestrator";

    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, props.getBootstrapServers());
        return new KafkaAdmin(configs);
    }

    @Bean
    public KafkaAdmin.NewTopics allTopics() {
        return new KafkaAdmin.NewTopics(
                props.getTopics().values().stream()
                        .map(topic -> new NewTopic(
                                topic.getName(),
                                props.getTopicConfig().getPartitions(),
                                props.getTopicConfig().getReplicationFactor()
                        ))
                        .toArray(NewTopic[]::new)
        );
    }

    @Bean
    public ProducerFactory<String, Object> producerFactory(ObjectMapper objectMapper) {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, props.getBootstrapServers());
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        // apply producer properties
        KafkaProperties.Producer producer = props.getProducer();
        configProps.put(ProducerConfig.ACKS_CONFIG, producer.getAcks());
        configProps.put(ProducerConfig.RETRIES_CONFIG, producer.getRetries());
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, producer.getLingerMs());
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, producer.getBatchSize());
        configProps.put(ProducerConfig.MAX_REQUEST_SIZE_CONFIG, producer.getMaxRequestSize());
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, producer.getEnableIdempotence());

        JsonSerializer<Object> jsonSerializer = new JsonSerializer<>(objectMapper);
        jsonSerializer.setAddTypeInfo(false);
        return new DefaultKafkaProducerFactory<>(configProps, new StringSerializer(), jsonSerializer);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> factory) {
        return new KafkaTemplate<>(factory);
    }

    @Bean
    public ConsumerFactory<String, Object> consumerFactory(ObjectMapper objectMapper) {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, props.getBootstrapServers());
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, props.getConsumer().getGroupId());
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        configProps.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class.getName());
        configProps.put(JsonDeserializer.TRUSTED_PACKAGES, TRUSTED_PACKAGES);
        configProps.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        configProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        JsonDeserializer<Object> jsonDeserializer = new JsonDeserializer<>(objectMapper);
        jsonDeserializer.addTrustedPackages(TRUSTED_PACKAGES);
        jsonDeserializer.setUseTypeHeaders(false);

        return new DefaultKafkaConsumerFactory<>(
                configProps,
                new StringDeserializer(),
                new ErrorHandlingDeserializer<>(jsonDeserializer)
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory,
            KafkaTemplate<String, Object> kafkaTemplate) {

        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(props.getListener().getConcurrency());

        // Set acknowledgment mode from properties
        ContainerProperties.AckMode ackMode = ContainerProperties.AckMode.valueOf(
                props.getListener().getAckMode().toUpperCase()
        );
        factory.getContainerProperties().setAckMode(ackMode);

        // Configure error handler with retry and DLQ
        factory.setCommonErrorHandler(kafkaErrorHandler(kafkaTemplate));

        return factory;
    }

    @Bean
    public CommonErrorHandler kafkaErrorHandler(KafkaTemplate<String, Object> kafkaTemplate) {
        // Get DLQ topic name from properties
        String dlqTopic = props.getTopics().get("dlq").getName();

        // Configure Dead Letter Publisher with DLQ topic
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, ex) -> {
                    // Send failed messages to DLQ
                    return new TopicPartition(dlqTopic, -1);
                }
        );

        // Configure exponential backoff with max retries from properties
        KafkaProperties.Retry retryConfig = props.getConsumer().getRetry();
        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(
                retryConfig.getMaxAttempts()
        );
        backOff.setInitialInterval(retryConfig.getBackoffDelay());
        backOff.setMultiplier(retryConfig.getBackoffMultiplier());
        backOff.setMaxInterval(retryConfig.getMaxInterval());

        // Create default error handler with backoff and recoverer
        return new DefaultErrorHandler(recoverer, backOff);
    }
}
