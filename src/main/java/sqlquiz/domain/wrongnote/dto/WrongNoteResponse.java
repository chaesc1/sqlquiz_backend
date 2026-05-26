package sqlquiz.domain.wrongnote.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import sqlquiz.domain.common.Difficulty;
import sqlquiz.domain.common.ExamType;
import sqlquiz.domain.wrongnote.entity.WrongNote;

import java.time.LocalDateTime;

/**
 * 오답노트 응답. "복기" 용도라 문제 본문/보기/정답/해설을 모두 포함.
 * (목록도 동일한 구조 — 화면에서 펼치기 흐름이 자연스러움)
 */
@Schema(description = "오답노트 응답")
public record WrongNoteResponse(

        @Schema(description = "오답노트 ID")
        Long id,

        @Schema(description = "문제 ID")
        Long questionId,

        @Schema(description = "문제 지문")
        String content,

        String option1, String option2, String option3, String option4,

        @Schema(description = "정답 번호 (1~4)")
        Integer answer,

        @Schema(description = "해설")
        String explanation,

        Difficulty difficulty,
        Long categoryId,
        String categoryName,
        ExamType examType,

        @Schema(description = "사용자 메모 (없으면 null)")
        String memo,

        @Schema(description = "해결 여부")
        Boolean isResolved,

        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static WrongNoteResponse from(WrongNote w) {
        var q = w.getQuestion();
        var c = q.getCategory();
        return new WrongNoteResponse(
                w.getId(),
                q.getId(),
                q.getContent(),
                q.getOption1(), q.getOption2(), q.getOption3(), q.getOption4(),
                q.getAnswer(),
                q.getExplanation(),
                q.getDifficulty(),
                c.getId(),
                c.getName(),
                c.getExamType(),
                w.getMemo(),
                w.getIsResolved(),
                w.getCreatedAt(),
                w.getUpdatedAt()
        );
    }
}
