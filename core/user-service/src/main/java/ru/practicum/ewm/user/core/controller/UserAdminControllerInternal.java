package ru.practicum.ewm.user.core.controller;

import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.ewm.interaction.core.dto.user.UserDto;
import ru.practicum.ewm.interaction.core.feign.contract.UserFeignContract;
import ru.practicum.ewm.user.core.UserService;

import java.util.List;

@RestController
@RequestMapping(path = "/internal/users")
@RequiredArgsConstructor
@Validated
@Slf4j
public class UserAdminControllerInternal implements UserFeignContract {
    private final UserService service;

    @Override
    public List<UserDto> getUsers(@RequestParam(required = false) List<Long> ids) {
        return service.findUsers(ids, Pageable.unpaged());
    }

    @Override
    public UserDto getUser(@Positive @PathVariable Long userId) {
        return service.findUsers(List.of(userId), Pageable.unpaged()).getFirst();
    }

    @Override
    public Boolean isExists(@Positive @PathVariable Long userId) {
        return service.userIsExist(userId);
    }

    @Override
    public void delete(@Positive @PathVariable Long userId) {
        service.delete(userId);
    }
}
