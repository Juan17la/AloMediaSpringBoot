# 06 - AI Audio Feature

## 1. Feature Purpose

The AI module provides audio enhancement and transcription by integrating with an external Flask microservice. The Java backend acts as a secure orchestrator and contract boundary for frontend clients.

## 2. Backend Components

- Controller: `AudioAiController`
- Service: `AudioAiService`
- DTOs:
  - `AudioCleanRequest`
  - `AudioTranscribeRequest`
- External dependency: Flask service (`flask.base-url`)

## 3. Processing Pipeline

1. Frontend uploads audio file as multipart/form-data.
2. Controller maps optional params into request DTO.
3. Service validates file size/type and extension.
4. Service forwards multipart request to Flask endpoint.
5. Flask returns processed bytes and metadata headers.
6. Spring response mirrors status/headers/body to frontend.

## 4. Input Validation Rules

### File constraints

- Required and non-empty
- Maximum size: 50 MB

### Extension allow-list

- wav
- mp3
- ogg
- flac
- m4a

### MIME allow-list

- audio/wav
- audio/mpeg
- audio/ogg
- audio/flac
- audio/x-m4a
- audio/mp4

Validation failures return typed errors via global exception handler.

## 5. Endpoint Behavior Details

## 5.1 Clean Audio

Route: `POST /ai/audio/clean`

Request parts:

- file (required)
- backend (optional)
- stationary (optional)
- targetSr (optional)

Forwarded Flask route:

- `POST {flask.base-url}/audio/clean`

Expected result:

- Binary audio output (cleaned track)

## 5.2 Transcribe Audio

Route: `POST /ai/audio/transcribe`

Request parts:

- file (required)
- model (optional)
- lang (optional)
- formats (optional repeated parameter)

Forwarded Flask route:

- `POST {flask.base-url}/audio/transcribe`

Expected result:

- Binary transcription payload (format depends on Flask options)

## 6. Error Mapping

### Integration errors

- Flask returns 4xx/5xx: mapped to 502 Bad Gateway with Flask context message.
- Flask unreachable/timeouts: mapped to 503 Service Unavailable.

### Validation errors

- Unsupported type: 415 Unsupported Media Type.
- Payload too large: 413 Payload Too Large.

## 7. Performance and Reliability Considerations

- RestTemplate timeout profile:
  - Connect timeout: 10 seconds
  - Read timeout: 120 seconds
- Large file transfer increases memory pressure due byte-array buffering.
- For larger workloads, streaming and async/offline jobs should be considered.

## 8. Frontend UX Recommendations for AI Calls

- Show upload progress and server-processing state separately.
- For long transcriptions, provide cancel/retry controls.
- Handle binary downloads with explicit filename/content-type logic.
- Distinguish validation errors from upstream service errors in UI messaging.
