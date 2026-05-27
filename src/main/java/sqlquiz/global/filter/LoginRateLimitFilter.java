package sqlquiz.global.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import sqlquiz.global.exception.ErrorCode;
import sqlquiz.global.exception.ErrorResponse;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * `/api/v1/auth/login` 에 대한 IP 별 Rate Limit.
 *
 * 정책: 분당 10회 (toke-bucket, refill rate 10/min, capacity 10).
 *   - 짧은 burst (예: 사용자 오타로 5번 연속 입력) 는 허용
 *   - 지속적인 분당 10회 초과는 429 Too Many Requests 반환
 *
 * 저장소: 인메모리 ConcurrentHashMap.
 *   - 단일 인스턴스 한정. 클러스터 환경에서는 Redis backed Bucket4j 로 교체 필요.
 *   - 학습 단계에서는 인메모리로 충분.
 *
 * 적용 범위: login 만. signup/reissue 는 별도 정책이 필요하면 매처를 확장.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/api/v1/auth/login";
    private static final int CAPACITY = 10;
    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if (!isLoginRequest(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String ip = clientIp(request);
        Bucket bucket = buckets.computeIfAbsent(ip, this::newBucket);

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
            return;
        }

        log.warn("[RateLimit] /auth/login 차단: ip={}", ip);
        writeTooManyRequests(response);
    }

    private boolean isLoginRequest(HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod())
                && pathMatcher.match(LOGIN_PATH, request.getRequestURI());
    }

    private Bucket newBucket(String ip) {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(CAPACITY)
                        .refillIntervally(CAPACITY, WINDOW)
                        .build())
                .build();
    }

    /**
     * 클라이언트 IP 추출.
     * 운영에서 reverse proxy 뒤에 있으면 X-Forwarded-For 첫 항목을 사용.
     * 단, 프록시가 신뢰할 수 있을 때만 — 그렇지 않으면 헤더 위변조로 우회 가능.
     */
    private String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void writeTooManyRequests(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), new ErrorResponse(ErrorCode.TOO_MANY_REQUESTS));
    }
}
