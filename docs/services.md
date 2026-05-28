# Services — Business Logic, Repositories, and Flows

## Service Classes

### AuthService

**Package:** `com.peciatech.alomediabackend.auth.service`  
**Dependencies:** `UserRepository`, `RecoveryTokenRepository`, `PasswordEncoder`, `JwtService`, `EmailService`

| Method | Description |
|--------|-------------|
| `register(RegisterRequest)` → `String` | Validates email uniqueness, creates `User` (role `USER`, provider `"local"`, enabled `true`), encodes password, saves, returns JWT |
| `login(LoginRequest)` → `String` | Looks up user by email, validates BCrypt password, returns JWT or throws `InvalidCredentialsException` |
| `requestPasswordRecovery(String email)` | Generates 64-hex-char secure random token, creates `RecoveryToken` (expires in 15 min), saves, sends recovery email via `EmailService` |
| `validateRecoveryToken(String token)` → `boolean` | Validates token exists, is not expired, and is not already used |
| `resetPassword(ResetPasswordRequest)` | Validates recovery token, checks `newPassword` matches `confirmPassword`, encodes and updates password, marks token as used |
| `getCurrentUser(String jwtToken)` → `User?` | Extracts email from JWT, looks up user. Returns `null` for blank/invalid tokens |

### EmailService

**Package:** `com.peciatech.alomediabackend.auth.service`  
**Dependencies:** `JavaMailSender`

| Method | Description |
|--------|-------------|
| `sendEmail(String to, String subject, String body)` | Sends plain text email via SMTP (Gmail) |
| `sendPasswordRecoveryEmail(String toEmail, String token)` | Constructs recovery link `{recoveryBaseUrl}?token={token}` and sends it |

### OAuth2Service

**Package:** `com.peciatech.alomediabackend.auth.service`  
**Dependencies:** `UserRepository`, `PasswordEncoder`, `JwtService`

| Method | Description |
|--------|-------------|
| `handleOAuth2Login(String email, String name, String provider)` → `String` | Creates user on-the-fly if not found (splits name at first space, random UUID as password placeholder, role `USER`, enabled `true`), returns JWT |

---

### ProjectService

**Package:** `com.peciatech.alomediabackend.project.service`  
**Dependencies:** `ProjectRepository`, `UserRepository`, `ProjectHistoryService`, `ProjectHistoryRepository`, `ProjectMediaSyncService`, `ProjectTimelinePersistenceService`, `ProjectShareRepository`, `ProjectMapper`

| Method | Description |
|--------|-------------|
| `createProject(CreateProjectRequest, String email)` → `ProjectResponse` | Normalizes timeline via `ProjectTimelinePersistenceService`, builds entity via `ProjectBuilder`, saves, syncs inline media to B2 via `ProjectMediaSyncService.syncOnSave()`, persists split timeline, records `CREATE` history |
| `getProject(Long id, String email)` → `ProjectResponse` | Access check (owner or shared-with), rebuilds full timeline from split tables, enriches with delivery URLs |
| `listOwnedProjects(String email, Pageable)` → `Page<ProjectSummaryResponse>` | Paginated list of projects owned by user |
| `updateProject(Long id, UpdateProjectRequest, String email)` → `ProjectResponse` | Validates ownership, conditionally updates name/timeline/status, diffs media (upload new, delete removed from B2), re-persists split timeline, records `EDIT` history |
| `deleteProject(Long id, String email)` | Owner-only check, deletes all B2 media, then cascades: shares → history → tracks/media rows → project |
| `getProjectMedia(Long id, String mediaId, String email)` → `byte[]` | Access check, looks up media entry in timeline, downloads from B2, overrides content-type from timeline metadata |

**Access control:** `hasProjectAccess()` checks ownership or share; `isProjectOwner()` checks ownership only.

---

### ProjectSharingService

**Package:** `com.peciatech.alomediabackend.project.service`  
**Dependencies:** `ProjectRepository`, `UserRepository`, `ProjectShareRepository`, `ProjectNotificationService`, `ProjectHistoryService`, `ProjectHistoryRepository`

| Method | Description |
|--------|-------------|
| `shareProject(Long id, String sharedByEmail, String sharedWithEmail)` | Prevents self-sharing, validates owner, validates target user exists, prevents duplicate shares, saves `ProjectShare`, triggers Observer notification chain (DB + email), records `SHARE` history |
| `listSharedProjects(String email, Pageable)` → `Page<ProjectSummaryResponse>` | Paginated list of projects shared with user |

---

### ProjectTimelinePersistenceService

**Package:** `com.peciatech.alomediabackend.project.service`  
**Dependencies:** `ObjectMapper`, `ProjectTracksRepository`, `ProjectMediaRepository`

Handles **splitting** a project's full JSON timeline into three separate persistence layers (metadata, tracks, media) and **reassembling** them on read. Uses SHA-256 content hashing for change detection — only modified sections trigger DB writes.

| Method | Description |
|--------|-------------|
| `normalizeIncomingTimeline(String)` → `String` | Returns input if non-blank, otherwise `{"version":1,"tracks":[],"media":[]}` |
| `buildFullTimeline(Project)` → `String` | Reassembles full timeline JSON from split tables |
| `buildFullTimelineNode(Project)` → `ObjectNode` | Same as above but returns `ObjectNode` |
| `buildFullTimelinesForProjects(List<Project>)` → `List<String>` | Batch version with efficient batch loading |
| `persistSplitTimeline(Project, String fullTimelineData)` | Parses full JSON, splits into metadata/tracks/media, canonicalizes JSON, computes SHA-256 hashes, upserts changed rows only |
| `deleteByProjectId(Long id)` | Deletes tracks and media rows for a project |

**Split strategy:** The root JSON object minus `"tracks"` and `"media"` keys is stored in `Project.timelineData`. The `tracks` array is stored in `ProjectTracks.tracksData` with its hash. The `media` array is stored in `ProjectMedia.mediaData` with its hash.

---

### ProjectMediaSyncService

**Package:** `com.peciatech.alomediabackend.project.media`  
**Dependencies:** `ObjectMapper`, `BackblazeB2StorageService`

Orchestrates the lifecycle of media files within project timelines. See [backblaze-b2.md](backblaze-b2.md) for full details.

| Method | Description |
|--------|-------------|
| `syncOnSave(Project, String previousTimeline, String incoming)` → `String` | Diffs old vs new media, uploads new inline payloads to B2, deletes removed files, replaces inline data with storage references |
| `enrichTimelineWithDeliveryUrls(Project, String)` → `String` | Injects `deliveryUrl` field (`/projects/{id}/media/{mediaId}`) into each stored media entry |
| `loadMediaForProject(Project, String mediaId)` → `StorageBinaryResource` | Downloads binary from B2, overrides content-type/fileName from timeline metadata |
| `deleteAllProjectMedia(String timeline)` | Deletes all referenced media files from B2 |

---

### BackblazeB2StorageService

**Package:** `com.peciatech.alomediabackend.project.media`

Low-level B2 API client. See [backblaze-b2.md](backblaze-b2.md) for full details.

| Method | Description |
|--------|-------------|
| `upload(String storageKey, byte[] content, String contentType)` → `StoredMediaFile` | Authenticates, gets upload URL, POSTs file with SHA-1 verification |
| `delete(String storageKey, String storageFileId)` | Deletes file version from B2; looks up `storageFileId` via `b2_list_file_names` if not provided |
| `download(String storageKey)` → `StorageBinaryResource` | GETs file from B2 download URL |

---

### ProjectHistoryService

**Package:** `com.peciatech.alomediabackend.project.history`  
**Dependencies:** `ProjectHistoryRepository`, `ProjectRepository`, `UserRepository`

| Method | Description |
|--------|-------------|
| `executeCommand(HistoryCommand)` → `ProjectHistoryResponse` | Invoker in the Command pattern — delegates to `command.execute()` |
| `getHistory(Long projectId, String email, Pageable)` → `Page<ProjectHistoryResponse>` | Validates project exists and requester is the owner |

---

### AudioAiService

**Package:** `com.peciatech.alomediabackend.ai.service`  
**Dependencies:** `RestTemplate`

Proxies audio processing to an external Flask microservice.

| Constant | Value |
|----------|-------|
| `MAX_FILE_SIZE_BYTES` | 50 MB |
| `ALLOWED_EXTENSIONS` | wav, mp3, ogg, flac, m4a |
| `ALLOWED_MIME_TYPES` | audio/wav, audio/mpeg, audio/ogg, audio/flac, audio/x-m4a, audio/mp4 |

| Method | Description |
|--------|-------------|
| `cleanAudio(MultipartFile, AudioCleanRequest)` → `ResponseEntity<byte[]>` | Validates audio, sends multipart form to `{flaskBaseUrl}/audio/clean` with optional `backend`, `stationary`, `target_sr` params |
| `transcribeAudio(MultipartFile, AudioTranscribeRequest)` → `ResponseEntity<byte[]>` | Validates audio, sends multipart form to `{flaskBaseUrl}/audio/transcribe` with optional `model`, `lang`, `formats` params |
| `validateAudioFile(MultipartFile)` | Checks null/empty, size > 50MB, extension against allowlist, MIME type against allowlist |

---

### ReportService

**Package:** `com.peciatech.alomediabackend.report.service`  
**Dependencies:** `UserRepository`, `ProjectRepository`, `ProjectHistoryRepository`, `ReportFactoryProvider`

| Method | Description |
|--------|-------------|
| `generateReport(ReportFormat)` → `Object` | Queries aggregate counts (totalUsers, totalProjects, history event counts), builds `ReportData`, delegates to `ReportFactoryProvider.getFactory(format).generateReport(data)` |

---

### NotificationService

**Package:** `com.peciatech.alomediabackend.notification.service`  
**Dependencies:** `NotificationRepository`

| Method | Description |
|--------|-------------|
| `getMyNotifications(String email, Pageable)` → `Page<NotificationResponse>` | Paginated, ordered by creation date descending |
| `getUnread(String email)` → `List<NotificationResponse>` | All unread notifications for user |
| `markAsRead(Long notificationId, String requesterEmail)` | Marks notification as read; validates requester is the recipient |

### ProjectNotificationService

**Package:** `com.peciatech.alomediabackend.notification`  
**Dependencies:** `DbNotificationObserver`, `EmailNotificationObserver`

Observer pattern hub. Registers two observers at startup (`@PostConstruct`): `DbNotificationObserver` (persists to DB) and `EmailNotificationObserver` (sends email).

| Method | Description |
|--------|-------------|
| `shareProject(Long projectId, String sharedByEmail, String sharedWithEmail)` | Creates `NotificationEvent` with type `"PROJECT_SHARED"`, notifies all observers |
| `addObserver(NotificationObserver)` / `removeObserver(NotificationObserver)` | Standard observer management |
| `notifyObservers(NotificationEvent)` | Iterates observers calling `onNotify()` |

---

### JwtService

**Package:** `com.peciatech.alomediabackend.security.jwt`

| Method | Description |
|--------|-------------|
| `generateToken(User)` → `String` | Creates JWT with subject=email, claim `"role" = "ROLE_" + role.name()`, signs with HMAC-SHA |
| `validateToken(String)` → `boolean` | Parses and verifies JWT; returns `false` for expired/malformed/unsupported/empty tokens |
| `extractUsername(String)` → `String` | Returns subject (email) |
| `extractRole(String)` → `String` | Returns custom `"role"` claim |

---

## Repositories

### ProjectRepository (`JpaRepository<Project, Long>`)

| Method | Description |
|--------|-------------|
| `findByOwnerId(Long)` → `List<Project>` | All projects owned by a user |
| `findByOwner(User, Pageable)` → `Page<Project>` | Paginated by owner entity |
| `findByOwnerId(Long, Pageable)` → `Page<Project>` | Paginated by owner ID |
| `existsByIdAndOwnerId(Long id, Long ownerId)` → `boolean` | Ownership check |
| `findSummariesByOwnerId(Long, Pageable)` → `Page<ProjectSummaryResponse>` | JPQL projection |

### ProjectMediaRepository (`JpaRepository<ProjectMedia, Long>`)

| Method | Description |
|--------|-------------|
| `findByProjectId(Long)` → `Optional<ProjectMedia>` | Single media row per project |
| `findByProjectIdIn(List<Long>)` → `List<ProjectMedia>` | Batch fetch with `JOIN FETCH project` |
| `deleteByProjectId(Long)` | Delete by project |

### ProjectTracksRepository (`JpaRepository<ProjectTracks, Long>`)

| Method | Description |
|--------|-------------|
| `findByProjectId(Long)` → `Optional<ProjectTracks>` | Single tracks row per project |
| `findByProjectIdIn(List<Long>)` → `List<ProjectTracks>` | Batch fetch with `JOIN FETCH project` |
| `deleteByProjectId(Long)` | Delete by project |

### ProjectShareRepository (`JpaRepository<ProjectShare, Long>`)

| Method | Description |
|--------|-------------|
| `existsByProjectIdAndSharedWithId(Long, Long)` → `boolean` | Access check |
| `deleteByProjectId(Long)` → `long` | Cascade delete |
| `findSharedProjectSummariesByUserId(Long, Pageable)` → `Page<ProjectSummaryResponse>` | JPQL: joins ProjectShare for shared project summaries |

### ProjectHistoryRepository (`JpaRepository<ProjectHistory, Long>`)

| Method | Description |
|--------|-------------|
| `findAllByProjectId(Long)` → `List<ProjectHistory>` | Unpaged list |
| `findAllByProjectId(Long, Pageable)` → `Page<ProjectHistory>` | Paginated |
| `deleteByProjectId(Long)` → `long` | Cascade delete |
| `countByEventType(EventType)` → `long` | Used by ReportService for aggregate stats |

### UserRepository (`JpaRepository<User, Long>`)

| Method | Description |
|--------|-------------|
| `findByEmail(String)` → `Optional<User>` | Primary lookup mechanism |
| `existsByEmail(String)` → `boolean` | Duplicate registration check |

### RecoveryTokenRepository (`JpaRepository<RecoveryToken, Long>`)

| Method | Description |
|--------|-------------|
| `findByToken(String)` → `Optional<RecoveryToken>` | Password recovery validation |

### NotificationRepository (`JpaRepository<Notification, Long>`)

| Method | Description |
|--------|-------------|
| `findByRecipientEmailOrderByCreatedAtDesc(String, Pageable)` → `Page<Notification>` | Paginated, newest first |
| `findByRecipientEmailAndReadFalse(String)` → `List<Notification>` | All unread notifications |

---

## Key Business Flows

### Project Creation Flow

```
Client → POST /projects
  → ProjectService.createProject()
    1. Look up owner by email
    2. Normalize incoming timeline (default if blank)
    3. Build Project entity (ProjectBuilder, status=DRAFT)
    4. Save project
    5. ProjectMediaSyncService.syncOnSave() — upload inline media to B2
    6. ProjectTimelinePersistenceService.persistSplitTimeline() — split & persist
    7. Save project again (with updated media references)
    8. Record CREATE history event
    9. Return ProjectResponse
```

### Project Share Flow

```
Client → POST /projects/{id}/share
  → ProjectSharingService.shareProject()
    1. Prevent self-sharing
    2. Validate sharer is owner
    3. Validate target user exists
    4. Prevent duplicate shares
    5. Save ProjectShare
    6. ProjectNotificationService.shareProject()
       → DbNotificationObserver.onNotify() — persist Notification
       → EmailNotificationObserver.onNotify() — send email
    7. Record SHARE history event
```

### Password Recovery Flow

```
Client → POST /auth/recover/request
  → AuthService.requestPasswordRecovery()
    1. Find user by email (silently handle not-found)
    2. Generate 64-hex-char secure random token
    3. Create RecoveryToken (15-min expiry)
    4. Save token
    5. EmailService.sendPasswordRecoveryEmail()

Client → GET /auth/recover/validate?token=xxx
  → AuthService.validateRecoveryToken() → { valid: true/false }

Client → POST /auth/recover/reset
  → AuthService.resetPassword()
    1. Validate token (not expired, not used)
    2. Check newPassword == confirmPassword
    3. Encode and update password
    4. Mark token as used
```