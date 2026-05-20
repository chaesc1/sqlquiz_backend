package sqlquiz.domain.wrongnote.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sqlquiz.domain.wrongnote.entity.WrongNote;

import java.util.List;
import java.util.UUID;

public interface WrongNoteRepository extends JpaRepository<WrongNote, Long> {
    List<WrongNote> findByUserId(UUID userId);
    boolean existsByUserIdAndQuestionId(UUID userId, Long questionId);
}
