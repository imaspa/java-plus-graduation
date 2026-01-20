package ru.practicum.ewm.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestClient;

@Configuration
@RequiredArgsConstructor
public class RestClientConfig {
    private final DiscoveryClient discoveryClient;
    private final RetryTemplate retryTemplate;

    @Value("${stats-server.id:stats-server}")
    private String statsServiceId;


    @Bean
    public RestClient restClient(RestClient.Builder builder) {
        return builder
                .baseUrl(getBaseUrl())
                .build();
    }

    public String getBaseUrl() {
        ServiceInstance instance = retryTemplate.execute(ctx -> getInstance());
        return instance.getUri().toString();
    }

    public ServiceInstance getInstance() {
        return discoveryClient.getInstances(statsServiceId)
                .stream().findAny()
                .orElseThrow(() -> new IllegalStateException("%s service unavailable".formatted(statsServiceId)));
    }
}
