package ru.practicum.ewm.interaction.core.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.ewm.interaction.core.constant.EventState;
import ru.practicum.ewm.interaction.core.dto.category.CategoryDto;
import ru.practicum.ewm.interaction.core.dto.comment.CommentDto;
import ru.practicum.ewm.interaction.core.dto.user.UserDto;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class EventFullDto {

    private Long id;

    private String annotation;

    private CategoryDto category;

    private Long confirmedRequests;

    private LocalDateTime createdOn;

    private String description;

    private LocalDateTime eventDate;

    private UserDto initiator;

    private EventLocationDto location;

    private Boolean paid;

    private Long participantLimit;

    private LocalDateTime publishedOn;

    private Boolean requestModeration;

    private EventState state;

    private String title;

    private Long views;

    private List<CommentDto> comments;
}
