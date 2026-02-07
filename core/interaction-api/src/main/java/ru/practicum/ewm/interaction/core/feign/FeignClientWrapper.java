package ru.practicum.ewm.interaction.core.feign;

import feign.FeignException;
import lombok.experimental.UtilityClass;
import ru.practicum.ewm.interaction.core.exception.NotFoundException;

import java.util.function.Consumer;
import java.util.function.Supplier;

@UtilityClass
public class FeignClientWrapper {

    public <T> T call(Supplier<T> feignCall, String id, String entityName) {
        try {
            return feignCall.get();
        } catch (FeignException.NotFound e) {
            throw new NotFoundException(
                    "Ресурс не найден: %s ID: '%s'".formatted(entityName, id)
            );
        } catch (FeignException e) {
            throw new IllegalStateException(
                    "Ошибка при вызове удалённого сервиса для %s ID: '%s': %s"
                            .formatted(entityName, id, e.getMessage()), e
            );
        }
    }

    public static <T> void call(Consumer<T> feignCall, T argument, String entityName) {
        try {
            feignCall.accept(argument);
        } catch (FeignException.NotFound e) {
            throw new NotFoundException("Ресурс не найден: %s".formatted(entityName));
        } catch (FeignException e) {
            throw new IllegalStateException(
                    "Ошибка при вызове удалённого сервиса для %s: %s".formatted(entityName, e.getMessage()), e
            );
        }
    }

    public static <T> T call(Supplier<T> feignCall, String entityName) {
        try {
            return feignCall.get();
        } catch (FeignException.NotFound e) {
            throw new NotFoundException("Ресурс не найден: %s".formatted(entityName));
        } catch (FeignException e) {
            throw new IllegalStateException(
                    "Ошибка при вызове удалённого сервиса для %s: %s".formatted(entityName, e.getMessage()), e
            );
        }
    }

    public void call(Runnable feignCall, String id, String entityName) {
        try {
            feignCall.run();
        } catch (FeignException.NotFound e) {
            throw new NotFoundException(
                    "Ресурс не найден: %s с ID '%s'".formatted(entityName, id)
            );
        } catch (FeignException e) {
            throw new IllegalStateException(
                    "Ошибка при вызове удалённого сервиса для %s ID: '%s': %s"
                            .formatted(entityName, id, e.getMessage()), e
            );
        }
    }

    public static <T, R> R callWithRequest(Supplier<R> feignCall, T requestObject, String entityName) {
        try {
            return feignCall.get();
        } catch (FeignException.NotFound e) {
            throw new NotFoundException(
                    "Ресурс не найден: %s. Тело запроса: %s".formatted(entityName, requestObject)
            );
        } catch (FeignException e) {
            throw new IllegalStateException(
                    "Ошибка при вызове удалённого сервиса для %s. Тело запроса: %s. Причина: %s"
                            .formatted(entityName, requestObject, e.getMessage()), e
            );
        }
    }

    public static <T> void callWithRequest(Consumer<T> feignCall, T requestObject, String operationName) {
        try {
            feignCall.accept(requestObject);
        } catch (FeignException.NotFound e) {
            throw new NotFoundException(
                    "Ресурс не найден при операции: %s. Тело запроса: %s".formatted(operationName, requestObject)
            );
        } catch (FeignException e) {
            throw new IllegalStateException(
                    "Ошибка при вызове удалённого сервиса (%s). Тело запроса: %s. Причина: %s"
                            .formatted(operationName, requestObject, e.getMessage()), e
            );
        }
    }
}
