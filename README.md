# Exhibition Backend

여러 전시 사이트를 하나의 플랫폼으로 운영하기 위한 Spring Boot 백엔드입니다.  
SRS(`srs.md`) 기준으로 전시/카테고리/항목/분류/미디어를 관리하는 API를 제공합니다.

[API Docs - 계속 업데이트 됨](https://systemconsultantgroup.github.io/exhibition-backend/src/main/resources/static/docs/index.html)

## 프로젝트 요약

- 목적: 다중 전시(테넌트형) 운영을 위한 통합 API 제공
- 핵심 도메인: `exhibition`, `category`, `item`, `classification`, `media`
- 기술 스택: Java 17, Spring Boot 4, Spring Data JPA, QueryDSL, MySQL, MinIO
- 문서화: Spring REST Docs + Asciidoctor
- 구조: 도메인별 패키지 + 공통 모듈(`global`) 분리

## 주요 기능 (SRS 기반)

- 전시 서비스 CRUD
- 전시별 계층형 카테고리 트리 관리
- 카테고리 내 항목(작품/게시물) CRUD 및 조회
- 항목 분류(Classification) 부여/조회
- MinIO 기반 미디어 업로드/조회
- 관리자 API 중심의 운영 기능

## 환경 변수

현재 `src/main/resources/application.yml` 기준으로 아래 환경변수를 사용합니다.

| 변수명 | 기본값 | 설명 |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://localhost:3306/exhibition?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC` | MySQL JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | `root` | MySQL 사용자명 |
| `SPRING_DATASOURCE_PASSWORD` | `root` | MySQL 비밀번호 |
| `MINIO_ENDPOINT` | `http://localhost` | MinIO 엔드포인트(스킴+호스트) |
| `MINIO_PORT` | `9000` | MinIO 포트 |
| `MINIO_ACCESS_KEY` | `minioadmin` | MinIO Access Key |
| `MINIO_SECRET_KEY` | `minioadmin` | MinIO Secret Key |
| `MINIO_BUCKET` | `exhibition` | MinIO 버킷명 |

참고:
- Spring Boot는 `spring.datasource.url`이 필요하므로 `SPRING_DATASOURCE_URL`을 반드시 설정해야 합니다.
- 테스트는 `src/test/resources/application.yml`의 H2 in-memory DB 설정을 사용합니다.

## 실행 방법

### 1) 로컬 의존 서비스 준비

- MySQL 실행 (DB: `exhibition`)
- MinIO 실행 및 버킷(`exhibition`) 생성

### 2) 환경 변수 설정 (예시)

```bash
export SPRING_DATASOURCE_URL='jdbc:mysql://localhost:3306/exhibition?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC'
export SPRING_DATASOURCE_USERNAME=root
export SPRING_DATASOURCE_PASSWORD=root
export MINIO_ENDPOINT=http://localhost
export MINIO_PORT=9000
export MINIO_ACCESS_KEY=minioadmin
export MINIO_SECRET_KEY=minioadmin
export MINIO_BUCKET=exhibition
```

### 3) 애플리케이션 실행

```bash
./gradlew bootRun
```

기본 포트는 Spring Boot 기본값(`8080`)입니다.

## 테스트 및 API 문서 생성

```bash
./gradlew test asciidoctor
```

생성된 문서:
- `build/docs/asciidoc/index.html`

## 참고 파일

- 요구사항 명세: `srs.md`
- 빌드 설정: `build.gradle`
- 실행 설정: `src/main/resources/application.yml`
- 테스트 설정: `src/test/resources/application.yml`
