# SQLQuiz 백엔드에서 배울 만한 기술 포인트

> Spring Boot 4.0.6 / Java 21 / PostgreSQL / Redis 기반 SQLD/SQLP 문제은행 프로젝트
> 단순 CRUD가 아닌 **실무 감각이 필요한 코드**만 추려서 정리

---

## 1. 인증 & 보안

### 1-1. Refresh Token Rotation
- **위치:** `AuthService.java:99-121`
- **기법:** Token 재발급 시
  1. RT 서명 검증
  2. Redis에 저장된 RT와 요청 RT 비교
  3. 일치 시 새 RT 발급 & Redis 덮어쓰기
  4. 불일치 시 탈취 의심 → 401 반환
- **왜 배울 가치:**
  - 토큰 탈취 시 무한 갱신을 방어하는 **업계 표준**
  - 탈취자가 한 번 재발급하는 순간, 정상 사용자의 RT는 자동 무효화됨
  - 재발급 과정 자체가 보안의 핵심 메커니즘

### 1-2. Filter 순서 설계 (조기 차단)
- **위치:** `SecurityConfig.java:73-75`
- **기법:** `addFilterBefore()`로 `LoginRateLimitFilter` → `JwtAuthenticationFilter` 순서 강제
- **왜 배울 가치:**
  - 차단될 요청에 **JWT 파싱 비용을 들이지 않음** (성능)
  - 비싼 연산을 뒤로 미루는 의도적 배치
  - Filter Chain 설계 시 항상 고려해야 할 원칙

### 1-3. Rate Limiting (Bucket4j, Token Bucket Algorithm)
- **위치:** `LoginRateLimitFilter.java:40-105`
- **기법:**
  - 분당 10회 제한 (IP 기준)
  - 인메모리 `ConcurrentHashMap<IP, Bucket>` + `bucket.tryConsume(1)`
  - `X-Forwarded-For` 헤더에서 실제 클라이언트 IP 추출
- **왜 배울 가치:**
  - 학습용 단일 인스턴스지만, 클러스터 확장 시 Redis Bucket4j로 교체 가능한 설계
  - Brute Force 공격 방어의 기본기

### 1-4. Redis 기반 RefreshToken 저장소
- **위치:** `RefreshTokenRepository.java:20-53`
- **기법:** `StringRedisTemplate`으로 `RT:{email}` 키에 token 저장, TTL 자동 만료
- **왜 배울 가치:**
  - TTL 있는 휘발성 데이터는 **DB가 아닌 Redis**
  - 토큰 무효화(DEL)가 O(1)
  - 로그아웃 즉시 반영 가능

### 1-5. IDOR 방어 (Insecure Direct Object Reference)
- **위치:** `ExamService.loadAndCheckOwnership()`, `WrongNoteService.loadAndCheckOwnership()`
- **기법:** 모든 진입점에서 email로 사용자 검증, 권한 없을 시 **403이 아닌 404** 반환
- **왜 배울 가치:**
  - 리소스 존재 여부 자체를 노출하지 않는 보안 원칙
  - 권한 검사 누락은 OWASP Top 10에 매년 등장하는 심각한 결함

### 1-6. 클래스 레벨 `@Transactional(readOnly = true)`
- **위치:** `AuthService.java:31`
- **기법:** 클래스 기본을 readOnly로, 쓰기 메서드(`signup`, `login`, `reissue`)만 `@Transactional` 오버라이드
- **왜 배울 가치:**
  - 성능: 읽기는 flush 불필요
  - 안전성: 의도하지 않은 쓰기 방지

---

## 2. JPA & 쿼리 패턴

### 2-1. Native Query + 2단계 분리 (랜덤 출제)
- **위치:** `QuestionRepository.java:43-54`
- **기법:**
  1. Native SQL의 `random()` + `LIMIT`으로 ID만 추출
  2. 별도 fetch join 쿼리로 `Question` + `Category`를 IN 절로 조회
- **왜 배울 가치:**
  - JPQL의 fetch join + LIMIT 조합은 Hibernate 경고가 발생하고 비결정적
  - **2단계로 쪼개는 것이 실무에서 자주 쓰는 우회법**

### 2-2. `@EntityGraph`로 N+1 제어
- **위치:** `AttemptRepository.java:21-23`
- **기법:** `@Query` + `@EntityGraph(attributePaths = ...)` 조합
- **왜 배울 가치:**
  - 연관관계는 LAZY로 두고, 필요한 쿼리에서만 명시적으로 EAGER 적용
  - JOIN FETCH보다 선언적이고 재사용 가능

### 2-3. Dirty Checking을 통한 암묵적 UPDATE
- **위치:** `QuestionService.update()` (`QuestionService.java:74-87`)
- **기법:** `q.update(...)` 호출만으로 변경, `save()` 호출 안 함
- **왜 배울 가치:**
  - 영속성 컨텍스트 내 변경은 커밋 시 flush로 자동 감지
  - JPA의 핵심 메커니즘 이해

### 2-4. PostgreSQL FILTER 절 활용
- **위치:** `StatisticsRepository.java:28-38`
- **기법:** `COUNT(*) FILTER (WHERE status = 'COMPLETED')` 등 여러 조건부 집계를 **단일 쿼리로** 수행
- **왜 배울 가치:**
  - JPQL로는 표현 불가능한 DBMS 고유 기능
  - 한 번의 테이블 스캔으로 여러 집계 → 통계성 쿼리 최적화

---

## 3. 도메인 설계

### 3-1. 사전 생성 + Stateless 검증 패턴 (시험 출제)
- **위치:** `ExamService.java:50-132`
- **기법:**
  - 시험 시작 시 `AttemptAnswer` 행 N개를 **미리 생성**
  - 제출 시에는 "이 시험에 포함된 문제인가"만 검증
  - 범위 밖 문제 감지 시 `INVALID_QUESTION_FOR_EXAM` 예외
- **왜 배울 가치:**
  - **부정행위 방지** (시험 중 다른 문제 제출 불가)
  - 제출 후처리 단순화
  - `Attempt.complete()` 같은 도메인 메서드로 상태 변경 캡슐화

### 3-2. 오답 자동 등록 (멱등성)
- **위치:** `ExamService.java:164-179`
- **기법:**
  ```java
  wrongNoteRepository.findByUserIdAndQuestionId(...)
      .ifPresentOrElse(
          note -> note.updateSelectedOption(...),  // 있으면 갱신
          () -> wrongNoteRepository.save(...)      // 없으면 생성
      );
  ```
- **왜 배울 가치:**
  - 복합 조건(user + question)에 대한 Optional 처리 패턴
  - 멱등성 보장 — 같은 입력에 같은 결과

### 3-3. 카테고리별 정답률 집계 (LinkedHashMap)
- **위치:** `ExamService.buildResult()` (`ExamService.java:193-204`)
- **기법:** `LinkedHashMap`으로 순서 보존하면서 `computeIfAbsent`로 누적
- **왜 배울 가치:**
  - 자료구조 선택의 중요성 (HashMap vs LinkedHashMap)
  - 메모리 효율과 가독성 사이의 균형

---

## 4. 예외 처리

### 4-1. Enum 기반 ErrorCode 중앙화
- **위치:** `ErrorCode.java`
- **기법:**
  - 44개의 ErrorCode를 enum 하나에서 관리
  - 각 코드가 `HttpStatus` + 사용자 메시지 쌍을 보유
  - 모든 예외는 `CustomException`으로 통일
- **왜 배울 가치:**
  - 일관된 응답 포맷
  - 국제화(i18n) 확장 용이
  - 상태코드 오류(어느 곳은 400, 어느 곳은 422 같은) 방지

### 4-2. 검증 예외 타입 분리
- **위치:** `GlobalExceptionHandler.java:27-48`
- **기법:**
  - `@Valid` 실패 → `MethodArgumentNotValidException`
  - `@RequestParam` 검증 실패 → `ConstraintViolationException`
  - **둘을 별도로 처리**
- **왜 배울 가치:**
  - Spring 검증 메커니즘의 구조 이해
  - 둘 다 처리해야 사용자 경험이 일관됨

---

## 5. 인프라 & DevOps

### 5-1. 다단계 Dockerfile
- **위치:** `Dockerfile:14-49`
- **기법:**
  1. Builder 스테이지: `gradlew` + 메타파일 먼저 COPY → 의존성 다운로드
  2. Builder 스테이지: 소스 COPY → `bootJar` 실행
  3. Runtime 스테이지: JRE 슬림 이미지에 jar만 복사, non-root 유저로 실행
- **왜 배울 가치:**
  - **CI/CD 빌드 시간 단축** (의존성 레이어 캐시)
  - 이미지 크기 최소화
  - 보안: root로 실행 안 함

### 5-2. Flyway + JPA validate 전략
- **위치:** `application-prod.yml:13-26`
- **기법:**
  - `spring.jpa.hibernate.ddl-auto = validate`
  - Flyway가 스키마 관리
  - 운영에서 `out-of-order = false`로 엄격한 순차 적용
- **왜 배울 가치:**
  - 운영에서 **JPA 자동 DDL 절대 금지** 원칙
  - 마이그레이션은 코드로 추적, 롤백 가능
  - 면접 단골 질문

### 5-3. Graceful Shutdown + Health Check
- **위치:** `Dockerfile:45-48`, `application-prod.yml:42`
- **기법:** `server.shutdown=graceful` + Docker `HEALTHCHECK`
- **왜 배울 가치:**
  - SIGTERM 시 진행 중 요청 처리 후 종료 → **zero-downtime 배포**
  - 컨테이너 오케스트레이션(k8s) 친화적

---

## 6. 안티패턴 — 반면교사

### 6-1. 빈 Filter (미구현 placeholder)
- **위치:** `JwtFilter.java`, `MdcFilter.java`
- **문제:**
  - 본문 없이 `filterChain.doFilter()`만 호출
  - 특히 **MdcFilter는 traceId 주입 로직이 빠져 있음** → CLAUDE.md에 적힌 "Logback + MDC" 의도가 미구현 상태
- **학습:**
  - 의도를 코드에 명시하거나, 불필요하면 제거
  - **직접 MDC 필터를 채워보는 것이 좋은 학습 거리** (요청 ID 생성 → MDC.put → 로그 패턴에 `%X{traceId}` 추가)

---

## 핵심 정리

이 프로젝트는 **단순 CRUD를 벗어나 실무 수준의 보안 · 성능 · 확장성**을 구현하고 있다.

| 영역 | 핵심 기법 |
|------|-----------|
| 보안 | RT Rotation, Rate Limiting, IDOR 방어, Filter 순서 |
| 성능 | N+1 방지, Native Query 2단계 분리, Dirty Checking |
| 설계 | 도메인 메서드 캡슐화, 사전 검증 + 사후 처리 분리 |
| 인프라 | 다단계 빌드, Graceful Shutdown, Flyway validate |

### 우선순위 추천 (깊게 볼 순서)
1. **Refresh Token Rotation** — 보안 면접 단골
2. **Native Query 2단계 분리** — 성능 사고력
3. **도메인 메서드 캡슐화** — 설계 감각
4. **Filter 순서 설계** — 시스템적 사고
5. **Flyway validate 전략** — 운영 마인드
