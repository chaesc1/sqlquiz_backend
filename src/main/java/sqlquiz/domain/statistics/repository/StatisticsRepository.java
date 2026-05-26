package sqlquiz.domain.statistics.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import sqlquiz.domain.exam.entity.Attempt;
import sqlquiz.domain.statistics.dto.CategoryStatView;
import sqlquiz.domain.statistics.dto.SummaryView;

import java.util.List;
import java.util.UUID;

/**
 * 통계 전용 Repository. Attempt 도메인의 CRUD 와 분리해 두기 위해
 * AttemptRepository 에 합치지 않고 별도 인터페이스로 둔다.
 *
 * 통계는 본질적으로 별도의 "View" 라서 Repository 도 별도로 두는 게 명확.
 * 본 Repository 는 CRUD 가 필요 없으므로 JpaRepository 가 아닌 최소 Repository<Attempt, UUID> 만 상속.
 */
public interface StatisticsRepository extends Repository<Attempt, UUID> {

    /**
     * 본인 요약 통계 한 줄.
     * PostgreSQL 의 FILTER (WHERE ...) 절을 사용해 단일 스캔으로 4가지 집계 동시 수행.
     */
    @Query(value = """
            SELECT
                COUNT(*)                                                     AS totalAttempts,
                COUNT(*) FILTER (WHERE a.status = 'COMPLETED')               AS completedAttempts,
                AVG(a.score) FILTER (WHERE a.status = 'COMPLETED')           AS averageScore,
                COALESCE(SUM(a.total_count)   FILTER (WHERE a.status = 'COMPLETED'), 0) AS totalQuestions,
                COALESCE(SUM(a.correct_count) FILTER (WHERE a.status = 'COMPLETED'), 0) AS totalCorrect
            FROM   attempts a
            WHERE  a.user_id = :userId
            """, nativeQuery = true)
    SummaryView findSummaryByUserId(@Param("userId") UUID userId);

    /**
     * 카테고리별 정답률 (전체 또는 Top N — limit 으로 제어).
     * accuracy 는 0~100 스케일.
     */
    @Query(value = """
            SELECT
                c.id           AS categoryId,
                c.name         AS categoryName,
                c.exam_type    AS examType,
                COUNT(*)       AS totalAttempted,
                COUNT(*) FILTER (WHERE aa.is_correct) AS totalCorrect,
                CASE WHEN COUNT(*) = 0 THEN 0.0
                     ELSE 100.0 * COUNT(*) FILTER (WHERE aa.is_correct) / COUNT(*)
                END            AS accuracy
            FROM   attempt_answers aa
            JOIN   attempts  a ON a.id = aa.attempt_id
            JOIN   questions q ON q.id = aa.question_id
            JOIN   categories c ON c.id = q.category_id
            WHERE  a.user_id = :userId
              AND  a.status  = 'COMPLETED'
            GROUP  BY c.id, c.name, c.exam_type
            ORDER  BY c.exam_type, c.id
            """, nativeQuery = true)
    List<CategoryStatView> findCategoryStatsByUserId(@Param("userId") UUID userId);

    /**
     * 정답률 낮은 카테고리 Top N. categories 쿼리와 유사하지만 ORDER BY + LIMIT 만 다름.
     * "최소 풀이 수" 임계값을 두지 않으면 1번 풀고 0/1 인 카테고리가 항상 1위가 되어 의미가 떨어짐.
     * 학습용으로는 임계값 없이 두고, 운영 단계에서 HAVING COUNT(*) >= 3 같은 보강 권장.
     */
    @Query(value = """
            SELECT
                c.id           AS categoryId,
                c.name         AS categoryName,
                c.exam_type    AS examType,
                COUNT(*)       AS totalAttempted,
                COUNT(*) FILTER (WHERE aa.is_correct) AS totalCorrect,
                100.0 * COUNT(*) FILTER (WHERE aa.is_correct) / COUNT(*) AS accuracy
            FROM   attempt_answers aa
            JOIN   attempts  a ON a.id = aa.attempt_id
            JOIN   questions q ON q.id = aa.question_id
            JOIN   categories c ON c.id = q.category_id
            WHERE  a.user_id = :userId
              AND  a.status  = 'COMPLETED'
            GROUP  BY c.id, c.name, c.exam_type
            ORDER  BY accuracy ASC, totalAttempted DESC
            LIMIT  :limit
            """, nativeQuery = true)
    List<CategoryStatView> findWeakPointsByUserId(@Param("userId") UUID userId,
                                                  @Param("limit") int limit);

    /** 응시 이력 (최신순). Pageable 로 ?page/?size/?sort 지원. */
    @Query("""
            SELECT a FROM Attempt a
            WHERE  a.user.id = :userId
            ORDER  BY a.startedAt DESC
            """)
    Page<Attempt> findHistoryByUserId(@Param("userId") UUID userId, Pageable pageable);
}
