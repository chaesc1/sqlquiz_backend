package sqlquiz.domain.user.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;

/**
 * Redis 기반 Refresh Token 저장소.
 * Key 포맷: "RT:{email}"  Value: refreshToken 문자열, TTL = refresh-expiration.
 *
 * Redis를 선택한 이유:
 * - Refresh Token은 TTL이 명확한 휘발성 데이터 → Redis의 expire 기능과 자연스럽게 맞물림
 * - DB 테이블/마이그레이션 부담 없이 "토큰 무효화(로그아웃)"를 한 줄(DEL)로 처리
 * - Rotation 시에도 SET 한 번이면 갱신 → 별도 update 로직 불필요
 */
@Repository
@RequiredArgsConstructor
public class RefreshTokenRepository {

    private static final String KEY_PREFIX = "RT:";

    private final StringRedisTemplate redisTemplate;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpirationMillis;

    /** 저장 (TTL = refresh-expiration). 기존 값이 있으면 덮어씀 → Rotation에 사용. */
    public void save(String email, String refreshToken) {
        redisTemplate.opsForValue().set(
                key(email),
                refreshToken,
                Duration.ofMillis(refreshExpirationMillis)
        );
    }

    /** 조회. 없거나 만료되었으면 Optional.empty(). */
    public Optional<String> find(String email) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(key(email)));
    }

    /** 삭제 (로그아웃). 키가 없어도 예외 없음. */
    public void delete(String email) {
        redisTemplate.delete(key(email));
    }

    private String key(String email) {
        return KEY_PREFIX + email;
    }
}
