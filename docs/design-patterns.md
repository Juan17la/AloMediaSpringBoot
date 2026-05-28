# Design Patterns

## 1. Repository Pattern

**Implementations:** All `*Repository` interfaces extending `JpaRepository`

Each domain entity has a dedicated repository that abstracts data access:
- `ProjectRepository`, `ProjectMediaRepository`, `ProjectTracksRepository`, `ProjectShareRepository`
- `ProjectHistoryRepository`
- `UserRepository`, `RecoveryTokenRepository`
- `NotificationRepository`

Custom queries use both Spring Data method naming (`findByEmail`, `existsByIdAndOwnerId`) and `@Query` JPQL (`findSummariesByOwnerId`, `findSharedProjectSummariesByUserId`).

---

## 2. Service Layer Pattern

**Implementations:** All `*Service` classes

Business logic is encapsulated in dedicated service classes with `@Transactional` annotations:
- `AuthService` — registration, login, password recovery
- `ProjectService` — project CRUD, media lifecycle orchestration
- `ProjectSharingService` — sharing workflow with validation
- `ProjectTimelinePersistenceService` — timeline split/reassembly
- `ProjectMediaSyncService` — media upload/sync/delete orchestration
- `ProjectHistoryService` — history event recording
- `NotificationService` — notification read-side operations
- `ReportService` — report generation

Controllers are thin delegates — they parse requests and call services.

---

## 3. DTO (Data Transfer Object) Pattern

**Implementations:** All `*Request` and `*Response` classes

Separate request and response DTOs isolate the API contract from the domain model:
- **Request DTOs:** `CreateProjectRequest`, `UpdateProjectRequest`, `ShareProjectRequest`, `LoginRequest`, `RegisterRequest`, `RecoverRequestDTO`, `ResetPasswordRequest`, `AudioTranscribeRequest`, `AudioCleanRequest`, `ShareNotificationRequest`
- **Response DTOs:** `ProjectResponse`, `ProjectSummaryResponse`, `AuthResponse`, `UserResponse`, `CurrentUserResponse`, `ProjectHistoryResponse`, `NotificationResponse`, `ReportData`, `RecoveryValidationResponse`

---

## 4. Builder Pattern

**Implementation:** `ProjectBuilder` (`project.builder.ProjectBuilder`)

Constructs `Project` entities with deterministic defaults:
- `status` defaults to `ProjectStatus.DRAFT`
- `timelineData` defaults to `{"version":1,"tracks":[],"media":[]}` if null

```java
Project project = new ProjectBuilder()
    .setName(request.getName())
    .setTimelineData(normalizedTimeline)
    .setOwner(owner)
    .build();
```

---

## 5. Observer Pattern

**Implementations:** `NotificationObservable`, `NotificationObserver`, `DbNotificationObserver`, `EmailNotificationObserver`

```
ProjectNotificationService (Observable)
       │
       ├── DbNotificationObserver    → Persists Notification to DB
       └── EmailNotificationObserver  → Sends email via EmailService
```

- `ProjectNotificationService` implements `NotificationObservable` with `addObserver()`, `removeObserver()`, `notifyObservers()`
- Observers are registered at startup via `@PostConstruct`
- `NotificationEvent` carries: `type`, `projectId`, `sharedByUserEmail`, `sharedWithUserEmail`, `occurredAt`
- Currently used only for project sharing events (`"PROJECT_SHARED"`)
- **Note:** Observer execution is synchronous within the request thread

---

## 6. Command Pattern

**Implementations:** `HistoryCommand` interface, `CreateProjectHistoryCommand`, `EditProjectHistoryCommand`, `ShareProjectHistoryCommand`, `ExportProjectHistoryCommand`

```java
public interface HistoryCommand {
    ProjectHistory execute();
}
```

Each command encapsulates a history event creation:
- `CreateProjectHistoryCommand` → `EventType.CREATE`
- `EditProjectHistoryCommand` → `EventType.EDIT`
- `ShareProjectHistoryCommand` → `EventType.SHARE`
- `ExportProjectHistoryCommand` → `EventType.EXPORT`

`ProjectHistoryService.executeCommand()` acts as the invoker.

---

## 7. Factory Method / Strategy Pattern

**Implementations:** `ReportFactory` (abstract), `JsonReportFactory`, `CsvReportFactory`, `SummaryReportFactory`, `ReportFactoryProvider`

```
ReportService
       │
       ▼
ReportFactoryProvider.getFactory(format)
       │
       ├── JsonReportFactory      → Returns ReportData DTO directly
       ├── CsvReportFactory       → Returns CSV string
       └── SummaryReportFactory   → Returns LinkedHashMap (subset of fields)
```

- `ReportFactory` defines `getFormat()` and `produce(ReportData)` as abstract methods
- `generateReport(ReportData)` is a template method in the base class that calls `produce()`
- `ReportFactoryProvider` maps `ReportFormat` enums to concrete factory instances
- Open/Closed: new formats only require adding a `ReportFormat` enum value and a `ReportFactory` subclass

---

## 8. Dependency Injection

**Implementation:** Constructor injection via Lombok `@RequiredArgsConstructor` across all services

Spring-managed beans are injected through constructors, enabling:
- Immutability of dependencies (`final` fields)
- Easy testing with mock injection
- No `@Autowired` field injection

---

## 9. Mapper Pattern (MapStruct)

**Implementations:** `ProjectMapper`, `UserMapper`

Type-safe, compile-time generated mappers that convert between entities and DTOs:
- `ProjectMapper.toResponse(Project)` → `ProjectResponse` (maps `owner.id` → `ownerId`)
- `ProjectMapper.toResponse(Project, String timelineData)` → `ProjectResponse`
- `UserMapper.toUserResponse(User)` → `UserResponse`
- `UserMapper.toAuthResponse(User)` → `AuthResponse` (ignores `token`, set separately)

Both use `@Mapper(componentModel = "spring")` for Spring integration.

---

## 10. Template Method (GlobalExceptionHandler)

**Implementation:** `GlobalExceptionHandler` (`@RestControllerAdvice`)

Provides a consistent error response envelope (`ErrorResponse`) with:
- Common fields: `status`, `error`, `message`, `timestamp`, `path`
- Optional field: `errors` (for validation details, `@JsonInclude(NON_NULL)`)

Each `@ExceptionHandler` method follows the template: catch exception → set HTTP status → build `ErrorResponse` → return. This ensures all API errors have a uniform structure.

---

## Pattern Interaction — Project Share Flow

```
Client: POST /projects/{id}/share
  │
  ▼ Controller (thin delegate)
  │
  ▼ ProjectSharingService.shareProject()
  │   ├── Validates business rules (no self-share, owner-only, no duplicates)
  │   ├── Saves ProjectShare entity (Repository Pattern)
  │   ├── ProjectNotificationService.shareProject()
  │   │   ├── Creates NotificationEvent
  │   │   └── notifyObservers() (Observer Pattern)
  │   │       ├── DbNotificationObserver → saves Notification (Repository)
  │   │       └── EmailNotificationObserver → sends email (Service Layer)
  │   └── HistoryService.executeCommand(new ShareProjectHistoryCommand(...))
  │       └── command.execute() → saves ProjectHistory (Command Pattern)
  │
  ▼ Response returned to client
```