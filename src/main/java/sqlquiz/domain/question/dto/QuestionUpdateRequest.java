package sqlquiz.domain.question.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import sqlquiz.domain.common.Difficulty;

/**
 * 문제 수정 요청 (ADMIN 전용).
 *
 * 전체 교체(PUT) 방식이므로 모든 필드가 필수. 일부 필드만 바꾸는 PATCH를 만들고 싶다면
 * 별도 DTO + 컨트롤러 메서드로 분리하는 게 깔끔.
 */
@Schema(description = "문제 수정 요청 (ADMIN 전용)")
public record QuestionUpdateRequest(

        @NotNull Long categoryId,
        @NotBlank String content,
        @NotBlank String option1,
        @NotBlank String option2,
        @NotBlank String option3,
        @NotBlank String option4,

        @NotNull
        @Min(1) @Max(4)
        Integer answer,

        String explanation,

        @NotNull Difficulty difficulty
) {
}
