package ru.practicum.ewm.service;

import com.querydsl.core.BooleanBuilder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import ru.practicum.ewm.filter.EventsFilter;
import ru.practicum.ewm.interaction.core.constant.EventState;
import ru.practicum.ewm.interaction.core.constant.EventStateAction;
import ru.practicum.ewm.interaction.core.constant.RequestStatus;
import ru.practicum.ewm.interaction.core.dto.comment.CommentDto;
import ru.practicum.ewm.interaction.core.dto.event.EventFullDto;
import ru.practicum.ewm.interaction.core.dto.event.EventNewDto;
import ru.practicum.ewm.interaction.core.dto.event.EventShortDto;
import ru.practicum.ewm.interaction.core.dto.event.EventUpdateDto;
import ru.practicum.ewm.interaction.core.dto.user.UserDto;
import ru.practicum.ewm.interaction.core.exception.ConditionsException;
import ru.practicum.ewm.interaction.core.exception.ConflictException;
import ru.practicum.ewm.interaction.core.exception.NotFoundException;
import ru.practicum.ewm.interaction.core.feign.FeignClientWrapper;
import ru.practicum.ewm.interaction.core.feign.client.CommentFeignClient;
import ru.practicum.ewm.interaction.core.feign.client.RequestFeignClient;
import ru.practicum.ewm.interaction.core.feign.client.UserFeignClient;
import ru.practicum.ewm.mapper.EventMapper;
import ru.practicum.ewm.mapper.EventMapperDep;
import ru.practicum.ewm.model.Category;
import ru.practicum.ewm.model.Event;
import ru.practicum.ewm.model.Location;
import ru.practicum.ewm.repository.CategoryRepository;
import ru.practicum.ewm.repository.EventRepository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Stream;

@Slf4j
@Service
@Validated
@RequiredArgsConstructor
public class EventService {

    private final EventRepository repository;
    private final EventMapper mapper;


    private final UserFeignClient userClient;
    private final RequestFeignClient requestClient;
    private final CommentFeignClient commentClient;

    private final LocationService locationService;
    private final CategoryRepository categoryRepository;
    private final StatsService statsService;

    @Transactional
    public EventFullDto create(@Valid EventNewDto dto, Long userId) throws ConditionsException {
        UserDto user = getUserOrThrow(userId);
        Category category = getCategoryOrThrow(dto.getCategory());
        Location location = locationService.getOrCreateLocation(dto.getLocation());

        Event event = mapper.toEntityWithNewDto(dto, user, category, location);
        event = event.toBuilder().createdOn(LocalDateTime.now()).build();
        event = repository.save(event);
        log.info("Создано событие с id = {}", event.getId());

        return mapper.toDto(event);
    }

    @Transactional
    public EventFullDto update(Long userId, Long eventId, @Valid EventUpdateDto dto) throws ConditionsException, ConflictException {
        if (!userIsExist(userId)) {
            throw new NotFoundException("Пользователь не найден");
        }
        Event event = getEventOrThrow(eventId, userId);
        if (event.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Нельзя изменять опубликованное событие");
        }

        Category category = (dto.getCategory() == null) ? null : getCategoryOrThrow(dto.getCategory());
        Location location = (dto.getLocation() == null) ? null : locationService.getOrCreateLocation(dto.getLocation());
        EventState state = dto.getStateAction() == null ?
                null :
                switch (dto.getStateAction()) {
                    case SEND_TO_REVIEW -> EventState.PENDING;
                    case CANCEL_REVIEW -> EventState.CANCELED;
                    case PUBLISH_EVENT, REJECT_EVENT -> null;
                };
        event = mapper.toEntityWithUpdateDto(event, dto, category, location, state);
        event = repository.save(event);
        log.info("Обновлено событие с id = {}", eventId);

        Long calcConfirmedRequests = getConfirmedRequests(eventId);
        Long calcView = statsService.getViewsForEvent(eventId);
        return mapper.toDto(event).toBuilder()
                .confirmedRequests(calcConfirmedRequests)
                .views(calcView)
                .comments(getComments(eventId))
                .build();
    }

    @Transactional
    public EventFullDto updateAdmin(Long eventId, @Valid EventUpdateDto dto) throws ConditionsException, ConflictException {
        Event event = getEventOrThrow(eventId);

        EventState currentState = event.getState();
        EventStateAction action = dto.getStateAction();
        LocalDateTime newDate = dto.getEventDate();

        if (action != null) {
            if (action == EventStateAction.PUBLISH_EVENT) {
                if (currentState != EventState.PENDING) {
                    throw new ConflictException("Можно публиковать только события в состоянии PENDING");
                }
            } else if (action == EventStateAction.REJECT_EVENT) {
                if (currentState == EventState.PUBLISHED) {
                    throw new ConflictException("Нельзя отклонить опубликованное событие");
                }
            }
        }
        if (dto.getEventDate() != null) {
            validateEventDate(newDate, action, currentState, event);
        }

        EventState state = action == null ?
                null :
                switch (action) {
                    case PUBLISH_EVENT -> EventState.PUBLISHED;
                    case REJECT_EVENT -> EventState.CANCELED;
                    case SEND_TO_REVIEW, CANCEL_REVIEW -> null;
                };

        LocalDateTime eventDate = dto.getEventDate() == null ? null : newDate;
        Category category = (dto.getCategory() == null) ? null : getCategoryOrThrow(dto.getCategory());
        Location location = (dto.getLocation() == null) ? null : locationService.getOrCreateLocation(dto.getLocation());
        dto = dto.toBuilder().eventDate(eventDate).build();

        event = mapper.toEntityWithUpdateDto(event, dto, category, location, state);
        event = repository.save(event);
        log.info("Администратор обновил событие с id = {}", eventId);

        Long calcConfirmedRequests = getConfirmedRequests(eventId);
        Long calcView = statsService.getViewsForEvent(eventId);
        return mapper.toDto(event).toBuilder()
                .confirmedRequests(calcConfirmedRequests)
                .views(calcView)
                .comments(getComments(eventId))
                .build();

    }

    @Transactional(readOnly = true)
    public EventFullDto findByUserIdAndEventId(Long userId, Long eventId) throws ConditionsException {
        if (!userIsExist(userId)) {
            throw new NotFoundException("Пользователь не найден");
        }
        Event event = getEventOrThrow(eventId, userId);
        Long calcConfirmedRequests = getConfirmedRequests(eventId);
        Long calcView = statsService.getViewsForEvent(eventId);
        log.info("Получено событие {} пользователя {}", eventId, userId);
        return mapper.toDto(event).toBuilder()
                .confirmedRequests(calcConfirmedRequests)
                .views(calcView)
                .comments(getComments(eventId))
                .build();

    }

    @Transactional(readOnly = true)
    public EventFullDto findPublicEventById(Long eventId, HttpServletRequest request) {
        Event event = getEventOrThrow(eventId, EventState.PUBLISHED);
        Long calcConfirmedRequests = getConfirmedRequests(eventId);
        Long calcView = statsService.getViewsForEvent(eventId);

        statsService.saveHit(
                "main-service",
                request.getRequestURI(),
                request.getRemoteAddr(),
                LocalDateTime.now()
        );

        log.info("Получено публичное событие {}", eventId);
        return mapper.toDto(event).toBuilder()
                .confirmedRequests(calcConfirmedRequests)
                .views(calcView)
                .comments(getComments(eventId))
                .build();
    }

    @Transactional(readOnly = true)
    public List<EventShortDto> findByUserId(Long userId, Pageable pageable) throws ConditionsException {
        if (!userIsExist(userId)) {
            throw new NotFoundException("Пользователь не найден");
        }

        return repository.findAllByInitiatorId(userId, pageable)
                .stream()
                .map(event -> EventMapperDep.eventToShortDto(
                        event,
                        getConfirmedRequests(event.getId()),
                        statsService.getViewsForEvent(event.getId())
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EventShortDto> findPublicEventsWithFilter(@Valid EventsFilter filter, Pageable pageable, HttpServletRequest request) {
        statsService.saveHit(
                "main-service",
                request.getRequestURI(),
                request.getRemoteAddr(),
                LocalDateTime.now()
        );
        return findEventsWithFilterInternal(
                filter,
                pageable,
                false,
                (event, viewsMap) -> {
                    String uri = "/events/" + event.getId();
                    Long views = viewsMap.getOrDefault(uri, 0L);
                    return EventMapperDep.eventToShortDto(event, getConfirmedRequests(event.getId()), views);
                }
        );
    }

    @Transactional(readOnly = true)
    public List<EventFullDto> findAdminEventsWithFilter(@Valid EventsFilter filter, Pageable pageable) {
        return findEventsWithFilterInternal(
                filter,
                pageable,
                true,
                (event, viewsMap) -> {
                    String uri = "/events/" + event.getId();
                    Long views = viewsMap.getOrDefault(uri, 0L);
                    return EventMapperDep.eventToFullDto(event, getConfirmedRequests(event.getId()), views,
                            getComments(event.getId()));
                }
        );
    }

    private <T> List<T> findEventsWithFilterInternal(
            EventsFilter filter,
            Pageable pageable,
            Boolean forAdmin,
            BiFunction<Event, Map<String, Long>, T> mapper) {

        BooleanBuilder predicate = EventPredicateBuilder.buildPredicate(filter, forAdmin);

        if (!forAdmin) {
            pageable = PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    Sort.by(Sort.Direction.DESC, "eventDate")

            );
        }
        Page<Event> eventsPage = repository.findAll(predicate, pageable);

        if (eventsPage.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> uris = eventsPage.stream()
                .map(e -> "/events/" + e.getId())
                .toList();

        Map<String, Long> viewsUriMap = statsService.getViewsForUris(uris);
        Stream<Event> eventStream = eventsPage.stream();
        List<T> result = eventStream
                .map(e -> mapper.apply(e, viewsUriMap))
                .toList();

        log.info("Найдено {} событий в режиме {}", result.size(), forAdmin ? "ADMIN" : "PUBLIC");
        return result;
    }

    private UserDto getUserOrThrow(Long userId) throws ConditionsException {
        UserDto requester = FeignClientWrapper.callWithRequest(
                () -> userClient.getUser(userId),
                userId,
                "Пользователь"
        );
        return requester;
    }

    private Category getCategoryOrThrow(Long catId) throws ConditionsException {
        return categoryRepository.findById(catId)
                .orElseThrow(() -> new ConditionsException("Категория с id=" + catId + " не найдена"));
    }

    private Event getEventOrThrow(Long eventId) {
        return repository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));
    }

    private Event getEventOrThrow(Long eventId, Long userId) throws ConditionsException {
        Event event = getEventOrThrow(eventId);
        if (!event.getInitiatorId().equals(userId)) {
            throw new ConditionsException("Пользователь не является инициатором события");
        }
        return event;
    }

    private Event getEventOrThrow(Long eventId, EventState state) {
        Event event = getEventOrThrow(eventId);
        if (event.getState() != state) {
            throw new NotFoundException("Событие не в состоянии " + state);
        }
        return event;
    }

    private Long getConfirmedRequests(Long eventId) {
        return FeignClientWrapper.call(
                () -> requestClient.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED),
                eventId.toString(),
                "Событие"
        );
    }

    private List<CommentDto> getComments(Long eventId) {
        return commentClient.findAllCommentsForEvent(eventId);
    }

    @Transactional(readOnly = true)
    public Boolean userIsExist(Long userId) {
        Boolean isUserExists = FeignClientWrapper.callWithRequest(
                () -> userClient.isExists(userId),
                userId,
                "Пользователь"
        );
        return isUserExists;
    }

    private void validateEventDate(LocalDateTime newDate, EventStateAction action, EventState currentState, Event event) throws ConditionsException {
        if (action == EventStateAction.PUBLISH_EVENT) {
            if (newDate.isBefore(LocalDateTime.now().plusHours(1))) {
                throw new ConditionsException("Дата начала должна быть не ранее чем через 1 час при публикации");
            }
        } else if (currentState == EventState.PUBLISHED && event.getPublishedOn() != null) {
            if (newDate.isBefore(event.getPublishedOn().plusHours(1))) {
                throw new ConditionsException("Дата начала должна быть не ранее чем через 1 час после публикации");
            }
        }
    }

    @Transactional(readOnly = true)
    public EventFullDto getEvent(Long eventId) {
        Event event = getEventOrThrow(eventId);
        Long userId = event.getInitiatorId();
        UserDto user = FeignClientWrapper.callWithRequest(
                () -> userClient.getUser(userId),
                userId,
                "Пользователь"
        );
        return mapper.toDto(event, user);
    }

    @Transactional(readOnly = true)
    public Boolean isExists(Long eventId) {
        return repository.existsById(eventId);
    }

    @Transactional()
    public void delete(Long eventId) {
        repository.deleteById(eventId);
    }
}