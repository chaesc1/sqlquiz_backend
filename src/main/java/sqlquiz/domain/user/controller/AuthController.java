package sqlquiz.domain.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import sqlquiz.domain.user.dto.LoginRequest;
import sqlquiz.domain.user.dto.ReissueRequest;
import sqlquiz.domain.user.dto.SignupRequest;
import sqlquiz.domain.user.dto.TokenResponse;
import sqlquiz.domain.user.service.AuthService;
import sqlquiz.global.exception.CustomException;
import sqlquiz.global.exception.ErrorCode;
import sqlquiz.global.response.ApiResponse;

/**
 * 인증/인가 컨트롤러.
 *
 * 설계 메모:
 * - 모든 응답은 {@link ApiResponse}로 통일 (CLAUDE.md 규약)
 * - signup/login/reissue: 인증 불필요 → SecurityConfig에서 permitAll
 * - logout: 인증 필요 → SecurityContext에서 email 추출 (request body 불필요)
 */
@Tag(name = "Auth", description = "인증/인가 API")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "회원가입", description = "이메일/비밀번호/닉네임으로 가입합니다.")
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Void>> signup(@Valid @RequestBody SignupRequest request) {
        authService.signup(request);
        return ResponseEntity.ok(ApiResponse.ok("회원가입이 완료되었습니다.", null));
    }

    @Operation(summary = "로그인", description = "이메일/비밀번호로 로그인하고 JWT를 발급받습니다.")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody LoginRequest request) {
        TokenResponse tokens = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok("로그인에 성공했습니다.", tokens));
    }

    @Operation(summary = "토큰 재발급", description = "Refresh Token으로 Access/Refresh Token을 모두 재발급합니다. (Rotation)")
    @PostMapping("/reissue")
    public ResponseEntity<ApiResponse<TokenResponse>> reissue(@Valid @RequestBody ReissueRequest request) {
        TokenResponse tokens = authService.reissue(request);
        return ResponseEntity.ok(ApiResponse.ok("토큰이 재발급되었습니다.", tokens));
    }

    @Operation(
            summary = "로그아웃",
            description = "서버에 저장된 Refresh Token을 무효화합니다. Authorization 헤더 필요.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        // JwtAuthenticationFilter가 principal에 email을 넣어줌 → Authentication#getName() 반환
        authService.logout(authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok("로그아웃이 완료되었습니다.", null));
    }
}
