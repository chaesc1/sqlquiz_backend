package sqlquiz.domain.exam.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import sqlquiz.domain.common.Difficulty;
import sqlquiz.domain.exam.entity.AttemptAnswer;

/**
 * 채점 결과 화면용 문항 단위 DTO.
 * 정답/해설을 포함하므로 시험이 COMPLETED 된 이후 경로(submit/result) 에서만 사용한다.
 */
@Schema(description = "문항별 채점 결과")
public record QuestionResult(
        Long questionId,
        String content,
        String option1, String option2, String option3, String option4,

        @Schema(description = "사용자가 선택한 답 (미선택이면 null)")
        Integer selectedOption,

        @Schema(description = "정답 보기 번호 (1~4)")
        Integer correctAnswer,

        Boolean isCorrect,
        String explanation,
        String categoryName,
        Difficulty difficulty
) {
    public static QuestionResult from(AttemptAnswer a) {
        var q = a.getQuestion();
        return new QuestionResult(
                q.getId(),
                q.getContent(),
                q.getOption1(), q.getOption2(), q.getOption3(), q.getOption4(),
                a.getSelectedOption(),
                q.getAnswer(),
                a.getIsCorrect(),
                q.getExplanation(),
                q.getCategory().getName(),
                q.getDifficulty()
        );
    }
}
