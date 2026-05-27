package sqlquiz.domain.statistics.dto;

/** 카테고리별 정답률 native query 프로젝션. */
public interface CategoryStatView {
    Long getCategoryId();
    String getCategoryName();
    String getExamType();        // PostgreSQL VARCHAR → String. 서비스 레이어에서 Enum 변환
    long getTotalAttempted();
    long getTotalCorrect();
    double getAccuracy();        // 쿼리에서 미리 계산해 반환
}
