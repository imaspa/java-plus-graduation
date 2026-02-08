package ru.practicum.ewm.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@AllArgsConstructor
@ConfigurationProperties("collector.kafka")
public class KafkaConfig {
    private final KafkaConfigProducer producer;
}