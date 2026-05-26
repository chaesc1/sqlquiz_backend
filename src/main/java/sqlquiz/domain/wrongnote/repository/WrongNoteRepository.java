package sqlquiz.domain.wrongnote.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sqlquiz.domain.wrongnote.entity.WrongNote;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WrongNoteRepository extends JpaRepository<WrongNote, Long> {

    List<WrongNote> findByUserId(UUID userId);

    /** ExamService 의 자동 등록 흐름에서 중복 방지에 사용. */
    boolean existsByUserIdAndQuestionId(UUID userId, Long questionId);

    /**
     * 본인 노트 검색 — categoryId / isResolved 부분 필터 + 페이지네이션.
     * Question + Category 까지 fetch 해서 응답 변환 시 N+1 회피.
     */
    @EntityGraph(attributePaths = {"question", "question.category"})
    @Query("""
            SELECT w FROM WrongNote w
            WHERE w.user.id = :userId
              AND (:categoryId IS NULL OR w.question.category.id = :categoryId)
              AND (:isResolved IS NULL OR w.isResolved = :isResolved)
            """)
    Page<WrongNote> search(@Param("userId") UUID userId,
                           @Param("categoryId") Long categoryId,
                           @Param("isResolved") Boolean isResolved,
                           Pageable pageable);

    /** 본인 단건 페치 + question/category fetch. 권한 검증 후 도메인 메서드 호출에 사용. */
    @EntityGraph(attributePaths = {"user", "question", "question.category"})
    @Query("SELECT w FROM WrongNote w WHERE w.id = :id")
    Optional<WrongNote> findByIdWithDetails(@Param("id") Long id);
}
