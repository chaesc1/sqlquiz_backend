package sqlquiz.domain.exam.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sqlquiz.domain.exam.entity.AttemptAnswer;

import java.util.List;
import java.util.UUID;

public interface AttemptAnswerRepository extends JpaRepository<AttemptAnswer, Long> {
    List<AttemptAnswer> findByAttemptId(UUID attemptId);
}
