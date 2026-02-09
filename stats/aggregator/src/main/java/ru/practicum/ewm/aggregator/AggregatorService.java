package ru.practicum.ewm.aggregator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.aggregator.config.KafkaConfig;
import ru.practicum.ewm.aggregator.config.KafkaConfigConsumer;
import ru.practicum.ewm.aggregator.config.KafkaConfigProducer;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AggregatorService {

    private final KafkaConfigConsumer consumerConfig;
    private final KafkaConfigProducer producerConfig;

    private final Map<TopicPartition, OffsetAndMetadata> currentOffsets = new HashMap<>();

    private final KafkaConsumer<String, UserActionAvro> consumer;
    private final KafkaProducer<String, EventSimilarityAvro> producer;


    private final Map<Long, Map<Long, Double>> eventUserWeights = new HashMap<>();
    private final Map<Long, Map<Long, Double>> scalarResultMatrix = new HashMap<>();

    @Autowired
    public AggregatorService(KafkaConfig kafkaConfig) {
        this.consumerConfig = kafkaConfig.getConsumer();
        this.producerConfig = kafkaConfig.getProducer();

        this.consumer = new KafkaConsumer<>(consumerConfig.getProperties());
        this.producer = new KafkaProducer<>(producerConfig.getProperties());

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("[Aggregation][JVM STOP]. Остановка Consumer ");
            consumer.wakeup();
        }));
    }

    public void processEvents() {
        try {
            log.info("[Aggregation] Subscribing to topic: {}", consumerConfig.getTopic());
            consumer.subscribe(List.of(consumerConfig.getTopic()));

            while (true) {
                ConsumerRecords<String, UserActionAvro> records = consumer.poll(consumerConfig.getPollTimeout());
                if (!records.isEmpty()) {
                    for (ConsumerRecord<String, UserActionAvro> record : records) {
                        log.trace("[Aggregation] [PROCESSING] хаб: {}; партиция: {}; смещение: {}", record.key(), record.partition(), record.offset());
                        List<EventSimilarityAvro> event = handleEvent(record.value());
                        event.forEach(result -> {
                            producer.send(new ProducerRecord<>(producerConfig.getTopic(), result));
                        });
                    }
                    producer.flush();
                    consumer.commitAsync();
                }
            }
        } catch (WakeupException ignored) {
        } catch (Exception e) {
            log.error("[Aggregation] [PROCESSING ERROR]", e);
        } finally {
            shutdownResources();
        }
    }


    private double getWeight(ActionTypeAvro request) {
        switch (request) {
            case VIEW:
                return 0.4;
            case REGISTER:
                return 0.8;
            case LIKE:
                return 1.0;
            default:
                return 0.0;
        }
    }

    private List<EventSimilarityAvro> updateEventWeight(Long eventId, Long userId, Double newWeight) {
        log.info("Получение или создание мапы веса по событию");
        Map<Long, Double> userWeights = eventUserWeights.computeIfAbsent(eventId, k -> new HashMap<>());
        Double currentWeight = userWeights.get(userId);

        log.info("Проверка пользователя и сравнение веса");
        if (currentWeight == null || currentWeight < newWeight) {
            List<EventSimilarityAvro> updatedSimilarities = recalculateSimilarities(
                    eventId,
                    userId,
                    newWeight,
                    currentWeight
            );
            userWeights.put(userId, newWeight);
            return updatedSimilarities;
        }
        return Collections.emptyList();
    }

    private List<EventSimilarityAvro> recalculateSimilarities(Long eventId, Long userId,
                                                              Double newWeight, Double oldWeight) {
        log.info("Получаем или создаем карту скаляров для события");
        Map<Long, Double> selfDotProducts = scalarResultMatrix.computeIfAbsent(eventId, k -> new HashMap<>());
        double currentSelfProduct = selfDotProducts.getOrDefault(eventId, 0.0);
        double weightDelta = (oldWeight == null) ? newWeight : newWeight - oldWeight;

        selfDotProducts.put(eventId, currentSelfProduct + weightDelta);

        return updateCrossDotProducts(eventId, userId, newWeight, oldWeight);
    }

    private List<EventSimilarityAvro> updateCrossDotProducts(Long updatedEventId, Long userId,
                                                             Double newWeight, Double oldWeight) {
        List<EventSimilarityAvro> updatedSimilarities = new ArrayList<>();

        log.info("Перебор событий кроме обновляемого");
        for (Long otherEventId : eventUserWeights.keySet()) {
            if (updatedEventId.equals(otherEventId)) continue;

            long eventA, eventB;
            boolean isUpdatedFirst;
            if (updatedEventId < otherEventId) {
                eventA = updatedEventId;
                eventB = otherEventId;
                isUpdatedFirst = true;
            } else {
                eventA = otherEventId;
                eventB = updatedEventId;
                isUpdatedFirst = false;
            }

            log.info("Проверка веса для пользователя в других событиях");
            Map<Long, Double> otherUserWeights = eventUserWeights.get(otherEventId);
            if (otherUserWeights != null) {
                Double otherWeight = otherUserWeights.get(userId);
                if (otherWeight != null) {
                    EventSimilarityAvro similarity = updateDotProductForPair(
                            eventA, eventB, newWeight, oldWeight, otherWeight, isUpdatedFirst
                    );
                    if (similarity != null) {
                        updatedSimilarities.add(similarity);
                    }
                }
            }
        }
        return updatedSimilarities;
    }

    private EventSimilarityAvro updateDotProductForPair(long eventA, long eventB,
                                                        Double newWeight, Double oldWeight,
                                                        Double otherWeight, boolean isUpdatedFirst) {
        log.info("Получаем или создаем карту для первого события + считаем скалярное произведение");
        Map<Long, Double> dotProducts = scalarResultMatrix.computeIfAbsent(eventA, k -> new HashMap<>());

        double currentDotProduct = dotProducts.getOrDefault(eventB, 0.0);
        double oldMinWeight = (oldWeight == null) ? 0.0 : Math.min(oldWeight, otherWeight);
        double weightForMin;
        if (isUpdatedFirst) {
            weightForMin = newWeight;
        } else {
            weightForMin = otherWeight;
        }
        double newMinWeight = Math.min(weightForMin, isUpdatedFirst ? otherWeight : newWeight);

        double dotProductDelta = newMinWeight - oldMinWeight;

        log.info("Обновление скалярного произведения и похожести");
        double updatedDotProduct = currentDotProduct + dotProductDelta;
        dotProducts.put(eventB, updatedDotProduct);

        return calculateSimilarity(eventA, eventB, updatedDotProduct);
    }

    private EventSimilarityAvro calculateSimilarity(long eventA, long eventB, double dotProduct) {
        log.info("Вычисление норм для событий");
        Double normA = calculateNorm(eventA);
        Double normB = calculateNorm(eventB);

        if (normA == null || normB == null || normA == 0 || normB == 0) {
            return null;
        }

        double similarity = dotProduct / (normA * normB);
        return new EventSimilarityAvro(eventA, eventB, similarity, Instant.now());
    }

    private Double calculateNorm(Long eventId) {
        Map<Long, Double> selfDotProducts = scalarResultMatrix.get(eventId);
        if (selfDotProducts == null) return null;

        Double selfProduct = selfDotProducts.get(eventId);
        return (selfProduct != null) ? Math.sqrt(selfProduct) : null;
    }


    private void shutdownResources() {
        try {
            producer.flush();
            consumer.commitSync(currentOffsets);
        } finally {
            log.info("[Aggregation] Closing Kafka clients");
            consumer.close();
            producer.close();
        }
    }


    public List<EventSimilarityAvro> handleEvent(UserActionAvro request) {
        double newWeight = getWeight(request.getActionType());

        List<EventSimilarityAvro> similarities = updateEventWeight(
                request.getEventId(),
                request.getUserId(),
                newWeight
        );

        return similarities.stream()
                .sorted(Comparator.comparingLong(EventSimilarityAvro::getEventA)
                        .thenComparingLong(EventSimilarityAvro::getEventB))
                .collect(Collectors.toList());
    }

}