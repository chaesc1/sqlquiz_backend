package sqlquiz.domain.wrongnote.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 다시 풀기 결과.
 * 정답이면 isResolved=true 가 함께 반환되어 카드 UI 가 즉시 해결 상태로 전환된다.
 */
@Schema(description = "오답노트 다시 풀기 결과")
public record WrongNoteRetryResponse(

        @Schema(description = "이번 시도 정답 여부")
        Boolean isCorrect,

        @Schema(description = "정답 보기 번호 (1~4)")
        Integer correctAnswer,

        @Schema(description = "해설")
        String explanation,

        @Schema(description = "이번 시도 후 해결 상태 (정답이면 true 로 자동 전환)")
        Boolean isResolved,

        @Schema(description = "이번에 선택한 보기 번호 — 카드의 selectedOption 표시 갱신용")
        Integer selectedOption
) {
}
