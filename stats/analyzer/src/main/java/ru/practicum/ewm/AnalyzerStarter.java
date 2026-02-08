package ru.practicum.ewm;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.processors.SimilarityProcessor;
import ru.practicum.ewm.processors.UserActionProcessor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnalyzerStarter implements ApplicationRunner {

    private final UserActionProcessor userAction;
    private final SimilarityProcessor similarity;

    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable ->
            new Thread(runnable, "UserEventHandlerThread")
    );

    @Override
    public void run(org.springframework.boot.ApplicationArguments args) {
        log.info("[Analyzer userAction] Starting in background...");
        executor.execute(userAction);

        log.info("[Analyzer similarity] Running processor...");
        similarity.processorSimilarity();
    }

    @PreDestroy
    public void shutdown() {
        log.info("[Analyzer userAction] Shutting down HubEventProcessor executor...");
        executor.shutdownNow();
    }

    /*

        final HubEventProcessor hubEventProcessor = context.getBean(HubEventProcessor.class);
        SnapshotProcessor snapshotProcessor = context.getBean(SnapshotProcessor.class);

        Thread hubEventsThread = new Thread(hubEventProcessor);
        hubEventsThread.setName("HubEventHandlerThread"); - user action
        hubEventsThread.start();

        snapshotProcessor.processorSnapshot(); - simp



    @Override
    public void run(org.springframework.boot.ApplicationArguments args){
        log.info("[Analyzer Hub] Starting in background...");
        executor.execute(hubEventProcessor);

        log.info("[Analyzer snapshot] Running processor...");
        snapshotProcessor.processorSnapshot();
    }
 */


}
