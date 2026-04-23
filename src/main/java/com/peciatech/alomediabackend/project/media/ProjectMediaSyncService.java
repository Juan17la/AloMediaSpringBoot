package com.peciatech.alomediabackend.project.media;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.peciatech.alomediabackend.common.exception.ResourceNotFoundException;
import com.peciatech.alomediabackend.project.entity.Project;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectMediaSyncService {

    private final ObjectMapper objectMapper;
    private final BackblazeB2StorageService backblazeB2StorageService;

    public String syncOnSave(Project project, String previousTimelineData, String incomingTimelineData) {
        if (!StringUtils.hasText(incomingTimelineData)) {
            return incomingTimelineData;
        }

        try {
            JsonNode root = objectMapper.readTree(incomingTimelineData);
            if (!(root instanceof ObjectNode rootObject)) {
                return incomingTimelineData;
            }

            JsonNode mediaNode = rootObject.path("media");
            if (!(mediaNode instanceof ArrayNode mediaArray)) {
                return incomingTimelineData;
            }

            Map<String, StoredMediaMetadata> previousMedia = extractStoredMediaById(previousTimelineData);
            Set<String> retainedIds = new HashSet<>();
            Map<String, StoredMediaMetadata> currentMediaByStorageKey = new HashMap<>();
            Map<String, StoredMediaMetadata> toDeleteByStorageKey = new LinkedHashMap<>();

            for (JsonNode node : mediaArray) {
                if (!(node instanceof ObjectNode mediaObject)) {
                    continue;
                }

                String mediaId = readMediaId(mediaObject);
                if (!StringUtils.hasText(mediaId)) {
                    continue;
                }

                retainedIds.add(mediaId);
                StoredMediaMetadata oldMedia = previousMedia.get(mediaId);
                InlinePayload inlinePayload = extractInlinePayload(mediaObject);

                if (inlinePayload != null) {
                    String fileName = resolveFileName(mediaObject, mediaId, inlinePayload.contentType());
                    String storageKey = buildStorageKey(project, mediaId, fileName);
                    StoredMediaFile stored = backblazeB2StorageService.upload(
                            storageKey,
                            inlinePayload.bytes(),
                            inlinePayload.contentType()
                    );

                    applyStoredMetadata(mediaObject, stored, fileName, inlinePayload.contentType());
                    removeInlineFields(mediaObject);

                    currentMediaByStorageKey.put(
                            stored.storageKey(),
                            new StoredMediaMetadata(mediaId, stored.storageKey(), stored.storageFileId())
                    );

                    if (oldMedia != null
                            && StringUtils.hasText(oldMedia.storageKey())
                            && !oldMedia.storageKey().equals(stored.storageKey())) {
                        toDeleteByStorageKey.put(oldMedia.storageKey(), oldMedia);
                    }
                    continue;
                }

                String existingStorageKey = text(mediaObject, "storageKey");
                if (StringUtils.hasText(existingStorageKey)) {
                    currentMediaByStorageKey.put(
                            existingStorageKey,
                            new StoredMediaMetadata(mediaId, existingStorageKey, text(mediaObject, "storageFileId"))
                    );
                    continue;
                }

                if (oldMedia != null && StringUtils.hasText(oldMedia.storageKey())) {
                    mediaObject.put("storageProvider", "backblaze-b2");
                    mediaObject.put("storageKey", oldMedia.storageKey());
                    if (StringUtils.hasText(oldMedia.storageFileId())) {
                        mediaObject.put("storageFileId", oldMedia.storageFileId());
                    }
                    currentMediaByStorageKey.put(oldMedia.storageKey(), oldMedia);
                    continue;
                }

                log.warn("Media entry id={} has no inline payload and no storage reference. It will not be uploaded.", mediaId);
            }

            for (Map.Entry<String, StoredMediaMetadata> entry : previousMedia.entrySet()) {
                if (!retainedIds.contains(entry.getKey()) && StringUtils.hasText(entry.getValue().storageKey())) {
                    toDeleteByStorageKey.put(entry.getValue().storageKey(), entry.getValue());
                }
            }

            for (Map.Entry<String, StoredMediaMetadata> entry : toDeleteByStorageKey.entrySet()) {
                if (!currentMediaByStorageKey.containsKey(entry.getKey())) {
                    StoredMediaMetadata item = entry.getValue();
                    backblazeB2StorageService.delete(item.storageKey(), item.storageFileId());
                }
            }

            return objectMapper.writeValueAsString(rootObject);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to synchronize project media with storage", ex);
        }
    }

    public String enrichTimelineWithDeliveryUrls(Project project, String timelineData) {
        if (!StringUtils.hasText(timelineData)) {
            return timelineData;
        }

        try {
            JsonNode root = objectMapper.readTree(timelineData);
            if (!(root instanceof ObjectNode rootObject)) {
                return timelineData;
            }

            JsonNode mediaNode = rootObject.path("media");
            if (!(mediaNode instanceof ArrayNode mediaArray)) {
                return timelineData;
            }

            for (JsonNode node : mediaArray) {
                if (!(node instanceof ObjectNode mediaObject)) {
                    continue;
                }

                String mediaId = readMediaId(mediaObject);
                String storageKey = text(mediaObject, "storageKey");
                if (!StringUtils.hasText(mediaId) || !StringUtils.hasText(storageKey)) {
                    continue;
                }

                String deliveryUrl = "/projects/" + project.getId() + "/media/" + URLEncoder.encode(mediaId, StandardCharsets.UTF_8);
                mediaObject.put("deliveryUrl", deliveryUrl);

                String src = text(mediaObject, "src");
                if (!StringUtils.hasText(src) || src.startsWith("blob:")) {
                    mediaObject.put("src", deliveryUrl);
                }
            }

            return objectMapper.writeValueAsString(rootObject);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to enrich media delivery URLs", ex);
        }
    }

    public StorageBinaryResource loadMediaForProject(Project project, String mediaId) {
        return loadMediaForProject(project, project.getTimelineData(), mediaId);
    }

    public StorageBinaryResource loadMediaForProject(Project project, String timelineData, String mediaId) {
        try {
            JsonNode root = objectMapper.readTree(timelineData);
            JsonNode mediaArray = root.path("media");
            if (!mediaArray.isArray()) {
                throw new ResourceNotFoundException("No media entries were found for this project");
            }

            for (JsonNode node : mediaArray) {
                if (!(node instanceof ObjectNode mediaObject)) {
                    continue;
                }

                if (!mediaId.equals(readMediaId(mediaObject))) {
                    continue;
                }

                String storageKey = text(mediaObject, "storageKey");
                if (!StringUtils.hasText(storageKey)) {
                    throw new ResourceNotFoundException("Media file is not available in storage");
                }

                StorageBinaryResource resource = backblazeB2StorageService.download(storageKey);
                String contentType = resource.contentType();
                if ((!StringUtils.hasText(contentType) || "application/octet-stream".equals(contentType))
                        && StringUtils.hasText(text(mediaObject, "mimeType"))) {
                    contentType = text(mediaObject, "mimeType");
                }

                return new StorageBinaryResource(
                        resource.data(),
                        contentType,
                        StringUtils.hasText(text(mediaObject, "fileName")) ? text(mediaObject, "fileName") : resource.fileName()
                );
            }

            throw new ResourceNotFoundException("Media entry not found for id: " + mediaId);
        } catch (ResourceNotFoundException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException("Failed to load media from project", ex);
        }
    }

    public void deleteAllProjectMedia(String timelineData) {
        Map<String, StoredMediaMetadata> mediaById = extractStoredMediaById(timelineData);
        Map<String, StoredMediaMetadata> uniqueByStorage = new LinkedHashMap<>();
        for (StoredMediaMetadata media : mediaById.values()) {
            if (StringUtils.hasText(media.storageKey())) {
                uniqueByStorage.put(media.storageKey(), media);
            }
        }

        for (StoredMediaMetadata media : uniqueByStorage.values()) {
            backblazeB2StorageService.delete(media.storageKey(), media.storageFileId());
        }
    }

    private Map<String, StoredMediaMetadata> extractStoredMediaById(String timelineData) {
        Map<String, StoredMediaMetadata> result = new HashMap<>();
        if (!StringUtils.hasText(timelineData)) {
            return result;
        }

        try {
            JsonNode root = objectMapper.readTree(timelineData);
            JsonNode media = root.path("media");
            if (!media.isArray()) {
                return result;
            }

            for (JsonNode node : media) {
                if (!(node instanceof ObjectNode mediaObject)) {
                    continue;
                }

                String mediaId = readMediaId(mediaObject);
                String storageKey = text(mediaObject, "storageKey");
                if (!StringUtils.hasText(mediaId) || !StringUtils.hasText(storageKey)) {
                    continue;
                }

                result.put(mediaId, new StoredMediaMetadata(mediaId, storageKey, text(mediaObject, "storageFileId")));
            }
            return result;
        } catch (Exception ex) {
            throw new RuntimeException("Failed to parse timeline data for media extraction", ex);
        }
    }

    private InlinePayload extractInlinePayload(ObjectNode mediaObject) {
        String[] candidateFields = new String[]{"data", "base64", "dataUrl", "sourceDataUrl", "fileData", "src", "source"};
        for (String field : candidateFields) {
            String value = text(mediaObject, field);
            if (!StringUtils.hasText(value) || value.startsWith("blob:")) {
                continue;
            }

            if (value.startsWith("data:") && value.contains(";base64,")) {
                String header = value.substring(5, value.indexOf(";base64,"));
                String payload = value.substring(value.indexOf(";base64,") + 8);
                byte[] bytes = decodeBase64(payload);
                if (bytes != null) {
                    return new InlinePayload(bytes, header, field);
                }
                continue;
            }

            if ("base64".equals(field) || "data".equals(field) || "fileData".equals(field)) {
                byte[] bytes = decodeBase64(value);
                if (bytes != null) {
                    return new InlinePayload(bytes, resolveMimeType(mediaObject), field);
                }
            }
        }

        return null;
    }

    private byte[] decodeBase64(String value) {
        try {
            String normalized = value.replaceAll("\\s", "");
            return Base64.getDecoder().decode(normalized);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private void removeInlineFields(ObjectNode mediaObject) {
        mediaObject.remove("data");
        mediaObject.remove("base64");
        mediaObject.remove("dataUrl");
        mediaObject.remove("sourceDataUrl");
        mediaObject.remove("fileData");

        String src = text(mediaObject, "src");
        if (StringUtils.hasText(src) && src.startsWith("data:")) {
            mediaObject.remove("src");
        }

        String source = text(mediaObject, "source");
        if (StringUtils.hasText(source) && source.startsWith("data:")) {
            mediaObject.remove("source");
        }
    }

    private void applyStoredMetadata(ObjectNode mediaObject, StoredMediaFile stored, String fileName, String contentType) {
        mediaObject.put("storageProvider", "backblaze-b2");
        mediaObject.put("storageKey", stored.storageKey());
        mediaObject.put("storageFileId", stored.storageFileId());
        mediaObject.put("fileName", fileName);
        if (StringUtils.hasText(contentType)) {
            mediaObject.put("mimeType", contentType);
        }
        mediaObject.remove("deliveryUrl");
    }

    private String resolveMimeType(ObjectNode mediaObject) {
        String mime = text(mediaObject, "mimeType");
        if (StringUtils.hasText(mime)) {
            return mime;
        }
        mime = text(mediaObject, "contentType");
        if (StringUtils.hasText(mime)) {
            return mime;
        }
        return "application/octet-stream";
    }

    private String resolveFileName(ObjectNode mediaObject, String mediaId, String contentType) {
        String fileName = text(mediaObject, "fileName");
        if (!StringUtils.hasText(fileName)) {
            fileName = text(mediaObject, "name");
        }
        if (!StringUtils.hasText(fileName)) {
            fileName = "media-" + sanitizeSegment(mediaId);
        }

        String clean = sanitizeSegment(fileName);
        if (!clean.contains(".")) {
            clean = clean + guessExtension(contentType);
        }
        return clean;
    }

    private String guessExtension(String contentType) {
        if (!StringUtils.hasText(contentType)) {
            return ".bin";
        }
        String lowered = contentType.toLowerCase();
        if (lowered.contains("png")) {
            return ".png";
        }
        if (lowered.contains("jpeg") || lowered.contains("jpg")) {
            return ".jpg";
        }
        if (lowered.contains("gif")) {
            return ".gif";
        }
        if (lowered.contains("webp")) {
            return ".webp";
        }
        if (lowered.contains("mp4")) {
            return ".mp4";
        }
        if (lowered.contains("webm")) {
            return ".webm";
        }
        if (lowered.contains("mpeg") || lowered.contains("mp3")) {
            return ".mp3";
        }
        if (lowered.contains("wav")) {
            return ".wav";
        }
        return ".bin";
    }

    private String buildStorageKey(Project project, String mediaId, String fileName) {
        return "projects/"
                + project.getOwner().getId()
                + "/"
                + project.getId()
                + "/media/"
                + sanitizeSegment(mediaId)
                + "/"
                + sanitizeSegment(fileName);
    }

    private String readMediaId(ObjectNode mediaObject) {
        String id = text(mediaObject, "id");
        if (StringUtils.hasText(id)) {
            return id;
        }
        id = text(mediaObject, "mediaId");
        if (StringUtils.hasText(id)) {
            return id;
        }
        id = text(mediaObject, "uuid");
        if (StringUtils.hasText(id)) {
            return id;
        }
        return null;
    }

    private String text(ObjectNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        String text = value.asText();
        return StringUtils.hasText(text) ? text : null;
    }

    private String sanitizeSegment(String input) {
        if (!StringUtils.hasText(input)) {
            return "unknown";
        }
        return input.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private record StoredMediaMetadata(String mediaId, String storageKey, String storageFileId) {
    }

    private record InlinePayload(byte[] bytes, String contentType, String sourceField) {
    }
}
