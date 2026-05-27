package sqlquiz.domain.exam.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

@Schema(description = "시험 답안 일괄 제출 요청")
public record ExamSubmitRequest(

        @Schema(description = "각 문항에 대한 답안 목록 (모든 문항을 포함하지 않아도 됨 — 빠진 것은 미선택 처리)")
        @NotEmpty(message = "답안은 1개 이상이어야 합니다.")
        @Valid
        List<AnswerSubmission> answers
) {
}
