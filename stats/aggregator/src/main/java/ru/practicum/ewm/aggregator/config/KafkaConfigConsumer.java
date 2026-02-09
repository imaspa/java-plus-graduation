package ru.practicum.ewm.aggregator.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.Duration;
import java.util.Properties;

@Getter
@Setter
@AllArgsConstructor
public class KafkaConfigConsumer {
    private String topic;
    private Duration pollTimeout;
    private Properties properties;
}