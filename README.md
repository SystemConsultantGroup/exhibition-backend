# Exhibition Backend

여러 전시 사이트를 하나의 플랫폼으로 운영하기 위한 Spring Boot 백엔드입니다.  
SRS(`srs.md`) 기준으로 전시/카테고리/항목/분류/미디어를 관리하는 API를 제공합니다.

[API Docs](https://systemconsultantgroup.github.io/exhibition-backend/src/main/resources/static/docs/index.html)


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

## 대량 업로드 성능 및 매니저 연동

- XLSX 전체의 셀·분류·카테고리·파일 참조·시그니처·미사용 파일을 **저장 전에 검증**합니다.
- 동일 카테고리/분류는 요청 안에서 한 번만 조회합니다. 조회 도중 자동 flush로 매 행의 저장이
  끊기는 것을 피하고 JDBC 배치(50개), insert/update 정렬을 사용합니다.
- MinIO I/O만 공유 제한 풀에서 병렬 처리합니다. 엔티티 접근과 DB 저장은 원래 트랜잭션 스레드에
  남습니다. 전송 하나가 실패해도 시작한 전송을 모두 기다려 정리하며 DB 실패/롤백에도 객체를 제거합니다.
- MinIO 객체 키는 기존 형식을 그대로 유지합니다. 전시 미디어는 `전시slug/파일명`, 작품 미디어는
  `전시slug/회차ID/파일명`이며 추가 디렉터리/UUID/이름 변경/포맷 변환이 없습니다.
  같은 파일의 반복 참조는 한 번만 전송하되 기존과 같이 각 미디어 레코드는 유지합니다.
- 엔티티/테이블 구성을 변경하지 않습니다.

| 변수 | 기본값 | 설명 |
|---|---|---|
| `APP_UPLOAD_WORKERS` | `4` | 공유 MinIO I/O 작업자 수 (1–32); 스토리지/대역폭에 맞춰 조정 |
| `APP_UPLOAD_MAX_FILE_SIZE` | `500MB` | 파일 한 개의 multipart 제한 |
| `APP_UPLOAD_MAX_REQUEST_SIZE` | `10GB` | multipart 전체 요청 제한 |
| `APP_UPLOAD_MAX_PART_COUNT` | `3001` | 최대 파트 수 (엑셀 1 + 미디어 최대 3000) |
| `APP_CORS_ADMIN_ALLOWED_HOSTS` | `manage.exhibition.scg.sh` | 기존 관리자 CORS 호스트 목록 (업로드 경로 변경 용도가 아님) |

Excel 제한은 10 MiB / 1000행입니다. 큰 파일을 메모리에 적재하지 않도록 multipart 임시 파일을
사용하므로 임시 디스크에 충분한 공간을 확보해야 합니다. 역프록시의 본문 크기/버퍼링/시간 제한도
일치시켜야 합니다. DB 배치의 네트워크 최적화가 필요하면 MySQL JDBC URL의
`rewriteBatchedStatements=true`를 환경에 맞춰 검토하세요.

업로드 경로는 반드시 **브라우저 → 매니저 사이트 → 백엔드 → MinIO**입니다.
`exhibition-manage`는 표준 `next start`와 동일 출처 스트리밍 프록시를 사용하며,
백엔드 주소(`API_BASE_URL`)는 매니저 서버에서만 사용합니다. 공개 업로드 URL이나 브라우저의
백엔드 직접 접속 기능은 제공하지 않습니다. 매니저는 본문 수신이 끝나기 전에 백엔드로 전송을
시작하고, 파일을 JS 메모리나 매니저 디스크에 통째로 적재하지 않습니다. 매니저 ingress와
백엔드 사이에 불필요한 공개 게이트웨이를 추가하지 않도록 내부 서비스 주소 사용을 권장합니다.
매니저 ingress의 본문 크기·요청 버퍼링·송수신 제한도 함께 확인해야 합니다.

로컬 테스트는 반복 참조 조회 수, 병렬성, 실패 정리와 H2/JPA 배치 FK 및 롤백을 검증합니다.
실제 처리량은 배포된 네트워크·MySQL·MinIO에 따라 달라지므로 동일 파일의 업로드 시간과 API 로그의
`processingMs`를 전후 비교하세요. 로그 시간은 multipart 수신이 끝난 뒤의 처리 시간입니다.

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
