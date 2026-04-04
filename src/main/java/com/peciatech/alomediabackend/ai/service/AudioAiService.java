package com.peciatech.alomediabackend.ai.service;

import com.peciatech.alomediabackend.ai.dto.AudioCleanRequest;
import com.peciatech.alomediabackend.ai.dto.AudioTranscribeRequest;
import com.peciatech.alomediabackend.common.exception.FileTooLargeException;
import com.peciatech.alomediabackend.common.exception.FlaskServiceException;
import com.peciatech.alomediabackend.common.exception.FlaskServiceUnavailableException;
import com.peciatech.alomediabackend.common.exception.UnsupportedAudioTypeException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AudioAiService {

    private final RestTemplate restTemplate;

    @Value("${flask.base-url}")
    private String flaskBaseUrl;

    private static final long MAX_FILE_SIZE_BYTES = 50L * 1024 * 1024;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("wav", "mp3", "ogg", "flac", "m4a");

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "audio/wav", "audio/mpeg", "audio/ogg", "audio/flac", "audio/x-m4a", "audio/mp4"
    );

    public ResponseEntity<byte[]> cleanAudio(MultipartFile file, AudioCleanRequest request) {
        validateAudioFile(file);

        try {
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", buildFileResource(file));

            if (request.getBackend() != null) {
                body.add("backend", request.getBackend());
            }
            if (request.getStationary() != null) {
                body.add("stationary", request.getStationary().toString());
            }
            if (request.getTargetSr() != null) {
                body.add("target_sr", request.getTargetSr().toString());
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<byte[]> flaskResponse = restTemplate.exchange(
                    flaskBaseUrl + "/audio/clean",
                    HttpMethod.POST,
                    requestEntity,
                    byte[].class
            );

            return ResponseEntity
                    .status(flaskResponse.getStatusCode())
                    .headers(flaskResponse.getHeaders())
                    .body(flaskResponse.getBody());

        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            throw new FlaskServiceException(ex.getResponseBodyAsString(), ex.getStatusCode());
        } catch (ResourceAccessException ex) {
            throw new FlaskServiceUnavailableException("Flask service is unreachable: " + ex.getMessage());
        }
    }

    public ResponseEntity<byte[]> transcribeAudio(MultipartFile file, AudioTranscribeRequest request) {
        validateAudioFile(file);

        try {
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", buildFileResource(file));

            if (request.getModel() != null) {
                body.add("model", request.getModel());
            }
            if (request.getLang() != null) {
                body.add("lang", request.getLang());
            }
            if (request.getFormats() != null) {
                for (String fmt : request.getFormats()) {
                    body.add("formats", fmt);
                }
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<byte[]> flaskResponse = restTemplate.exchange(
                    flaskBaseUrl + "/audio/transcribe",
                    HttpMethod.POST,
                    requestEntity,
                    byte[].class
            );

            return ResponseEntity
                    .status(flaskResponse.getStatusCode())
                    .headers(flaskResponse.getHeaders())
                    .body(flaskResponse.getBody());

        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            throw new FlaskServiceException(ex.getResponseBodyAsString(), ex.getStatusCode());
        } catch (ResourceAccessException ex) {
            throw new FlaskServiceUnavailableException("Flask service is unreachable: " + ex.getMessage());
        }
    }

    private void validateAudioFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new UnsupportedAudioTypeException("No audio file provided or file is empty.");
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            long sizeMb = file.getSize() / (1024 * 1024);
            throw new FileTooLargeException(
                    "File size " + sizeMb + " MB exceeds the maximum allowed 50 MB."
            );
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new UnsupportedAudioTypeException(
                    "File has no extension. Allowed types: wav, mp3, ogg, flac, m4a."
            );
        }

        String extension = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new UnsupportedAudioTypeException(
                    "File extension '." + extension + "' is not supported. Allowed: wav, mp3, ogg, flac, m4a."
            );
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new UnsupportedAudioTypeException(
                    "Audio type '" + contentType + "' is not supported. Allowed MIME types: " +
                    "audio/wav, audio/mpeg, audio/ogg, audio/flac, audio/x-m4a, audio/mp4."
            );
        }
    }

    private ByteArrayResource buildFileResource(MultipartFile file) {
        try {
            byte[] bytes = file.getBytes();
            String filename = file.getOriginalFilename();
            return new ByteArrayResource(bytes) {
                @Override
                public String getFilename() {
                    return filename;
                }
            };
        } catch (IOException ex) {
            throw new RuntimeException("Failed to read uploaded file: " + ex.getMessage(), ex);
        }
    }
}
