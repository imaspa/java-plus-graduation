package ru.practicum.ewm.processors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.configuration.KafkaConfig;
import ru.practicum.ewm.configuration.KafkaConfigConsumer;
import ru.practicum.ewm.service.SimilarityService;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Slf4j
@Component
@RequiredArgsConstructor
public class SimilarityProcessor{
    private final SimilarityService service;
    private final Map<TopicPartition, OffsetAndMetadata> currentOffsets = new HashMap<>();

    private final KafkaConsumer<String, EventSimilarityAvro> consumer;
    private final List<String> topics;
    private final Duration pollTimeout;

    @Autowired
    public SimilarityProcessor(SimilarityService service, KafkaConfig kafkaConfig) {
        this.service = service;

        final KafkaConfigConsumer consumerConfig = kafkaConfig.getConsumers().get(this.getClass().getSimpleName());

        this.consumer = new KafkaConsumer<>(consumerConfig.getProperties());
        this.topics = consumerConfig.getTopics();
        this.pollTimeout = consumerConfig.getPollTimeout();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("[Analyzer Similarity][JVM STOP]. Остановка Consumer Similarity ");
            consumer.wakeup();
        }));
    }

    public void processorSimilarity() {
        try {
            log.info("[Analyzer Similarity] Subscribing to topic: {}", topics);
            consumer.subscribe(topics);

            while (true) {
                ConsumerRecords<String, EventSimilarityAvro> records = consumer.poll(pollTimeout);

                if (!records.isEmpty()) {
                    int count = 0;
                    for (ConsumerRecord<String, EventSimilarityAvro> record : records) {
                        service.saveSimilarity(record.value());
                        trackAndCommitOffset(record, count++);
                    }
                    consumer.commitAsync();
                }
            }
        } catch (WakeupException ignored) {
        } catch (Exception e) {
            log.error("[Analyzer Similarity] [PROCESSING ERROR]", e);
        } finally {
            shutdownResources();
        }
    }

    private void shutdownResources() {
        try {
            consumer.commitSync(currentOffsets);
        } finally {
            log.info("[Analyzer Similarity] Closing Kafka clients");
            consumer.close();
        }
    }

    private void trackAndCommitOffset(ConsumerRecord<?, ?> record, int count) {
        currentOffsets.put(
                new TopicPartition(record.topic(), record.partition()),
                new OffsetAndMetadata(record.offset() + 1)
        );

        if (count % 100 == 0) {
            consumer.commitAsync(currentOffsets, (offsets, exception) -> {
                if (exception != null) {
                    log.warn("[Analyzer Similarity] [ERR] trackAndCommitOffset смещение: {}", offsets, exception);
                }
            });
        }
    }
}