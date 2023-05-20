package com.utilsbot.service;

import com.utilsbot.config.AppProperties;
import com.utilsbot.domain.enums.OcrLanguages;
import com.utilsbot.service.dto.OcrResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;
import java.util.UUID;

import static com.utilsbot.domain.enums.OcrLanguages.isE3;

/*
 * inspired from https://github.com/bsuhas/OCRTextRecognitionAndroidApp/blob/be7bb24a0e880cf174de9f16047fcb1b8c7447c6/app/src/main/java/com/ocrtextrecognitionapp/OCRAsyncTask.java
 */

@Service
public class OcrService {

    private final Logger log = LoggerFactory.getLogger(OcrService.class);

    private static final String API_URL = "https://api.ocr.space/parse/image";

    private final RestTemplate restTemplate;
    private final AppProperties appProperties;
    private final HttpHeaders headers = new HttpHeaders();

    public OcrService(RestTemplate restTemplate,
                      AppProperties appProperties) {
        this.restTemplate = restTemplate;
        this.appProperties = appProperties;
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
    }

    public Optional<String> getTextFromImage(byte[] imageData, OcrLanguages lang) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        ByteArrayResource byteArrayResource = new ByteArrayResource(imageData) {
            @Override
            public String getFilename() {
                return UUID.randomUUID() + ".jpg";
            }
        };
        body.add("file", byteArrayResource);
        body.add("apikey", appProperties.getOcrApiKey());
        body.add("language", lang.toString().toLowerCase());
        body.add("detectOrientation", true);
        body.add("scale", true);
        if (isE3(lang)) {
            body.add("OCREngine", 3);
        }
        OcrResponse ocrResponse = null;
        try {
            ocrResponse = restTemplate.exchange(
                    API_URL,
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    OcrResponse.class
            ).getBody();
        } catch (RestClientException e) {
            log.error("Response error: {}", e.getMessage());
            e.printStackTrace();
        }
        if (ocrResponse != null) {
            if (ocrResponse.IsErroredOnProcessing()) {
                String responseError = String.join(", ", ocrResponse.ErrorMessage());
                log.error("received error response: {}", responseError);
            } else return Optional.of(ocrResponse.ParsedResults().get(0).ParsedText());
        }
        return Optional.empty();
    }
}
