package ru.practicum.ewm.mapper;

import org.mapstruct.Mapper;
import ru.practicum.ewm.model.Location;
import ru.practicum.ewm.interaction.core.config.CommonMapperConfiguration;
import ru.practicum.ewm.interaction.core.dto.event.EventLocationDto;

@Mapper(config = CommonMapperConfiguration.class)
public interface LocationMapper {

    Location toEntity(EventLocationDto dto);

    EventLocationDto toDto(Location entity);
}
