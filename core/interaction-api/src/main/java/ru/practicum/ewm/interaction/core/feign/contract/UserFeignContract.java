package ru.practicum.ewm.interaction.core.feign.contract;

import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.ewm.interaction.core.dto.user.UserDto;

import java.util.List;

public interface UserFeignContract {
    @GetMapping
    List<UserDto> getUsers(@RequestParam(required = false) List<Long> ids);

    @GetMapping("/{userId}")
    UserDto getUser(@Positive @PathVariable Long userId);

    @GetMapping("/isExists/{userId}")
    Boolean isExists(@Positive @PathVariable Long userId);

    @DeleteMapping("/{userId}")
    void delete(@Positive @PathVariable Long userId);
}
