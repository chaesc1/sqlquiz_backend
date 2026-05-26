package sqlquiz.domain.wrongnote.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "오답노트 수동 등록 요청")
public record WrongNoteCreateRequest(

        @Schema(description = "오답노트에 추가할 문제 ID")
        @NotNull(message = "questionId 는 필수입니다.")
        Long questionId
) {
}
