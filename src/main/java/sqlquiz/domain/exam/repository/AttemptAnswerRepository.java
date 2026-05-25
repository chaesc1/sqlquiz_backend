package sqlquiz.domain.exam.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sqlquiz.domain.exam.entity.AttemptAnswer;

import java.util.List;
import java.util.UUID;

public interface AttemptAnswerRepository extends JpaRepository<AttemptAnswer, Long> {

    List<AttemptAnswer> findByAttemptId(UUID attemptId);

    /**
     * 시험 응답 변환 시 questions + category 까지 함께 페치.
     * JOIN FETCH 두 개를 동시에 쓰면 Hibernate 가 카르테시안 곱을 그리지만,
     * 시험당 N(≤50)개의 AttemptAnswer + 1개 Question + 1개 Category 라 영향 없음.
     */
    @Query("""
            SELECT aa
            FROM   AttemptAnswer aa
            JOIN   FETCH aa.question q
            JOIN   FETCH q.category
            WHERE  aa.attempt.id = :attemptId
            ORDER  BY aa.id
            """)
    List<AttemptAnswer> findByAttemptIdWithQuestion(@Param("attemptId") UUID attemptId);
}
