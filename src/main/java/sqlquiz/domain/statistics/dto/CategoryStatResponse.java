package sqlquiz.domain.statistics.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import sqlquiz.domain.common.ExamType;

@Schema(description = "카테고리별 정답률")
public record CategoryStatResponse(
        Long categoryId,
        String categoryName,
        ExamType examType,

        @Schema(description = "해당 카테고리에서 푼 문제 수")
        long totalAttempted,

        @Schema(description = "그 중 정답 수")
        long totalCorrect,

        @Schema(description = "정답률 (0~100)")
        double accuracy
) {
}
