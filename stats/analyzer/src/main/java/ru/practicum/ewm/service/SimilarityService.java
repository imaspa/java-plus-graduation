package ru.practicum.ewm.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.mapper.SimilarityMapper;
import ru.practicum.ewm.models.Similarity;
import ru.practicum.ewm.repositories.SimilarityRepository;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SimilarityService {
    private final SimilarityRepository repository;
    private final SimilarityMapper mapper;

    public void saveSimilarity(EventSimilarityAvro request) {
        repository.save(mapper.toEntity(request));
    }

    @Transactional(readOnly = true)
    public List<Similarity> getSimilarToEvent(Long eventId) {
        List<Similarity> similarities = findByEventAIdOrEventBId(eventId);

        return similarities.stream()
                .map(s -> {
                    if (eventId.equals(s.getEventA())) {
                        return s;
                    } else {
                        return swapEvents(s);
                    }
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Similarity> getSimilarToEvents(List<Long> eventIds) {
        List<Similarity> similarities = findByEventAIdInOrEventBIdIn(eventIds);

        return similarities.stream()
                .map(s -> {
                    if (eventIds.contains(s.getEventA())) {
                        return s;
                    } else {
                        return swapEvents(s);
                    }
                })
                .collect(Collectors.toList());
    }

    private List<Similarity> findByEventAIdOrEventBId(Long eventId) {
        List<Similarity> result = new ArrayList<>();
        result.addAll(repository.findByEventA(eventId));
        result.addAll(repository.findByEventB(eventId));
        return result;
    }

    private List<Similarity> findByEventAIdInOrEventBIdIn(List<Long> eventIds) {
        List<Similarity> result = new ArrayList<>();
        result.addAll(repository.findByEventAIn(eventIds));
        result.addAll(repository.findByEventBIn(eventIds));
        return result;
    }

    private Similarity swapEvents(Similarity similarity) {
        return new Similarity(
                similarity.getId(),
                similarity.getEventB(),
                similarity.getEventA(),
                similarity.getScore(),
                similarity.getTimestamp()
        );
    }
}
