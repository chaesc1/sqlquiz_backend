package sqlquiz.domain.user.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import sqlquiz.domain.common.Role;
import sqlquiz.domain.user.dto.LoginRequest;
import sqlquiz.domain.user.dto.ReissueRequest;
import sqlquiz.domain.user.dto.SignupRequest;
import sqlquiz.domain.user.dto.TokenResponse;
import sqlquiz.domain.user.entity.User;
import sqlquiz.domain.user.repository.RefreshTokenRepository;
import sqlquiz.domain.user.repository.UserRepository;
import sqlquiz.global.exception.CustomException;
import sqlquiz.global.exception.ErrorCode;
import sqlquiz.global.util.JwtUtil;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AuthService 단위 테스트.
 *
 * 의존성을 모두 Mock 으로 대체 — 데이터베이스/Redis 없이 비즈니스 규칙만 검증.
 * 학습 포인트: Mockito stubbing, 분기마다 한 테스트로 분리, Nested 로 가독성.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtUtil jwtUtil;

    @InjectMocks AuthService authService;

    private static final String EMAIL = "user@example.com";
    private static final String RAW_PASSWORD = "password123!";
    private static final String HASHED_PASSWORD = "$2a$10$hashed";

    @Nested
    @DisplayName("signup()")
    class Signup {

        @Test
        @DisplayName("신규 이메일이면 BCrypt 인코딩 후 저장한다")
        void signup_success() {
            SignupRequest req = new SignupRequest(EMAIL, RAW_PASSWORD, "닉네임");
            when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
            when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(HASHED_PASSWORD);

            authService.signup(req);

            verify(userRepository).save(argThat(u ->
                    u.getEmail().equals(EMAIL)
                            && u.getPassword().equals(HASHED_PASSWORD)
                            && u.getNickname().equals("닉네임")
                            && u.getRole() == Role.ROLE_USER));
        }

        @Test
        @DisplayName("이미 존재하는 이메일이면 EMAIL_ALREADY_EXISTS")
        void signup_duplicate_email_throws() {
            when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

            assertThatThrownBy(() ->
                    authService.signup(new SignupRequest(EMAIL, RAW_PASSWORD, "x")))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS);

            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("login()")
    class Login {

        @Test
        @DisplayName("정상 자격증명이면 AT/RT 발급 + Redis 에 RT 저장")
        void login_success() {
            User user = userFixture();
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(RAW_PASSWORD, HASHED_PASSWORD)).thenReturn(true);
            when(jwtUtil.generateAccessToken(EMAIL, "ROLE_USER")).thenReturn("AT");
            when(jwtUtil.generateRefreshToken(EMAIL)).thenReturn("RT");

            TokenResponse tokens = authService.login(new LoginRequest(EMAIL, RAW_PASSWORD));

            assertThat(tokens.accessToken()).isEqualTo("AT");
            assertThat(tokens.refreshToken()).isEqualTo("RT");
            assertThat(tokens.tokenType()).isEqualTo("Bearer");
            verify(refreshTokenRepository).save(EMAIL, "RT");
        }

        @Test
        @DisplayName("이메일이 없으면 USER_NOT_FOUND")
        void login_user_not_found() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    authService.login(new LoginRequest(EMAIL, RAW_PASSWORD)))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.USER_NOT_FOUND);
        }

        @Test
        @DisplayName("비밀번호가 틀리면 INVALID_PASSWORD 이고 RT 는 저장되지 않는다")
        void login_wrong_password() {
            User user = userFixture();
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(RAW_PASSWORD, HASHED_PASSWORD)).thenReturn(false);

            assertThatThrownBy(() ->
                    authService.login(new LoginRequest(EMAIL, RAW_PASSWORD)))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_PASSWORD);

            verify(refreshTokenRepository, never()).save(anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("reissue()")
    class Reissue {

        @Test
        @DisplayName("저장된 RT 와 일치하면 새 AT/RT 발급 + Rotation 적용")
        void reissue_success_rotates() {
            String oldRT = "OLD_RT", newAT = "NEW_AT", newRT = "NEW_RT";
            User user = userFixture();
            when(jwtUtil.getEmail(oldRT)).thenReturn(EMAIL);
            when(refreshTokenRepository.find(EMAIL)).thenReturn(Optional.of(oldRT));
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(jwtUtil.generateAccessToken(EMAIL, "ROLE_USER")).thenReturn(newAT);
            when(jwtUtil.generateRefreshToken(EMAIL)).thenReturn(newRT);

            TokenResponse tokens = authService.reissue(new ReissueRequest(oldRT));

            assertThat(tokens.accessToken()).isEqualTo(newAT);
            assertThat(tokens.refreshToken()).isEqualTo(newRT);
            // 새 RT 를 저장 (덮어쓰기) — rotation
            verify(refreshTokenRepository).save(EMAIL, newRT);
        }

        @Test
        @DisplayName("Redis 에 RT 가 없으면 TOKEN_NOT_FOUND (로그아웃됐거나 만료된 케이스)")
        void reissue_token_not_in_redis() {
            String rt = "RT";
            when(jwtUtil.getEmail(rt)).thenReturn(EMAIL);
            when(refreshTokenRepository.find(EMAIL)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.reissue(new ReissueRequest(rt)))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.TOKEN_NOT_FOUND);
        }

        @Test
        @DisplayName("저장된 RT 와 다르면 INVALID_TOKEN (탈취 시나리오)")
        void reissue_rt_mismatch() {
            String submitted = "STOLEN_RT", saved = "ROTATED_RT";
            when(jwtUtil.getEmail(submitted)).thenReturn(EMAIL);
            when(refreshTokenRepository.find(EMAIL)).thenReturn(Optional.of(saved));

            assertThatThrownBy(() -> authService.reissue(new ReissueRequest(submitted)))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_TOKEN);
        }
    }

    @Nested
    @DisplayName("logout()")
    class Logout {

        @Test
        @DisplayName("Redis 에서 RT 삭제 — 키가 없어도 예외 없음")
        void logout_deletes_rt() {
            authService.logout(EMAIL);
            verify(refreshTokenRepository).delete(EMAIL);
        }
    }

    private User userFixture() {
        return User.builder()
                .id(UUID.randomUUID())
                .email(EMAIL)
                .password(HASHED_PASSWORD)
                .nickname("닉네임")
                .role(Role.ROLE_USER)
                .build();
    }
}
