# API Endpoints Reference

## Overview

The AloMedia API is a RESTful HTTP API. All requests and responses use JSON unless otherwise noted (e.g., the CSV report endpoint). Authentication is handled via JWT tokens passed in the `Authorization` header as a Bearer token.

### Base URL

The application serves all endpoints directly from the root. In a local development setup this is typically `http://localhost:8080`.

### Authentication Header

For all protected endpoints, include the JWT token obtained from login or registration:

```
Authorization: Bearer <your_jwt_token>
```

### Standard Error Response

When an error occurs, the API returns a consistent error envelope regardless of the endpoint:

```json
{
  "status": 400,
  "error": "BAD_REQUEST",
  "message": "Human-readable description of the error",
  "timestamp": "2026-03-22T15:00:00",
  "path": "/projects/42",
  "errors": [
    { "field": "name", "message": "must not be blank" }
  ]
}
```

The `errors` array is only populated for validation failures. For other errors, it is empty.

---

## Authentication Endpoints

These endpoints handle user registration, login, logout, OAuth2 flows, and password recovery. Most are publicly accessible without a JWT token.

---

### `POST /auth/register`

Registers a new user account using email and password. On success, the user is immediately logged in and receives a JWT token — no email verification step is required.

**Authentication Required**: No

**Request Body**:

```json
{
  "firstName": "Maria",
  "lastName": "Garcia",
  "email": "maria@example.com",
  "password": "securePassword123"
}
```

| Field | Type | Required | Constraints |
|---|---|---|---|
| `firstName` | string | Yes | Not blank |
| `lastName` | string | Yes | Not blank |
| `email` | string | Yes | Valid email format, unique |
| `password` | string | Yes | Minimum 8 characters |

**Success Response** — `201 Created`:

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "id": 1,
  "firstName": "Maria",
  "lastName": "Garcia",
  "email": "maria@example.com",
  "role": "USER"
}
```

**Possible Errors**:
- `409 Conflict` — An account with that email already exists.
- `400 Bad Request` — Validation failed (e.g., password too short, invalid email format).

---

### `POST /auth/login`

Authenticates an existing user with their email and password. Returns a JWT token to be used for subsequent authenticated requests.

**Authentication Required**: No

**Request Body**:

```json
{
  "email": "maria@example.com",
  "password": "securePassword123"
}
```

**Success Response** — `200 OK`:

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "id": 1,
  "firstName": "Maria",
  "lastName": "Garcia",
  "email": "maria@example.com",
  "role": "USER"
}
```

**Possible Errors**:
- `401 Unauthorized` — Invalid email or password combination.

---

### `POST /auth/logout`

A convenience endpoint for logout. Because authentication is stateless (JWT-based), the server does not maintain session state. The actual logout happens client-side by discarding the token. This endpoint exists as a semantic hook for any client-side cleanup logic.

**Authentication Required**: Optional

**Request Body**: None

**Success Response** — `200 OK`: Empty body.

---

### `GET /auth/me`

Returns the currently authenticated user's profile. If the `Authorization` header is present and valid, the full user object is returned. If no token is provided or the token is invalid, the response still returns `200 OK` but with `authenticated: false`.

This design is intentional — it allows the frontend to call this endpoint on load to determine whether the user is logged in without triggering a 401 error.

**Authentication Required**: No (but returns user data only if a valid token is provided)

**Request Body**: None

**Success Response — Authenticated** — `200 OK`:

```json
{
  "authenticated": true,
  "user": {
    "id": 1,
    "firstName": "Maria",
    "lastName": "Garcia",
    "email": "maria@example.com",
    "role": "USER"
  }
}
```

**Success Response — Not Authenticated** — `200 OK`:

```json
{
  "authenticated": false,
  "user": null
}
```

---

### `POST /auth/recover/request`

Initiates the password recovery flow for a given email address. The server generates a time-limited recovery token (valid for 15 minutes), saves it to the database, and sends an email to the user containing a recovery link.

**Authentication Required**: No

**Request Body**:

```json
{
  "email": "maria@example.com"
}
```

**Success Response** — `200 OK`: Empty body. The response is deliberately the same whether or not the email exists, to prevent user enumeration attacks.

---

### `GET /auth/recover/validate`

Checks whether a given recovery token is still valid (i.e., exists in the database, has not expired, and has not already been used). This endpoint is typically called when the user clicks the recovery link in their email and the frontend needs to confirm the token is usable before showing the reset password form.

**Authentication Required**: No

**Query Parameters**:

| Parameter | Type | Required | Description |
|---|---|---|---|
| `token` | string | Yes | The recovery token from the email link |

**Example**: `GET /auth/recover/validate?token=abc123xyz`

**Success Response** — `200 OK`:

```json
{
  "valid": true
}
```

**Possible Errors**:
- `404 Not Found` — Token does not exist.
- `410 Gone` — Token has expired or has already been used.

---

### `POST /auth/recover/reset`

Completes the password recovery flow. Validates the recovery token and, if valid, updates the user's password to the new value provided. The token is then marked as used and cannot be reused.

**Authentication Required**: No

**Request Body**:

```json
{
  "token": "abc123xyz",
  "newPassword": "newSecurePassword456",
  "confirmPassword": "newSecurePassword456"
}
```

| Field | Type | Required | Constraints |
|---|---|---|---|
| `token` | string | Yes | Must be a valid, unexpired, unused token |
| `newPassword` | string | Yes | Minimum 8 characters |
| `confirmPassword` | string | Yes | Must match `newPassword` |

**Success Response** — `200 OK`: Empty body.

**Possible Errors**:
- `400 Bad Request` — `newPassword` and `confirmPassword` do not match.
- `404 Not Found` — Token does not exist.
- `410 Gone` — Token expired or already used.

---

## Project Endpoints

Projects are the core resource in AloMedia. A project belongs to an owner and contains timeline data stored as a JSON string. Projects can be shared with other users, and all actions on a project are tracked in a history log.

All project endpoints require authentication.

---

### `POST /projects`

Creates a new project for the authenticated user. The user making the request automatically becomes the project owner. The project is created in `DRAFT` status. If no `timelineData` is provided, a default empty timeline structure is used: `{"version":1,"tracks":[],"media":[]}`.

**Authentication Required**: Yes

**Request Body**:

```json
{
  "name": "My First Timeline",
  "timelineData": "{\"version\":1,\"tracks\":[],\"media\":[]}"
}
```

| Field | Type | Required | Description |
|---|---|---|---|
| `name` | string | Yes | Display name of the project |
| `timelineData` | string | No | JSON string representing the timeline |

**Success Response** — `201 Created`:

```json
{
  "id": 10,
  "name": "My First Timeline",
  "status": "DRAFT",
  "timelineData": "{\"version\":1,\"tracks\":[],\"media\":[]}",
  "ownerId": 1,
  "createdAt": "2026-03-22T10:00:00",
  "updatedAt": "2026-03-22T10:00:00"
}
```

A `CREATE` event is automatically recorded in the project history upon creation.

---

### `GET /projects/{id}`

Retrieves a single project by its ID. Only the **owner** of the project can retrieve it through this endpoint. Users who have been given shared access to a project should use `GET /projects/shared` instead.

**Authentication Required**: Yes

**Path Parameters**:

| Parameter | Type | Description |
|---|---|---|
| `id` | long | The project ID |

**Success Response** — `200 OK`:

```json
{
  "id": 10,
  "name": "My First Timeline",
  "status": "DRAFT",
  "timelineData": "{\"version\":1,\"tracks\":[{\"id\":\"t1\"}],\"media\":[]}",
  "ownerId": 1,
  "createdAt": "2026-03-22T10:00:00",
  "updatedAt": "2026-03-22T11:30:00"
}
```

**Possible Errors**:
- `404 Not Found` — Project does not exist or the authenticated user is not the owner.

---

### `GET /projects`

Returns a paginated list of all projects **owned by** the authenticated user. Shared projects are not included here.

**Authentication Required**: Yes

**Query Parameters (Pagination)**:

| Parameter | Type | Default | Description |
|---|---|---|---|
| `page` | int | 0 | Zero-based page index |
| `size` | int | 20 | Number of items per page |
| `sort` | string | — | Field and direction, e.g. `createdAt,desc` |

**Example**: `GET /projects?page=0&size=10&sort=createdAt,desc`

**Success Response** — `200 OK`:

```json
{
  "content": [
    {
      "id": 10,
      "name": "My First Timeline",
      "status": "DRAFT",
      "timelineData": "...",
      "ownerId": 1,
      "createdAt": "2026-03-22T10:00:00",
      "updatedAt": "2026-03-22T11:30:00"
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "size": 10,
  "number": 0
}
```

---

### `GET /projects/shared`

Returns a paginated list of projects that have been **shared with** the authenticated user by other users. This is separate from the owned projects list.

**Authentication Required**: Yes

**Query Parameters**: Same pagination parameters as `GET /projects`.

**Success Response** — `200 OK`: Same paginated structure as `GET /projects`.

---

### `PATCH /projects/{id}`

Partially updates an existing project. Only the **owner** can update a project. All fields are optional — only the fields included in the request body will be updated. An `EDIT` event is recorded in the project history.

**Authentication Required**: Yes

**Path Parameters**:

| Parameter | Type | Description |
|---|---|---|
| `id` | long | The project ID |

**Request Body** (all fields optional):

```json
{
  "name": "Renamed Project",
  "timelineData": "{\"version\":1,\"tracks\":[{\"id\":\"t1\",\"name\":\"Track 1\"}],\"media\":[]}",
  "status": "SHARED"
}
```

| Field | Type | Description |
|---|---|---|
| `name` | string | New display name |
| `timelineData` | string | Updated timeline JSON |
| `status` | enum | `DRAFT`, `SHARED`, or `ARCHIVED` |

**Success Response** — `200 OK`: The updated `ProjectResponse` object.

**Possible Errors**:
- `404 Not Found` — Project not found or user is not the owner.

---

### `DELETE /projects/{id}`

Permanently deletes a project. Only the **owner** can delete a project. This action is irreversible.

**Authentication Required**: Yes

**Path Parameters**:

| Parameter | Type | Description |
|---|---|---|
| `id` | long | The project ID |

**Success Response** — `204 No Content`: Empty body.

**Possible Errors**:
- `404 Not Found` — Project not found or user is not the owner.

---

### `POST /projects/{id}/share`

Shares a project with another registered user, identified by their email address. Only the **owner** can share a project. A project cannot be shared with the owner themselves, and sharing the same project with the same user twice is not allowed.

When a project is shared:
1. A `ProjectShare` record is created linking the project, the sharer, and the recipient.
2. The recipient receives an in-app notification (stored in the `notifications` table).
3. The recipient receives an email notification.
4. A `SHARE` event is recorded in the project's history.

**Authentication Required**: Yes

**Path Parameters**:

| Parameter | Type | Description |
|---|---|---|
| `id` | long | The project ID |

**Request Body**:

```json
{
  "sharedWithEmail": "collaborator@example.com"
}
```

**Success Response** — `200 OK`: Empty body.

**Possible Errors**:
- `404 Not Found` — Project not found, user is not the owner, or recipient email is not registered.
- `409 Conflict` — Project is already shared with that user.
- `400 Bad Request` — Trying to share with yourself.

---

## Notification Endpoints

Notifications are automatically created when a project is shared with a user. They can be retrieved and marked as read.

All notification endpoints require authentication.

---

### `GET /notifications`

Returns all notifications for the authenticated user, ordered from newest to oldest.

**Authentication Required**: Yes

**Success Response** — `200 OK`:

```json
[
  {
    "id": 5,
    "type": "PROJECT_SHARED",
    "message": "User john@example.com shared project 'My Timeline' with you.",
    "read": false,
    "projectId": 10,
    "createdAt": "2026-03-22T14:00:00"
  }
]
```

---

### `GET /notifications/unread`

Returns only the **unread** notifications for the authenticated user. This is useful for displaying a notification badge count or a filtered unread view.

**Authentication Required**: Yes

**Success Response** — `200 OK`: Same structure as `GET /notifications`, but filtered to `read: false` entries only.

---

### `PATCH /notifications/{id}/read`

Marks a specific notification as read. Only the **recipient** of the notification can mark it as read. Attempting to mark another user's notification as read will result in a 403 error.

**Authentication Required**: Yes

**Path Parameters**:

| Parameter | Type | Description |
|---|---|---|
| `id` | long | The notification ID |

**Success Response** — `200 OK`: Empty body.

**Possible Errors**:
- `403 Forbidden` — The authenticated user is not the recipient of this notification.
- `404 Not Found` — Notification does not exist.

---

## Project History Endpoint

Every significant action on a project (create, edit, share, export) is recorded as a history entry. This provides a full audit trail.

---

### `GET /history/{projectId}`

Returns the complete history log for a given project. Only the **owner** of the project can view its history.

**Authentication Required**: Yes

**Path Parameters**:

| Parameter | Type | Description |
|---|---|---|
| `projectId` | long | The project ID |

**Success Response** — `200 OK`:

```json
[
  {
    "id": 1,
    "projectId": 10,
    "eventType": "CREATE",
    "timelineSnapshot": "{\"version\":1,\"tracks\":[],\"media\":[]}",
    "authorUserId": 1,
    "createdAt": "2026-03-22T10:00:00"
  },
  {
    "id": 2,
    "projectId": 10,
    "eventType": "EDIT",
    "timelineSnapshot": "{\"version\":1,\"tracks\":[{\"id\":\"t1\"}],\"media\":[]}",
    "authorUserId": 1,
    "createdAt": "2026-03-22T11:30:00"
  },
  {
    "id": 3,
    "projectId": 10,
    "eventType": "SHARE",
    "timelineSnapshot": null,
    "authorUserId": 1,
    "createdAt": "2026-03-22T14:00:00"
  }
]
```

Each entry includes a `timelineSnapshot` — a JSON string capturing the state of the timeline at the moment the event occurred. This is useful for implementing undo functionality or reviewing the evolution of a project.

**Possible Errors**:
- `404 Not Found` — Project not found or authenticated user is not the owner.

---

## Admin Report Endpoint

This endpoint is restricted to users with the `ADMIN` role. It provides aggregate statistics about the platform's usage.

---

### `GET /admin/reports`

Generates a usage report for the platform. The response format can be specified via a query parameter, allowing the same data to be returned as structured JSON, a raw CSV string, or an abbreviated summary.

**Authentication Required**: Yes — must have `ADMIN` role.

**Query Parameters**:

| Parameter | Type | Required | Allowed Values |
|---|---|---|---|
| `format` | string | Yes | `JSON`, `CSV`, `SUMMARY` |

**Example**: `GET /admin/reports?format=JSON`

---

**Format: `JSON`**

Returns the full report as a JSON object.

```json
{
  "totalUsers": 150,
  "totalProjects": 320,
  "totalProjectsCreated": 320,
  "totalProjectsEdited": 210,
  "totalProjectsExported": 45,
  "totalProjectsShared": 88,
  "generatedAt": "2026-03-22T15:00:00"
}
```

---

**Format: `CSV`**

Returns a plain-text CSV string. The `Content-Type` header in the response is set to `text/csv`.

```
totalUsers,totalProjects,totalProjectsCreated,totalProjectsEdited,totalProjectsExported,totalProjectsShared,generatedAt
150,320,320,210,45,88,2026-03-22T15:00:00
```

---

**Format: `SUMMARY`**

Returns a lightweight summary with only the top-level counts and generation timestamp.

```json
{
  "totalUsers": 150,
  "totalProjects": 320,
  "generatedAt": "2026-03-22T15:00:00"
}
```

**Possible Errors**:
- `400 Bad Request` — Unsupported format value.
- `403 Forbidden` — Authenticated user does not have the `ADMIN` role.
