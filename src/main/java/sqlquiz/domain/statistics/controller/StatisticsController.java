package sqlquiz.domain.statistics.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sqlquiz.domain.statistics.dto.CategoryStatResponse;
import sqlquiz.domain.statistics.dto.HistoryItemResponse;
import sqlquiz.domain.statistics.dto.SummaryResponse;
import sqlquiz.domain.statistics.service.StatisticsService;
import sqlquiz.global.exception.CustomException;
import sqlquiz.global.exception.ErrorCode;
import sqlquiz.global.response.ApiResponse;

import java.util.List;

@Tag(name = "Statistics", description = "응시 통계 API")
@RestController
@RequestMapping("/api/v1/statistics")
@RequiredArgsConstructor
@Validated
public class StatisticsController {

    private final StatisticsService statisticsService;

    @Operation(summary = "본인 요약 통계",
            description = "총 응시/완료 횟수, 평균 점수, 총 문제 수, 정답 수.",
            security = @SecurityRequirement(name = "BearerAuth"))
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<SummaryResponse>> summary(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(statisticsService.summary(currentEmail(authentication))));
    }

    @Operation(summary = "카테고리별 정답률",
            description = "본인이 푼 모든 카테고리의 정답률을 ExamType / id 순으로 반환.",
            security = @SecurityRequirement(name = "BearerAuth"))
    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<CategoryStatResponse>>> categories(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(statisticsService.categoryStats(currentEmail(authentication))));
    }

    @Operation(summary = "취약 카테고리 Top N",
            description = "정답률 낮은 카테고리 N개. limit 기본 5, 최대 20.",
            security = @SecurityRequirement(name = "BearerAuth"))
    @GetMapping("/weak-points")
    public ResponseEntity<ApiResponse<List<CategoryStatResponse>>> weakPoints(
            @RequestParam(defaultValue = "5") @Min(1) @Max(20) int limit,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.ok(statisticsService.weakPoints(currentEmail(authentication), limit)));
    }

    @Operation(summary = "최근 응시 이력 (페이지)",
            description = "본인 attempts 를 최신순으로. ?page/?size/?sort 지원.",
            security = @SecurityRequirement(name = "BearerAuth"))
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<Page<HistoryItemResponse>>> history(
            @ParameterObject @PageableDefault(size = 20) Pageable pageable,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.ok(statisticsService.history(currentEmail(authentication), pageable)));
    }

    private String currentEmail(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        return authentication.getName();
    }
}
