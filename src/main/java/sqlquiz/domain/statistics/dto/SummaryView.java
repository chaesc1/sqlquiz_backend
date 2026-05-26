package sqlquiz.domain.statistics.dto;

/**
 * 통계 요약 native query 결과를 매핑하는 Spring Data 인터페이스 프로젝션.
 *
 * 인터페이스 프로젝션을 선택한 이유:
 * - native query 의 컬럼 별칭 (totalAttempts 등) 만 맞으면 자동 매핑
 * - 별도 DTO 클래스에 생성자 매개변수를 정확히 맞추지 않아도 됨
 * - record 와 달리 nullable 필드도 wrapper 타입으로 깔끔하게 표현 가능
 */
public interface SummaryView {
    long getTotalAttempts();
    long getCompletedAttempts();
    Double getAverageScore();     // 시험 안 본 경우 NULL → wrapper 로 받음
    Long   getTotalQuestions();
    Long   getTotalCorrect();
}
