package ru.practicum.ewm.interaction.core.feign.client;

import org.springframework.cloud.openfeign.FeignClient;
import ru.practicum.ewm.interaction.core.feign.contract.CommentFeignContract;

@FeignClient(name = "comment-service", path = "/internal/comments")
public interface CommentFeignClient extends CommentFeignContract {
}
