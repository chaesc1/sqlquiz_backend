package sqlquiz.domain.question.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import sqlquiz.domain.common.Difficulty;
import sqlquiz.domain.common.ExamType;
import sqlquiz.domain.question.entity.Question;

/**
 * 문제 상세 응답 — 정답/해설 포함.
 *
 * 학습 단계 단순화를 위해 일반 GET /api/v1/questions/{id} 도 본 DTO를 사용한다.
 * (운영에서는 "시험 완료 여부" 체크 후 정답/해설을 분리 노출하는 것이 정석)
 */
@Schema(description = "문제 상세 응답 (정답/해설 포함)")
public record QuestionDetailResponse(
        Long id,
        String content,
        String option1, String option2, String option3, String option4,

        @Schema(description = "정답 번호 (1~4)")
        Integer answer,

        @Schema(description = "해설")
        String explanation,

        Difficulty difficulty,
        Long categoryId,
        String categoryName,
        ExamType examType
) {
    public static QuestionDetailResponse from(Question q) {
        return new QuestionDetailResponse(
                q.getId(),
                q.getContent(),
                q.getOption1(), q.getOption2(), q.getOption3(), q.getOption4(),
                q.getAnswer(),
                q.getExplanation(),
                q.getDifficulty(),
                q.getCategory().getId(),
                q.getCategory().getName(),
                q.getCategory().getExamType()
        );
    }
}
