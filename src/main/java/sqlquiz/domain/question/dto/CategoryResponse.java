package sqlquiz.domain.question.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import sqlquiz.domain.common.ExamType;
import sqlquiz.domain.question.entity.Category;

@Schema(description = "카테고리 응답")
public record CategoryResponse(

        @Schema(description = "카테고리 ID", example = "1")
        Long id,

        @Schema(description = "카테고리 이름", example = "데이터 모델링의 이해")
        String name,

        @Schema(description = "시험 종류", example = "SQLD")
        ExamType examType
) {
    public static CategoryResponse from(Category c) {
        return new CategoryResponse(c.getId(), c.getName(), c.getExamType());
    }
}
