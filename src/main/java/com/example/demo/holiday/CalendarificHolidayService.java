package com.example.demo.holiday;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.LocalDate;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Service
public class CalendarificHolidayService implements HolidayService {

    private final WebClient webClient;
    private final CalendarificProperties properties;

    public CalendarificHolidayService(CalendarificProperties properties, WebClient.Builder webClientBuilder) {
        this.properties = properties;
        this.webClient = webClientBuilder
                .baseUrl(properties.getBaseUrl())
                .defaultHeader(HttpHeaders.USER_AGENT, "budget-tracker-api/1.0")
                .build();
    }

    @Override
    public Optional<String> findHoliday(LocalDate date) {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            return Optional.empty();
        }
        try {
            CalendarificResponse response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/holidays")
                            .queryParam("api_key", properties.getApiKey())
                            .queryParam("country", properties.getCountry())
                            .queryParam("year", date.getYear())
                            .queryParam("month", date.getMonthValue())
                            .queryParam("day", date.getDayOfMonth())
                            .build())
                    .retrieve()
                    .bodyToMono(CalendarificResponse.class)
                    .block(Duration.ofSeconds(5));

            if (response != null && response.response != null && response.response.holidays != null) {
                return response.response.holidays.stream()
                        .findFirst()
                        .map(h -> h.name);
            }
        } catch (WebClientResponseException ex) {
            // Fall through and return empty on errors to avoid breaking expense creation
        }
        return Optional.empty();
    }

    // Response mappings
    public static class CalendarificResponse {
        public CalendarificData response;
    }

    public static class CalendarificData {
        public List<Holiday> holidays;
    }

    public static class Holiday {
        public String name;
    }
}
