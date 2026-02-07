package ru.practicum.ewm.comment.config;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;
import ru.practicum.ewm.interaction.core.feign.client.EventFeignClient;
import ru.practicum.ewm.interaction.core.feign.client.UserFeignClient;

@Configuration
@EnableFeignClients(clients = {
        UserFeignClient.class,
        EventFeignClient.class
})
public class FeignClientConfig {
}
