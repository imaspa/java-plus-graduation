package ru.practicum.ewm.interaction.core.feign.contract;

import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.practicum.ewm.interaction.core.dto.comment.CommentDto;

import java.util.List;

public interface CommentFeignContract {

    @GetMapping("/{eventId}")
    List<CommentDto> findAllCommentsForEvent(@Positive @PathVariable Long eventId);

}
