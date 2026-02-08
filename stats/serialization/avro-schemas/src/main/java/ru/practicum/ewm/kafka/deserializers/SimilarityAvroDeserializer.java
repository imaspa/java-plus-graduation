package ru.practicum.ewm.kafka.deserializers;


import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

public class SimilarityAvroDeserializer extends BaseAvroDeserializer<EventSimilarityAvro> {
    public SimilarityAvroDeserializer() {
        super(EventSimilarityAvro.getClassSchema());
    }
}
