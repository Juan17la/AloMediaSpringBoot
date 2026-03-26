# Design Patterns

## Overview

AloMedia Backend uses several well-established software design patterns to keep the codebase organized, maintainable, and extensible. This document explains each pattern used, why it was chosen for this project, where it is implemented, and what problem it solves.

Understanding these patterns is essential for working on this codebase, because many behaviors that might look surprising at first glance (like notifications being triggered from within a service that seems unrelated, or history being recorded without an explicit `save` call at the call site) are intentional applications of these patterns.

---

## 1. Repository Pattern

### What It Is

The Repository pattern creates an abstraction layer between the business logic (services) and the data access layer (SQL queries). Instead of writing raw queries or calling a database driver directly, services interact with a repository interface that speaks in domain terms.

### Where It Is Used

Every persistent entity has a corresponding repository:
- `UserRepository`
- `RecoveryTokenRepository`
- `ProjectRepository`
- `ProjectShareRepository`
- `NotificationRepository`
- `ProjectHistoryRepository`

All of these extend Spring Data's `JpaRepository<Entity, ID>`, which provides CRUD operations for free. Custom queries are defined as interface methods — Spring Data either derives the SQL from the method name or uses `@Query` annotations for more complex queries.

### Example

```java
// Derived query — Spring generates SQL automatically from the method name
Optional<User> findByEmail(String email);
boolean existsByEmail(String email);

// Custom JPQL query
@Query("SELECT ps.project FROM ProjectShare ps WHERE ps.sharedWith.id = :userId")
Page<Project> findSharedProjectsByUserId(@Param("userId") Long userId, Pageable pageable);
```

### Why It Matters

Without this pattern, every service would need to know how to construct JPA queries or prepare statements. If the query needs to change — say, because a table gets renamed — every service that queries that table would need updating. With repositories, that change is isolated to a single interface method.

---

## 2. Service Layer Pattern

### What It Is

The Service Layer pattern consolidates business logic in dedicated service classes, keeping controllers thin. Controllers translate HTTP requests into service calls, and services apply the actual business rules.

### Where It Is Used

Every domain module has at least one service:
- `AuthService` — registration, login, password recovery
- `ProjectService` — project lifecycle (create, read, update, delete)
- `ProjectSharingService` — project sharing rules
- `NotificationService` — notification retrieval and management
- `ProjectHistoryService` — history recording and retrieval
- `ReportService` — statistics aggregation and report generation
- `EmailService` — email sending abstraction
- `OAuth2Service` — OAuth2 account management

### Example

Instead of the controller doing this:

```java
// ❌ BAD — business logic in a controller
@PostMapping
public ResponseEntity<?> createProject(@RequestBody CreateProjectRequest request) {
    User owner = userRepository.findByEmail(getCurrentUser().getEmail()).orElseThrow();
    Project project = new Project();
    project.setName(request.getName());
    project.setOwner(owner);
    project.setStatus(ProjectStatus.DRAFT);
    // ... more logic
    projectRepository.save(project);
    // record history
    // ...
}
```

The controller delegates everything:

```java
// ✅ GOOD — thin controller
@PostMapping
public ResponseEntity<ProjectResponse> createProject(
        @Valid @RequestBody CreateProjectRequest request,
        @AuthenticationPrincipal UserDetails userDetails) {
    ProjectResponse response = projectService.createProject(request, userDetails.getUsername());
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
}
```

### Why It Matters

Services are much easier to unit test in isolation than controllers. Business rules are not scattered across controller methods — they live in a consistent, predictable place. Services can also call other services, which would be messy if the logic lived in controllers.

---

## 3. DTO Pattern (Data Transfer Object)

### What It Is

The DTO pattern uses separate objects for transferring data between layers, rather than exposing entity objects directly. There are two kinds of DTOs in this project: **request DTOs** (what the client sends) and **response DTOs** (what the server sends back).

### Where It Is Used

Every domain module has a `dto/request/` and `dto/response/` sub-package:

**Request DTOs**:
- `LoginRequest`, `RegisterRequest` — login and registration forms
- `RecoverRequestDTO`, `ResetPasswordRequest` — password recovery flow
- `CreateProjectRequest`, `UpdateProjectRequest`, `ShareProjectRequest` — project management
- `ShareNotificationRequest` — notification creation

**Response DTOs**:
- `AuthResponse`, `UserResponse`, `CurrentUserResponse` — user and auth responses
- `ProjectResponse` — project data returned to clients
- `NotificationResponse` — notification data
- `ProjectHistoryResponse` — history log entries
- `ReportData` — admin report statistics

### Why It Matters

Without DTOs, the API contract would be tied directly to the database schema. Consider what happens if `User` is returned directly:

- The `password` field would be included in the response (a serious security vulnerability).
- Any change to the entity schema (e.g., renaming a column) would immediately change the API contract.
- Bean validation annotations would need to live on the entity itself, creating confusion between persistence constraints and API validation.

DTOs solve all of this. They define an explicit, stable API surface that is independent of how data is stored internally.

---

## 4. Builder Pattern

### What It Is

The Builder pattern is a creational pattern that constructs complex objects step by step using a fluent API, rather than passing many arguments to a constructor.

### Where It Is Used

**`ProjectBuilder`** (`project/builder/ProjectBuilder.java`) is the explicit, custom builder in this codebase:

```java
Project project = new ProjectBuilder()
    .setName(request.getName())
    .setTimelineData(request.getTimelineData())
    .setOwner(owner)
    .build();
```

The `build()` method applies defaults (like setting status to `DRAFT` and defaulting `timelineData` if null), centralizing project initialization logic.

Lombok's `@Builder` annotation is also used on DTOs and some entities to auto-generate a builder.

### Why It Matters

Project creation has several optional fields and default values. Without a builder, constructors would either become bloated with parameters or force callers to call many setters in the right order. The builder pattern makes it obvious which fields are required, which are optional, and what the defaults are.

---

## 5. Observer Pattern

### What It Is

The Observer (or Publish/Subscribe) pattern defines a one-to-many dependency between objects: when one object (the subject/observable) changes state, all of its registered dependents (observers) are automatically notified.

### Where It Is Used

The notification system is built around this pattern:

**Interfaces**:
- `NotificationObservable` — defines `addObserver()`, `removeObserver()`, `notifyObservers()`
- `NotificationObserver` — defines `onNotify(NotificationEvent event)`

**Subject**:
- `ProjectNotificationService` implements `NotificationObservable`

**Observer**:
- `EmailNotificationObserver` implements `NotificationObserver`

**Event**:
- `NotificationEvent` carries the event data (type, projectId, sharedByEmail, sharedWithEmail, timestamp)

### The Flow

When `ProjectSharingService` completes a share operation, it calls `ProjectNotificationService.shareProject()`. That method builds a `NotificationEvent` and calls `notifyObservers(event)`, which iterates over all registered observers and calls `onNotify(event)` on each.

Currently, `EmailNotificationObserver` is the only observer. It:
1. Saves a `Notification` record to the database.
2. Sends an email to the recipient via `EmailService`.

### Why It Matters

Without this pattern, `ProjectSharingService` would need to directly call `NotificationService` and `EmailService`. Every time a new notification delivery channel was added (e.g., push notifications, SMS, Slack), the sharing service would need to be modified.

With the observer pattern, `ProjectSharingService` is completely unaware of how notifications are delivered. Adding a new delivery channel means implementing `NotificationObserver` and registering it — the sharing service never changes.

---

## 6. Command Pattern

### What It Is

The Command pattern encapsulates a request or action as an object, allowing the action to be parameterized, queued, logged, or undone. In this codebase, it is used to encapsulate each type of project history event as an executable command object.

### Where It Is Used

**Interface**:
```java
public interface HistoryCommand {
    ProjectHistory execute();
}
```

**Concrete Commands**:
- `CreateProjectHistoryCommand` — records a project creation event
- `EditProjectHistoryCommand` — records a project edit event
- `ShareProjectHistoryCommand` — records a project share event
- `ExportProjectHistoryCommand` — records a project export event

Each command is constructed with its dependencies (projectId, authorUserId, timelineSnapshot, repository) at the call site, then passed to `ProjectHistoryService.executeCommand()`.

### Example Usage in ProjectService

```java
// The service creates and passes the command — it doesn't need to know how history is saved
historyService.executeCommand(
    new CreateProjectHistoryCommand(project.getId(), owner.getId(), project.getTimelineData(), historyRepository)
);
```

### Why It Matters

Without this pattern, history recording would require either a large switch/if-else block in the history service, or direct calls to the repository from every other service. With the command pattern, each history event type is self-contained and fully testable in isolation. Adding a new event type (e.g., `RESTORE`) only requires a new command class — no existing classes change.

---

## 7. Strategy Pattern (via Factory)

### What It Is

The Strategy pattern defines a family of algorithms (or behaviors), encapsulates each one, and makes them interchangeable. The Factory pattern is used here as the mechanism for selecting and instantiating the right strategy at runtime.

### Where It Is Used

The report generation system uses this pattern to support multiple output formats (JSON, CSV, SUMMARY) without embedding conditional logic in the service.

**Abstract Factory**:
```java
public abstract class ReportFactory {
    public abstract String getFormat();
    public abstract Object produce(ReportData data);
    public Object generateReport(ReportData data) { return produce(data); }
}
```

**Concrete Factories**:
- `JsonReportFactory` — returns the `ReportData` object as-is (serialized to JSON by Spring)
- `CsvReportFactory` — converts `ReportData` to a CSV-formatted string
- `SummaryReportFactory` — returns a `LinkedHashMap` with only the top-level fields

**Provider**:
```java
@Component
public class ReportFactoryProvider {
    private final Map<String, ReportFactory> factories = Map.of(
        "JSON",    new JsonReportFactory(),
        "CSV",     new CsvReportFactory(),
        "SUMMARY", new SummaryReportFactory()
    );

    public ReportFactory getFactory(String format) { ... }
}
```

**Usage in ReportService**:
```java
ReportFactory factory = factoryProvider.getFactory(format); // throws if format unknown
return factory.generateReport(reportData);
```

### Why It Matters

Without this pattern, the `ReportService` would contain a large `if/else` or `switch` block. Adding a new format (e.g., XML, PDF) would require modifying the service class. With the factory/strategy approach, adding a new format means creating a new class that extends `ReportFactory` and registering it in `ReportFactoryProvider`. The service never changes.

---

## 8. Dependency Injection

### What It Is

Dependency Injection (DI) is a pattern where a class's dependencies (the objects it needs to do its work) are provided to it rather than created by it. Spring is an IoC (Inversion of Control) container that manages the creation and wiring of all beans.

### Where It Is Used

Throughout the entire codebase. Every service, controller, repository, and handler is a Spring-managed bean. Dependencies are injected via **constructor injection**, made concise with Lombok's `@RequiredArgsConstructor`:

```java
@Service
@RequiredArgsConstructor
public class ProjectService {
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectHistoryService historyService;
    private final ProjectMapper projectMapper;
    // All injected automatically by Spring
}
```

Constructor injection is preferred over field injection (`@Autowired` on a field) because:
- It makes dependencies explicit and visible.
- It makes classes easier to unit test (dependencies can be passed as constructor arguments in tests).
- It prevents the class from being instantiated without all its required dependencies.

### Why It Matters

Without DI, every service would need to instantiate its own repositories and sub-services, leading to tight coupling and making testing extremely difficult. With Spring's DI container, all of this wiring happens automatically, and swapping out implementations (e.g., for testing with mock repositories) requires no changes to the service code itself.

---

## 9. Mapper Pattern (via MapStruct)

### What It Is

The Mapper pattern converts objects from one type to another — in this case, from entity objects to response DTO objects. MapStruct is used to generate the mapping code automatically at compile time.

### Where It Is Used

**`UserMapper`**:
```java
@Mapper(componentModel = "spring")
public interface UserMapper {
    UserResponse toUserResponse(User user);
    AuthResponse toAuthResponse(User user);
}
```

**`ProjectMapper`**:
```java
@Mapper(componentModel = "spring")
public interface ProjectMapper {
    @Mapping(source = "owner.id", target = "ownerId")
    ProjectResponse toResponse(Project project);
}
```

MapStruct reads these interfaces and generates the implementation at compile time. The `@Mapping` annotation handles cases where the source and target field names differ (e.g., `owner.id` → `ownerId`).

### Why It Matters

Without MapStruct, mapping code would be written by hand:
```java
// ❌ Without MapStruct — repetitive, error-prone
ProjectResponse response = new ProjectResponse();
response.setId(project.getId());
response.setName(project.getName());
response.setStatus(project.getStatus());
response.setOwnerId(project.getOwner().getId());
// ... etc
```

With MapStruct, this is replaced by a single method call (`projectMapper.toResponse(project)`), and the implementation is generated, type-safe, and guaranteed to compile. If a field is added to the entity or DTO, the compiler will catch any missing mappings.

---

## 10. Template Method (via GlobalExceptionHandler)

### What It Is

The Template Method pattern defines the skeleton of an algorithm in a base class, letting subclasses fill in specific steps. In this codebase, `GlobalExceptionHandler` applies a consistent structure to all error responses, regardless of the exception type.

### Where It Is Used

**`GlobalExceptionHandler`** is annotated with `@RestControllerAdvice`, which means Spring automatically routes any unhandled exception through this class before sending a response.

Each exception type has a dedicated handler method that:
1. Receives the exception and the HTTP request.
2. Determines the appropriate HTTP status code.
3. Builds an `ErrorResponse` with a consistent structure.
4. Returns it as the HTTP response body.

**Example Error Response Structure**:
```json
{
  "status": 404,
  "error": "NOT_FOUND",
  "message": "Project with id 42 was not found",
  "timestamp": "2026-03-22T15:30:00",
  "path": "/projects/42",
  "errors": []
}
```

### Why It Matters

Without a global exception handler, each controller method would need its own `try/catch` blocks, and the error response format would vary between endpoints. With this pattern, error handling is centralized, consistent, and never leaks stack traces or internal implementation details to the client.

---

## Pattern Interaction Diagram

The patterns in this codebase do not operate in isolation — they interact and complement each other. Here is how they connect for the most complex flow (project sharing with notification):

```
Controller (thin — calls service)
     │
     ▼
ProjectSharingService (Service Layer)
     │
     ├─► ProjectRepository (Repository Pattern)
     │     └─ Loads Project entity
     │
     ├─► UserRepository (Repository Pattern)
     │     └─ Loads recipient User entity
     │
     ├─► ProjectShareRepository (Repository Pattern)
     │     └─ Saves ProjectShare entity (built with Builder-like setters)
     │
     ├─► ProjectNotificationService (Observer Pattern — notifyObservers)
     │     └─► EmailNotificationObserver (Observer)
     │           ├─ NotificationRepository.save()  ← Repository Pattern
     │           └─ EmailService.sendEmail()        ← Service Layer
     │
     └─► ProjectHistoryService.executeCommand()     ← Command Pattern
           └─► ShareProjectHistoryCommand.execute()
                 └─ ProjectHistoryRepository.save()  ← Repository Pattern
```

The DTO pattern and Mapper pattern are applied at the edges: when the request arrives (request DTO → parsed by Spring) and when the response leaves (entity → response DTO via ProjectMapper).

Dependency Injection is the invisible thread tying all of this together — every service, repository, mapper, and observer is a Spring bean injected wherever it is needed.
