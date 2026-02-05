package ru.practicum.ewm.request.core;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.ewm.interaction.core.config.CommonMapperConfiguration;
import ru.practicum.ewm.interaction.core.dto.request.ParticipationRequestDto;
import ru.practicum.ewm.request.core.model.Request;


@Mapper(config = CommonMapperConfiguration.class)
public interface RequestMapper {

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "event", source = "entity.eventId")
    @Mapping(target = "requester", source = "entity.requesterId")
    @Mapping(target = "status", source = "entity.status")
    @Mapping(target = "created", source = "entity.created")
    @Mapping(target = "id", source = "entity.id")
    ParticipationRequestDto toDto(Request entity);
}
