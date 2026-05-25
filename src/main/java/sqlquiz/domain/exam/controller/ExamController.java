package sqlquiz.domain.exam.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import sqlquiz.domain.exam.dto.*;
import sqlquiz.domain.exam.service.ExamService;
import sqlquiz.global.exception.CustomException;
import sqlquiz.global.exception.ErrorCode;
import sqlquiz.global.response.ApiResponse;

import java.net.URI;
import java.util.UUID;

/**
 * 시험 API. 모든 엔드포인트는 인증 필요.
 *
 * 인증 객체에서 email 을 꺼내 ExamService 가 본인 권한을 검증.
 * SecurityConfig 의 anyRequest().authenticated() 가 인증 자체를 보장.
 */
@Tag(name = "Exam", description = "시험 응시 API")
@RestController
@RequestMapping("/api/v1/exams")
@RequiredArgsConstructor
public class ExamController {

    private final ExamService examService;

    @Operation(summary = "시험 시작",
            description = "examType / count(1~50) / difficulty(선택) 로 무작위 N문제 추출 후 세션 생성.",
            security = @SecurityRequirement(name = "BearerAuth"))
    @PostMapping
    public ResponseEntity<ApiResponse<ExamSessionResponse>> start(
            @Valid @RequestBody ExamStartRequest request,
            Authentication authentication
    ) {
        ExamSessionResponse session = examService.start(currentEmail(authentication), request);
        return ResponseEntity
                .created(URI.create("/api/v1/exams/" + session.attemptId()))
                .body(ApiResponse.ok("시험이 시작되었습니다.", session));
    }

    @Operation(summary = "시험 세션 조회 (이어풀기)",
            description = "본인 attempt 만 조회 가능. IN_PROGRESS/COMPLETED 모두 조회 가능.",
            security = @SecurityRequirement(name = "BearerAuth"))
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ExamSessionResponse>> get(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                examService.resume(currentEmail(authentication), id)
        ));
    }

    @Operation(summary = "답안 제출 + 자동 채점",
            description = "한 번 제출하면 COMPLETED 로 잠긴다. 오답은 자동으로 WrongNote 에 등록.",
            security = @SecurityRequirement(name = "BearerAuth"))
    @PostMapping("/{id}/submit")
    public ResponseEntity<ApiResponse<ExamResultResponse>> submit(
            @PathVariable UUID id,
            @Valid @RequestBody ExamSubmitRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "채점이 완료되었습니다.",
                examService.submit(currentEmail(authentication), id, request)
        ));
    }

    @Operation(summary = "채점 결과 조회",
            description = "COMPLETED 상태에서만 호출 가능. 카테고리별 정답 통계 포함.",
            security = @SecurityRequirement(name = "BearerAuth"))
    @GetMapping("/{id}/result")
    public ResponseEntity<ApiResponse<ExamResultResponse>> result(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                examService.result(currentEmail(authentication), id)
        ));
    }

    private String currentEmail(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        return authentication.getName();
    }
}
