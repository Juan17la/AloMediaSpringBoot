# 05 - API Reference (Non-Auth)

Base URL (local): `http://localhost:8080`

All routes below require valid authentication context, but auth endpoint contracts are intentionally excluded.

## 1. Common Error Response

```json
{
  "status": 400,
  "error": "BAD_REQUEST",
  "message": "Validation failed",
  "timestamp": "2026-04-02T16:20:31.010Z",
  "path": "/projects",
  "errors": [
    { "field": "name", "message": "must not be blank" }
  ]
}
```

## 2. AI Audio Endpoints

### 2.1 POST /ai/audio/clean

- Content-Type: multipart/form-data
- Parts:
  - file (required)
  - backend (optional)
  - stationary (optional string boolean)
  - targetSr (optional string integer)
- Success: binary body (cleaned audio)
- Typical errors: 413, 415, 502, 503

### 2.2 POST /ai/audio/transcribe

- Content-Type: multipart/form-data
- Parts:
  - file (required)
  - model (optional)
  - lang (optional)
  - formats (optional repeated values)
- Success: binary body with transcription output format from Flask
- Typical errors: 413, 415, 502, 503

## 3. Project Endpoints

### 3.1 POST /projects

Request:

```json
{
  "name": "Demo Project",
  "timelineData": "{\"version\":1,\"tracks\":[],\"media\":[]}"
}
```

Response 201:

```json
{
  "id": 1,
  "name": "Demo Project",
  "status": "DRAFT",
  "timelineData": "{\"version\":1,\"tracks\":[],\"media\":[]}",
  "ownerId": 12,
  "createdAt": "2026-04-02T15:10:21.123",
  "updatedAt": "2026-04-02T15:10:21.123"
}
```

### 3.2 GET /projects/{id}

Response 200: ProjectResponse

### 3.3 GET /projects

Query params:

- page (optional)
- size (optional)
- sort (optional)

Response 200: Spring Page of ProjectResponse

### 3.4 GET /projects/shared

Query params:

- page (optional)
- size (optional)
- sort (optional)

Response 200: Spring Page of ProjectResponse

### 3.5 PATCH /projects/{id}

Request:

```json
{
  "name": "Updated Name",
  "description": "Accepted by DTO",
  "timelineData": "{\"version\":2,\"tracks\":[...],\"media\":[...]}",
  "status": "ARCHIVED"
}
```

Response 200: ProjectResponse

### 3.6 DELETE /projects/{id}

Response 204: empty body

### 3.7 POST /projects/{id}/share

Request:

```json
{
  "sharedWithEmail": "collaborator@example.com"
}
```

Response 200: empty body

Conflict scenarios:

- Share with self
- Duplicate share target for same project

## 4. History Endpoint

### 4.1 GET /history/{projectId}

Response 200:

```json
[
  {
    "id": 51,
    "projectId": 10,
    "eventType": "CREATE",
    "timelineSnapshot": null,
    "authorUserId": 3,
    "createdAt": "2026-04-02T15:10:21.200"
  }
]
```

## 5. Notification Endpoints

### 5.1 GET /notifications

Response 200:

```json
[
  {
    "id": 7,
    "type": "PROJECT_SHARED",
    "message": "A project was shared with you by owner@example.com.",
    "read": false,
    "projectId": 10,
    "createdAt": "2026-04-02T16:00:00.000"
  }
]
```

### 5.2 GET /notifications/unread

Response 200: unread NotificationResponse array

### 5.3 PATCH /notifications/{id}/read

Response 200: empty body

## 6. Admin Report Endpoint

### 6.1 GET /admin/reports

Query:

- format: JSON | CSV | SUMMARY (default JSON)

Response variants:

- JSON: full report structure
- CSV: `text/csv` with attachment disposition
- SUMMARY: compact map

JSON example:

```json
{
  "totalUsers": 120,
  "totalProjects": 340,
  "totalProjectsCreated": 300,
  "totalProjectsEdited": 180,
  "totalProjectsExported": 45,
  "totalProjectsShared": 77,
  "generatedAt": "2026-04-02T16:12:22.004"
}
```
