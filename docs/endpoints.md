# Endpoints — Complete API Reference

## Authentication

### POST /auth/register

Create a new user account and receive a JWT.

**Access:** Public

**Request Body:**
```json
{
  "firstName": "string (required)",
  "lastName": "string (required)",
  "email": "string (required, valid email)",
  "password": "string (required, min 8 chars)"
}
```

**Response:** `201 Created`
```json
{
  "token": "jwt-token-here",
  "id": 1,
  "firstName": "John",
  "lastName": "Doe",
  "email": "john@example.com",
  "role": "USER"
}
```

**Errors:** `409 Conflict` (email already exists), `400 Bad Request` (validation errors)

---

### POST /auth/login

Authenticate and receive a JWT.

**Access:** Public

**Request Body:**
```json
{
  "email": "string (required, valid email)",
  "password": "string (required)"
}
```

**Response:** `200 OK`
```json
{
  "token": "jwt-token-here",
  "id": 1,
  "firstName": "John",
  "lastName": "Doe",
  "email": "john@example.com",
  "role": "USER"
}
```

**Errors:** `401 Unauthorized` (invalid credentials)

---

### POST /auth/logout

Semantic logout (stateless — client discards token).

**Access:** Public

**Response:** `200 OK` (empty body)

---

### GET /auth/me

Get current authenticated user profile. Returns `{authenticated: false, user: null}` for invalid/missing tokens.

**Access:** Public (manually checks Authorization header)

**Request Header:** `Authorization: Bearer <token>` (optional)

**Response:** `200 OK`
```json
{
  "authenticated": true,
  "user": {
    "id": 1,
    "firstName": "John",
    "lastName": "Doe",
    "email": "john@example.com",
    "role": "USER"
  }
}
```

---

### POST /auth/recover/request

Initiate password recovery. Silently succeeds even if email doesn't exist (prevents email enumeration).

**Access:** Public

**Request Body:**
```json
{
  "email": "string (required, valid email)"
}
```

**Response:** `200 OK` (empty body)

---

### GET /auth/recover/validate?token={token}

Validate whether a recovery token is valid, not expired, and not already used.

**Access:** Public

**Query Param:** `token` (required)

**Response:** `200 OK`
```json
{
  "valid": true
}
```

---

### POST /auth/recover/reset

Reset password using a valid recovery token.

**Access:** Public

**Request Body:**
```json
{
  "token": "string (required)",
  "newPassword": "string (required, min 8 chars)",
  "confirmPassword": "string (required)"
}
```

**Response:** `200 OK` (empty body)

**Errors:** `404 Not Found` (token not found), `410 Gone` (token expired or already used), `400 Bad Request` (passwords don't match)

---

## Projects

### POST /projects

Create a new project for the authenticated user.

**Access:** Authenticated

**Request Body:**
```json
{
  "name": "string (required)",
  "timelineData": "string (optional JSON)"
}
```

**Response:** `201 Created`
```json
{
  "id": 1,
  "name": "My Project",
  "status": "DRAFT",
  "timelineData": "{...}",
  "ownerId": 1,
  "createdAt": "2025-01-01T00:00:00",
  "updatedAt": "2025-01-01T00:00:00"
}
```

---

### GET /projects/{id}

Get full project details (including timeline data). Accessible by owner or shared-with users.

**Access:** Authenticated

**Path Param:** `id` (Long)

**Response:** `200 OK` — `ProjectResponse`

**Errors:** `404 Not Found` (project not found or no access)

---

### GET /projects/{id}/media/{mediaId}

Download a media binary file from Backblaze B2 storage. Accessible by owner or shared-with users.

**Access:** Authenticated

**Path Params:** `id` (Long), `mediaId` (String)

**Response:** `200 OK` — `byte[]` with `Content-Type` and `Content-Disposition: inline` headers

**Errors:** `404 Not Found`

---

### GET /projects

List projects owned by the authenticated user (paginated).

**Access:** Authenticated

**Query Params:** `page`, `size`, `sort`

**Response:** `200 OK` — `Page<ProjectSummaryResponse>`

---

### GET /projects/shared

List projects shared with the authenticated user (paginated).

**Access:** Authenticated

**Query Params:** `page`, `size`, `sort`

**Response:** `200 OK` — `Page<ProjectSummaryResponse>`

---

### PATCH /projects/{id}

Partially update a project. Only owner can update.

**Access:** Authenticated (owner only)

**Path Param:** `id` (Long)

**Request Body:** (all fields optional)
```json
{
  "name": "string",
  "description": "string",
  "timelineData": "string (JSON)",
  "status": "DRAFT | SHARED | ARCHIVED"
}
```

**Response:** `200 OK` — `ProjectResponse`

**Errors:** `404 Not Found`, `403 Forbidden` (not owner)

---

### DELETE /projects/{id}

Delete a project and all associated data (media files, shares, history, tracks/media rows). Only owner can delete.

**Access:** Authenticated (owner only)

**Path Param:** `id` (Long)

**Response:** `204 No Content`

**Errors:** `404 Not Found`, `403 Forbidden` (not owner)

---

### POST /projects/{id}/share

Share a project with another user by email. Only owner can share. Prevents self-sharing and duplicate shares.

**Access:** Authenticated (owner only)

**Path Param:** `id` (Long)

**Request Body:**
```json
{
  "sharedWithEmail": "string (required)"
}
```

**Response:** `200 OK` (empty body)

**Errors:** `404 Not Found` (project or user), `403 Forbidden` (not owner), `409 Conflict` (self-share or duplicate)

---

## History

### GET /history/{projectId}

Get paginated history events for a project. Only the project owner can view history.

**Access:** Authenticated (owner only)

**Path Param:** `projectId` (Long)  
**Query Params:** `page`, `size`

**Response:** `200 OK` — `Page<ProjectHistoryResponse>`
```json
{
  "id": 1,
  "projectId": 1,
  "eventType": "CREATE",
  "timelineSnapshot": "...",
  "authorUserId": 1,
  "createdAt": "2025-01-01T00:00:00"
}
```

---

## Notifications

### GET /notifications

List all notifications for the authenticated user (paginated, newest first).

**Access:** Authenticated

**Query Params:** `page`, `size`

**Response:** `200 OK` — `Page<NotificationResponse>`
```json
{
  "id": 1,
  "type": "PROJECT_SHARED",
  "message": "A project was shared with you by john@example.com.",
  "read": false,
  "projectId": 1,
  "createdAt": "2025-01-01T00:00:00"
}
```

---

### GET /notifications/unread

List all unread notifications for the authenticated user.

**Access:** Authenticated

**Response:** `200 OK` — `List<NotificationResponse>`

---

### PATCH /notifications/{id}/read

Mark a notification as read. Only the recipient can mark it.

**Access:** Authenticated (recipient only)

**Path Param:** `id` (Long)

**Response:** `200 OK` — `NotificationResponse`

---

## Admin Reports

### GET /admin/reports?format={format}

Generate a platform-wide statistical report. Admin role required.

**Access:** Admin only (`@PreAuthorize("hasRole('ADMIN')")`)

**Query Param:** `format` — `JSON` (default) | `CSV` | `SUMMARY`

**Response (JSON):** `200 OK`
```json
{
  "totalUsers": 100,
  "totalProjects": 500,
  "totalProjectsCreated": 450,
  "totalProjectsEdited": 300,
  "totalProjectsExported": 50,
  "totalProjectsShared": 120,
  "generatedAt": "2025-01-01T00:00:00"
}
```

**Response (CSV):** `200 OK` with `Content-Type: text/csv` and `Content-Disposition: attachment; filename="report.csv"`

**Response (SUMMARY):** `200 OK`
```json
{
  "totalUsers": 100,
  "totalProjects": 500,
  "generatedAt": "2025-01-01T00:00:00"
}
```

**Errors:** `400 Bad Request` (invalid format), `403 Forbidden` (not admin)

---

## AI Audio

### POST /ai/audio/clean

Clean/remove noise from an audio file via the Flask AI microservice.

**Access:** Authenticated

**Content-Type:** `multipart/form-data`

**Form Fields:**
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `file` | MultipartFile | Yes | Audio file (max 50 MB, extensions: wav/mp3/ogg/flac/m4a) |
| `backend` | String | No | Noise reduction backend |
| `stationary` | Boolean | No | Stationary noise flag |
| `targetSr` | Integer | No | Target sample rate |

**Response:** `200 OK` — `byte[]` (processed audio binary, proxied from Flask)

**Errors:** `413 Content Too Large` (>50 MB), `415 Unsupported Media Type` (invalid extension/MIME), `502 Bad Gateway` (Flask error), `503 Service Unavailable` (Flask unreachable)

---

### POST /ai/audio/transcribe

Transcribe an audio file via the Flask AI microservice.

**Access:** Authenticated

**Content-Type:** `multipart/form-data`

**Form Fields:**
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `file` | MultipartFile | Yes | Audio file (max 50 MB, extensions: wav/mp3/ogg/flac/m4a) |
| `model` | String | No | Transcription model |
| `lang` | String | No | Language code |
| `formats` | List<String> | No | Output formats |

**Response:** `200 OK` — `byte[]` (transcription result, proxied from Flask)

**Errors:** Same as `/ai/audio/clean`

---

## OAuth2

### GET /oauth2/authorize/{provider}

Initiate OAuth2 login flow. Supported providers: `google`, `github`.

**Access:** Public

**Redirect:** Browser redirects to the OAuth2 provider's consent screen.

---

### GET /login/oauth2/code/{provider}

OAuth2 callback endpoint. After successful authentication, redirects to the frontend with a JWT token parameter.

**Access:** Public

**Redirect:** `{OAUTH2_REDIRECT_URL}?token={jwt}`

---

## Error Response Format

All errors follow a consistent envelope:

```json
{
  "status": 404,
  "error": "NOT_FOUND",
  "message": "Project not found: 123",
  "timestamp": "2025-01-01T00:00:00Z",
  "path": "/api/projects/123",
  "errors": null
}
```

For validation errors (`400 Bad Request`), the `errors` array is populated:

```json
{
  "status": 400,
  "error": "BAD_REQUEST",
  "message": "Validation failed",
  "timestamp": "2025-01-01T00:00:00Z",
  "path": "/api/auth/register",
  "errors": [
    { "field": "email", "message": "Invalid email format" },
    { "field": "password", "message": "Password must be at least 8 characters" }
  ]
}
```

## Error Code Summary

| HTTP Status | Error | Trigger |
|-------------|-------|---------|
| 400 | BAD_REQUEST | Validation errors, password mismatch, invalid report format |
| 401 | UNAUTHORIZED | Invalid credentials, invalid/expired JWT |
| 403 | FORBIDDEN | Not owner/admin, access denied |
| 404 | NOT_FOUND | User/project/token not found |
| 409 | CONFLICT | Email already exists, self-share, duplicate share |
| 410 | GONE | Recovery token expired or already used |
| 413 | CONTENT_TOO_LARGE | Audio file > 50 MB |
| 415 | UNSUPPORTED_MEDIA_TYPE | Invalid audio extension/MIME |
| 502 | BAD_GATEWAY | Flask service returned an error |
| 503 | SERVICE_UNAVAILABLE | Flask service unreachable |