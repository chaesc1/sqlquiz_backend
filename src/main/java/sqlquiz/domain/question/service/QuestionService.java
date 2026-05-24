package sqlquiz.domain.question.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sqlquiz.domain.question.dto.*;
import sqlquiz.domain.question.entity.Category;
import sqlquiz.domain.question.entity.Question;
import sqlquiz.domain.question.repository.QuestionRepository;
import sqlquiz.global.exception.CustomException;
import sqlquiz.global.exception.ErrorCode;

/**
 * 문제 도메인 서비스.
 *
 * 트랜잭션 전략은 Auth와 동일: 기본 readOnly, 쓰기만 @Transactional 명시.
 * 변경 감지(Dirty Checking)를 이용해 update 시 별도 save() 호출 없이 영속성 컨텍스트 flush로 반영.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final CategoryService categoryService;

    /** 목록 조회 (필터 + 페이지). 정답/해설은 응답에서 제외 (QuestionResponse). */
    public Page<QuestionResponse> search(QuestionSearchCondition cond, Pageable pageable) {
        return questionRepository.search(
                cond.examType(), cond.categoryId(), cond.difficulty(), pageable
        ).map(QuestionResponse::from);
    }

    /** 상세 조회 (정답/해설 포함). 학습 단계 단순화 결정 (Detail DTO Javadoc 참고). */
    public QuestionDetailResponse getDetail(Long id) {
        Question q = questionRepository.findByIdWithCategory(id)
                .orElseThrow(() -> new CustomException(ErrorCode.QUESTION_NOT_FOUND));
        return QuestionDetailResponse.from(q);
    }

    /** 등록 (ADMIN 전용). 권한 검사는 Controller @PreAuthorize가 담당. */
    @Transactional
    public Long create(QuestionCreateRequest req) {
        Category category = categoryService.getOrThrow(req.categoryId());

        Question saved = questionRepository.save(
                Question.builder()
                        .category(category)
                        .content(req.content())
                        .option1(req.option1())
                        .option2(req.option2())
                        .option3(req.option3())
                        .option4(req.option4())
                        .answer(req.answer())
                        .explanation(req.explanation())
                        .difficulty(req.difficulty())
                        .build()
        );
        log.info("[Question] 등록: id={}, categoryId={}", saved.getId(), category.getId());
        return saved.getId();
    }

    /**
     * 수정 (ADMIN 전용).
     *
     * 영속 상태 엔티티에 update()를 호출만 하고 별도 save 호출은 하지 않음 — Dirty Checking으로 flush 시점에 반영.
     * 학습 메모: 만약 영속 컨텍스트 밖에서 받은 detached 엔티티라면 merge가 필요하나, 본 메서드는 같은 트랜잭션에서 조회+수정이라 OK.
     */
    @Transactional
    public void update(Long id, QuestionUpdateRequest req) {
        Question q = questionRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.QUESTION_NOT_FOUND));
        Category category = categoryService.getOrThrow(req.categoryId());

        q.update(
                category,
                req.content(),
                req.option1(), req.option2(), req.option3(), req.option4(),
                req.answer(),
                req.explanation(),
                req.difficulty()
        );
        log.info("[Question] 수정: id={}", id);
    }

    /** 삭제 (ADMIN 전용). FK 제약(attempt_answers/wrong_notes)에 걸리면 DB 예외 → GlobalExceptionHandler 500. */
    @Transactional
    public void delete(Long id) {
        if (!questionRepository.existsById(id)) {
            throw new CustomException(ErrorCode.QUESTION_NOT_FOUND);
        }
        questionRepository.deleteById(id);
        log.info("[Question] 삭제: id={}", id);
    }
}
