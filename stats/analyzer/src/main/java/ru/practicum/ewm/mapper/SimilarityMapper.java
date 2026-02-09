package ru.practicum.ewm.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.ewm.models.Similarity;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

@Mapper(componentModel = "spring")
public interface SimilarityMapper {

    @Mapping(target = "id", ignore = true)
    Similarity toEntity(EventSimilarityAvro avro);
}
