package sqlquiz.domain.wrongnote.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sqlquiz.domain.question.entity.Question;
import sqlquiz.domain.question.repository.QuestionRepository;
import sqlquiz.domain.user.entity.User;
import sqlquiz.domain.user.repository.UserRepository;
import sqlquiz.domain.wrongnote.dto.WrongNoteCreateRequest;
import sqlquiz.domain.wrongnote.dto.WrongNoteMemoUpdateRequest;
import sqlquiz.domain.wrongnote.dto.WrongNoteResponse;
import sqlquiz.domain.wrongnote.dto.WrongNoteRetryRequest;
import sqlquiz.domain.wrongnote.dto.WrongNoteRetryResponse;
import sqlquiz.domain.wrongnote.entity.WrongNote;
import sqlquiz.domain.wrongnote.repository.WrongNoteRepository;
import sqlquiz.global.exception.CustomException;
import sqlquiz.global.exception.ErrorCode;

/**
 * 오답노트 도메인 서비스.
 *
 * 권한 정책: 모든 조작은 "본인의 노트만" — 노트 ID 가 다른 유저 것이면 404 처리.
 * (403 대신 404 를 선택한 이유: "이 노트가 누구 것인지" 정보 자체를 숨기는 게 보안상 권장)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WrongNoteService {

    private final WrongNoteRepository wrongNoteRepository;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;

    /** 본인 노트 검색 (페이지네이션 + 필터). */
    public Page<WrongNoteResponse> list(String email, Long categoryId, Boolean isResolved, Pageable pageable) {
        User user = getUser(email);
        return wrongNoteRepository.search(user.getId(), categoryId, isResolved, pageable)
                .map(WrongNoteResponse::from);
    }

    /**
     * 수동 등록.
     * - 같은 (user, question) 조합이 이미 있으면 409 — DB UNIQUE 제약과 별개로 사전 검증.
     * - 사전 검증을 둔 이유: DataIntegrityViolation 예외보다 도메인 에러 메시지가 명확.
     */
    @Transactional
    public Long create(String email, WrongNoteCreateRequest request) {
        User user = getUser(email);
        Question question = questionRepository.findById(request.questionId())
                .orElseThrow(() -> new CustomException(ErrorCode.QUESTION_NOT_FOUND));

        if (wrongNoteRepository.existsByUserIdAndQuestionId(user.getId(), question.getId())) {
            throw new CustomException(ErrorCode.WRONG_NOTE_ALREADY_EXISTS);
        }

        WrongNote saved = wrongNoteRepository.save(WrongNote.builder()
                .user(user)
                .question(question)
                .isResolved(false)
                .build());
        log.info("[WrongNote] 수동 등록: id={}, user={}, qId={}",
                saved.getId(), email, question.getId());
        return saved.getId();
    }

    /** 메모 수정. null/빈 문자열은 "메모 지우기"로 해석. */
    @Transactional
    public void updateMemo(String email, Long id, WrongNoteMemoUpdateRequest request) {
        WrongNote note = loadAndCheckOwnership(id, email);
        note.updateMemo(request.memo());
        log.info("[WrongNote] 메모 수정: id={}", id);
    }

    /** 해결 표시 (멱등 — 이미 해결된 상태에 다시 호출해도 무관). */
    @Transactional
    public void resolve(String email, Long id) {
        WrongNote note = loadAndCheckOwnership(id, email);
        note.resolve();
        log.info("[WrongNote] 해결 처리: id={}", id);
    }

    /**
     * 다시 풀기 — 오답노트 카드에서 정답/해설을 가린 채 한 번 더 풀어볼 때 호출.
     * - 정답이면 isResolved=true 자동 전환 + selected_option 도 정답 값으로 갱신 (복기 상태 일관성).
     * - 오답이면 selected_option 만 새 선택값으로 덮어쓰기.
     * 응답에는 정답·해설을 포함해 화면에서 즉시 노출.
     */
    @Transactional
    public WrongNoteRetryResponse retry(String email, Long id, WrongNoteRetryRequest request) {
        WrongNote note = loadAndCheckOwnership(id, email);
        Question question = note.getQuestion();
        Integer selected = request.selectedOption();
        boolean correct = selected.equals(question.getAnswer());

        note.updateSelectedOption(selected);
        if (correct) {
            note.resolve();
        }

        log.info("[WrongNote] 다시 풀기: id={}, selected={}, correct={}", id, selected, correct);
        return new WrongNoteRetryResponse(
                correct,
                question.getAnswer(),
                question.getExplanation(),
                note.getIsResolved(),
                selected
        );
    }

    /** 삭제. */
    @Transactional
    public void delete(String email, Long id) {
        WrongNote note = loadAndCheckOwnership(id, email);
        wrongNoteRepository.delete(note);
        log.info("[WrongNote] 삭제: id={}", id);
    }

    // ────────────────────────────────────────────────────────────────────────
    // 내부 헬퍼
    // ────────────────────────────────────────────────────────────────────────

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    /**
     * 본인 노트인지 확인하며 페치. 다른 사람 노트 ID 를 시도하면 404 로 응답.
     * (FORBIDDEN 대신 NOT_FOUND 를 선택한 이유: 노트 ID 공간을 enumerable 하게 만들지 않음)
     */
    private WrongNote loadAndCheckOwnership(Long id, String email) {
        WrongNote note = wrongNoteRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new CustomException(ErrorCode.WRONG_NOTE_NOT_FOUND));
        if (!note.getUser().getEmail().equals(email)) {
            throw new CustomException(ErrorCode.WRONG_NOTE_NOT_FOUND);
        }
        return note;
    }
}
