package ru.practicum.ewm.comment.core.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.ewm.comment.core.CommentService;
import ru.practicum.ewm.interaction.core.dto.comment.CommentDto;
import ru.practicum.ewm.interaction.core.feign.contract.CommentFeignContract;

import java.util.List;


@RestController
@RequestMapping(path = "/internal/comments")
@RequiredArgsConstructor
@Slf4j
public class CommentControllerInternal implements CommentFeignContract {
    private final CommentService commentService;

    @Override
    public List<CommentDto> findAllCommentsForEvent(Long eventId) {
        return commentService.findAllCommentsForEvent(eventId);
    }
}
