package ru.practicum.ewm.user.core;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.ewm.interaction.core.config.CommonMapperConfiguration;
import ru.practicum.ewm.interaction.core.dto.user.UserDto;
import ru.practicum.ewm.user.core.model.User;


@Mapper(config = CommonMapperConfiguration.class)
public interface UserMapper {

    UserDto toDto(User entity);

    @Mapping(target = "email", ignore = true)
    UserDto toUserDtoShort(User entity);

    User toEntity(UserDto dto);

}
