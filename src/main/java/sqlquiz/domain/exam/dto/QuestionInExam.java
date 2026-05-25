package sqlquiz.domain.exam.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import sqlquiz.domain.common.Difficulty;
import sqlquiz.domain.exam.entity.AttemptAnswer;

/**
 * 시험 진행 중 노출되는 문제 정보.
 * 정답/해설은 절대 포함하지 않는다 — 진행 중에 새 나가면 시험 의미가 사라짐.
 * 이전에 고른 답(selectedOption)은 "이어풀기" 용도로 함께 반환.
 */
@Schema(description = "시험 중 문제 정보 (정답/해설 제외)")
public record QuestionInExam(
        Long questionId,
        String content,
        String option1, String option2, String option3, String option4,
        Difficulty difficulty,
        String categoryName,

        @Schema(description = "이전에 선택한 답 (없으면 null)")
        Integer selectedOption
) {
    public static QuestionInExam from(AttemptAnswer answer) {
        var q = answer.getQuestion();
        return new QuestionInExam(
                q.getId(),
                q.getContent(),
                q.getOption1(), q.getOption2(), q.getOption3(), q.getOption4(),
                q.getDifficulty(),
                q.getCategory().getName(),
                answer.getSelectedOption()
        );
    }
}
