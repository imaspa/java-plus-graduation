package ru.practicum.ewm.interaction.core.feign.client;

import org.springframework.cloud.openfeign.FeignClient;
import ru.practicum.ewm.interaction.core.feign.contract.RequestFeignContract;

@FeignClient(name = "request-service", path = "/internal/request")
public interface RequestFeignClient extends RequestFeignContract {
}
