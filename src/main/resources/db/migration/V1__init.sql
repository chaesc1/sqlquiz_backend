-- ============================================================================
-- V1__init.sql  : SQLD/SQLP 문제은행 초기 스키마 (PostgreSQL 16)
--
-- 주의:
--  - 엔티티 정의(JPA) 기준으로 DDL 작성. Flyway가 먼저 테이블을 생성하므로
--    application.yml 의 spring.jpa.hibernate.ddl-auto 는 'validate' 권장.
--  - UUID 컬럼은 pgcrypto 의 gen_random_uuid() 사용 (PostgreSQL 13+ 기본 포함)
--  - Enum은 VARCHAR + CHECK 제약으로 표현 (DB 종속성 최소화)
-- ============================================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ---------------------------------------------------------------------------
-- users
-- ---------------------------------------------------------------------------
CREATE TABLE users (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    email       VARCHAR(100) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    nickname    VARCHAR(20)  NOT NULL,
    role        VARCHAR(20)  NOT NULL CHECK (role IN ('ROLE_USER', 'ROLE_ADMIN')),
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_email ON users (email);

-- ---------------------------------------------------------------------------
-- categories
-- ---------------------------------------------------------------------------
CREATE TABLE categories (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(50)  NOT NULL,
    exam_type   VARCHAR(10)  NOT NULL CHECK (exam_type IN ('SQLD', 'SQLP')),
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_categories_exam_type ON categories (exam_type);

-- ---------------------------------------------------------------------------
-- questions
-- ---------------------------------------------------------------------------
CREATE TABLE questions (
    id            BIGSERIAL   PRIMARY KEY,
    category_id   BIGINT      NOT NULL REFERENCES categories (id),
    content       TEXT        NOT NULL,
    option1       TEXT        NOT NULL,
    option2       TEXT        NOT NULL,
    option3       TEXT        NOT NULL,
    option4       TEXT        NOT NULL,
    answer        INT         NOT NULL CHECK (answer BETWEEN 1 AND 4),
    explanation   TEXT,
    difficulty    VARCHAR(10) NOT NULL CHECK (difficulty IN ('EASY', 'MEDIUM', 'HARD')),
    created_at    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_questions_category    ON questions (category_id);
CREATE INDEX idx_questions_difficulty  ON questions (difficulty);

-- ---------------------------------------------------------------------------
-- attempts (시험 세션)
-- ---------------------------------------------------------------------------
CREATE TABLE attempts (
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        UUID         NOT NULL REFERENCES users (id),
    exam_type      VARCHAR(10)  NOT NULL CHECK (exam_type IN ('SQLD', 'SQLP')),
    status         VARCHAR(20)  NOT NULL CHECK (status IN ('IN_PROGRESS', 'COMPLETED')),
    total_count    INT          NOT NULL,
    correct_count  INT          NOT NULL DEFAULT 0,
    score          INT          NOT NULL DEFAULT 0,
    started_at     TIMESTAMP    NOT NULL,
    completed_at   TIMESTAMP
);

CREATE INDEX idx_attempts_user   ON attempts (user_id);
CREATE INDEX idx_attempts_status ON attempts (status);

-- ---------------------------------------------------------------------------
-- attempt_answers (각 문항 풀이 기록 / 로그성)
--  - 단순 로그 테이블이라 PK는 BIGSERIAL 사용 (attempts와 다른 전략)
-- ---------------------------------------------------------------------------
CREATE TABLE attempt_answers (
    id               BIGSERIAL  PRIMARY KEY,
    attempt_id       UUID       NOT NULL REFERENCES attempts (id) ON DELETE CASCADE,
    question_id      BIGINT     NOT NULL REFERENCES questions (id),
    selected_option  INT        CHECK (selected_option BETWEEN 1 AND 4),
    is_correct       BOOLEAN    NOT NULL,
    answered_at      TIMESTAMP  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_attempt_answers_attempt  ON attempt_answers (attempt_id);
CREATE INDEX idx_attempt_answers_question ON attempt_answers (question_id);

-- ---------------------------------------------------------------------------
-- wrong_notes (오답노트)
--  - 동일 user_id + question_id 중복 등록 방지 → UNIQUE 제약
-- ---------------------------------------------------------------------------
CREATE TABLE wrong_notes (
    id           BIGSERIAL   PRIMARY KEY,
    user_id      UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    question_id  BIGINT      NOT NULL REFERENCES questions (id),
    memo         TEXT,
    is_resolved  BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_wrong_notes_user_question UNIQUE (user_id, question_id)
);

CREATE INDEX idx_wrong_notes_user     ON wrong_notes (user_id);
CREATE INDEX idx_wrong_notes_resolved ON wrong_notes (user_id, is_resolved);
