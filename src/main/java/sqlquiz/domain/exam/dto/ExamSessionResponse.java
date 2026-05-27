package sqlquiz.domain.exam.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import sqlquiz.domain.common.AttemptStatus;
import sqlquiz.domain.common.ExamType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "시험 세션 응답 (시작 직후 / 이어풀기)")
public record ExamSessionResponse(
        UUID attemptId,
        ExamType examType,
        AttemptStatus status,
        Integer totalCount,
        LocalDateTime startedAt,
        List<QuestionInExam> questions
) {
}
