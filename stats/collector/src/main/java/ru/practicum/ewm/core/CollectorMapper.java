package ru.practicum.ewm.core;

import com.google.protobuf.Timestamp;
import org.mapstruct.Mapper;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.grpc.stats.action.ActionTypeProto;
import ru.practicum.grpc.stats.action.UserActionProto;

import java.time.Instant;

@Mapper(componentModel = "spring")
public interface CollectorMapper {
    UserActionAvro mapToAvro(UserActionProto action);

    default ActionTypeAvro mapActionType(ActionTypeProto actionType) {
        if (actionType == null) {
            return null;
        }
        return switch (actionType) {
            case ACTION_VIEW -> ActionTypeAvro.VIEW;
            case ACTION_REGISTER -> ActionTypeAvro.REGISTER;
            case ACTION_LIKE -> ActionTypeAvro.LIKE;
            default -> throw new IllegalArgumentException("Неизвестный тип действия: " + actionType);
        };
    }

    default Instant mapTimestamp(Timestamp timestamp) {
        return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
    }
}