package ru.practicum.ewm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.models.Similarity;
import ru.practicum.ewm.models.UserAction;
import ru.practicum.grpc.stats.eventPredictions.InteractionsCountRequestProto;
import ru.practicum.grpc.stats.eventPredictions.RecommendedEventProto;
import ru.practicum.grpc.stats.eventPredictions.SimilarEventsRequestProto;
import ru.practicum.grpc.stats.eventPredictions.UserPredictionsRequestProto;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationService {
    private final UserActionService userActionService;
    private final SimilarityService similarityService;

    public List<RecommendedEventProto> getRecommendationsForUser(UserPredictionsRequestProto request) {
        Map<Long, Double> seenEvents = getUserSeenEvents(request.getUserId());
        Map<Long, Double> similarEvents = findSimilarEvents(seenEvents.keySet());
        List<Long> candidateEvents = filterAndSortEvents(seenEvents, similarEvents, request.getMaxResults());
        Map<Long, Double> scoredEvents = calculateScores(candidateEvents, seenEvents);
        return buildRecommendations(scoredEvents);
    }

    public List<RecommendedEventProto> getSimilarEvents(SimilarEventsRequestProto request) {
        log.info("Поиск похожих событий с исключением уже просмотренных");
        Set<Long> seenEvents = getUserSeenEventIds(request.getUserId());
        Map<Long, Double> similarEvents = similarityService.getSimilarToEvent(request.getEventId())
                .stream()
                .collect(Collectors.toMap(Similarity::getEventB, Similarity::getScore));

        return similarEvents.entrySet().stream()
                .filter(entry -> !seenEvents.contains(entry.getKey()))
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(request.getMaxResults())
                .map(entry -> buildRecommendation(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    public List<RecommendedEventProto> getInteractionsCount(InteractionsCountRequestProto request) {
        log.info("Рассчет веса взаимодействий с событиями");
        Map<Long, Double> eventWeights = userActionService.getMaxWeighted(request.getEventIdList())
                .stream()
                .collect(Collectors.groupingBy(
                        UserAction::getEventId,
                        Collectors.summingDouble(UserAction::getActionWeight)
                ));

        return eventWeights.entrySet().stream()
                .map(entry -> buildRecommendation(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    private Map<Long, Double> getUserSeenEvents(Long userId) {
        log.info("Получение событий с максимальным весом");
        return userActionService.getByUser(userId).stream()
                .collect(Collectors.toMap(
                        UserAction::getEventId,
                        UserAction::getActionWeight,
                        Math::max
                ));
    }

    private Set<Long> getUserSeenEventIds(Long userId) {
        log.info("Получение id событий с которыми работал юзер");
        return userActionService.getByUser(userId).stream()
                .map(UserAction::getEventId)
                .collect(Collectors.toSet());
    }

    private Map<Long, Double> findSimilarEvents(Set<Long> eventIds) {
        log.info("Поиск похожих событий");
        return similarityService.getSimilarToEvents(new ArrayList<>(eventIds)).stream()
                .collect(Collectors.toMap(Similarity::getEventB, Similarity::getScore));
    }

    private List<Long> filterAndSortEvents(Map<Long, Double> seenEvents, Map<Long,
            Double> similarEvents, Long maxResults) {
        log.info("Фильтрация событий кадидатов");
        return similarEvents.keySet().stream()
                .filter(event -> !seenEvents.containsKey(event))
                .sorted(Comparator.comparing(similarEvents::get).reversed())
                .limit(maxResults)
                .collect(Collectors.toList());
    }

    private Map<Long, Double> calculateScores(List<Long> candidateEvents, Map<Long, Double> seenEvents) {
        log.info("Скоринг кандидатов");
        return candidateEvents.stream()
                .collect(Collectors.toMap(
                        event -> event,
                        event -> calculateEventScore(event, seenEvents)
                ));
    }

    private double calculateEventScore(Long eventId, Map<Long, Double> seenEvents) {
        log.info("Рассчет рейтинга на основе ближайших событий соседей");
        List<Similarity> neighbours = similarityService.getSimilarToEvent(eventId);

        int nearestNeighbours = 10;
        double weightedSum = neighbours.stream()
                .filter(neighbour -> seenEvents.containsKey(neighbour.getEventB()))
                .sorted(Comparator.comparing(Similarity::getScore).reversed())
                .limit(nearestNeighbours)
                .mapToDouble(neighbour -> neighbour.getScore() * seenEvents.get(neighbour.getEventB()))
                .sum();

        double scoreSum = neighbours.stream()
                .filter(neighbour -> seenEvents.containsKey(neighbour.getEventB()))
                .sorted(Comparator.comparing(Similarity::getScore).reversed())
                .limit(nearestNeighbours)
                .mapToDouble(Similarity::getScore)
                .sum();

        return scoreSum == 0 ? 0 : weightedSum / scoreSum;
    }

    private List<RecommendedEventProto> buildRecommendations(Map<Long, Double> scoredEvents) {
        return scoredEvents.entrySet().stream()
                .map(entry -> buildRecommendation(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    private RecommendedEventProto buildRecommendation(Long eventId, Double score) {
        return RecommendedEventProto.newBuilder()
                .setEventId(eventId)
                .setScore(score)
                .build();
    }
}