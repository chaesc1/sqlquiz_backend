package sqlquiz.domain.question.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import sqlquiz.domain.common.Difficulty;
import sqlquiz.domain.common.ExamType;

/**
 * 문제 목록 검색 조건. 모든 필드가 nullable → 부분 필터를 자유롭게 조합.
 *
 * record를 사용하지만 Spring MVC가 query string을 record 생성자로 바인딩하려면
 * {@code @ConstructorBinding} 없이도 동작 (Spring 6 / Boot 3.2+ 기준).
 * 컨트롤러에서 @ModelAttribute로 받는다.
 */
@Schema(description = "문제 검색 조건 (전부 선택값)")
public record QuestionSearchCondition(

        @Schema(description = "시험 종류 필터 (SQLD/SQLP)")
        ExamType examType,

        @Schema(description = "카테고리 ID 필터")
        Long categoryId,

        @Schema(description = "난이도 필터")
        Difficulty difficulty
) {
}
