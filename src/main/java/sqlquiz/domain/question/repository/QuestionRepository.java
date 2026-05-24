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

    /**
     * 동적 필터 검색.
     *
     * 설계 선택:
     * - "{@code :param IS NULL OR ... = :param}" 패턴으로 nullable 필터를 한 JPQL에 표현.
     *   Specification/QueryDSL을 안 쓰는 이유 → 학습자가 SQL 흐름을 그대로 따라가기 쉬움.
     * - {@link EntityGraph}로 category를 함께 fetch → 응답 변환 시 N+1 방지.
     * - 정렬은 Pageable 의 sort 파라미터로 클라이언트가 결정 (?sort=id,desc).
     */
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

    /**
     * 단건 조회 시도 category 함께 fetch → 응답 변환 시 추가 쿼리 방지.
     * findById를 그대로 쓰면 LAZY 로딩이 트리거되어 1+1 발생.
     */
    @EntityGraph(attributePaths = "category")
    @Query("SELECT q FROM Question q WHERE q.id = :id")
    Optional<Question> findByIdWithCategory(@Param("id") Long id);

    // === 기존 시험/오답노트 도메인에서 쓰던 메서드는 유지 ===

    @Query("SELECT q FROM Question q WHERE q.category.examType = :examType ORDER BY FUNCTION('RAND') ")
    List<Question> findRandomByExamType(@Param("examType") ExamType examType, Pageable pageable);

    List<Question> findByCategoryIdAndDifficulty(Long categoryId, Difficulty difficulty);
}
