package ru.practicum.ewm.comment.core;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import ru.practicum.ewm.comment.core.model.Comment;
import ru.practicum.ewm.interaction.core.dto.comment.CommentDto;
import ru.practicum.ewm.interaction.core.dto.comment.CommentUpdateDto;
import ru.practicum.ewm.interaction.core.dto.event.EventFullDto;
import ru.practicum.ewm.interaction.core.dto.user.UserDto;
import ru.practicum.ewm.interaction.core.exception.ConditionsException;
import ru.practicum.ewm.interaction.core.exception.NotFoundException;
import ru.practicum.ewm.interaction.core.feign.FeignClientWrapper;
import ru.practicum.ewm.interaction.core.feign.client.EventFeignClient;
import ru.practicum.ewm.interaction.core.feign.client.UserFeignClient;
import ru.practicum.ewm.interaction.core.interfaceValidation.CreateValidation;
import ru.practicum.ewm.interaction.core.interfaceValidation.UpdateValidation;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Validated
@Slf4j
public class CommentService {
    private final CommentRepository repository;
    private final CommentMapper mapper;

    private final UserFeignClient userClient;
    private final EventFeignClient eventClient;

    @Transactional(readOnly = true)
    public CommentDto findById(Long id) throws ConditionsException {
        return mapper.toDto(getCommentOrThrow(id));
    }

    @Transactional
    @Validated(CreateValidation.class)
    public CommentDto create(@Valid CommentUpdateDto entity, Long userId) throws ConditionsException {
        var comment = Comment.builder()
                .authorId(getUserOrThrow(userId).getId())
                .eventId(getEventOrThrow(entity.getEventId()).getId())
                .text(entity.getText())
                .deleted(false)
                .created(LocalDateTime.now())
                .updated(LocalDateTime.now())
                .build();
        log.info("Создание нового комментария к событию с id={} пользователем с id={}", entity.getEventId(), userId);
        return mapper.toDto(repository.save(comment));
    }

    @Transactional
    @Validated(UpdateValidation.class)
    public CommentDto update(@Valid CommentUpdateDto dto, Long commentId, Long userId) throws ConditionsException {
        var comment = getCommentOrThrow(commentId);
        if (!dto.getIsAdmin() && !isAuthorComment(comment.getAuthorId(), userId)) {
            throw new ConditionsException("Вы не можете редактировать данный комментарий");
        }
        comment = mapper.mapEntityFromDto(comment, dto);
        log.info("Обновление комментария к событию с id={} пользователем с id {}", comment.getEventId(), userId);
        return mapper.toDto(repository.save(comment));
    }

    @Transactional
    public void delete(Long commentId, Long userId) throws ConditionsException {
        CommentUpdateDto dto = CommentUpdateDto.builder().deleted(true).build();
        update(dto, commentId, userId);
    }

    @Transactional
    public void deleteAdmin(Long commentId) throws ConditionsException {
        CommentUpdateDto dto = CommentUpdateDto.builder()
                .deleted(true)
                .isAdmin(true)
                .build();
        update(dto, commentId, null);
    }

    private boolean isAuthorComment(Long author, Long userId) {
        if (userId == null || author == null) {
            return false;
        }
        return Objects.equals(author, userId);
    }


    @Transactional(readOnly = true)
    public List<CommentDto> findAllCommentsForEvent(Long eventId) {

        Boolean isEventExists = FeignClientWrapper.callWithRequest(
                () -> eventClient.isExists(eventId),
                eventId,
                "Мероприятие"
        );

        if (!isEventExists) {
            throw new NotFoundException("Отсутствует событие с id=" + eventId);
        }

        log.info("Получаем комментарии по событию с id={}", eventId);
        var comments = repository.findCommentsByEvent(eventId).orElse(new ArrayList<>());

        log.info("Возвращаем {} комментариев события с id={}", comments.size(), eventId);
        return comments.stream()
                .map(mapper::toDto)
                .toList();
    }

    private Comment getCommentOrThrow(Long commentId) {
        return repository.findByIdAndDeletedFalse(commentId)
                .orElseThrow(() -> new NotFoundException("Комментарий с id=" + commentId + " не найден."));
    }

    private UserDto getUserOrThrow(Long userId) {
        UserDto requester = FeignClientWrapper.callWithRequest(
                () -> userClient.getUser(userId),
                userId,
                "Пользователь"
        );
        return requester;
    }

    private EventFullDto getEventOrThrow(Long eventId) {
        EventFullDto event = FeignClientWrapper.callWithRequest(
                () -> eventClient.getEvent(eventId),
                eventId,
                "Мероприятие"
        );
        return event;
    }
}
