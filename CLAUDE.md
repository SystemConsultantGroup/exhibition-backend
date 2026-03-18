# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Run application (requires MySQL and MinIO running)
./gradlew bootRun

# Run all tests + generate API docs
./gradlew test asciidoctor

# Run tests only
./gradlew test

# Run a single test class
./gradlew test --tests "kr.ac.skku.scg.exhibition.exhibition.controller.ExhibitionControllerTest"

# Run a single test method
./gradlew test --tests "kr.ac.skku.scg.exhibition.exhibition.controller.ExhibitionControllerTest.getById"

# Build
./gradlew build
```

Generated API docs are written to `build/docs/asciidoc/index.html` and copied to `src/main/resources/static/docs/`.

## Architecture

### Package Structure

Domain-based under `kr.ac.skku.scg.exhibition`:

```
global/          # Cross-cutting concerns (auth, config, error, dto, entity)
  auth/          # JWT filter, Kakao OAuth client, AuthService, CurrentUser resolver
  config/        # SecurityConfig, WebConfig, MinioConfig
  error/         # ApiExceptionHandler, exception classes (NotFoundException, etc.)
exhibition/      # Exhibition CRUD
category/        # Hierarchical category tree per exhibition
item/            # Items (works/projects) within categories; bulk upload via Excel
classification/  # Item classification tags per exhibition
media/           # MinIO-backed media asset upload/serve
board/           # Announcement board posts per exhibition
eventperiod/     # Event periods (semesters) per exhibition
user/            # User profile
```

Each domain has: `controller/`, `service/`, `domain/`, `repository/`, `dto/request/`, `dto/response/`.

### Auth

- No local login. Only **Kakao OAuth** is supported (`global/auth/client/KakaoAuthClient`).
- On OAuth success, the backend issues its own JWT (`JwtTokenService`).
- `JwtAuthenticationFilter` populates `SecurityContext` on each request.
- Spring Security permits all requests (`anyRequest().permitAll()`); authorization is enforced manually in service/controller layer.
- Use `@CurrentUser AuthenticatedUser user` in controller parameters to inject the current user (resolved by `CurrentUserArgumentResolver`). This is nullable for public endpoints.

### Persistence

- **MySQL** in prod; **H2 in-memory** in tests (`src/test/resources/application.yml`).
- All entities extend `BaseEntity` (provides `createdAt`, `updatedAt`).
- Complex queries use **QueryDSL** (see `ItemRepositoryCustom` / `ItemRepositoryImpl`).
- DDL is managed externally (`ddl-auto: none` in prod).

### Media

- Files are stored in **MinIO** (S3-compatible). The MinIO URL is never exposed to clients.
- `MediaController` fetches from MinIO and streams the file through the backend endpoint.

### Profiles

- Default (prod): activates `common` group → reads `application-common.yml`.
- `dev`: reads `application-dev.yml`.
- Tests: use `src/test/resources/application.yml` (H2, no profile needed).

### Environment Variables (prod)

| Variable | Purpose |
|---|---|
| `SPRING_DATASOURCE_URL` | MySQL JDBC URL |
| `SPRING_DATASOURCE_USERNAME` / `_PASSWORD` | MySQL credentials |
| `MINIO_ENDPOINT`, `MINIO_PORT`, `MINIO_ACCESS_KEY`, `MINIO_SECRET_KEY`, `MINIO_BUCKET` | MinIO config |
| `APP_AUTH_JWT_SECRET`, `APP_AUTH_JWT_ACCESS_TOKEN_EXPIRATION_SECONDS`, `APP_AUTH_JWT_REFRESH_TOKEN_EXPIRATION_SECONDS`, `APP_AUTH_JWT_ISSUER` | JWT config |
| `APP_AUTH_KAKAO_CLIENT_ID`, `APP_AUTH_KAKAO_CLIENT_SECRET`, `APP_AUTH_KAKAO_REDIRECT_URI`, `APP_AUTH_KAKAO_TOKEN_URI`, `APP_AUTH_KAKAO_USER_INFO_URI` | Kakao OAuth config |

## Testing Pattern

Controller tests use `@WebMvcTest` + `@MockitoBean` (service) + `@AutoConfigureRestDocs`. They must import `SecurityConfig` and `ApiExceptionHandler`:

```java
@WebMvcTest(controllers = ExhibitionController.class)
@Import({SecurityConfig.class, ApiExceptionHandler.class})
@AutoConfigureRestDocs
class ExhibitionControllerTest { ... }
```

REST Docs snippets are generated per test via `.andDo(document(...))` and assembled by `src/docs/asciidoc/index.adoc`.

## Coding Conventions

- Write a brief comment on each method (per SRS requirement).
- Controller and service must always be separate classes.
- New domains follow the same domain-package layout as existing ones.
- Error responses use `ApiErrorResponse` with `code` and `message` fields. Throw `NotFoundException`, `UnauthorizedException`, or `ForbiddenException` — they are caught by `ApiExceptionHandler`.
