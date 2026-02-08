package ru.practicum.ewm.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.mapper.UserActionMapper;
import ru.practicum.ewm.models.UserAction;
import ru.practicum.ewm.repositories.UserActionRepository;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class UserActionService {
    private final UserActionMapper mapper;
    private final UserActionRepository repository;

    public void saveUserAction(UserActionAvro request) {
        repository.save(mapper.toEntity(request));
    }

    @Transactional(readOnly = true)
    public List<UserAction> getMaxWeighted(List<Long> eventIds) {
        return findMaxWeightedForEvents(eventIds);
    }

    @Transactional(readOnly = true)
    public List<UserAction> getByUser(Long userId) {
        return repository.findAllByUserId(userId);
    }

    private List<UserAction> findMaxWeightedForEvents(List<Long> eventIds) {
        List<UserAction> allActions = repository.findByEventIdIn(eventIds);

        return allActions.stream()
                .collect(Collectors.groupingBy(
                        action -> action.getEventId() + "_" + action.getUserId(),
                        Collectors.maxBy(Comparator
                                .comparing(UserAction::getActionWeight)
                                .thenComparing(UserAction::getTimestamp)
                        )
                ))
                .values()
                .stream()
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }
}
