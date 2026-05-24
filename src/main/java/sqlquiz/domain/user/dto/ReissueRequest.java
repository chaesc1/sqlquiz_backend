package sqlquiz.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "토큰 재발급 요청")
public record ReissueRequest(

        @Schema(description = "현재 보유 중인 Refresh Token")
        @NotBlank(message = "Refresh Token은 필수입니다.")
        String refreshToken
) {
}
