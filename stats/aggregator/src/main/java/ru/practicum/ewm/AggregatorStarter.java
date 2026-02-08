package ru.practicum.ewm;


import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.aggregator.AggregatorService;

@Component
@RequiredArgsConstructor
public class AggregatorStarter {
    private final AggregatorService aggregator;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        aggregator.processEvents();
    }
}
