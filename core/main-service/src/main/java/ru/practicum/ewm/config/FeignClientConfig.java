package ru.practicum.ewm.config;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;
import ru.practicum.ewm.client.StatsClient;

@Configuration
@EnableFeignClients(clients = {StatsClient.class})
public class FeignClientConfig {
}
