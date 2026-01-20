package ru.practicum.ewm.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.ewm.EndpointHitDto;
import ru.practicum.ewm.ViewStatsDto;
import ru.practicum.ewm.config.RestClientConfig;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatsClient {

    private final RestClient restClient;
    private final RestClientConfig restClientConfig;

    public void saveHit(EndpointHitDto hitDto) {
        String baseUrl = restClientConfig.getStatsServerUrl();
        String url = baseUrl + "/hit";
        log.info("Отправка запроса saveHit: url={}, body={}", url, hitDto);

        restClient.post()
                .uri("/hit")
                .body(hitDto)
                .retrieve()
                .toBodilessEntity();

        log.info("Hit был сохранен");
    }

    public List<ViewStatsDto> getStats(String start, String end, String[] uris, boolean unique) {
        String baseUrl = restClientConfig.getStatsServerUrl();
        String url = baseUrl + UriComponentsBuilder
                .fromPath("/stats")
                .queryParam("start", start)
                .queryParam("end", end)
                .queryParam("uris", (Object[]) uris)
                .queryParam("unique", unique)
                .build(false)
                .toUriString();

        String logUrl = url.replace(" ", "%20");
        log.info("Отправка запроса getStats: url={}", logUrl);

        List<ViewStatsDto> stats = Arrays.asList(
                restClient.get()
                        .uri(url)
                        .retrieve()
                        .body(ViewStatsDto[].class)
        );

        log.info("getStats вернул {} записей", stats.size());
        return stats;
    }


}
