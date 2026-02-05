package ru.practicum.ewm.config;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;
import ru.practicum.ewm.interaction.core.feign.client.CommentFeignClient;
import ru.practicum.ewm.interaction.core.feign.client.EventFeignClient;
import ru.practicum.ewm.interaction.core.feign.client.RequestFeignClient;
import ru.practicum.ewm.interaction.core.feign.client.UserFeignClient;

@Configuration
@EnableFeignClients(clients = {
        UserFeignClient.class,
        EventFeignClient.class,
        RequestFeignClient.class,
        CommentFeignClient.class
})
public class FeignClientConfig {
}
