package com.utilsbot.service;

import com.utilsbot.config.AppProperties;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.File;

@Service
public class TranscriptionService {

    private final Logger log = LoggerFactory.getLogger(TranscriptionService.class);
    private final HttpHeaders headers = new HttpHeaders();

    private String apiUrl;

    private final RestTemplate restTemplate;
    private final AppProperties appProperties;

    public TranscriptionService(RestTemplate restTemplate,
                                AppProperties appProperties) {
        this.restTemplate = restTemplate;
        this.appProperties = appProperties;
    }

    @PostConstruct
    private void postConstruct() {
        headers.set("accept", "application/json");
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        apiUrl = appProperties.getMicroservices().getWhisperUrl() + "/asr?method=openai-whisper&task=transcribe&encode=true&output=txt";
    }

    public String getTranscription(File file) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("audio_file", new FileSystemResource(file));
        try {
            return restTemplate.exchange(
                    apiUrl,
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    String.class
            ).getBody();
        } catch (RestClientException e) {
            log.error("Response error: {}", e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
}
