package ru.practicum.ewm.interaction.core.feign.contract;

import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.practicum.ewm.interaction.core.dto.event.EventFullDto;

public interface EventFeignContract {

    @GetMapping("/{eventId}")
    EventFullDto getEvent(@Positive @PathVariable Long eventId);

    @GetMapping("/isExists/{eventId}")
    Boolean isExists(@Positive @PathVariable Long eventId);

    @DeleteMapping("/{eventId}")
    void delete(@Positive @PathVariable Long eventId);
}
