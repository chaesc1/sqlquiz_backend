package sqlquiz.domain.question.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sqlquiz.domain.common.ExamType;
import sqlquiz.domain.question.dto.CategoryResponse;
import sqlquiz.domain.question.service.CategoryService;
import sqlquiz.global.response.ApiResponse;

import java.util.List;

@Tag(name = "Category", description = "카테고리 조회 API")
@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "카테고리 목록", description = "examType 필터 선택. 인증 불필요.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> list(
            @RequestParam(required = false) ExamType examType
    ) {
        return ResponseEntity.ok(ApiResponse.ok(categoryService.list(examType)));
    }
}
