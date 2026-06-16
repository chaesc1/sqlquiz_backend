package sqlquiz.domain.wrongnote.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import sqlquiz.domain.wrongnote.dto.WrongNoteCreateRequest;
import sqlquiz.domain.wrongnote.dto.WrongNoteMemoUpdateRequest;
import sqlquiz.domain.wrongnote.dto.WrongNoteResponse;
import sqlquiz.domain.wrongnote.dto.WrongNoteRetryRequest;
import sqlquiz.domain.wrongnote.dto.WrongNoteRetryResponse;
import sqlquiz.domain.wrongnote.service.WrongNoteService;
import sqlquiz.global.exception.CustomException;
import sqlquiz.global.exception.ErrorCode;
import sqlquiz.global.response.ApiResponse;

import java.net.URI;

/**
 * 오답노트 API. 모든 엔드포인트는 인증 필요 + 본인 데이터만 접근.
 */
@Tag(name = "WrongNote", description = "오답노트 API")
@RestController
@RequestMapping("/api/v1/wrong-notes")
@RequiredArgsConstructor
public class WrongNoteController {

    private final WrongNoteService wrongNoteService;

    @Operation(summary = "오답노트 목록",
            description = "본인 노트 검색. categoryId / isResolved 부분 필터, ?page/?size/?sort 지원.",
            security = @SecurityRequirement(name = "BearerAuth"))
    @GetMapping
    public ResponseEntity<ApiResponse<Page<WrongNoteResponse>>> list(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Boolean isResolved,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                wrongNoteService.list(currentEmail(authentication), categoryId, isResolved, pageable)
        ));
    }

    @Operation(summary = "오답노트 수동 등록",
            description = "questionId 로 본인 노트에 추가. 이미 존재하면 409 WRONG_NOTE_ALREADY_EXISTS.",
            security = @SecurityRequirement(name = "BearerAuth"))
    @PostMapping
    public ResponseEntity<ApiResponse<Long>> create(
            @Valid @RequestBody WrongNoteCreateRequest request,
            Authentication authentication
    ) {
        Long id = wrongNoteService.create(currentEmail(authentication), request);
        return ResponseEntity
                .created(URI.create("/api/v1/wrong-notes/" + id))
                .body(ApiResponse.ok("오답노트에 등록되었습니다.", id));
    }

    @Operation(summary = "오답노트 메모 수정",
            description = "본인 노트의 메모만 수정 가능. 빈 문자열/null 은 메모 지우기로 처리.",
            security = @SecurityRequirement(name = "BearerAuth"))
    @PatchMapping("/{id}/memo")
    public ResponseEntity<ApiResponse<Void>> updateMemo(
            @PathVariable Long id,
            @Valid @RequestBody WrongNoteMemoUpdateRequest request,
            Authentication authentication
    ) {
        wrongNoteService.updateMemo(currentEmail(authentication), id, request);
        return ResponseEntity.ok(ApiResponse.ok("메모가 수정되었습니다.", null));
    }

    @Operation(summary = "오답노트 해결 표시 (멱등)",
            description = "isResolved 를 true 로 토글. 이미 true 여도 200.",
            security = @SecurityRequirement(name = "BearerAuth"))
    @PatchMapping("/{id}/resolve")
    public ResponseEntity<ApiResponse<Void>> resolve(
            @PathVariable Long id,
            Authentication authentication
    ) {
        wrongNoteService.resolve(currentEmail(authentication), id);
        return ResponseEntity.ok(ApiResponse.ok("해결로 표시되었습니다.", null));
    }

    @Operation(summary = "오답노트 다시 풀기",
            description = "정답/해설 비공개 상태에서 한 번 더 풀고 결과를 받는다. 정답이면 자동으로 isResolved=true.",
            security = @SecurityRequirement(name = "BearerAuth"))
    @PostMapping("/{id}/retry")
    public ResponseEntity<ApiResponse<WrongNoteRetryResponse>> retry(
            @PathVariable Long id,
            @Valid @RequestBody WrongNoteRetryRequest request,
            Authentication authentication
    ) {
        WrongNoteRetryResponse result = wrongNoteService.retry(currentEmail(authentication), id, request);
        String msg = Boolean.TRUE.equals(result.isCorrect()) ? "정답입니다." : "오답입니다.";
        return ResponseEntity.ok(ApiResponse.ok(msg, result));
    }

    @Operation(summary = "오답노트 삭제",
            description = "본인 노트만 삭제 가능.",
            security = @SecurityRequirement(name = "BearerAuth"))
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long id,
            Authentication authentication
    ) {
        wrongNoteService.delete(currentEmail(authentication), id);
        return ResponseEntity.ok(ApiResponse.ok("오답노트가 삭제되었습니다.", null));
    }

    private String currentEmail(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        return authentication.getName();
    }
}
