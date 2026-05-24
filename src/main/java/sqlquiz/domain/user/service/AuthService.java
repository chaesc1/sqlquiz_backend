package sqlquiz.domain.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

/**
 * 인증/인가 도메인 서비스.
 *
 * 트랜잭션 전략:
 * - 클래스 레벨에 readOnly = true → 기본은 읽기 전용 (성능/안전성)
 * - 쓰기 메서드(signup, login의 토큰 저장은 Redis라서 트랜잭션 영향 없음)에만 @Transactional 명시
 *   참고: Redis는 Spring JPA 트랜잭션에 자동 참여하지 않으므로 RT 저장/삭제는 트랜잭션 외부 동작
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    /**
     * 회원가입.
     * - 이메일 중복 시 409 (EMAIL_ALREADY_EXISTS)
     * - 비밀번호는 절대 평문 저장하지 않고 BCrypt 해시로 변환
     * - 기본 권한은 ROLE_USER (ADMIN은 DB 직접 변경 또는 관리자 콘솔에서만)
     */
    @Transactional
    public void signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new CustomException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        User user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .nickname(request.nickname())
                .role(Role.ROLE_USER)
                .build();

        userRepository.save(user);
        log.info("[Auth] 회원가입 완료: {}", request.email());
    }

    /**
     * 로그인.
     * - 이메일/비밀번호 검증 후 Access/Refresh Token 발급
     * - 보안 메모: 사용자에게 "이메일 없음"과 "비밀번호 틀림"을 동일 메시지로 응답하면
     *   계정 존재 여부가 노출되지 않음. 다만 학습용이라 명확한 분리를 유지함
     *   (운영 단계에서는 INVALID_CREDENTIALS 하나로 합치는 것을 권장)
     * - Refresh Token은 Redis에 저장 → 서버 측 무효화/검증 가능
     */
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }

        String accessToken = jwtUtil.generateAccessToken(user.getEmail(), user.getRole().name());
        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());

        refreshTokenRepository.save(user.getEmail(), refreshToken);
        log.info("[Auth] 로그인 완료: {}", user.getEmail());

        return TokenResponse.of(accessToken, refreshToken);
    }

    /**
     * 토큰 재발급 (Refresh Token Rotation 적용).
     * 절차:
     *  1) RT 자체 서명/만료 검증
     *  2) RT에 담긴 email 추출
     *  3) Redis 저장본과 일치 여부 검증 → 일치하지 않으면 탈취 의심으로 간주, 401
     *  4) 새 AT/RT 발급 → 새 RT를 Redis에 덮어쓰기 (Rotation)
     *
     * 왜 Rotation인가:
     * - 동일 RT를 계속 쓰면 탈취 시 무한 갱신 가능
     * - 매번 새 RT로 교체하면, 탈취자가 한 번 갱신한 순간 정상 사용자의 RT는 무효화됨
     */
    public TokenResponse reissue(ReissueRequest request) {
        jwtUtil.validateToken(request.refreshToken());

        String email = jwtUtil.getEmail(request.refreshToken());

        String savedToken = refreshTokenRepository.find(email)
                .orElseThrow(() -> new CustomException(ErrorCode.TOKEN_NOT_FOUND));

        if (!savedToken.equals(request.refreshToken())) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        String newAccessToken = jwtUtil.generateAccessToken(email, user.getRole().name());
        String newRefreshToken = jwtUtil.generateRefreshToken(email);

        refreshTokenRepository.save(email, newRefreshToken);
        log.info("[Auth] 토큰 재발급 완료: {}", email);

        return TokenResponse.of(newAccessToken, newRefreshToken);
    }

    /**
     * 로그아웃.
     * - 서버는 Refresh Token만 무효화 (Access Token은 stateless → 만료까지 유효)
     * - 운영 단계에서 즉시 차단이 필요하면 Access Token 블랙리스트(Redis) 추가 도입 검토
     */
    public void logout(String email) {
        refreshTokenRepository.delete(email);
        log.info("[Auth] 로그아웃 완료: {}", email);
    }
}
