package ru.practicum.ewm.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import ru.practicum.ewm.interaction.core.constant.EventState;
import ru.practicum.ewm.model.Event;

import java.util.Collection;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long>, QuerydslPredicateExecutor<Event> {
    Page<Event> findAllByInitiatorId(Long userId, Pageable page);

    Boolean existsByCategoryId(Long categoryId);

    List<Event> findByIdInAndStateIs(Collection<Long> ids, EventState state);
}
