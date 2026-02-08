package ru.practicum.ewm.controller;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.practicum.ewm.service.RecommendationService;
import ru.practicum.grpc.stats.eventPredictions.InteractionsCountRequestProto;
import ru.practicum.grpc.stats.eventPredictions.RecommendedEventProto;
import ru.practicum.grpc.stats.eventPredictions.SimilarEventsRequestProto;
import ru.practicum.grpc.stats.eventPredictions.UserPredictionsRequestProto;
import ru.practicum.stats.service.dashboard.RecommendationsControllerGrpc;

import java.util.List;

@GrpcService
@RequiredArgsConstructor
@Slf4j

public class RecommendationController extends RecommendationsControllerGrpc.RecommendationsControllerImplBase {
    private final RecommendationService service;

    @Override
    public void getRecommendationsForUser(UserPredictionsRequestProto request,
                                          StreamObserver<RecommendedEventProto> response) {
        log.info("Запрос на получение пользовательских рекомендаций");
        try {
            List<RecommendedEventProto> events = service.getRecommendationsForUser(request);
            events.forEach(response::onNext);
            response.onCompleted();
        } catch (Exception e) {
            response.onError(new StatusRuntimeException(
                    Status.INTERNAL
                            .withDescription(e.getLocalizedMessage())
                            .withCause(e)
            ));
        }
    }

    @Override
    public void getSimilarEvents(SimilarEventsRequestProto request, StreamObserver<RecommendedEventProto> response) {
        log.info("Запрос на получение похожих событий");
        try {
            List<RecommendedEventProto> events = service.getSimilarEvents(request);
            events.forEach(response::onNext);
            response.onCompleted();
        } catch (Exception e) {
            response.onError(new StatusRuntimeException(
                    Status.INTERNAL
                            .withDescription(e.getLocalizedMessage())
                            .withCause(e)
            ));
        }
    }

    @Override
    public void getInteractionsCount(InteractionsCountRequestProto request,
                                     StreamObserver<RecommendedEventProto> response) {
        log.info("Запрос на получение количества взаимодействий");
        try {
            List<RecommendedEventProto> events = service.getInteractionsCount(request);
            events.forEach(response::onNext);
            response.onCompleted();
        } catch (Exception e) {
            response.onError(new StatusRuntimeException(
                    Status.INTERNAL
                            .withDescription(e.getLocalizedMessage())
                            .withCause(e)
            ));
        }
    }
}
