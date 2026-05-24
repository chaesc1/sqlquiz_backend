package sqlquiz.domain.question.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import sqlquiz.domain.common.Difficulty;
import sqlquiz.domain.common.ExamType;
import sqlquiz.domain.question.entity.Question;

/**
 * 문제 목록용 응답.
 *
 * 의도적으로 {@code answer}와 {@code explanation}을 제외했다.
 * 목록 단계에서는 풀이 전 사용자가 정답을 미리 볼 수 있어선 안 되기 때문.
 * 정답/해설은 {@link QuestionDetailResponse}를 통해 (시험 완료 후 / ADMIN / 오답노트 흐름에서만) 노출.
 */
@Schema(description = "문제 목록 응답 (정답/해설 제외)")
public record QuestionResponse(

        @Schema(description = "문제 ID")
        Long id,

        @Schema(description = "문제 지문")
        String content,

        String option1,
        String option2,
        String option3,
        String option4,

        @Schema(description = "난이도")
        Difficulty difficulty,

        @Schema(description = "카테고리 ID")
        Long categoryId,

        @Schema(description = "카테고리 이름")
        String categoryName,

        @Schema(description = "시험 종류")
        ExamType examType
) {
    public static QuestionResponse from(Question q) {
        return new QuestionResponse(
                q.getId(),
                q.getContent(),
                q.getOption1(), q.getOption2(), q.getOption3(), q.getOption4(),
                q.getDifficulty(),
                q.getCategory().getId(),
                q.getCategory().getName(),
                q.getCategory().getExamType()
        );
    }
}
