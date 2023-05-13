package com.utilsbot.service;

import com.utilsbot.config.AppProperties;
import com.utilsbot.service.dto.LocationResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@Service
public class LocationService {
    private final Logger log = LoggerFactory.getLogger(LocationService.class);

    // consider using http://api.timezonedb.com/ for cords
    //private static final String CORDS_URL = "";
    private static final String LOCATION_NAME_URL = "https://timezone.abstractapi.com/v1/current_time/?api_key=%s&location=%s";

    private final RestTemplate restTemplate;
    private final AppProperties appProperties;

    public LocationService(RestTemplate restTemplate,
                           AppProperties appProperties) {
        this.restTemplate = restTemplate;
        this.appProperties = appProperties;
    }

    public Optional<LocationResponseDTO> getLocationData(String location) {
        log.info("Requesting location: {}", location);
        try {
            LocationResponseDTO body = restTemplate.exchange(
                    String.format(LOCATION_NAME_URL, appProperties.getTimezoneApiKey(), location),
                    HttpMethod.GET,
                    new HttpEntity<>(null, null),
                    LocationResponseDTO.class
            ).getBody();
            if (body != null && !body.isEmpty()) return Optional.of(body);
        } catch (RestClientException e) {
            log.error("Failed to get location, response message {}", e.getMessage());
            e.printStackTrace();
        }
        return Optional.empty();
    }
}
