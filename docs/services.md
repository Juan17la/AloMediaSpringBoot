# Services

## What Is the Service Layer?

The service layer is the heart of the application. It is where all business logic lives — the rules that govern how data can be created, read, updated, or deleted, and the orchestration of complex operations that span multiple repositories, external systems (like email), and cross-cutting concerns (like audit history).

Services are injected into controllers via constructor injection (using Lombok's `@RequiredArgsConstructor`) and are the only layer that controllers interact with. Services, in turn, interact with repositories, other services, and external components.

By keeping all logic in services and keeping controllers thin, the business rules stay in one place and are easy to test, reason about, and modify independently of the HTTP layer.

---

## AuthService

**File**: `auth/service/AuthService.java`

`AuthService` is responsible for everything related to identity: creating accounts, verifying credentials, and managing the password recovery lifecycle.

### `register(RegisterRequest request) → AuthResponse`

Creates a new user account. Before saving anything to the database, the service checks whether the email is already taken using `UserRepository.existsByEmail()`. If it is, a `UserAlreadyExistsException` is thrown, which the global exception handler maps to a `409 Conflict` response.

If the email is available, the password is encoded using BCrypt and the user is saved with the `USER` role and `provider = "local"` (to distinguish from OAuth2 accounts). Once the user is persisted, `JwtService` generates a JWT token for them and the `AuthResponse` is assembled via `UserMapper`.

### `login(LoginRequest request) → AuthResponse`

Authenticates an existing user. The service uses Spring Security's `AuthenticationManager` to validate the email/password pair. If authentication fails (wrong password or unknown email), Spring throws an `AuthenticationException` which is translated into an `InvalidCredentialsException` (401).

On success, the user is loaded and a JWT token is generated. The `AuthResponse` is returned just like in registration — so from the client's perspective, login and register have identical response shapes.

### `requestPasswordRecovery(String email) → void`

This is the first step of the two-step password reset flow. The service looks up the user by email and generates a new recovery token — a securely random UUID string. The token is saved to the `recovery_tokens` table with an expiration timestamp set 15 minutes in the future.

Then `EmailService.sendPasswordRecoveryEmail()` is called to deliver a link (containing the token) to the user's inbox. Critically, if the email does not exist in the database, the method still returns normally rather than throwing an exception. This prevents attackers from probing which emails are registered on the platform (a technique known as user enumeration).

### `validateRecoveryToken(String token) → boolean`

Looks up the token in the database. If found, it checks two conditions: is the current time before `expiresAt`? And is `used == false`? If both checks pass, the token is valid.

This method throws specific exceptions rather than returning a boolean for invalid states — `RecoveryTokenExpiredException` if expired, `RecoveryTokenAlreadyUsedException` if used — so that the frontend can display a meaningful message to the user.

### `resetPassword(ResetPasswordRequest request) → void`

The final step. The service validates the token again (same checks as `validateRecoveryToken`), then verifies that `newPassword` and `confirmPassword` are identical. If they differ, a `PasswordMismatchException` is thrown.

If everything passes, the new password is encoded with BCrypt, saved to the user record, and the recovery token's `used` flag is set to `true`. The token can never be reused after this point.

### `getCurrentUser(String jwtToken) → User`

A utility method used by `AuthController` to serve the `GET /auth/me` endpoint. Extracts the email from the JWT via `JwtService.extractUsername()`, then fetches the full user record from the database.

---

## EmailService

**File**: `auth/service/EmailService.java`

`EmailService` is a thin wrapper around Spring's `JavaMailSender`. It abstracts the email-sending infrastructure so that other services don't need to know anything about SMTP configuration.

### `sendEmail(String to, String subject, String body) → void`

The generic method. Constructs a `SimpleMailMessage` with the provided recipient, subject, and body and sends it via the configured SMTP server (Gmail, port 587 with TLS). The sender address comes from the `MAIL_USERNAME` environment variable.

### `sendPasswordRecoveryEmail(String toEmail, String token) → void`

A higher-level method that calls `sendEmail` with a pre-formatted subject and body. The body contains the full password recovery link, constructed by appending the token to the `RECOVERY_BASE_URL` environment variable (e.g., `https://app.alomedia.com/recover?token=abc123`).

---

## OAuth2Service

**File**: `auth/service/OAuth2Service.java`

`OAuth2Service` handles the user persistence side of OAuth2 logins. When a user authenticates via Google or GitHub for the first time, an account needs to be created for them. On subsequent logins, the existing account should be found and reused.

### `handleOAuth2Login(String email, String name, String provider) → String`

Looks up the user by email. If the user exists, a JWT is generated for them immediately. If they don't exist, `createOAuth2User()` is called first.

This means that a user who registered via email/password can later log in via Google (if they use the same email address) and will be logged into their existing account — the provider is not checked for uniqueness, only the email.

### `createOAuth2User(String email, String name, String provider) → User` *(private)*

Creates a new `User` record for an OAuth2-authenticated user. Because these users never set a password, a random UUID is encoded with BCrypt and stored as the password field. This ensures the field satisfies the database constraint without being usable for regular login.

The `name` parameter (e.g., "Maria Garcia") is split on the first space to extract `firstName` and `lastName`. If the name contains only one word, the `lastName` is set to an empty string.

---

## ProjectService

**File**: `project/service/ProjectService.java`

`ProjectService` is the core business logic layer for project management. It enforces ownership rules, delegates to `ProjectHistoryService` for audit recording, and uses `ProjectBuilder` for clean object construction.

### `createProject(CreateProjectRequest request, String requesterEmail) → ProjectResponse`

Fetches the authenticated user by email from `UserRepository`, then uses `ProjectBuilder` to construct a `Project` entity. The builder sets the status to `DRAFT` automatically and defaults `timelineData` to `{"version":1,"tracks":[],"media":[]}` if not provided in the request. The project is saved, and a `CreateProjectHistoryCommand` is executed to record the creation event.

### `getProject(Long projectId, String requesterEmail) → ProjectResponse`

Fetches the project and verifies that `project.getOwner().getEmail()` matches `requesterEmail`. If the project does not exist, or if the user is not the owner, a `ProjectNotFoundException` is thrown. This means unauthorized access and non-existence produce the same 404 error — intentionally, to avoid leaking information about which project IDs exist.

### `listOwnedProjects(String requesterEmail, Pageable pageable) → Page<ProjectResponse>`

Fetches the authenticated user, then calls `ProjectRepository.findByOwner(user, pageable)`. The `Pageable` object is passed in directly from the controller, meaning pagination and sorting parameters come straight from the HTTP request.

### `updateProject(Long projectId, UpdateProjectRequest request, String requesterEmail) → ProjectResponse`

After verifying ownership, this method applies a **partial update**: only fields that are non-null in `UpdateProjectRequest` are applied to the entity. This means a client can send `{"name": "New Name"}` without affecting `timelineData` or `status`. After saving, an `EditProjectHistoryCommand` is executed with a snapshot of the updated timeline.

### `deleteProject(Long projectId, String requesterEmail) → void`

Verifies ownership and calls `ProjectRepository.delete(project)`. Deletion cascades according to the database's foreign key constraints.

---

## ProjectSharingService

**File**: `project/service/ProjectSharingService.java`

`ProjectSharingService` handles the logic of sharing a project with another user. It is intentionally separate from `ProjectService` to keep each service focused on a single responsibility.

### `shareProject(Long projectId, String sharedByEmail, String sharedWithEmail) → void`

This method enforces several business rules in sequence:

1. **Ownership check**: The project is fetched and the `sharedByEmail` must match the owner. If not, `ProjectNotFoundException` is thrown.
2. **Self-share prevention**: If `sharedByEmail` equals `sharedWithEmail`, the operation is rejected.
3. **Recipient existence**: The recipient is looked up by email. If they don't exist, `UserNotFoundException` is thrown.
4. **Duplicate prevention**: `ProjectShareRepository.existsByProjectIdAndSharedWithId()` checks if the share already exists. If it does, an appropriate exception is thrown.

If all checks pass:
- A `ProjectShare` entity is created and saved.
- `ProjectNotificationService.shareProject()` is called, which triggers the Observer pattern to send both an in-app notification and an email.
- A `ShareProjectHistoryCommand` is executed to record the event.

### `listSharedProjects(String requesterEmail, Pageable pageable) → Page<ProjectResponse>`

Returns the projects that have been shared **with** the authenticated user (not by them). Uses a custom JPQL query in `ProjectShareRepository.findSharedProjectsByUserId()` to join the `project_shares` table and return the actual `Project` entities.

---

## NotificationService

**File**: `notification/service/NotificationService.java`

`NotificationService` is the consumer-facing side of the notification system. While `ProjectNotificationService` is responsible for *creating* notifications (via the Observer pattern), `NotificationService` is what the user calls to *read* and *manage* their notifications.

### `getMyNotifications(String email) → List<NotificationResponse>`

Calls `NotificationRepository.findByRecipientEmailOrderByCreatedAtDesc()`. The notifications are returned newest-first so that the most recent activity appears at the top of any notification feed.

### `getUnread(String email) → List<NotificationResponse>`

Calls `NotificationRepository.findByRecipientEmailAndReadFalse()`. This is the subset of notifications where `read = false`. It's used for badge counts and unread-only views.

### `markAsRead(Long notificationId, String requesterEmail) → void`

Fetches the notification by ID. Then checks whether `notification.getRecipientEmail()` matches `requesterEmail`. If not, an `UnauthorizedException` is thrown — a user should not be able to mark another person's notifications as read. If the check passes, `notification.setRead(true)` is called and the entity is saved.

---

## ProjectHistoryService

**File**: `project/history/ProjectHistoryService.java`

`ProjectHistoryService` acts as the executor for the Command pattern used for history recording. It doesn't know what kind of history event is being recorded — it just executes whatever `HistoryCommand` it receives.

### `executeCommand(HistoryCommand command) → ProjectHistoryResponse`

Calls `command.execute()`, which returns a `ProjectHistory` entity that has already been persisted by the command itself. The entity is then mapped to a `ProjectHistoryResponse` DTO and returned.

By depending on the `HistoryCommand` interface rather than any concrete command class, this method is fully open for extension — new types of history events can be added without modifying the service.

### `getHistory(Long projectId, String requesterEmail) → List<ProjectHistoryResponse>`

Verifies that the requesting user is the owner of the project (by checking `ProjectRepository.existsByIdAndOwnerId()`), then fetches all history records ordered by creation time using `ProjectHistoryRepository.findAllByProjectId()`.

---

## ReportService

**File**: `report/service/ReportService.java`

`ReportService` aggregates usage statistics from multiple repositories and delegates report generation to the appropriate factory. It uses the Strategy/Factory pattern to support multiple output formats without adding conditional logic to the service itself.

### `generateReport(String format) → Object`

First, the service gathers data from across the system:
- Total user count from `UserRepository.count()`
- Total project count from `ProjectRepository.count()`
- Per-event-type counts from `ProjectHistoryRepository.countByEventType()` for `CREATE`, `EDIT`, `EXPORT`, and `SHARE`

This data is assembled into a `ReportData` object with a `generatedAt` timestamp set to now.

Then `ReportFactoryProvider.getFactory(format)` is called to get the right `ReportFactory`. If the format string is unrecognized, `InvalidReportFormatException` is thrown (400 Bad Request). The factory's `generateReport(data)` method is called and its return value is passed back to the controller.

---

## ProjectNotificationService

**File**: `notification/ProjectNotificationService.java`

This service implements the `NotificationObservable` interface and acts as the publisher in the Observer pattern. It is called by `ProjectSharingService` when a project is shared.

### `shareProject(Long projectId, String sharedByEmail, String sharedWithEmail) → void`

Constructs a `NotificationEvent` with the type `"PROJECT_SHARED"`, the project ID, both email addresses, and the current timestamp. Then calls `notifyObservers(event)`.

### `notifyObservers(NotificationEvent event) → void`

Iterates over all registered `NotificationObserver` instances and calls `onNotify(event)` on each. Currently, there is one observer: `EmailNotificationObserver`.

The `EmailNotificationObserver` performs two actions:
1. Persists a `Notification` entity to the database (so the user can retrieve it via the API).
2. Calls `EmailService.sendEmail()` to dispatch a notification email to the recipient.

This design means that adding a new type of notification delivery (e.g., push notifications, Slack messages) only requires implementing `NotificationObserver` and registering it — no changes to `ProjectSharingService` or `ProjectNotificationService` are needed.
