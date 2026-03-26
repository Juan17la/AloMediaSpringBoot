# Entities

## What Are Entities?

In this project, entities are Java classes annotated with `@Entity` that map directly to database tables. They are managed by Hibernate (via Spring Data JPA) and represent the persistent state of the application. Each entity corresponds to a table in PostgreSQL, and each field annotated with `@Column` (or inferred) corresponds to a column.

Entities are the lowest level of the data model. They are not returned directly to API clients — instead, they are mapped to DTOs (Data Transfer Objects) before leaving the service layer.

---

## User

**Table**: `users`
**File**: `user/entity/User.java`
**Implements**: Spring Security's `UserDetails`

The `User` entity is the central record of the system. Almost every other entity references a user in some way — as an owner, a sharer, or a recipient. It also acts as a Spring Security principal, which means it is used directly by the authentication framework to check whether a user is authorized to perform an action.

### Fields

| Column | Java Type | Nullable | Unique | Notes |
|---|---|---|---|---|
| `id` | `Long` | No | Yes (PK) | Auto-incremented primary key |
| `first_name` | `String` | No | No | User's given name |
| `last_name` | `String` | No | No | User's family name |
| `email` | `String` | No | Yes | Used as the login identifier |
| `password` | `String` | No | No | BCrypt-encoded. OAuth2 users store a random UUID here |
| `role` | `Role` (enum) | No | No | `USER` or `ADMIN`, stored as a string |
| `enabled` | `boolean` | No | No | Defaults to `true`. Used by Spring Security to allow/deny login |
| `provider` | `String` | No | No | `"local"` for email/password users, `"google"` or `"github"` for OAuth2 |

### Relationships

The `User` entity does not declare JPA relationships to other entities. Instead, the dependent entities (`Project`, `ProjectShare`, `RecoveryToken`) own the foreign key relationship and point to `User`. This keeps the `User` entity clean and prevents Hibernate from accidentally loading entire object graphs.

### Spring Security Integration

Because `User` implements `UserDetails`, it plugs directly into Spring Security's authentication pipeline:

- `getUsername()` returns `email` — the unique identifier used during authentication.
- `getPassword()` returns the BCrypt-encoded password.
- `getAuthorities()` returns a single `SimpleGrantedAuthority` built from the role, prefixed with `ROLE_` (e.g., `ROLE_USER`, `ROLE_ADMIN`).
- `isAccountNonExpired()`, `isAccountNonLocked()`, `isCredentialsNonExpired()` all return `true`.
- `isEnabled()` returns the `enabled` field value.

---

## RecoveryToken

**Table**: `recovery_tokens`
**File**: `user/entity/RecoveryToken.java`

The `RecoveryToken` entity represents a one-time-use password reset token. When a user requests password recovery, a new token record is created. The token has a limited lifespan (15 minutes) and can only be used once.

### Fields

| Column | Java Type | Nullable | Unique | Notes |
|---|---|---|---|---|
| `id` | `Long` | No | Yes (PK) | Auto-incremented primary key |
| `token` | `String` | No | Yes | The recovery string sent to the user's email |
| `user_id` | FK → `users` | No | No | The user this token belongs to |
| `expires_at` | `LocalDateTime` | No | No | Token expiry time (15 minutes from creation) |
| `used` | `boolean` | No | No | Defaults to `false`. Set to `true` after a successful password reset |

### Relationships

- **Many-to-One with User**: A user can have multiple recovery tokens over time (e.g., if they request recovery multiple times). The `user_id` foreign key points to the `users` table. The relationship is `LAZY` loaded — Hibernate won't fetch the full `User` object unless explicitly accessed.

### Lifecycle

A recovery token is created, used once, and then becomes permanently invalid. The `used` flag ensures that even if an attacker intercepts a token, it cannot be reused after the legitimate user has already reset their password.

---

## Project

**Table**: `projects`
**File**: `project/entity/Project.java`

The `Project` entity is the main data artifact of the platform. It represents a timeline project that a user creates and manages. The actual timeline content is stored as a JSON string in the `timelineData` field, keeping the schema flexible and forward-compatible as the timeline format evolves.

### Fields

| Column | Java Type | Nullable | Unique | Notes |
|---|---|---|---|---|
| `id` | `Long` | No | Yes (PK) | Auto-incremented primary key |
| `name` | `String` | No | No | Display name of the project |
| `status` | `ProjectStatus` (enum) | No | No | `DRAFT`, `SHARED`, or `ARCHIVED` |
| `timeline_data` | `String` (TEXT) | Yes | No | JSON string storing the full timeline structure |
| `created_at` | `LocalDateTime` | No | No | Set automatically on insert (`@CreationTimestamp`) |
| `updated_at` | `LocalDateTime` | No | No | Updated automatically on every save (`@UpdateTimestamp`) |
| `owner_id` | FK → `users` | No | No | The user who created and owns this project |

### Relationships

- **Many-to-One with User (owner)**: Many projects can belong to one user. The `owner_id` column stores the foreign key. This relationship is `LAZY` loaded.

### Timeline Data

The `timelineData` column is declared as `TEXT` (a PostgreSQL CLOB-like type that supports large strings). The column stores whatever JSON the frontend sends. The default value when creating a project without specifying timeline data is:

```json
{"version": 1, "tracks": [], "media": []}
```

This structure is defined by the frontend application and is treated as an opaque string by the backend — the backend stores and returns it without parsing or validating its internal structure.

### Status Transitions

A project starts in `DRAFT` status. When it is shared with another user, the status can be updated to `SHARED`. When the owner is done with it, they can archive it by setting the status to `ARCHIVED`.

---

## ProjectShare

**Table**: `project_shares`
**File**: `project/entity/ProjectShare.java`

`ProjectShare` is a join entity that represents the act of one user sharing a project with another user. It captures not just the relationship (which project, which recipient) but also who performed the share action and when.

### Fields

| Column | Java Type | Nullable | Unique | Notes |
|---|---|---|---|---|
| `id` | `Long` | No | Yes (PK) | Auto-incremented primary key |
| `project_id` | FK → `projects` | No | No | The project being shared |
| `shared_by_id` | FK → `users` | No | No | The user who initiated the share (always the owner) |
| `shared_with_id` | FK → `users` | No | No | The recipient of the share |
| `shared_at` | `LocalDateTime` | No | No | Timestamp of when the share was created |

### Constraints

A unique constraint is defined on `(project_id, shared_with_id)`. This enforces at the database level that the same project cannot be shared with the same user more than once, even if the application-level check is somehow bypassed.

### Relationships

- **Many-to-One with Project**: Many share records can reference the same project.
- **Many-to-One with User (sharedBy)**: Stores who performed the share.
- **Many-to-One with User (sharedWith)**: Stores who the project was shared with.

All three relationships use `LAZY` fetching to avoid unnecessary joins when loading share records.

---

## Notification

**Table**: `notifications`
**File**: `notification/entity/Notification.java`

The `Notification` entity stores in-app notification messages. Currently, notifications are generated when a project is shared with a user. The entity is designed to be extensible — new notification types can be added by creating new event types and messages.

### Fields

| Column | Java Type | Nullable | Unique | Notes |
|---|---|---|---|---|
| `id` | `Long` | No | Yes (PK) | Auto-incremented primary key |
| `recipient_email` | `String` | No | No | The email of the user who should receive this notification |
| `type` | `String` | No | No | The event type, e.g. `"PROJECT_SHARED"` |
| `message` | `String` | No | No | Human-readable notification text |
| `read` | `boolean` | No | No | Defaults to `false`. Set to `true` when the user reads it |
| `project_id` | `Long` | Yes | No | The ID of the related project, if applicable |
| `created_at` | `LocalDateTime` | Yes | No | Set automatically on insert |

### Design Note

Unlike other entities, `Notification` does not store a foreign key relationship to the `User` entity — it stores `recipientEmail` as a plain string. This is a deliberate simplification that avoids a join when querying notifications by the currently authenticated user (whose email is already known from the JWT). The tradeoff is that if a user's email changes, historical notifications would not automatically update. For the current scope of this system, this is acceptable.

---

## ProjectHistory

**Table**: `project_history`
**File**: `project/history/ProjectHistory.java`

The `ProjectHistory` entity is an audit log. Every significant action taken on a project — creation, editing, sharing, exporting — is recorded here. This table gives a complete, chronological view of how a project has changed over time.

### Fields

| Column | Java Type | Nullable | Unique | Notes |
|---|---|---|---|---|
| `id` | `Long` | No | Yes (PK) | Auto-incremented primary key |
| `project_id` | `Long` | No | No | The ID of the project this event belongs to |
| `event_type` | `EventType` (enum) | No | No | `CREATE`, `EDIT`, `EXPORT`, or `SHARE` |
| `timeline_snapshot` | `String` (TEXT) | Yes | No | A copy of the timeline JSON at the time of the event |
| `author_user_id` | `Long` | No | No | The ID of the user who performed the action |
| `created_at` | `LocalDateTime` | No | No | Timestamp of the event |

### Design Note

`ProjectHistory` stores `projectId` and `authorUserId` as raw `Long` values rather than JPA foreign key relationships. This is an intentional design choice: history records should be immutable and independent. If a project or user is deleted, the history record should still be readable as a historical artifact rather than cascading or becoming invalid. By storing IDs instead of entity references, history records remain intact regardless of what happens to the referenced data.

### Timeline Snapshot

For `CREATE` and `EDIT` events, the `timelineSnapshot` column captures the exact state of the timeline JSON at the moment the event occurred. This is valuable for seeing how a project evolved, debugging issues, or potentially implementing undo/restore features in the future.

For `SHARE` events, the snapshot is not meaningful (the sharing action doesn't change the timeline content), so it may be `null`.

---

## Enumerations

Enums are used throughout the entity model to enforce valid states at both the Java type level and the database level (stored as strings via `@Enumerated(EnumType.STRING)`).

### `Role`

**File**: `user/enums/Role.java`

Defines what kind of user an account represents:

- `USER` — A standard user. Can create, manage, and share projects. Can view their own notifications and history.
- `ADMIN` — Has all USER capabilities plus access to the admin reporting endpoint.

### `ProjectStatus`

**File**: `project/enums/ProjectStatus.java`

Represents the lifecycle state of a project:

- `DRAFT` — The initial state. The project is being worked on by the owner.
- `SHARED` — The project has been shared with at least one other user.
- `ARCHIVED` — The project is no longer active. It can be viewed but not edited.

### `EventType`

**File**: `project/history/EventType.java`

Categorizes the type of activity recorded in a history entry:

- `CREATE` — The project was first created.
- `EDIT` — The project content (name, timeline data, or status) was modified.
- `EXPORT` — The project was exported to an external format.
- `SHARE` — The project was shared with another user.

---

## Entity Relationship Summary

```
User ──────────────────────────────────┐
 │                                     │
 │ (owner_id)                           │ (shared_by_id, shared_with_id)
 ▼                                     ▼
Project ◄──────────── ProjectShare
 │
 │ (project_id — stored as Long)
 ▼
ProjectHistory


User ──────────────────────────────────┐
 │                                     │
 │ (user_id)                            │ (recipient_email — stored as String)
 ▼                                     ▼
RecoveryToken                      Notification
```

All relationships use `LAZY` loading to prevent Hibernate from performing unnecessary joins when entities are loaded. This means that accessing a related entity (e.g., `project.getOwner()`) after the Hibernate session is closed would throw a `LazyInitializationException` — a known JPA pattern that is avoided by fetching required associations eagerly within service method transaction boundaries or by using explicit JPQL queries.
