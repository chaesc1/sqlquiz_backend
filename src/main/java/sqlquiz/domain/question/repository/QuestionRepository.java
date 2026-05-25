package sqlquiz.domain.question.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sqlquiz.domain.common.Difficulty;
import sqlquiz.domain.common.ExamType;
import sqlquiz.domain.question.entity.Question;

import java.util.List;
import java.util.Optional;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    @EntityGraph(attributePaths = "category")
    @Query("""
            SELECT q FROM Question q
            WHERE (:examType   IS NULL OR q.category.examType = :examType)
              AND (:categoryId IS NULL OR q.category.id       = :categoryId)
              AND (:difficulty IS NULL OR q.difficulty        = :difficulty)
            """)
    Page<Question> search(@Param("examType") ExamType examType,
                          @Param("categoryId") Long categoryId,
                          @Param("difficulty") Difficulty difficulty,
                          Pageable pageable);

    @EntityGraph(attributePaths = "category")
    @Query("SELECT q FROM Question q WHERE q.id = :id")
    Optional<Question> findByIdWithCategory(@Param("id") Long id);

    /**
     * 무작위 N개의 문제 ID 추출 (PostgreSQL 의 random() 사용).
     *
     * 왜 두 단계로 나눴는가:
     * - 원래는 `ORDER BY random()` + JOIN FETCH 로 한 번에 끝내고 싶지만,
     *   JPQL 의 fetch join + LIMIT 조합은 결과가 비결정적/Hibernate 경고 발생.
     * - 일단 ID 만 native 로 뽑고 → 별도로 fetch join 하면 명확하고 안전.
     * - 학습용 데이터 규모(<수만)에서는 `ORDER BY random()` 비용이 무시할 수 있음.
     */
    @Query(value = """
            SELECT q.id
            FROM   questions q
            JOIN   categories c ON c.id = q.category_id
            WHERE  c.exam_type = :examType
              AND  (:difficulty IS NULL OR q.difficulty = :difficulty)
            ORDER  BY random()
            LIMIT  :limit
            """, nativeQuery = true)
    List<Long> findRandomIdsByExamType(@Param("examType") String examType,
                                       @Param("difficulty") String difficulty,
                                       @Param("limit") int limit);

    /** IN 절에 ID를 묶어 fetch join 으로 한 번에 페치. 호출자 쪽에서 순서를 보존하고 싶다면 별도 정렬 필요. */
    @Query("SELECT q FROM Question q JOIN FETCH q.category WHERE q.id IN :ids")
    List<Question> findAllByIdInWithCategory(@Param("ids") List<Long> ids);

    /** 카테고리별 정답 통계용 (시험 결과). attemptAnswers 의 group by 와 함께 사용. */
    List<Question> findByCategoryIdAndDifficulty(Long categoryId, Difficulty difficulty);
}
