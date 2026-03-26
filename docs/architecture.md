# Architecture Overview

## What Is AloMedia Backend?

AloMedia Backend is a **Spring Boot REST API** built for a timeline-based project management platform. It allows users to create and manage media timeline projects, share them with collaborators, receive notifications, and track the history of all project events. The system also exposes an admin reporting endpoint for platform-level statistics.

The backend is designed to be **stateless, secure, and modular**. Every major domain — authentication, users, projects, notifications, reports — is organized into its own package with its own controllers, services, repositories, and DTOs. This separation of concerns makes the codebase easy to navigate and extend.

---

## Technology Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 4.0.3 |
| Database | PostgreSQL |
| ORM | Spring Data JPA / Hibernate |
| Security | Spring Security + JWT (JJWT 0.12.6) |
| OAuth2 | Spring Security OAuth2 Client |
| Email | Spring Boot Starter Mail (Gmail SMTP) |
| Object Mapping | MapStruct 1.6.3 |
| Boilerplate Reduction | Lombok |
| Build Tool | Maven |
| Containerization | Docker |

---

## High-Level Architecture

The application follows a classic **layered architecture**, where each layer has a well-defined responsibility and only communicates with the layer directly below it.

```
Client (HTTP)
     │
     ▼
┌─────────────────────────────────────────────┐
│              Security Layer                  │
│  (JWT Filter → SecurityContext population)   │
└─────────────────────┬───────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────┐
│             Controller Layer                 │
│  (Parse request, validate, call service)     │
└─────────────────────┬───────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────┐
│              Service Layer                   │
│  (Business logic, orchestration, patterns)   │
└─────────────────────┬───────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────┐
│            Repository Layer                  │
│  (Spring Data JPA — database queries)        │
└─────────────────────┬───────────────────────┘
                      │
                      ▼
                 PostgreSQL
```

### Security Layer

Every incoming HTTP request first passes through the **JWT Filter** (`JwtFilter.java`). If an `Authorization: Bearer <token>` header is present, the filter extracts the token, validates its signature and expiration using `JwtService`, and then loads the corresponding `UserDetails` from the database. Once the user is authenticated, their identity is stored in Spring's `SecurityContext` so that any subsequent code in the same request thread can retrieve who is making the request.

Public endpoints (like login and register) are exempt from this filter — they are configured explicitly in `SecurityConfig` to allow unauthenticated access.

### Controller Layer

Controllers are thin. Their only responsibilities are:
- Receiving and deserializing the HTTP request body
- Running `@Valid` bean validation on incoming DTOs
- Calling the appropriate service method
- Wrapping the result in a `ResponseEntity` with the correct HTTP status

Controllers do not contain business logic. They delegate everything to the service layer.

### Service Layer

This is where all business logic lives. Services orchestrate the use of repositories, enforce business rules (e.g., "only the owner can delete a project"), and use design patterns like Observer, Command, Builder, and Factory to handle complex behaviors in a clean, decoupled way.

### Repository Layer

Repositories extend Spring's `JpaRepository` interface, which provides CRUD operations for free. Custom query methods are defined using Spring Data's method naming conventions or via `@Query` annotations with JPQL. No raw SQL is written anywhere — Hibernate handles all translation to PostgreSQL queries.

---

## Module Structure

The project is organized by **domain module**, not by layer. Each domain (auth, user, project, notification, report) contains its own controller, service, repository, DTO, and entity. This makes each domain self-contained.

```
com.peciatech.alomediabackend
│
├── auth/             → Login, registration, OAuth2, password recovery
├── user/             → User entity, roles, user info endpoints
├── project/          → Project CRUD, sharing, history tracking
├── notification/     → Notification persistence, observer system
├── report/           → Admin reporting with pluggable formats
├── security/         → JWT filter, Spring Security config, OAuth2 handler
└── common/exception/ → Global exception handler, custom exceptions
```

---

## Authentication & Authorization

### JWT-Based Authentication

Authentication in AloMedia is **stateless**. After logging in, the server issues a signed JWT (JSON Web Token). The client stores this token and includes it in the `Authorization` header of every subsequent request.

The token contains:
- **Subject**: the user's email address
- **Claim `role`**: either `ROLE_USER` or `ROLE_ADMIN`
- **Expiration**: configurable via the `JWT_EXPIRATION` environment variable

Because the server never stores session state, it can scale horizontally — any server instance can validate any token without coordination.

### Role-Based Authorization

Two roles exist: `USER` and `ADMIN`.

- `USER`: can manage their own projects, share them, view notifications, and track history.
- `ADMIN`: has all USER permissions plus access to the `/admin/reports` endpoint.

Role checks are enforced at the `SecurityConfig` level using `hasRole()` rules, and additionally at the service level where ownership must be verified (e.g., only the owner of a project can update or delete it).

### OAuth2 Login

Google and GitHub are supported as OAuth2 providers. The flow works as follows:

1. The client redirects the user to the Spring Security OAuth2 login URL.
2. Spring Security handles the provider redirect and callback.
3. On success, `OAuth2SuccessHandler` extracts the user's email, name, and provider from the `OAuth2User` object.
4. `OAuth2Service` either finds the existing user in the database or creates a new one with a random UUID as the password (since password-based login is not used for OAuth2 accounts).
5. A JWT token is generated and the user is redirected to the frontend URL with `?token=<jwt>`.

---

## Database Design

### Tables

| Table | Description |
|---|---|
| `users` | All registered users |
| `recovery_tokens` | Password recovery tokens with expiry |
| `projects` | Timeline projects owned by users |
| `project_shares` | Many-to-many relationship for shared projects |
| `notifications` | Notification records for project events |
| `project_history` | Audit log of all project-related events |

### Schema Management

Hibernate is configured with `ddl-auto=update`, meaning it automatically creates or alters tables to match the entity definitions on startup. This is suitable for development and early production stages.

### Connection Pool

HikariCP is used as the connection pool. The maximum pool size is set to **2 connections**, appropriate for a prototype or low-traffic deployment. This can be increased in production via configuration.

---

## Key Technical Flows

### Request Authentication Flow

```
Request arrives
     │
     ▼
JwtFilter extracts Bearer token
     │
     ├─ No token? → Continue (SecurityContext empty, protected routes return 401)
     │
     ▼
JwtService.validateToken()
     │
     ├─ Invalid/Expired? → 401 Unauthorized
     │
     ▼
JwtService.extractUsername() → email
     │
     ▼
UserRepository.findByEmail(email) → UserDetails
     │
     ▼
SecurityContextHolder.setAuthentication(token)
     │
     ▼
Controller executes with authenticated principal
```

### Project Sharing Flow

```
POST /projects/{id}/share
     │
     ▼
ProjectController.shareProject()
     │
     ▼
ProjectSharingService.shareProject()
     │
     ├─ Validate: requester is owner
     ├─ Validate: not sharing with self
     ├─ Validate: not already shared
     │
     ▼
ProjectShare saved to DB
     │
     ▼
ProjectNotificationService.shareProject()
     │  (Observer pattern — notifies all registered observers)
     │
     ▼
EmailNotificationObserver.onNotify()
     │
     ├─ Notification saved to DB
     └─ Email sent to recipient via EmailService
     │
     ▼
ShareProjectHistoryCommand.execute()
     │
     └─ ProjectHistory record saved to DB
```

---

## Environment Configuration

All sensitive values are externalized as environment variables. The application will not start without them.

| Variable | Purpose |
|---|---|
| `DB_URL` | PostgreSQL JDBC connection URL |
| `DB_USERNAME` | Database user |
| `DB_PASSWORD` | Database password |
| `JWT_SECRET` | Base64-encoded secret for JWT signing |
| `JWT_EXPIRATION` | Token expiry in milliseconds |
| `CORS_ALLOWED_ORIGIN` | Frontend origin allowed by CORS |
| `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` | Google OAuth2 credentials |
| `GITHUB_CLIENT_ID` / `GITHUB_CLIENT_SECRET` | GitHub OAuth2 credentials |
| `OAUTH2_REDIRECT_URL` | URL to redirect after OAuth2 success |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | Gmail SMTP credentials |
| `RECOVERY_BASE_URL` | Base URL for password recovery links |

---

## Error Handling

All unhandled exceptions are caught by `GlobalExceptionHandler`, a `@RestControllerAdvice` class. It maps each custom exception type to an appropriate HTTP status code and returns a consistent error envelope:

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

The `errors` array is populated when bean validation fails, containing one entry per invalid field with the field name and violation message.

---

## Deployment

A `Dockerfile` is included in the project root, enabling containerized deployments. The application listens on the port defined by the `PORT` environment variable (defaults to `8080`).
