package ru.practicum.ewm.interaction.core.feign.client;

import org.springframework.cloud.openfeign.FeignClient;
import ru.practicum.ewm.interaction.core.feign.contract.EventFeignContract;

@FeignClient(name = "event-service", path = "/internal/event")
public interface EventFeignClient extends EventFeignContract {
}
