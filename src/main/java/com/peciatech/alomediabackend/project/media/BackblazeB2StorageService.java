package com.peciatech.alomediabackend.project.media;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class BackblazeB2StorageService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${b2.authorize-url}")
    private String authorizeUrl;

    @Value("${b2.key-id:}")
    private String keyId;

    @Value("${b2.application-key:}")
    private String applicationKey;

    @Value("${b2.bucket-id:}")
    private String bucketId;

    @Value("${b2.bucket-name:}")
    private String bucketName;

    private volatile String authorizationToken;
    private volatile String apiUrl;
    private volatile String downloadUrl;
    private volatile Instant authExpiresAt;

    public StoredMediaFile upload(String storageKey, byte[] content, String contentType) {
        try {
            AuthContext auth = authorizeIfNeeded();

            JsonNode uploadData = postJson(
                    auth.apiUrl(),
                    "/b2api/v2/b2_get_upload_url",
                    auth.authorizationToken(),
                    Map.of("bucketId", bucketId)
            );

            String uploadUrl = uploadData.path("uploadUrl").asText();
            String uploadAuthToken = uploadData.path("authorizationToken").asText();

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", uploadAuthToken);
            headers.set("X-Bz-File-Name", encodePathSegment(storageKey));
            headers.set("X-Bz-Content-Sha1", sha1Hex(content));
            headers.setContentType(MediaType.parseMediaType(resolveContentType(contentType)));

            HttpEntity<byte[]> request = new HttpEntity<>(content, headers);
            ResponseEntity<String> response = restTemplate.exchange(uploadUrl, HttpMethod.POST, request, String.class);
            JsonNode body = objectMapper.readTree(response.getBody());

            return new StoredMediaFile(
                    body.path("fileName").asText(storageKey),
                    body.path("fileId").asText(),
                    body.path("contentType").asText(resolveContentType(contentType))
            );
        } catch (Exception ex) {
            throw new RuntimeException("Failed to upload file to Backblaze B2", ex);
        }
    }

    public void delete(String storageKey, String storageFileId) {
        if (!StringUtils.hasText(storageKey)) {
            return;
        }

        try {
            AuthContext auth = authorizeIfNeeded();

            String fileId = storageFileId;
            if (!StringUtils.hasText(fileId)) {
                fileId = findFileIdByName(storageKey, auth);
            }

            if (!StringUtils.hasText(fileId)) {
                log.debug("Skipping Backblaze delete because file id was not found for key={}", storageKey);
                return;
            }

            postJson(
                    auth.apiUrl(),
                    "/b2api/v2/b2_delete_file_version",
                    auth.authorizationToken(),
                    Map.of("fileName", storageKey, "fileId", fileId)
            );
        } catch (Exception ex) {
            throw new RuntimeException("Failed to delete file from Backblaze B2", ex);
        }
    }

    public StorageBinaryResource download(String storageKey) {
        try {
            AuthContext auth = authorizeIfNeeded();
            String encodedStorageKey = encodePath(storageKey);
            String url = auth.downloadUrl() + "/file/" + bucketName + "/" + encodedStorageKey;

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", auth.authorizationToken());

            ResponseEntity<byte[]> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    byte[].class
            );

            String contentType = response.getHeaders().getContentType() != null
                    ? response.getHeaders().getContentType().toString()
                    : MediaType.APPLICATION_OCTET_STREAM_VALUE;

            String fileName = storageKey.contains("/")
                    ? storageKey.substring(storageKey.lastIndexOf('/') + 1)
                    : storageKey;

            return new StorageBinaryResource(response.getBody(), contentType, fileName);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to download file from Backblaze B2", ex);
        }
    }

    private JsonNode postJson(String baseApiUrl, String path, String authToken, Object payload) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", authToken);

        HttpEntity<Object> request = new HttpEntity<>(payload, headers);
        ResponseEntity<String> response = restTemplate.exchange(
                baseApiUrl + path,
                HttpMethod.POST,
                request,
                String.class
        );

        return objectMapper.readTree(response.getBody());
    }

    private synchronized AuthContext authorizeIfNeeded() {
        try {
            if (StringUtils.hasText(authorizationToken)
                    && authExpiresAt != null
                    && Instant.now().isBefore(authExpiresAt)) {
                return new AuthContext(apiUrl, downloadUrl, authorizationToken);
            }

            validateConfiguration();
            String resolvedAuthorizeUrl = normalizeAuthorizeUrl(authorizeUrl);

            String raw = keyId + ":" + applicationKey;
            String basic = java.util.Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Basic " + basic);

            ResponseEntity<String> response = restTemplate.exchange(
                    resolvedAuthorizeUrl,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );

            JsonNode body = objectMapper.readTree(response.getBody());
            this.authorizationToken = body.path("authorizationToken").asText();
            this.apiUrl = body.path("apiUrl").asText();
            this.downloadUrl = body.path("downloadUrl").asText();
            this.authExpiresAt = Instant.now().plusSeconds(60L * 60L * 23L);

            return new AuthContext(apiUrl, downloadUrl, authorizationToken);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to authorize with Backblaze B2", ex);
        }
    }

    private String findFileIdByName(String storageKey, AuthContext authContext) throws Exception {
        JsonNode listResponse = postJson(
                authContext.apiUrl(),
                "/b2api/v2/b2_list_file_names",
                authContext.authorizationToken(),
                Map.of(
                        "bucketId", bucketId,
                        "startFileName", storageKey,
                        "maxFileCount", 1
                )
        );

        JsonNode files = listResponse.path("files");
        if (!files.isArray() || files.isEmpty()) {
            return null;
        }

        JsonNode first = files.get(0);
        if (!storageKey.equals(first.path("fileName").asText())) {
            return null;
        }

        return first.path("fileId").asText(null);
    }

    private void validateConfiguration() {
        if (!StringUtils.hasText(keyId)
                || !StringUtils.hasText(applicationKey)
                || !StringUtils.hasText(bucketId)
                || !StringUtils.hasText(bucketName)) {
                throw new RuntimeException(
                    "Backblaze B2 configuration is missing. Please set b2.key-id, b2.application-key, b2.bucket-id and b2.bucket-name"
            );
        }
    }

    static String normalizeAuthorizeUrl(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        if (!StringUtils.hasText(normalized)) {
            return normalized;
        }

        if (normalized.regionMatches(true, 0, "%3A", 0, 3)) {
            normalized = normalized.substring(3);
        }

        if (normalized.startsWith(":")) {
            normalized = normalized.substring(1);
        }

        return normalized;
    }

    private String resolveContentType(String contentType) {
        return StringUtils.hasText(contentType) ? contentType : MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }

    private String encodePath(String value) {
        String[] segments = value.split("/");
        StringBuilder encoded = new StringBuilder();
        for (int i = 0; i < segments.length; i++) {
            if (i > 0) {
                encoded.append('/');
            }
            encoded.append(encodePathSegment(segments[i]));
        }
        return encoded.toString();
    }

    private String encodePathSegment(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private String sha1Hex(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            return HexFormat.of().formatHex(digest.digest(content));
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException("SHA-1 algorithm not available", ex);
        }
    }

    private record AuthContext(String apiUrl, String downloadUrl, String authorizationToken) {
    }
}
