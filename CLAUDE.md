# SQLD/SQLP 문제은행 — 프로젝트 설계서

SQLD/SQLP 자격증 시험 대비 문제은행 웹 애플리케이션.
Jenkins CI/CD + Docker 기반 로컬 배포, 추후 AWS 확장을 목표로 한다.

---

## 기술 스택

### Backend
| 항목 | 기술 |
|------|------|
| Language | Java 21 |
| Framework | Spring Boot 4.0.6 |
| Build Tool | Gradle (Groovy) |
| ORM | Spring Data JPA + Hibernate |
| Security | Spring Security + JWT |
| DB Migration | Flyway |
| Cache | Redis |
| API 문서 | Springdoc OpenAPI (Swagger) |
| 모니터링 | Spring Actuator + Prometheus |
| 쿼리 모니터링 | P6Spy |
| Rate Limiting | Bucket4j |
| 로깅 | Logback + MDC |

### Frontend
| 항목 | 기술 |
|------|------|
| Framework | React 18 + TypeScript |
| Build Tool | Vite |
| 상태관리 | Zustand + React Query |
| 스타일 | Tailwind CSS |
| 라우팅 | React Router v6 |
| Mock | MSW (Mock Service Worker) |
| 차트 | Recharts |

### Infrastructure
| 항목 | 기술 |
|------|------|
| DB | PostgreSQL 16 |
| Cache | Redis 7 |
| Container | Docker + Docker Compose |
| CI/CD | Jenkins |
| 모니터링 | Prometheus + Grafana |
| 배포 | 로컬 서버 → 추후 AWS (EC2 + RDS + ElastiCache) |

---

## 레포지토리

멀티레포 전략 (BE/FE 독립 배포)

```
sqlquiz-backend   ← Spring Boot
sqlquiz-frontend  ← React
```

### 브랜치 전략

```
main        ← 배포 가능 상태 (Jenkins 트리거)
develop     ← 기능 통합
feature/xxx ← 기능 개발
hotfix/xxx  ← 긴급 수정
```

### 커밋 컨벤션

```
feat:     새 기능
fix:      버그 수정
refactor: 리팩토링
test:     테스트 코드
docs:     문서
chore:    빌드/설정 변경
```

---

## 백엔드 디렉토리 구조

```
src/main/java/sqlquiz/
├── domain/
│   ├── user/
│   │   ├── controller/
│   │   ├── service/
│   │   ├── repository/
│   │   ├── entity/
│   │   └── dto/
│   ├── question/
│   ├── exam/
│   ├── wrongnote/
│   └── statistics/
├── global/
│   ├── config/       ← SecurityConfig, RedisConfig, SwaggerConfig
│   ├── exception/    ← GlobalExceptionHandler, CustomException, ErrorCode
│   ├── filter/       ← JwtAuthenticationFilter, MDCLoggingFilter
│   ├── response/     ← ApiResponse<T>, ErrorResponse
│   └── util/         ← JwtUtil
└── SqlquizApplication.java

src/main/resources/
├── db/migration/
│   ├── V1__init.sql
│   └── V2__seed_data.sql
├── application.yml
├── application-local.yml
└── application-prod.yml
```

---

## ERD

```
users
├── id            UUID         PK
├── email         VARCHAR      UNIQUE
├── password      VARCHAR
├── nickname      VARCHAR
├── role          ENUM         USER | ADMIN
├── created_at    TIMESTAMP
└── updated_at    TIMESTAMP

categories
├── id            BIGINT       PK
├── name          VARCHAR
├── exam_type     ENUM         SQLD | SQLP
└── created_at    TIMESTAMP

questions
├── id            BIGINT       PK
├── category_id   FK           → categories
├── content       TEXT         문제 지문
├── option1~4     TEXT         보기
├── answer        INT          정답 번호 (1~4)
├── explanation   TEXT         해설
├── difficulty    ENUM         EASY | MEDIUM | HARD
├── created_at    TIMESTAMP
└── updated_at    TIMESTAMP

attempts
├── id            UUID         PK
├── user_id       FK           → users
├── exam_type     ENUM         SQLD | SQLP
├── status        ENUM         IN_PROGRESS | COMPLETED
├── total_count   INT
├── correct_count INT
├── score         INT
├── started_at    TIMESTAMP
└── completed_at  TIMESTAMP

attempt_answers
├── id            BIGINT       PK  ← 단순 로그성 레코드라 BIGINT 사용 (attempts는 UUID)
├── attempt_id    FK           → attempts
├── question_id   FK           → questions
├── selected_option INT
├── is_correct    BOOLEAN
└── answered_at   TIMESTAMP

wrong_notes
├── id            BIGINT       PK
├── user_id       FK           → users
├── question_id   FK           → questions
├── memo          TEXT
├── is_resolved   BOOLEAN
├── created_at    TIMESTAMP
└── updated_at    TIMESTAMP
```

---

## API 설계

### 공통 응답 포맷

```json
// 성공
{ "success": true, "data": {}, "message": "요청이 완료되었습니다." }

// 실패
{ "success": false, "code": "QUESTION_NOT_FOUND", "message": "문제를 찾을 수 없습니다.", "timestamp": "2025-01-01T00:00:00" }
```

### 엔드포인트

```
# 인증
POST   /api/v1/auth/signup
POST   /api/v1/auth/login
POST   /api/v1/auth/reissue
POST   /api/v1/auth/logout

# 문제
GET    /api/v1/questions           필터: category, difficulty, exam_type
GET    /api/v1/questions/{id}
POST   /api/v1/questions           [ADMIN]
PUT    /api/v1/questions/{id}      [ADMIN]
DELETE /api/v1/questions/{id}      [ADMIN]

# 시험
POST   /api/v1/exams               시험 시작 (랜덤 출제)
GET    /api/v1/exams/{id}          세션 조회 (이어풀기)
POST   /api/v1/exams/{id}/submit   제출 + 자동 채점
GET    /api/v1/exams/{id}/result   채점 결과

# 오답노트
GET    /api/v1/wrong-notes         필터: category, is_resolved
POST   /api/v1/wrong-notes
PATCH  /api/v1/wrong-notes/{id}/memo
PATCH  /api/v1/wrong-notes/{id}/resolve
DELETE /api/v1/wrong-notes/{id}

# 통계
GET    /api/v1/statistics/summary
GET    /api/v1/statistics/categories
GET    /api/v1/statistics/weak-points
GET    /api/v1/statistics/history
```

---

## 보안 설계

### JWT 전략
- Access Token: 유효기간 15분, `Authorization: Bearer` 헤더
- Refresh Token: 유효기간 7일, DB 저장 (서버에서 무효화 가능)
- Refresh Token Rotation 적용

### Role 권한
| Role | 권한 |
|------|------|
| `ROLE_USER` | 문제 풀기, 오답노트, 통계 조회 |
| `ROLE_ADMIN` | 문제 등록/수정/삭제 + USER 전체 권한 |

---

## 로컬 실행 방법

```bash
# 1. 인프라 기동 (PostgreSQL + Redis)
docker compose up -d

# 2. 애플리케이션 실행
./gradlew bootRun --args='--spring.profiles.active=local'

# 3. 헬스 확인
curl http://localhost:8080/actuator/health
# → {"status":"UP"}
```

---

## 주요 규칙

- `.env` 는 Git에 올리지 않는다
- 모든 API는 `/api/v1/` prefix 사용
- 스키마 변경은 반드시 Flyway 마이그레이션 파일로 관리
- `application-prod.yml` 민감정보는 환경변수로 주입

---

## Claude 작업 지침

- 베이스 패키지는 `sqlquiz.*` 고정 (com.yourname 없음)
- 새 클래스 추가 시 domain/global 패키지 구조 준수
- 스키마 변경 시 반드시 `db/migration/Vn__설명.sql` 파일로 작성
- 새 API는 `/api/v1/` prefix 필수
- 응답은 항상 `ApiResponse<T>` 포맷 사용
- 민감 정보는 코드에 하드코딩하지 않고 `.env` → `application-local.yml` 환경변수로 주입