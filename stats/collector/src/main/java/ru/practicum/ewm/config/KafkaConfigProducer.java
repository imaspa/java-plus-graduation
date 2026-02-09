package ru.practicum.ewm.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.Properties;

@Getter
@Setter
@AllArgsConstructor
public class KafkaConfigProducer {
    private String topic;
    private Properties properties;
}