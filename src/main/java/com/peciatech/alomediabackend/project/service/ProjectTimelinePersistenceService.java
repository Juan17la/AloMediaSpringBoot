package com.peciatech.alomediabackend.project.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.peciatech.alomediabackend.project.entity.Project;
import com.peciatech.alomediabackend.project.entity.ProjectMedia;
import com.peciatech.alomediabackend.project.entity.ProjectTracks;
import com.peciatech.alomediabackend.project.repository.ProjectMediaRepository;
import com.peciatech.alomediabackend.project.repository.ProjectTracksRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectTimelinePersistenceService {

    private static final String DEFAULT_FULL_TIMELINE = "{\"version\":1,\"tracks\":[],\"media\":[]}";
    private static final String DEFAULT_METADATA = "{\"version\":1}";

    private final ObjectMapper objectMapper;
    private final ProjectTracksRepository projectTracksRepository;
    private final ProjectMediaRepository projectMediaRepository;

    public String normalizeIncomingTimeline(String incomingTimelineData) {
        return StringUtils.hasText(incomingTimelineData) ? incomingTimelineData : DEFAULT_FULL_TIMELINE;
    }

    public String buildFullTimeline(Project project) {
        try {
            ObjectNode merged = buildFullTimelineNode(project);
            return objectMapper.writeValueAsString(merged);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to rebuild full project timeline", ex);
        }
    }

    public ObjectNode buildFullTimelineNode(Project project) {
        JsonNode metadataRoot = parseObjectOrNull(project.getTimelineData());
        if (metadataRoot == null) {
            try {
                return (ObjectNode) objectMapper.readTree(DEFAULT_FULL_TIMELINE);
            } catch (Exception ex) {
                throw new RuntimeException("Failed to parse default timeline", ex);
            }
        }

        JsonNode tracksNode = resolveTracksNode(project.getId(), metadataRoot);
        JsonNode mediaNode = resolveMediaNode(project.getId(), metadataRoot);

        ObjectNode merged = objectMapper.createObjectNode();
        merged.setAll((ObjectNode) metadataRoot.deepCopy());
        merged.set("tracks", tracksNode);
        merged.set("media", mediaNode);
        return merged;
    }

    public Map<Long, String> buildFullTimelinesForProjects(List<Project> projects) {
        List<Long> projectIds = projects.stream().map(Project::getId).toList();

        Map<Long, String> tracksMap = projectTracksRepository.findByProjectIdIn(projectIds).stream()
                .collect(Collectors.toMap(t -> t.getProject().getId(), ProjectTracks::getTracksData, (a, b) -> a));

        Map<Long, String> mediaMap = projectMediaRepository.findByProjectIdIn(projectIds).stream()
                .collect(Collectors.toMap(m -> m.getProject().getId(), ProjectMedia::getMediaData, (a, b) -> a));

        return projects.stream().collect(Collectors.toMap(
                Project::getId,
                p -> buildFullTimelineString(p, tracksMap.get(p.getId()), mediaMap.get(p.getId()))
        ));
    }

    private String buildFullTimelineString(Project project, String tracksData, String mediaData) {
        try {
            JsonNode metadataRoot = parseObjectOrNull(project.getTimelineData());
            if (metadataRoot == null) {
                return DEFAULT_FULL_TIMELINE;
            }

        JsonNode tracksNode = resolveTracksNode(metadataRoot, tracksData);
        JsonNode mediaNode = resolveMediaNode(metadataRoot, mediaData);

            ObjectNode merged = objectMapper.createObjectNode();
            merged.setAll((ObjectNode) metadataRoot.deepCopy());
            merged.set("tracks", tracksNode);
            merged.set("media", mediaNode);
            return objectMapper.writeValueAsString(merged);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to rebuild full project timeline", ex);
        }
    }

    public void persistSplitTimeline(Project project, String fullTimelineData) {
        try {
            SplitTimeline splitTimeline = splitTimeline(fullTimelineData);

            if (!splitTimeline.metadataJson().equals(project.getTimelineData())) {
                project.setTimelineData(splitTimeline.metadataJson());
            }

            upsertTracks(project, splitTimeline.tracksJson(), splitTimeline.tracksHash());
            upsertMedia(project, splitTimeline.mediaJson(), splitTimeline.mediaHash());
        } catch (Exception ex) {
            throw new RuntimeException("Failed to persist split project timeline", ex);
        }
    }

    public void deleteByProjectId(Long projectId) {
        projectTracksRepository.deleteByProjectId(projectId);
        projectMediaRepository.deleteByProjectId(projectId);
    }

    private void upsertTracks(Project project, String tracksJson, String tracksHash) {
        ProjectTracks tracks = projectTracksRepository.findByProjectId(project.getId()).orElse(null);
        if (tracks == null) {
            ProjectTracks created = new ProjectTracks();
            created.setProject(project);
            created.setTracksData(tracksJson);
            created.setTracksHash(tracksHash);
            projectTracksRepository.save(created);
            return;
        }

        if (!tracksHash.equals(tracks.getTracksHash())) {
            tracks.setTracksData(tracksJson);
            tracks.setTracksHash(tracksHash);
            projectTracksRepository.save(tracks);
        }
    }

    private void upsertMedia(Project project, String mediaJson, String mediaHash) {
        ProjectMedia media = projectMediaRepository.findByProjectId(project.getId()).orElse(null);
        if (media == null) {
            ProjectMedia created = new ProjectMedia();
            created.setProject(project);
            created.setMediaData(mediaJson);
            created.setMediaHash(mediaHash);
            projectMediaRepository.save(created);
            return;
        }

        if (!mediaHash.equals(media.getMediaHash())) {
            media.setMediaData(mediaJson);
            media.setMediaHash(mediaHash);
            projectMediaRepository.save(media);
        }
    }

    private JsonNode resolveTracksNode(Long projectId, JsonNode metadataRoot) {
        ProjectTracks persistedTracks = projectTracksRepository.findByProjectId(projectId).orElse(null);
        return resolveTracksNode(metadataRoot, persistedTracks != null ? persistedTracks.getTracksData() : null);
    }

    private JsonNode resolveTracksNode(JsonNode metadataRoot, String preloadedTracksData) {
        if (StringUtils.hasText(preloadedTracksData)) {
            return parseArrayOrEmpty(preloadedTracksData);
        }
        JsonNode legacyTracks = metadataRoot.path("tracks");
        return legacyTracks.isArray() ? legacyTracks.deepCopy() : objectMapper.createArrayNode();
    }

    private JsonNode resolveMediaNode(Long projectId, JsonNode metadataRoot) {
        ProjectMedia persistedMedia = projectMediaRepository.findByProjectId(projectId).orElse(null);
        return resolveMediaNode(metadataRoot, persistedMedia != null ? persistedMedia.getMediaData() : null);
    }

    private JsonNode resolveMediaNode(JsonNode metadataRoot, String preloadedMediaData) {
        if (StringUtils.hasText(preloadedMediaData)) {
            return parseArrayOrEmpty(preloadedMediaData);
        }
        JsonNode legacyMedia = metadataRoot.path("media");
        return legacyMedia.isArray() ? legacyMedia.deepCopy() : objectMapper.createArrayNode();
    }

    private SplitTimeline splitTimeline(String fullTimelineData) throws Exception {
        JsonNode parsed = objectMapper.readTree(normalizeIncomingTimeline(fullTimelineData));
        if (!(parsed instanceof ObjectNode root)) {
            return defaultSplitTimeline();
        }

        JsonNode tracksNode = root.path("tracks").isArray() ? root.path("tracks") : objectMapper.createArrayNode();
        JsonNode mediaNode = root.path("media").isArray() ? root.path("media") : objectMapper.createArrayNode();

        ObjectNode metadataNode = root.deepCopy();
        metadataNode.remove("tracks");
        metadataNode.remove("media");

        String metadataJson = metadataNode.size() == 0 ? DEFAULT_METADATA : objectMapper.writeValueAsString(metadataNode);
        String tracksJson = objectMapper.writeValueAsString(tracksNode);
        String mediaJson = objectMapper.writeValueAsString(mediaNode);

        return new SplitTimeline(
                metadataJson,
                tracksJson,
                mediaJson,
                sha256Hex(canonicalizeJson(tracksNode)),
                sha256Hex(canonicalizeJson(mediaNode))
        );
    }

    private SplitTimeline defaultSplitTimeline() {
        try {
            return splitTimeline(DEFAULT_FULL_TIMELINE);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to initialize default split timeline", ex);
        }
    }

    private JsonNode parseObjectOrNull(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(raw);
            return node.isObject() ? node : null;
        } catch (Exception ex) {
            return null;
        }
    }

    private JsonNode parseArrayOrEmpty(String raw) {
        if (!StringUtils.hasText(raw)) {
            return objectMapper.createArrayNode();
        }
        try {
            JsonNode node = objectMapper.readTree(raw);
            return node.isArray() ? node : objectMapper.createArrayNode();
        } catch (Exception ex) {
            return objectMapper.createArrayNode();
        }
    }

    private String canonicalizeJson(JsonNode node) {
        if (node == null || node.isNull()) {
            return "null";
        }
        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            StringBuilder builder = new StringBuilder();
            builder.append('{');
            Iterator<Map.Entry<String, JsonNode>> fields = objectNode.fields();
            fieldsToCanonical(builder, fields);
            builder.append('}');
            return builder.toString();
        }
        if (node.isArray()) {
            ArrayNode arrayNode = (ArrayNode) node;
            StringBuilder builder = new StringBuilder();
            builder.append('[');
            for (int i = 0; i < arrayNode.size(); i++) {
                if (i > 0) {
                    builder.append(',');
                }
                builder.append(canonicalizeJson(arrayNode.get(i)));
            }
            builder.append(']');
            return builder.toString();
        }
        return node.toString();
    }

    private void fieldsToCanonical(StringBuilder builder, Iterator<Map.Entry<String, JsonNode>> fields) {
        fieldsToStream(fields)
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .forEach(entry -> {
                    if (builder.charAt(builder.length() - 1) != '{') {
                        builder.append(',');
                    }
                    builder.append('"').append(entry.getKey()).append('"').append(':');
                    builder.append(canonicalizeJson(entry.getValue()));
                });
    }

    private java.util.stream.Stream<Map.Entry<String, JsonNode>> fieldsToStream(Iterator<Map.Entry<String, JsonNode>> fields) {
        Iterable<Map.Entry<String, JsonNode>> iterable = () -> fields;
        return java.util.stream.StreamSupport.stream(iterable.spliterator(), false);
    }

    private String sha256Hex(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm is not available", ex);
        }
    }

    private record SplitTimeline(String metadataJson, String tracksJson, String mediaJson, String tracksHash,
                                 String mediaHash) {
    }
}
