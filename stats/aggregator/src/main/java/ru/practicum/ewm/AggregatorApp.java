package ru.practicum.ewm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import ru.practicum.ewm.aggregator.config.KafkaConfig;

@SpringBootApplication
@EnableDiscoveryClient
@EnableConfigurationProperties(KafkaConfig.class)
public class AggregatorApp {

    public static void main(String[] args) {
        SpringApplication.run(AggregatorApp.class, args);
    }
}
