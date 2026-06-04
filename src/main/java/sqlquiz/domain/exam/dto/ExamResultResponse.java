package sqlquiz.domain.exam.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import sqlquiz.domain.common.ExamType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "시험 채점 결과")
public record ExamResultResponse(
        UUID attemptId,
        ExamType examType,

        @Schema(description = "맞춘 문제 수")
        Integer correctCount,

        @Schema(description = "전체 문제 수")
        Integer totalCount,

        @Schema(description = "점수 (0~100)")
        Integer score,

        @Schema(description = "응시 종료 시각")
        LocalDateTime completedAt,

        @Schema(description = "카테고리별 정답 통계")
        List<CategoryScore> categoryStats,

        @Schema(description = "문항별 채점 결과 (정답·선택·해설 포함)")
        List<QuestionResult> questionResults
) {
}
