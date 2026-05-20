package sqlquiz.domain.question.repository;

import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import sqlquiz.domain.common.Difficulty;
import sqlquiz.domain.common.ExamType;
import sqlquiz.domain.question.entity.Question;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    @Query("SELECT q FROM Question q WHERE q.category.examType = :examType ORDER BY FUNCTION('RAND') ")
    List<Question> findRandomByExamType(@Param("examType") ExamType examType, Pageable pageable);

    List<Question> findByCategoryIdAndDifficulty(Long categoryId, Difficulty difficulty);
}
