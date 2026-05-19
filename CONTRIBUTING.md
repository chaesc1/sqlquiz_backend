# Contributing Guide

## 브랜치 전략

```
main
 └─ develop
     ├─ feature/기능명
     └─ hotfix/버그명
```

| 브랜치 | 용도 | 머지 대상 |
|--------|------|-----------|
| `main` | 배포 가능한 상태 (Jenkins 트리거) | — |
| `develop` | 기능 통합 | `main` |
| `feature/*` | 기능 개발 | `develop` |
| `hotfix/*` | 긴급 버그 수정 | `main` + `develop` |

### 브랜치 네이밍
```
feature/login-api
feature/question-crud
hotfix/jwt-expiry-bug
```

---

## 커밋 컨벤션

```
<type>: <제목>

<본문 (선택)>
```

| type | 용도 |
|------|------|
| `feat` | 새 기능 |
| `fix` | 버그 수정 |
| `refactor` | 리팩토링 |
| `test` | 테스트 코드 |
| `docs` | 문서 |
| `chore` | 빌드/설정 변경 |

### 예시
```
feat: 사용자 로그인 API 추가
fix: JWT 만료 시간 계산 오류 수정
refactor: UserService 의존성 분리
chore: Flyway 의존성 추가
```

---

## PR 흐름

```
feature/xxx → develop  (기능 개발 완료 시)
develop     → main     (릴리즈 시)
hotfix/xxx  → main     (긴급 수정 시, develop 에도 머지)
```

- PR은 항상 `develop`을 타겟으로 연다 (`main` 직접 머지 금지)
- 1 PR = 1 기능/수정
- 머지 방식: **Squash and merge** (develop → main 은 Merge commit)
