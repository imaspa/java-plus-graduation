package ru.practicum.ewm.request.core;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.ewm.interaction.core.constant.RequestStatus;
import ru.practicum.ewm.request.core.model.Request;

import java.util.List;
import java.util.Optional;

public interface RequestRepository extends JpaRepository<Request, Long> {
    List<Request> findByRequesterId(Long requesterId);

    List<Request> findByEventId(Long eventId);

    long countByEventIdAndStatus(Long eventId, RequestStatus status);

    Optional<Request> findByEventIdAndRequesterId(Long eventId, Long requesterId);

    List<Request> findByEventIdAndStatus(Long eventId, RequestStatus status);

    List<Request> findAllByIdInAndStatus(List<Long> ids, RequestStatus status);
}
