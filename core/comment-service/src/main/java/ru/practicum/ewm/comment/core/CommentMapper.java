package ru.practicum.ewm.comment.core;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import ru.practicum.ewm.comment.core.model.Comment;
import ru.practicum.ewm.interaction.core.config.CommonMapperConfiguration;
import ru.practicum.ewm.interaction.core.dto.comment.CommentDto;
import ru.practicum.ewm.interaction.core.dto.comment.CommentUpdateDto;


@Mapper(config = CommonMapperConfiguration.class)
public interface CommentMapper {

    CommentDto toDto(Comment entity);

    @BeanMapping(ignoreByDefault = true, nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "updated", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "text", source = "dto.text")
    @Mapping(target = "deleted", source = "dto.deleted")
    Comment mapEntityFromDto(@MappingTarget Comment entity, CommentUpdateDto dto);

}
