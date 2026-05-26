package sqlquiz.domain.wrongnote.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/**
 * 메모는 빈 문자열이나 null 로 "지우기" 처리도 가능하다.
 * 따라서 @NotBlank 는 쓰지 않음 — @Size 만 적용.
 */
@Schema(description = "오답노트 메모 수정 요청")
public record WrongNoteMemoUpdateRequest(

        @Schema(description = "메모 (null/빈 문자열로 지우기 가능)", example = "Index Range Scan 조건 다시 정리할 것")
        @Size(max = 2000, message = "메모는 최대 2000자까지 입력할 수 있습니다.")
        String memo
) {
}
