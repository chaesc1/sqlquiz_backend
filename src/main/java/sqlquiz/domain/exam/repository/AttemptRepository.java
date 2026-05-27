package sqlquiz.domain.exam.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sqlquiz.domain.exam.entity.Attempt;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AttemptRepository extends JpaRepository<Attempt, UUID> {

    List<Attempt> findByUserIdOrderByStartedAtDesc(UUID userId);

    /**
     * 본인 권한 검증 / 응답 변환에 user 정보가 필요한 경로용.
     * EntityGraph 로 user 를 함께 fetch.
     */
    @EntityGraph(attributePaths = "user")
    @Query("SELECT a FROM Attempt a WHERE a.id = :id")
    Optional<Attempt> findByIdWithUser(@Param("id") UUID id);
}
