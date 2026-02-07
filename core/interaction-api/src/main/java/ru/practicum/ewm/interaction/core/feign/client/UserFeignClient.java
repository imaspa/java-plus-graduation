package ru.practicum.ewm.interaction.core.feign.client;

import org.springframework.cloud.openfeign.FeignClient;
import ru.practicum.ewm.interaction.core.feign.contract.UserFeignContract;

@FeignClient(name = "user-service", path = "/internal/users")
public interface UserFeignClient extends UserFeignContract {
}
