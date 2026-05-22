package sqlquiz.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "JWT 토큰 응답")
public record TokenResponse(

        @Schema(description = "토큰 타입", example = "Bearer")
        String tokenType,

        @Schema(description = "Access Token (15분)")
        String accessToken,

        @Schema(description = "Refresh Token (7일)")
        String refreshToken
) {
    public static TokenResponse of(String accessToken, String refreshToken) {
        return new TokenResponse("Bearer", accessToken, refreshToken);
    }
}
