package ru.practicum.ewm.core.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.ewm.core.RequestService;
import ru.practicum.ewm.interaction.core.constant.RequestStatus;
import ru.practicum.ewm.interaction.core.feign.contract.RequestFeignContract;

@RestController
@RequestMapping(path = "/internal/request")
@RequiredArgsConstructor
@Slf4j
public class UserEventsRequestControllerInternal implements RequestFeignContract {
    private final RequestService service;

    @Override
    public Long countByEventIdAndStatus(Long eventId, RequestStatus status) {
        return service.countByEventIdAndStatus(eventId, status);
    }

    @Override
    public Boolean checkUserParticipation(Long userId, Long eventId) {
        return service.checkUserParticipation(userId, eventId);
    }
}
