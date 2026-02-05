package ru.practicum.ewm.request.core;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.interaction.core.constant.EventState;
import ru.practicum.ewm.interaction.core.constant.RequestStatus;
import ru.practicum.ewm.interaction.core.dto.event.EventFullDto;
import ru.practicum.ewm.interaction.core.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.interaction.core.dto.request.EventRequestStatusUpdateResult;
import ru.practicum.ewm.interaction.core.dto.request.ParticipationRequestDto;
import ru.practicum.ewm.interaction.core.dto.user.UserDto;
import ru.practicum.ewm.interaction.core.exception.ConditionsException;
import ru.practicum.ewm.interaction.core.exception.ConflictException;
import ru.practicum.ewm.interaction.core.exception.NotFoundException;
import ru.practicum.ewm.interaction.core.feign.FeignClientWrapper;
import ru.practicum.ewm.interaction.core.feign.client.EventFeignClient;
import ru.practicum.ewm.interaction.core.feign.client.UserFeignClient;
import ru.practicum.ewm.request.core.model.Request;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequestService {

    private final UserFeignClient userClient;
    private final EventFeignClient eventClient;

    private final RequestRepository repository;
    private final RequestMapper mapper;


    @Transactional
    public ParticipationRequestDto create(Long userId, Long eventId) throws ConditionsException, ConflictException {
        if (repository.findByEventIdAndRequesterId(eventId, userId).isPresent()) {
            throw new ConflictException("Запрос уже существует");
        }

        UserDto requester = FeignClientWrapper.callWithRequest(
                () -> userClient.getUser(userId),
                userId,
                "Пользователь" //new NotFoundException("Пользователь с id=" + userId + " не найден"));
        );

        EventFullDto event = FeignClientWrapper.callWithRequest(
                () -> eventClient.getEvent(eventId),
                eventId,
                "Мероприятие" //new NotFoundException("Мероприятие с id=" + eventId + " не найдено"));
        );


        if (Objects.equals(event.getInitiator().getId(), userId)) {
            throw new ConflictException("Нельзя подать заявку на своё мероприятие");
        }

        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Подать заявку можно только на опубликованные мероприятия");
        }

        Long limit = event.getParticipantLimit();
        if (limit != null && limit > 0) {
            long confirmedCount = repository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
            if (confirmedCount >= limit) {
                throw new ConflictException("Достигнут лимит участников");
            }
        }

        Request request = Request.builder()
                .created(LocalDateTime.now())
                .eventId(event.getId())
                .requesterId(requester.getId())
                .status(RequestStatus.PENDING)
                .build();

        Boolean moderation = event.getRequestModeration();
        if (!moderation || event.getParticipantLimit() == 0) {
            request.setStatus(RequestStatus.CONFIRMED);
        }
//        if (moderation != null && !moderation) {
//            request.setStatus(RequestStatus.CONFIRMED);
//        }
        request = repository.save(request);
        log.info("Создан запрос, id = {}", request.getId());
        return mapper.toDto(request);
    }


    @Transactional(readOnly = true)
    public List<ParticipationRequestDto> getRequestsByUser(Long userId) {
        Boolean isUserExists = FeignClientWrapper.callWithRequest(
                () -> userClient.isExists(userId),
                userId,
                "Пользователь" //NotFoundException("Пользователь с id=" + userId + " не найден");
        );
        if (!isUserExists) {
            throw new NotFoundException("Пользователь с id=" + userId + " не найден");
        }
        return repository.findByRequesterId(userId).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }


    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) throws ConditionsException {
        Request request = repository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Заявка не найдена"));
        if (!Objects.equals(request.getRequesterId(), userId)) {
            throw new ConditionsException("Только владелец может отменить заявку");
        }
        request.setStatus(RequestStatus.CANCELED);
        request = repository.save(request);
        log.info("Отменен запрос id = {}", requestId);
        return mapper.toDto(repository.save(request));
    }


    @Transactional(readOnly = true)
    public List<ParticipationRequestDto> getRequestsForEventOwner(Long ownerId, Long eventId) throws ConditionsException {
//        Event event = eventClient.findById(eventId)
//                .orElseThrow(() -> new NotFoundException("Мероприятие с id=" + eventId + " не найдено"));
        EventFullDto event = FeignClientWrapper.callWithRequest(
                () -> eventClient.getEvent(eventId),
                eventId,
                "Мероприятие" //new NotFoundException("Мероприятие с id=" + eventId + " не найдено"));
        );


        if (!Objects.equals(event.getInitiator().getId(), ownerId)) {
            throw new ConditionsException("Только владелец мероприятия может просматривать запросы на это мероприятие");
        }
        return repository.findByEventId(eventId).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }


    @Transactional
    public EventRequestStatusUpdateResult updateRequestStatus(
            Long ownerId,
            Long eventId,
            EventRequestStatusUpdateRequest updateDto) throws ConditionsException, ConflictException {

        EventFullDto event = FeignClientWrapper.callWithRequest(
                () -> eventClient.getEvent(eventId),
                eventId,
                "Мероприятие" //new NotFoundException("Мероприятие с id=" + eventId + " не найдено"));
        );

        if (!Objects.equals(event.getInitiator().getId(), ownerId)) {
            throw new ConditionsException("Только владелец мероприятия может изменять статус запроса");
        }

        if (updateDto.getRequestIds() == null || updateDto.getRequestIds().isEmpty()) {
            return new EventRequestStatusUpdateResult(List.of(), List.of());
        }
        if (updateDto.getStatus() == null) {
            throw new ConditionsException("Не указан статус");
        }

        if (updateDto.getStatus() == RequestStatus.CONFIRMED &&
                (!event.getRequestModeration() || event.getParticipantLimit() == 0)) {
            throw new ConditionsException("Подтверждение заявок не требуется");
        }

        Long freeLimit = event.getParticipantLimit();
        if (freeLimit != null && freeLimit > 0) {
            freeLimit = freeLimit - repository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
            if (freeLimit <= 0) {
                throw new ConflictException("Лимит по заявкам на данное событие уже достигнут");
            }
        }

        List<Request> requests =
                repository.findAllByIdInAndStatus(updateDto.getRequestIds(), RequestStatus.PENDING);
        Map<Long, Request> map = requests.stream()
                .collect(Collectors.toMap(Request::getId, r -> r));

        List<Long> notFound = updateDto.getRequestIds().stream()
                .filter(id -> !map.containsKey(id))
                .toList();

        if (!notFound.isEmpty()) {
            throw new ConflictException("Не найдены заявки: " + notFound);
        }

        List<Request> toUpdate = new ArrayList<>();
        List<ParticipationRequestDto> confirmed = new ArrayList<>();
        List<ParticipationRequestDto> rejected = new ArrayList<>();

        for (Long id : updateDto.getRequestIds()) {
            Request r = map.get(id);

            if (updateDto.getStatus() == RequestStatus.CONFIRMED) {
                if (freeLimit != null && freeLimit <= 0) {
                    r.setStatus(RequestStatus.REJECTED);
                    rejected.add(mapper.toDto(r));
                    log.info("Заявка {} будет отклонена", r.getId());
                } else {
                    if (freeLimit != null) {
                        freeLimit--;
                    }
                    r.setStatus(RequestStatus.CONFIRMED);
                    confirmed.add(mapper.toDto(r));
                    log.info("Заявка {} будет подтверждена", r.getId());
                }
            } else if (updateDto.getStatus() == RequestStatus.REJECTED) {
                r.setStatus(RequestStatus.REJECTED);
                rejected.add(mapper.toDto(r));
            } else {
                throw new ConflictException("Доступны только статусы CONFIRMED или REJECTED");
            }
            toUpdate.add(r);
        }

        if (!toUpdate.isEmpty()) {
            repository.saveAll(toUpdate);
            log.info("Список заявок обновлен");
        }

        if (freeLimit != null && freeLimit == 0 && event.getParticipantLimit() > 0) {
            List<Request> pendingRequests =
                    repository.findByEventIdAndStatus(eventId, RequestStatus.PENDING);
            if (!pendingRequests.isEmpty()) {
                pendingRequests.forEach(r -> r.setStatus(RequestStatus.REJECTED));
                List<Request> rejectedRequests = repository.saveAll(pendingRequests);
                rejected.addAll(rejectedRequests.stream().map(mapper::toDto).toList());
                log.info("Был достигнут лимит заявок, все оставшиеся PENDING, переведены в REJECTED");
            }
        }

        return new EventRequestStatusUpdateResult(confirmed, rejected);
    }


    @Transactional(readOnly = true)
    public Long countByEventIdAndStatus(Long eventId, RequestStatus status) {
        return repository.countByEventIdAndStatus(eventId, status);
    }
}
