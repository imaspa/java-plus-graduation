package ru.practicum.ewm.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.ewm.interaction.core.dto.event.EventFullDto;
import ru.practicum.ewm.interaction.core.feign.contract.EventFeignContract;
import ru.practicum.ewm.service.EventService;

@RestController
@RequestMapping(path = "/internal/event")
@RequiredArgsConstructor
@Slf4j
public class UserEventsControllerInternal implements EventFeignContract {
    private final EventService service;

    @Override
    public EventFullDto getEvent(Long eventId) {
        return service.getEvent(eventId);
    }

    @Override
    public Boolean isExists(Long eventId) {
        return service.isExists(eventId);
    }

    @Override
    public void delete(Long eventId) {
        service.delete(eventId);

    }
}
