package sqlquiz.domain.wrongnote.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 오답노트 단건 재시도 요청.
 * selectedOption 은 반드시 1~4 — "다시 풀기"는 미선택 의미가 없으므로 NotNull.
 */
@Schema(description = "오답노트 다시 풀기 요청")
public record WrongNoteRetryRequest(

        @Schema(description = "이번에 선택한 답 (1~4)")
        @NotNull(message = "선택지를 골라야 합니다.")
        @Min(value = 1, message = "선택지는 1~4 사이여야 합니다.")
        @Max(value = 4, message = "선택지는 1~4 사이여야 합니다.")
        Integer selectedOption
) {
}
