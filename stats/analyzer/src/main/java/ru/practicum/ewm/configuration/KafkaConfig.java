package ru.practicum.ewm.configuration;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
@ConfigurationProperties("analyzer.kafka")
public class KafkaConfig {
    private final Map<String, KafkaConfigConsumer> consumers;

    public KafkaConfig(Map<String, String> commonProperties, List<KafkaConfigConsumer> consumers) {
        this.consumers = consumers
                .stream()
                .peek(config -> {
                    Properties mergedProps = new Properties();
                    mergedProps.putAll(commonProperties);
                    mergedProps.putAll(config.getProperties());
                    config.setProperties(mergedProps);
                })
                .collect(Collectors.toMap(KafkaConfigConsumer::getType, Function.identity()));
    }
}