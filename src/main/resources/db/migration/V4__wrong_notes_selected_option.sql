-- ============================================================================
-- V4__wrong_notes_selected_option.sql
--   오답노트에 "사용자가 선택했던 답" 을 보관하기 위한 컬럼 추가.
--   기존 행은 NULL 로 남고, 다음 응시 시 ExamService.autoCreateWrongNotes 가
--   최신 선택값으로 갱신한다.
-- ============================================================================

ALTER TABLE wrong_notes
    ADD COLUMN selected_option INT
    CHECK (selected_option IS NULL OR selected_option BETWEEN 1 AND 4);
