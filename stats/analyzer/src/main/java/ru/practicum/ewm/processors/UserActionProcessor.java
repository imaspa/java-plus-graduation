package ru.practicum.ewm.processors;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.configuration.KafkaConfig;
import ru.practicum.ewm.configuration.KafkaConfigConsumer;
import ru.practicum.ewm.service.UserActionService;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Duration;
import java.util.List;


@Slf4j
@Component
public class UserActionProcessor implements Runnable {
    private final KafkaConsumer<String, UserActionAvro> consumer;
    private final UserActionService service;
    private final List<String> topics;
    private final Duration pollTimeout;

    public UserActionProcessor(KafkaConfig config, UserActionService service) {
        this.service = service;

        final KafkaConfigConsumer consumerConfig = config.getConsumers().get(this.getClass().getSimpleName());
        this.consumer = new KafkaConsumer<>(consumerConfig.getProperties());
        this.topics = consumerConfig.getTopics();
        this.pollTimeout = consumerConfig.getPollTimeout();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("[Analyzer UserAction][JVM STOP]. Остановка Consumer UserAction ");
            consumer.wakeup();
        }));
    }

    @Override
    public void run() {
        log.info("[Analyzer UserAction] Subscribing to topic: {}", topics);
        consumer.subscribe(topics);
        try {
            while (true) {
                ConsumerRecords<String, UserActionAvro> records = consumer.poll(pollTimeout);
                if (!records.isEmpty()) {
                    for (ConsumerRecord<String, UserActionAvro> record : records) {
                        service.saveUserAction(record.value());
                    }
                    consumer.commitSync();
                }
            }
        } catch (WakeupException e) {
        } catch (Exception e) {
            log.error("[Analyzer UserAction] [PROCESSING ERROR]", e);
        } finally {
            consumer.close();
        }
    }

}
