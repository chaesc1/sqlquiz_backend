package sqlquiz.domain.statistics.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "본인 통계 요약")
public record SummaryResponse(

        @Schema(description = "시작한 시험 횟수 (IN_PROGRESS 포함)")
        long totalAttempts,

        @Schema(description = "완료한 시험 횟수")
        long completedAttempts,

        @Schema(description = "완료한 시험들의 평균 점수 (0~100)")
        double averageScore,

        @Schema(description = "완료한 시험들의 총 문제 수")
        long totalQuestions,

        @Schema(description = "완료한 시험들에서 맞춘 문제 수")
        long totalCorrect
) {
    /** 전체 정답률(0~100). 분모가 0이면 0. */
    public double overallAccuracy() {
        return totalQuestions == 0 ? 0.0 : (double) totalCorrect / totalQuestions * 100.0;
    }
}
