package sqlquiz.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // 공통
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "잘못된 입력값입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "리소스를 찾을 수 없습니다."),
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "요청이 너무 잦습니다. 잠시 후 다시 시도해 주세요."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다."),

    // 회원
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "비밀번호가 올바르지 않습니다."),

    // JWT
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 토큰입니다."),
    TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "토큰이 존재하지 않습니다."),

    // 문제
    QUESTION_NOT_FOUND(HttpStatus.NOT_FOUND, "문제를 찾을 수 없습니다."),
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "카테고리를 찾을 수 없습니다."),
    INVALID_ANSWER(HttpStatus.BAD_REQUEST, "정답은 1~4 사이여야 합니다."),

    // 시험
    EXAM_NOT_FOUND(HttpStatus.NOT_FOUND, "시험 세션을 찾을 수 없습니다."),
    EXAM_ALREADY_COMPLETED(HttpStatus.BAD_REQUEST, "이미 완료된 시험입니다."),
    EXAM_ACCESS_DENIED(HttpStatus.FORBIDDEN, "본인의 시험 세션만 접근할 수 있습니다."),
    EXAM_NOT_COMPLETED(HttpStatus.BAD_REQUEST, "아직 완료되지 않은 시험입니다."),
    NOT_ENOUGH_QUESTIONS(HttpStatus.BAD_REQUEST, "조건에 맞는 문제가 부족합니다."),
    INVALID_QUESTION_FOR_EXAM(HttpStatus.BAD_REQUEST, "이 시험에 포함되지 않은 문제가 제출되었습니다."),

    // 오답노트
    WRONG_NOTE_NOT_FOUND(HttpStatus.NOT_FOUND, "오답노트를 찾을 수 없습니다."),
    WRONG_NOTE_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 오답노트에 등록된 문제입니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String message) {
        this.status = httpStatus;
        this.message = message;
    }
}
