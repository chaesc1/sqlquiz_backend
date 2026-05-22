package sqlquiz.global.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger / OpenAPI 설정.
 *
 * 컨트롤러의 {@code @SecurityRequirement(name = "BearerAuth")} 는 "이 엔드포인트는 BearerAuth 가 필요하다"는 선언일 뿐,
 * "BearerAuth" 라는 이름이 무엇인지(=어떻게 인증하는지)는 별도로 정의해야 한다.
 * 그 정의가 {@link SecurityScheme} — 이게 있어야 Swagger UI 우상단의 Authorize 버튼이 나타난다.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "SQLQuiz API",
                version = "v1",
                description = "SQLD/SQLP 문제은행 API 문서"
        )
)
@SecurityScheme(
        name = "BearerAuth",                // 컨트롤러의 @SecurityRequirement(name = ...) 와 일치해야 함
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class SwaggerConfig {
}
