package sqlquiz.domain.exam.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sqlquiz.domain.exam.entity.Attempt;

import java.util.List;
import java.util.UUID;

public interface AttemptRepository extends JpaRepository<Attempt, UUID> {
    List<Attempt> findByUserIdOrderByStartedAtDesc(UUID userId);
}