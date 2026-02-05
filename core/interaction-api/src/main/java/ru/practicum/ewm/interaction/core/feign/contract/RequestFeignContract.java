package ru.practicum.ewm.interaction.core.feign.contract;

import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.ewm.interaction.core.constant.RequestStatus;

public interface RequestFeignContract {

    @GetMapping("/{eventId}")
    Long countByEventIdAndStatus(@Positive @PathVariable Long eventId, @RequestParam RequestStatus status);


}
