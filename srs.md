# 소프트웨어 요구사항 명세서(SRS)

## 1. 소개

### 1.1 목적
본 문서는 여러 전시 사이트를 하나의 통합 전시 플랫폼으로 연결하는 백엔드 서비스의 요구사항을 명시한다. 프론트엔드는 별도 팀에서 개발한다. 백엔드는 다중 전시 서비스, 계층형 분류(디렉터리와 유사한 구조), 쿠버네티스 배포 및 객체 스토리지로서 MinIO를 지원해야 한다.

### 1.2 범위
본 시스템은 여러 전시 서비스(예: 졸업 전시, 멘토링 전시, 단과대 전시)와 그 구조화된 카테고리를 관리하기 위한 API 및 저장 기능을 제공한다. 전시 서비스의 유연한 생성/삭제, 계층형 콘텐츠 구성, 미디어 관리, 클라우드 네이티브 환경 배포를 지원한다.

### 1.3 정의, 약어, 축약어
- 전시 서비스(Exhibition Service): 특정 전시를 나타내는 테넌트형 단위(예: 졸업 전시).
- 카테고리/디렉터리(Category/Directory): 카페 게시판 또는 폴더 트리와 유사한 계층 구조.
- MinIO: S3 호환 객체 스토리지.
- K8s: Kubernetes.
- SRS: Software Requirements Specification.

### 1.4 참고 문헌
- IEEE 29148(SRS 구조 참고).

### 1.5 개요
2장은 전체 시스템을 설명한다. 3장은 상세 요구사항을 명시한다. 4장은 제약 및 배포 요구사항을 다룬다.

## 2. 전체 설명

### 2.1 제품 관점
백엔드는 여러 전시 프론트엔드를 위한 RESTful(또는 GraphQL) API를 제공하는 독립 서비스이다. 메타데이터용 데이터베이스와 미디어 자산용 MinIO를 통합하며, 쿠버네티스 배포를 전제로 설계된다.

### 2.2 제품 기능(상위 수준)
- 전시 서비스 생성, 수정, 삭제.
- 각 전시 내 계층형 카테고리/디렉터리 정의.
- 카테고리 내 전시 항목(게시물, 작품, 프로젝트) 관리.
- MinIO를 통한 미디어 자산(이미지, 영상, 파일) 관리.
- 전시 탐색 및 검색 API 제공.
- 전시, 카테고리, 항목 관리를 위한 관리자 API 제공.

### 2.3 사용자 클래스 및 특성
- 관리자(Admin): 모든 전시회에 대한 전체 접근 및 관리.
- 서브 관리자(SubAdmin): 할당된 전시회에 대해서만 수정 가능.
- 방문자(Visitor): 일반 사용자로, 전시 콘텐츠 열람 및 탐색. 관리자 서비스에는 접근할 수 없음.

### 2.4 운영 환경
- 컨테이너화된 백엔드(Docker).
- 쿠버네티스 배포(K8s).
- 객체 스토리지 MinIO.
- 관계형 데이터베이스는 MySQL을 사용해야 한다.
- 성능 향상을 위한 선택적 캐시(예: Redis).

### 2.5 설계 및 구현 제약
- 테넌트형 격리를 갖는 다중 전시 서비스를 지원해야 한다.
- 전시별 계층형 카테고리를 지원해야 한다.
- 쿠버네티스에 배포 가능해야 한다.
- 파일 저장을 위해 MinIO와 연동해야 한다.
- 프론트엔드 연동을 위한 API 엔드포인트를 제공해야 한다.
- 복잡한 쿼리는 QueryDSL을 사용해야 한다.
- 환경변수 설정은 `application.yml` 형식으로 관리하고, 필요 시 `application-prod.yml`와 `application-dev.yml`로 분리해야 한다.
- MySQL 접속 정보는 설정 파일에 하드코딩하지 않고 `DB_HOST`, `DB_PORT`, `DB_NAME` 환경변수(필요 시 추가 파라미터 변수 포함)로 주입해야 한다.
- `.env`, `application-*.yml` 등 비밀정보(환경변수, 키, 토큰)를 포함할 수 있는 파일은 반드시 `.gitignore`에 포함해야 하며, 원격 저장소에 커밋되어서는 안 된다.
- 개발 시 도메인형 디렉터리 구조를 사용해야 하며, 레이어형(예: controller/service/repository 전역 분리) 단독 구조는 지양한다.
- 개발 시 각 함수에 간단한 주석을 작성해야 한다.
- 컨트롤러와 서비스를 반드시 분리해야 한다.

#### 2.5.1 디렉터리 구조 원칙(도메인형)
- 패키지는 `exhibition`, `category`, `item`, `classification` 등 도메인 단위로 구성해야 한다.
- 각 도메인 내부에서 `controller`, `service`, `domain`, `repository` 계층을 분리할 수 있어야 한다.
- 공통 모듈(auth, common, config, error, infra, util 등)은 반드시 `global` 디렉터리 하위에 위치해야 한다.
- 예시 구조:
```text
src/main/java/.../exhibition
  ├─ global
  │  ├─ auth
  │  ├─ common
  │  ├─ config
  │  ├─ error
  │  ├─ infra
  │  └─ util
  ├─ exhibition
  │  ├─ controller
  │  ├─ service
  │  ├─ domain
  │  └─ repository
  ├─ category
  │  ├─ controller
  │  ├─ service
  │  ├─ domain
  │  └─ repository
  └─ item
     ├─ controller
     ├─ service
     ├─ domain
     └─ repository
```

### 2.6 가정 및 의존성
- 프론트엔드는 백엔드 API를 사용한다.
- 배포 환경에서 MinIO가 사용 가능하고 접근 가능하다.
- 배포를 위한 쿠버네티스 클러스터가 उपलब्ध하다.

## 3. 상세 요구사항

### 3.1 기능 요구사항

#### 3.1.1 전시 서비스 관리
- FR-1: 시스템은 고유 식별자와 메타데이터(이름, 설명, 로고, 일정)를 가진 전시 서비스를 생성할 수 있어야 한다.
- FR-2: 시스템은 전시 서비스를 수정 및 삭제할 수 있어야 한다.
- FR-3: 시스템은 모든 전시 서비스를 목록 조회하고 식별자로 단일 전시 서비스를 조회할 수 있어야 한다.

#### 3.1.2 카테고리/디렉터리 구조
- FR-4: 시스템은 각 전시 서비스 내에 계층형 카테고리를 생성할 수 있어야 한다.
- FR-5: 시스템은 임의 깊이의 카테고리 중첩을 지원해야 한다.
- FR-6: 시스템은 계층 내 카테고리의 순서 변경 및 이동을 지원해야 한다.
- FR-7: 시스템은 전시별 카테고리 트리를 조회할 수 있어야 한다.

#### 3.1.3 전시 항목(콘텐츠)
- FR-8: 시스템은 카테고리 내 전시 항목(예: 작품, 게시물, 프로젝트)을 생성할 수 있어야 한다.
- FR-9: 시스템은 전시 항목을 수정, 삭제, 조회할 수 있어야 한다.
- FR-10: 시스템은 카테고리별 항목 목록 조회 및 키워드 검색을 지원해야 한다.
- FR-11: 시스템은 로그인한 사용자가 전시 항목에 좋아요를 추가할 수 있어야 한다.
- FR-12: 시스템은 로그인한 사용자가 전시 항목에 대해 좋아요를 취소할 수 있어야 한다.
- FR-13: 시스템은 전시 항목에 작품 분류 태그/타입(예: 논문, 작품)을 부여하고 필터링할 수 있어야 한다.

#### 3.1.4 미디어 자산 관리
- FR-11: 시스템은 MinIO에 미디어 자산(이미지/영상/파일)을 업로드할 수 있어야 한다.
- FR-12: 시스템은 항목과 미디어 자산 간의 메타데이터 연결을 저장해야 한다.
- FR-13: 시스템은 MinIO의 URL을 직접 제공하지 않고, 백엔드가 MinIO에서 파일을 가져와 자체 엔드포인트로 자산을 제공해야 한다.
- FR-14: 각 전시 항목은 썸네일, 포스터, 발표 동영상을 가진다. 해당 자산은 MinIO에 업로드되며, 시스템은 이를 참조할 수 있어야 한다.

#### 3.1.5 접근 제어
- FR-14: 시스템은 역할 기반 접근 제어(Admin, SubAdmin, Visitor)를 제공해야 한다.
- FR-15: 시스템은 쓰기 작업을 권한 있는 역할로 제한해야 한다.

#### 3.1.6 API 요구사항
- FR-16: 시스템은 모든 CRUD 작업에 대해 RESTful API를 제공해야 한다.
- FR-17: 시스템은 카테고리 트리 조회 및 전시 탐색 API를 제공해야 한다.

#### 3.1.7 OAuth 인증
- 본 서비스는 외부 OAuth만을 사용하여 로그인 시스템을 구축해야 하며, 로컬 계정(ID/비밀번호) 로그인은 지원하지 않는다.
- FR-18: 시스템은 외부 OAuth 2.0 / OpenID Connect(OIDC) 로그인을 지원해야 한다.
- FR-19: 시스템은 OAuth 제공자로부터 받은 ID 토큰의 서명, 발급자, 대상(audience), 만료를 검증해야 한다.
- FR-20: 시스템은 OAuth 주체 식별자를 기반으로 로컬 사용자 프로필을 생성 또는 업데이트해야 한다.
- FR-21: 시스템은 OAuth 로그인 성공 후 API 호출을 위한 1자 액세스 토큰을 발급해야 한다.
- FR-22: 시스템은 안정적인 주체 매핑을 사용하여 하나의 사용자 계정에 여러 OAuth 제공자를 연결할 수 있어야 한다.

#### 3.1.8 전시회 팝업 및 소개 페이지
- FR-23: 시스템은 전시회별로 팝업 기능을 설정할 수 있어야 하며, 사용자가 접속 시 이미지 배너를 표시할 수 있어야 한다. 팝업 기능은 관리자 또는 개별 전시회 관리자가 선택적으로 활성/비활성할 수 있어야 한다.
- FR-24: 시스템은 전시회 소개 페이지(전시회당 1개)를 생성할 수 있어야 하며, 소개 페이지에 영상을 연결할 수 있어야 한다.

### 3.2 비기능 요구사항

#### 3.2.1 성능
- NFR-1: 시스템은 일반적인 조건에서 1,000명 이상의 동시 방문자를 지원해야 하며(읽기 작업 응답시간 < 500ms), 허용 가능한 응답 시간을 유지해야 한다.

#### 3.2.2 확장성
- NFR-2: 시스템은 쿠버네티스 환경에서 수평 확장이 가능해야 한다.

#### 3.2.3 가용성
- NFR-3: 시스템은 계획된 유지보수를 제외하고 99.5%의 가용성을 목표로 해야 한다.

#### 3.2.4 보안
- NFR-4: 시스템은 API 엔드포인트에 HTTPS를 사용해야 한다.
- NFR-5: 시스템은 MinIO 접근 자격증명을 안전하게 보호하고 미디어는 백엔드가 직접 제공해야 한다.
- NFR-6: 시스템은 제공자의 JWKS로 OAuth 토큰을 검증하고 토큰 만료 및 대상(audience) 검사를 강제해야 한다.
- NFR-7: 시스템은 식별과 권한 부여에 필요한 최소한의 OAuth 사용자 데이터만 저장해야 한다.
- NFR-7a: 시스템은 환경변수 및 비밀정보가 Git에 노출되지 않도록 `.gitignore` 정책을 유지해야 하며, 비밀값의 저장소 커밋을 금지해야 한다.

#### 3.2.5 유지보수성
- NFR-8: 시스템은 API 문서(예: OpenAPI/Swagger)를 제공해야 한다.
- NFR-8a: 시스템은 기능별 단위 테스트 코드를 기반으로 Spring REST Docs를 생성해야 한다.
- NFR-9: 시스템은 시스템 상태 및 오류를 로깅하고 모니터링해야 한다.
- NFR-10: 시스템의 모든 기능에 대해 단위 테스트를 수행해야 한다.

#### 3.2.6 호환성
- NFR-11: 시스템은 쿠버네티스 및 MinIO와 호환되어야 한다.

## 4. 데이터 요구사항

### 4.1 핵심 엔티티(논리)
- ExhibitionService: id, name, description, start_date, end_date, logo, created_at, updated_at
- Category: id, exhibition_id, parent_id, name, order_index, path, created_at, updated_at
- Item: id, exhibition_id, category_id, title, description, author_name, author_email, visibility, thumbnail_media_id, poster_media_id, presentation_video_media_id, created_at, updated_at, published_at
- ItemClassification: id, exhibition_id, name, created_at
- ItemClassificationMap: id, item_id, classification_id, created_at
- MediaAsset: id, item_id, exhibition_id, object_key, media_type, size, checksum, created_at
- User: id, name, role, email, created_at, updated_at, last_login_at
- OAuthAccount: id, user_id, provider, subject, email, created_at, updated_at

### 4.2 관계형 스키마(상세)

#### 4.2.1 `exhibition_services`
- `id` UUID PK
- `slug` VARCHAR(64) UNIQUE NOT NULL
- `name` VARCHAR(128) NOT NULL
- `description` TEXT NULL
- `start_date` DATE NULL
- `end_date` DATE NULL
- `logo_object_key` VARCHAR(255) NULL
- `is_active` BOOLEAN NOT NULL DEFAULT true
- `created_at` TIMESTAMP NOT NULL
- `updated_at` TIMESTAMP NOT NULL

#### 4.2.2 `categories`
- `id` UUID PK
- `exhibition_id` UUID FK -> exhibition_services.id NOT NULL
- `parent_id` UUID FK -> categories.id NULL
- `name` VARCHAR(128) NOT NULL
- `order_index` INT NOT NULL DEFAULT 0
- `path` VARCHAR(512) NOT NULL
- `depth` INT NOT NULL DEFAULT 0
- `created_at` TIMESTAMP NOT NULL
- `updated_at` TIMESTAMP NOT NULL
- 인덱스: (`exhibition_id`, `parent_id`)
- 인덱스: (`exhibition_id`, `path`)

#### 4.2.3 `items`
- `id` UUID PK
- `exhibition_id` UUID FK -> exhibition_services.id NOT NULL
- `category_id` UUID FK -> categories.id NOT NULL
- `title` VARCHAR(200) NOT NULL
- `summary` TEXT NULL
- `description` TEXT NULL
- `author_name` VARCHAR(100) NULL
- `author_email` VARCHAR(200) NULL
- `visibility` VARCHAR(20) NOT NULL DEFAULT 'public'
- `thumbnail_media_id` UUID FK -> media_assets.id NULL
- `poster_media_id` UUID FK -> media_assets.id NULL
- `presentation_video_media_id` UUID FK -> media_assets.id NULL
- `published_at` TIMESTAMP NULL
- `created_at` TIMESTAMP NOT NULL
- `updated_at` TIMESTAMP NOT NULL
- 인덱스: (`exhibition_id`, `category_id`)
- 인덱스: (`exhibition_id`, `published_at`)
- 인덱스: (`exhibition_id`, `title`)

#### 4.2.4 `item_classifications`
- `id` UUID PK
- `exhibition_id` UUID FK -> exhibition_services.id NOT NULL
- `name` VARCHAR(100) NOT NULL
- `created_at` TIMESTAMP NOT NULL
- 고유 제약: (`exhibition_id`, `name`)
- 인덱스: (`exhibition_id`)

#### 4.2.5 `item_classification_map`
- `id` UUID PK
- `item_id` UUID FK -> items.id NOT NULL
- `classification_id` UUID FK -> item_classifications.id NOT NULL
- `created_at` TIMESTAMP NOT NULL
- 고유 제약: (`item_id`, `classification_id`)
- 인덱스: (`item_id`)
- 인덱스: (`classification_id`)

#### 4.2.4 `media_assets`
- `id` UUID PK
- `item_id` UUID FK -> items.id NOT NULL
- `exhibition_id` UUID FK -> exhibition_services.id NOT NULL
- `object_key` VARCHAR(255) NOT NULL
- `media_type` VARCHAR(50) NOT NULL
- `size` BIGINT NOT NULL
- `checksum` VARCHAR(128) NULL
- `created_at` TIMESTAMP NOT NULL
- 인덱스: (`exhibition_id`, `item_id`)
- 고유 제약: (`exhibition_id`, `object_key`)

#### 4.2.5 `item_likes`
- `id` UUID PK
- `item_id` UUID FK -> items.id NOT NULL
- `user_id` UUID FK -> users.id NOT NULL
- `created_at` TIMESTAMP NOT NULL
- 고유 제약: (`item_id`, `user_id`)
- 인덱스: (`item_id`)
- 인덱스: (`user_id`)

#### 4.2.6 `users`
- `id` UUID PK
- `name` VARCHAR(100) NOT NULL
- `email` VARCHAR(200) NULL
- `role` VARCHAR(20) NOT NULL DEFAULT 'visitor'
- `created_at` TIMESTAMP NOT NULL
- `updated_at` TIMESTAMP NOT NULL
- `last_login_at` TIMESTAMP NULL
- 고유 제약: (`email`)

#### 4.2.7 `oauth_accounts`
- `id` UUID PK
- `user_id` UUID FK -> users.id NOT NULL
- `provider` VARCHAR(50) NOT NULL
- `subject` VARCHAR(200) NOT NULL
- `email` VARCHAR(200) NULL
- `created_at` TIMESTAMP NOT NULL
- `updated_at` TIMESTAMP NOT NULL
- 고유 제약: (`provider`, `subject`)
- 인덱스: (`user_id`)

### 4.3 관계
- 하나의 전시 서비스는 여러 카테고리를 가진다.
- 카테고리는 계층 구조(자기 참조 parent_id)이다.
- 하나의 전시 서비스는 여러 항목을 가진다.
- 하나의 항목은 여러 미디어 자산을 가진다.
- 하나의 사용자는 여러 OAuth 계정을 가진다.

## 5. 외부 인터페이스 요구사항

### 5.1 API 인터페이스(상세)

#### 5.1.1 인증(Auth) (OAuth/OIDC)
- `GET /auth/oauth/{provider}/start`
  - OAuth 플로우 시작. 제공자 인가 엔드포인트로 리다이렉트.
- `GET /auth/oauth/{provider}/callback`
  - OAuth 콜백 처리. 코드를 토큰으로 교환하고 ID 토큰을 검증한 뒤 1자 액세스 토큰을 발급.
- `POST /auth/token`
  - 직접 토큰 플로우 사용 시 OAuth ID 토큰을 1자 액세스 토큰으로 교환.
- `POST /auth/logout`
  - 1자 액세스 토큰을 철회 또는 무효화.
- 응답: `access_token`, `token_type`, `expires_in`, `user` 요약.
  - 예시:
```json
{
  "access_token": "eyJhbGciOi...",
  "token_type": "Bearer",
  "expires_in": 3600,
  "user": {
    "id": "2b5b1c3f-1a2b-4d9b-9c2f-9e2a1d0f8a10",
    "name": "Kim User",
    "email": "kim@example.com",
    "role": "curator"
  }
}
```

#### 5.1.2 전시 서비스
- `GET /exhibitions`
  - 전시 목록. `?active=true|false` 지원.
  - 예시 응답:
```json
{
  "items": [
    {
      "id": "b4c1f4c2-0f4b-4d86-9c2b-3c9b3b2c0b0a",
      "slug": "cse-graduation-2026",
      "name": "CSE Graduation Exhibition 2026",
      "description": "Capstone projects and portfolios",
      "start_date": "2026-02-10",
      "end_date": "2026-03-01",
      "is_active": true
    }
  ],
  "page": 1,
  "page_size": 20,
  "total": 1
}
```
- `POST /exhibitions`
  - 전시 생성.
  - 예시 요청:
```json
{
  "slug": "cse-graduation-2026",
  "name": "CSE Graduation Exhibition 2026",
  "description": "Capstone projects and portfolios",
  "start_date": "2026-02-10",
  "end_date": "2026-03-01"
}
```
- `GET /exhibitions/{exhibitionId}`
  - ID로 전시 조회.
- `PATCH /exhibitions/{exhibitionId}`
  - 전시 메타데이터 수정.
- `DELETE /exhibitions/{exhibitionId}`
  - 전시 삭제.

#### 5.1.3 카테고리
- `GET /exhibitions/{exhibitionId}/categories/tree`
  - 전시의 전체 카테고리 트리 조회.
  - 예시 응답:
```json
[
  {
    "id": "c1",
    "name": "Software",
    "children": [
      { "id": "c2", "name": "Web", "children": [] }
    ]
  }
]
```
- `POST /exhibitions/{exhibitionId}/categories`
  - 카테고리 생성. payload에 `parent_id`(선택) 포함.
  - 예시 요청:
```json
{
  "name": "Web",
  "parent_id": "c1",
  "order_index": 2
}
```
- `PATCH /categories/{categoryId}`
  - 카테고리 이름/순서/부모 수정.
- `DELETE /categories/{categoryId}`
  - 카테고리 삭제. 선택적으로 재배치 또는 연쇄 삭제.

#### 5.1.4 항목(Items)
- `GET /exhibitions/{exhibitionId}/items`
  - `category_id`, `q`, `visibility`, `published`, `classification` 필터로 항목 목록 조회.
  - 예시 응답:
```json
{
  "items": [
    {
      "id": "i1",
      "category_id": "c2",
      "title": "Smart Campus",
      "summary": "IoT project",
      "visibility": "public",
      "published_at": "2026-02-12T09:00:00Z"
    }
  ],
  "page": 1,
  "page_size": 20,
  "total": 1
}
```
- `POST /exhibitions/{exhibitionId}/items`
  - 카테고리에 항목 생성.
  - 예시 요청:
```json
{
  "category_id": "c2",
  "title": "Smart Campus",
  "summary": "IoT project",
  "description": "Full description",
  "author_name": "Lee",
  "author_email": "lee@example.com",
  "visibility": "public"
}
```
- `GET /items/{itemId}`
  - 항목 조회.
- `PATCH /items/{itemId}`
  - 항목 수정.
- `DELETE /items/{itemId}`
  - 항목 삭제.

#### 5.1.4.1 좋아요(Items Likes)
- `POST /items/{itemId}/likes`
  - 로그인 사용자의 좋아요 추가.
- `DELETE /items/{itemId}/likes`
  - 로그인 사용자의 좋아요 취소.

#### 5.1.4.2 작품 분류(Classifications)
- `GET /exhibitions/{exhibitionId}/classifications`
  - 전시회 내 작품 분류 목록 조회.
- `POST /exhibitions/{exhibitionId}/classifications`
  - 작품 분류 생성.
- `DELETE /classifications/{classificationId}`
  - 작품 분류 삭제.

#### 5.1.5 미디어 자산
- `POST /items/{itemId}/media`
  - 업로드 요청 생성. 업로드 처리를 위한 식별자와 object key 반환.
  - 예시 응답:
```json
{
  "media_id": "m1",
  "object_key": "exhibitions/b4c1/items/i1/cover.jpg"
}
```
- `GET /items/{itemId}/media`
  - 항목의 미디어 목록 조회.
- `GET /media/{mediaId}`
  - 백엔드가 MinIO에서 파일을 가져와 직접 제공하는 미디어 파일 엔드포인트.
- `DELETE /media/{mediaId}`
  - 미디어 및 기저 객체 삭제.

#### 5.1.6 공통 API 규칙
- 인증: `Authorization: Bearer <access_token>`.
- 페이지네이션: `?page` 및 `?page_size` 또는 `?cursor`.
- 오류: `code`, `message`, `details`를 포함한 JSON 본문.
- ID: 응답에서 UUID 사용.
  - 오류 예시:
```json
{
  "code": "CATEGORY_NOT_FOUND",
  "message": "Category does not exist",
  "details": { "category_id": "c99" }
}
```

### 5.1.7 관리자 페이지 요구사항
- 관리자 페이지는 2종류가 존재한다.
- Admin 관리자 페이지: 모든 전시회에 대한 생성, 수정, 삭제 및 전시회 전반 관리가 가능해야 한다.
- SubAdmin 관리자 페이지: 할당된 개별 전시회에 대해 전시물 업로드, 수정, 삭제 등 전시물 관리가 가능해야 한다.

### 5.2 권한 매트릭스

#### 5.2.1 역할 정의
- Admin: 모든 전시회에 대한 전체 접근 및 관리 권한.
- SubAdmin: 할당된 전시회에 대해서만 수정 권한.
- Visitor: 일반 사용자로 공개 전시 및 항목에 대한 읽기 전용 접근. 관리자 서비스에는 접근할 수 없음.

#### 5.2.2 엔드포인트 접근
- 인증 엔드포인트: 공개
- `GET /exhibitions`: 공개
- `POST /exhibitions`: Admin
- `GET /exhibitions/{exhibitionId}`: 공개
- `PATCH /exhibitions/{exhibitionId}`: Admin, SubAdmin(할당된 전시)
- `DELETE /exhibitions/{exhibitionId}`: Admin
- `GET /exhibitions/{exhibitionId}/categories/tree`: 공개
- `POST /exhibitions/{exhibitionId}/categories`: Admin, SubAdmin(할당된 전시)
- `PATCH /categories/{categoryId}`: Admin, SubAdmin(할당된 전시)
- `DELETE /categories/{categoryId}`: Admin, SubAdmin(할당된 전시)
- `GET /exhibitions/{exhibitionId}/items`: 공개(visibility 필터 적용)
- `POST /exhibitions/{exhibitionId}/items`: Admin, SubAdmin(할당된 전시)
- `GET /items/{itemId}`: 공개(visibility 적용)
- `PATCH /items/{itemId}`: Admin, SubAdmin(할당된 전시)
- `DELETE /items/{itemId}`: Admin, SubAdmin(할당된 전시)
- `POST /items/{itemId}/media`: Admin, SubAdmin(할당된 전시)
- `GET /items/{itemId}/media`: 공개(visibility 적용)
- `DELETE /media/{mediaId}`: Admin, SubAdmin(할당된 전시)

#### 5.2.3 권한 규칙
- R1: Visitor는 `visibility = public` 항목만 접근할 수 있으며 관리자 서비스에는 접근할 수 없다.
- R2: SubAdmin의 수정 권한은 명시적으로 할당된 전시에 한정된다.
- R3: Admin은 모든 전시회에 대해 교차 접근 권한을 가진다.

### 5.3 카테고리 트리 운영 정책

#### 5.3.1 생성
- C1: 카테고리는 동일 부모 아래 형제 간 이름이 고유해야 한다.
- C2: `parent_id`는 선택이며, 없을 경우 루트 노드가 된다.
- C3: `path`는 조상 ID를 구분자로 연결한 체인으로 계산된다.

#### 5.3.2 이동
- M1: 카테고리를 이동하면 `parent_id`, `depth`, `path`가 갱신된다.
- M2: 이동 시 모든 하위 항목의 `path`가 갱신된다.
- M3: 카테고리는 자기 하위 항목으로 이동될 수 없다.
- M4: 형제 간 순서는 `order_index`로 결정된다.

#### 5.3.3 삭제
- D1: 기본 동작은 하위 항목의 연쇄 삭제(cascade delete)이다.
- D2: 선택적 동작은 삭제된 노드의 부모로 하위 항목을 재배치하는 것이다.
- D3: 카테고리 삭제는 항목을 삭제하지 않으며, 항목은 대체 카테고리로 이동하거나 `category_id`가 지정된 `uncategorized` 루트로 설정된다.

#### 5.3.4 재정렬
- R1: `order_index`는 0 이상의 정수이다.
- R2: 재정렬은 동일 부모 아래 형제 범위에서 수행된다.

#### 5.3.5 동시성 및 무결성
- I1: 카테고리 트리 작업은 트랜잭션이어야 한다.
- I2: 충돌하는 업데이트는 버전 체크 또는 `updated_at` 가드를 통해 거부되어야 한다.

### 5.2 저장소 인터페이스
- 미디어 저장을 위해 MinIO의 S3 호환 API 사용.
- MinIO의 URL은 외부에 제공하지 않으며, 미디어 제공은 백엔드 엔드포인트를 통해서만 이뤄진다.

### 5.3 배포 인터페이스
- 백엔드 및 MinIO 통합을 위한 Kubernetes 매니페스트(또는 Helm 차트).

## 6. 제약 및 배포

### 6.1 Kubernetes 배포
- 시스템은 컨테이너 이미지로 패키징되어야 한다.
- 시스템은 Service 및 Ingress를 포함한 Kubernetes Deployment로 배포 가능해야 한다.
- 시스템은 환경 변수와 ConfigMaps/Secrets로 구성 가능해야 한다.

### 6.2 MinIO 연동
- 시스템은 MinIO S3 호환 엔드포인트를 사용해야 한다.
- 시스템은 환경 변수로 접근 키와 버킷 이름을 구성할 수 있어야 한다.

## 7. 향후 고려사항
- 전시 다국어 지원.
- 고급 검색 및 필터링.
- 방문자 지표를 위한 분석 대시보드.
