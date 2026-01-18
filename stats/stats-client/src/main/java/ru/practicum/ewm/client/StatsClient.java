package ru.practicum.ewm.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import ru.practicum.ewm.EndpointHitDto;
import ru.practicum.ewm.ViewStatsDto;

import java.util.List;

@FeignClient(name = "stats-server")
public interface StatsClient {
    @PostMapping("/hit")
    void saveHit(EndpointHitDto hitDto);

    @GetMapping("/stats")
    List<ViewStatsDto> getStats(String start, String end, String[] uris, Boolean unique);
}
