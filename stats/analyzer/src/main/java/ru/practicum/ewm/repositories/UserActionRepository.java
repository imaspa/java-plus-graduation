package ru.practicum.ewm.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.ewm.models.UserAction;

import java.util.List;

public interface UserActionRepository extends JpaRepository<UserAction, Long> {

    List<UserAction> findAllByUserId(Long userId);

    List<UserAction> findByEventIdIn(List<Long> eventIds);
}
