package sqlquiz.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL) // null 필드는 JSON에서 제외
public class ApiResponse<T> {

    private final boolean success;
    private final String message;
    private final T data;

    private ApiResponse(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    // 성공 (데이터 있음)
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "요청이 완료되었습니다.", data);
    }

    // 성공 (데이터 없음)
    public static <T> ApiResponse<T> ok() {
        return new ApiResponse<>(true, "요청이 완료되었습니다.", null);
    }

    // 성공 (메시지 커스텀)
    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }

    // 실패
    public static <T> ApiResponse<T> fail(String message) {
        return new ApiResponse<>(false, message, null);
    }
}
