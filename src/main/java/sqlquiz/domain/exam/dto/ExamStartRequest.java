package sqlquiz.domain.exam.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import sqlquiz.domain.common.Difficulty;
import sqlquiz.domain.common.ExamType;

@Schema(description = "시험 시작 요청")
public record ExamStartRequest(

        @Schema(description = "시험 종류", example = "SQLD")
        @NotNull(message = "시험 종류는 필수입니다.")
        ExamType examType,

        @Schema(description = "출제 문항 수", example = "10")
        @NotNull
        @Min(value = 1, message = "최소 1문항 이상이어야 합니다.")
        @Max(value = 50, message = "최대 50문항까지 가능합니다.")
        Integer count,

        @Schema(description = "난이도 필터 (선택)")
        Difficulty difficulty
) {
}
