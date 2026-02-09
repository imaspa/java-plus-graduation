package ru.practicum.ewm.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.ewm.filter.EventsFilter;
import ru.practicum.ewm.interaction.core.dto.event.EventFullDto;
import ru.practicum.ewm.interaction.core.dto.event.EventShortDto;
import ru.practicum.ewm.service.EventService;

import java.util.List;

@RestController
@RequestMapping(path = "/events")
@RequiredArgsConstructor
@Slf4j
public class EventController {
    private static final String X_EWM_USER_ID = "X-EWM-USER-ID";
    private final EventService service;

    @GetMapping
    public List<EventShortDto> find(
            @ParameterObject EventsFilter filter,
            @PageableDefault(page = 0, size = 10) Pageable pageable, HttpServletRequest request) {
        return service.findPublicEventsWithFilter(filter, pageable, request);
    }

    @GetMapping("/{id}")
    public EventFullDto findById(@PathVariable @Positive Long id, @RequestHeader(X_EWM_USER_ID) Long userId) {
        return service.findPublicEventById(id, userId);
    }

    @GetMapping("/recommendations")
    public List<EventShortDto> getRecommendation(@RequestParam Long max, @RequestHeader(X_EWM_USER_ID) Long userId) {
        return service.getRecommendations(max, userId);
    }

    @PutMapping("/{eventId}/like")
    public void likeEvent(@PathVariable Long eventId, @RequestHeader(X_EWM_USER_ID) Long userId) {
        service.addLike(eventId, userId);
    }

}
