 package sqlquiz.domain.question.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import sqlquiz.domain.question.dto.*;
import sqlquiz.domain.question.service.QuestionService;
import sqlquiz.global.response.ApiResponse;

import java.net.URI;

/**
 * 문제 API.
 *
 * 권한 매트릭스:
 *   GET   /api/v1/questions       — permitAll (목록, 정답/해설 제외)
 *   GET   /api/v1/questions/{id}  — permitAll (상세, 정답/해설 포함 / 학습 단계 단순화)
 *   POST  /api/v1/questions       — ROLE_ADMIN
 *   PUT   /api/v1/questions/{id}  — ROLE_ADMIN
 *   DELETE                         — ROLE_ADMIN
 *
 * GET의 permitAll은 SecurityConfig의 경로 기반으로 이미 처리됨.
 * 변경 계열(POST/PUT/DELETE)은 SecurityConfig "anyRequest().authenticated()" 만으로는 ADMIN/USER 구분이 안 되므로
 * 메서드 레벨 @PreAuthorize로 제어 — 권한 규칙을 엔드포인트 옆에 두는 게 가독성·유지보수에 유리.
 */
@Tag(name = "Question", description = "문제 CRUD API")
@RestController
@RequestMapping("/api/v1/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

    @Operation(summary = "문제 목록 검색",
            description = "examType / categoryId / difficulty 부분 필터 + 페이지네이션 (?page=0&size=20&sort=id,desc)")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<QuestionResponse>>> list(
            @ModelAttribute QuestionSearchCondition condition,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok(questionService.search(condition, pageable)));
    }

    @Operation(summary = "문제 상세 조회", description = "정답/해설 포함.")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<QuestionDetailResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(questionService.getDetail(id)));
    }

    @Operation(summary = "문제 등록",
            description = "ADMIN 전용. 성공 시 Location 헤더에 새 문제 URI 반환.",
            security = @SecurityRequirement(name = "BearerAuth"))
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ApiResponse<Long>> create(@Valid @RequestBody QuestionCreateRequest request) {
        Long id = questionService.create(request);
        return ResponseEntity
                .created(URI.create("/api/v1/questions/" + id))
                .body(ApiResponse.ok("문제가 등록되었습니다.", id));
    }

    @Operation(summary = "문제 수정",
            description = "ADMIN 전용. 전체 교체(PUT).",
            security = @SecurityRequirement(name = "BearerAuth"))
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> update(
            @PathVariable Long id,
            @Valid @RequestBody QuestionUpdateRequest request
    ) {
        questionService.update(id, request);
        return ResponseEntity.ok(ApiResponse.ok("문제가 수정되었습니다.", null));
    }

    @Operation(summary = "문제 삭제",
            description = "ADMIN 전용.",
            security = @SecurityRequirement(name = "BearerAuth"))
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        questionService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("문제가 삭제되었습니다.", null));
    }
}
