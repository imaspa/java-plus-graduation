package ru.practicum.ewm.aggregator.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@AllArgsConstructor
@ConfigurationProperties("aggregator.kafka")
public class KafkaConfig {
    private final KafkaConfigProducer producer;
    private final KafkaConfigConsumer consumer;
}