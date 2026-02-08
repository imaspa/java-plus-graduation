package ru.practicum.ewm.core;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.config.KafkaConfig;
import ru.practicum.ewm.config.KafkaConfigProducer;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.grpc.stats.action.UserActionProto;

@Service
@Slf4j
@RequiredArgsConstructor
public class CollectorService {

    private final CollectorMapper mapper;

    private final KafkaConfigProducer producerConfig;
    private final KafkaProducer<String, UserActionAvro> producer;

    @Autowired
    public CollectorService(KafkaConfig kafkaConfig, CollectorMapper mapper) {
        this.mapper = mapper;
        this.producerConfig = kafkaConfig.getProducer();
        this.producer = new KafkaProducer<String, UserActionAvro>(producerConfig.getProperties());
    }

    public void createUserAction(UserActionProto request) {
        UserActionAvro avro = mapper.mapToAvro(request);

        ProducerRecord<String, UserActionAvro> record =
                new ProducerRecord<>(producerConfig.getTopic(), avro);
        producer.send(record, new Callback() {
            @Override
            public void onCompletion(RecordMetadata metadata, Exception exception) {
                if (exception == null) {
                    log.info("Действие успешно отправлено. Топик: {}, Партиция: {}, Offset: {}",
                            metadata.topic(), metadata.partition(), metadata.offset());
                } else {
                    log.error("Не удалось отправить действие: {}", exception.getMessage(), exception);
                }
            }
        });

    }
}