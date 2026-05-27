package sqlquiz.domain.exam.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 한 문제에 대한 답안.
 * selectedOption 이 null 이면 "포기/미선택" → 자동으로 오답 처리.
 */
@Schema(description = "문항별 답안")
public record AnswerSubmission(

        @Schema(description = "문제 ID")
        @NotNull
        Long questionId,

        @Schema(description = "선택한 보기 번호 (1~4). null 허용 = 미선택", example = "2")
        @Min(value = 1, message = "선택지는 1~4 사이여야 합니다.")
        @Max(value = 4, message = "선택지는 1~4 사이여야 합니다.")
        Integer selectedOption
) {
}
