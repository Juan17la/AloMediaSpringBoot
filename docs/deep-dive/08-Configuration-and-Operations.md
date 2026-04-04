# 08 - Configuration and Operations

## 1. Required Environment Variables

Application and infrastructure:

- PORT
- CORS_ALLOWED_ORIGIN

Database:

- DB_URL
- DB_USERNAME
- DB_PASSWORD

JWT/security:

- JWT_SECRET
- JWT_EXPIRATION
- COOKIE_SECURE

OAuth2 integration:

- GOOGLE_CLIENT_ID
- GOOGLE_CLIENT_SECRET
- GITHUB_CLIENT_ID
- GITHUB_CLIENT_SECRET
- OAUTH2_REDIRECT_URL

Password recovery/email:

- RECOVERY_BASE_URL
- MAIL_USERNAME
- MAIL_PASSWORD

AI microservice:

- FLASK_BASE_URL

## 2. Persistence and JPA Settings

- PostgreSQL driver configured.
- `spring.jpa.hibernate.ddl-auto=update` currently enables schema evolution at startup.
- Hikari pool max size is configured as 2, which may be restrictive under concurrent traffic.

## 3. Multipart and Upload Constraints

- `spring.servlet.multipart.max-file-size=50MB`
- `spring.servlet.multipart.max-request-size=55MB`

Combined with service-level checks, this defends AI upload endpoints from oversized payloads.

## 4. Security and CORS Operational Notes

- CORS allowed origin is explicitly configured and credentials are enabled.
- Allowed methods include GET, POST, PUT, DELETE, PATCH, OPTIONS.
- Most endpoints are secured and require authentication context.
- Admin report endpoint also requires role-based authorization.

## 5. External Dependency Health

Critical dependencies:

- PostgreSQL availability and latency
- Flask AI microservice availability
- SMTP provider availability

If Flask fails:

- endpoint returns 502 (upstream error) or 503 (service unavailable)

If SMTP fails during notification observer execution:

- sharing flow may be impacted because observer execution is synchronous

## 6. Monitoring Recommendations

- Add health checks for DB, Flask, and mail transport.
- Add structured logs for endpoint latency and external call duration.
- Track error rates by exception type and endpoint.
- Track AI route payload size and processing duration metrics.

## 7. Deployment Considerations

- Ensure frontend domain is included in CORS origin.
- Use secret managers for JWT and OAuth credentials.
- Consider increasing DB pool size for production workloads.
- Evaluate separating long-running AI tasks into async processing if latency grows.

## 8. Documentation Maintenance Process

- Update endpoint docs whenever controller mappings change.
- Update payload examples when DTO fields change.
- Keep this deep-dive set as source of truth until OpenAPI generation is added.
