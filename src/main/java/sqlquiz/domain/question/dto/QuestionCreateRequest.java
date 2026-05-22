package sqlquiz.domain.question.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import sqlquiz.domain.common.Difficulty;

@Schema(description = "문제 등록 요청 (ADMIN 전용)")
public record QuestionCreateRequest(

        @Schema(description = "카테고리 ID", example = "1")
        @NotNull(message = "카테고리 ID는 필수입니다.")
        Long categoryId,

        @Schema(description = "문제 지문")
        @NotBlank(message = "문제 지문은 필수입니다.")
        String content,

        @NotBlank String option1,
        @NotBlank String option2,
        @NotBlank String option3,
        @NotBlank String option4,

        @Schema(description = "정답 번호 (1~4)", example = "2")
        @NotNull
        @Min(value = 1, message = "정답은 1~4 사이여야 합니다.")
        @Max(value = 4, message = "정답은 1~4 사이여야 합니다.")
        Integer answer,

        @Schema(description = "해설 (선택)")
        String explanation,

        @NotNull
        Difficulty difficulty
) {
}
