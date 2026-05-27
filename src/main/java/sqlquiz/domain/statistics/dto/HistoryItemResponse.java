package sqlquiz.domain.statistics.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import sqlquiz.domain.common.AttemptStatus;
import sqlquiz.domain.common.ExamType;
import sqlquiz.domain.exam.entity.Attempt;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "응시 이력 1건")
public record HistoryItemResponse(
        UUID attemptId,
        ExamType examType,
        AttemptStatus status,
        Integer totalCount,
        Integer correctCount,
        Integer score,
        LocalDateTime startedAt,
        LocalDateTime completedAt
) {
    public static HistoryItemResponse from(Attempt a) {
        return new HistoryItemResponse(
                a.getId(),
                a.getExamType(),
                a.getStatus(),
                a.getTotalCount(),
                a.getCorrectCount(),
                a.getScore(),
                a.getStartedAt(),
                a.getCompletedAt()
        );
    }
}
