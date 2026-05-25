package sqlquiz.domain.exam.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sqlquiz.domain.common.AttemptStatus;
import sqlquiz.domain.exam.dto.*;
import sqlquiz.domain.exam.entity.Attempt;
import sqlquiz.domain.exam.entity.AttemptAnswer;
import sqlquiz.domain.exam.repository.AttemptAnswerRepository;
import sqlquiz.domain.exam.repository.AttemptRepository;
import sqlquiz.domain.question.entity.Question;
import sqlquiz.domain.question.repository.QuestionRepository;
import sqlquiz.domain.user.entity.User;
import sqlquiz.domain.user.repository.UserRepository;
import sqlquiz.domain.wrongnote.entity.WrongNote;
import sqlquiz.domain.wrongnote.repository.WrongNoteRepository;
import sqlquiz.global.exception.CustomException;
import sqlquiz.global.exception.ErrorCode;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 시험 도메인 서비스.
 *
 * 설계 결정:
 * 1) 시험 시작 시점에 AttemptAnswer 행을 N개 사전 생성한다.
 *    → "이 시험에 포함된 문제 집합"이 DB 에 못 박혀, 이어풀기/제출 시 후처리가 단순.
 *    → selectedOption 은 null, isCorrect 는 false (placeholder) 로 시작.
 * 2) 제출 시 도메인 메서드 (Attempt.complete, AttemptAnswer.updateSubmission) 만 호출 → setter 노출 없음.
 * 3) 오답에 대해 WrongNote 자동 생성 — 이미 존재하면 중복 안 만듦 (existsBy.. 활용).
 * 4) 모든 진입점에서 본인 권한 검증 (IDOR 방어).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExamService {

    private final AttemptRepository attemptRepository;
    private final AttemptAnswerRepository attemptAnswerRepository;
    private final QuestionRepository questionRepository;
    private final WrongNoteRepository wrongNoteRepository;
    private final UserRepository userRepository;

    /** 시험 시작: 무작위 N문제 추출 → Attempt + AttemptAnswer N개 사전 생성. */
    @Transactional
    public ExamSessionResponse start(String email, ExamStartRequest req) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        List<Long> ids = questionRepository.findRandomIdsByExamType(
                req.examType().name(),
                req.difficulty() != null ? req.difficulty().name() : null,
                req.count()
        );
        if (ids.size() < req.count()) {
            throw new CustomException(ErrorCode.NOT_ENOUGH_QUESTIONS);
        }

        // ID 리스트 순서대로 페치 (findAllByIdIn 은 순서 보장 X → ID 기준 정렬 후 사용)
        List<Question> fetched = questionRepository.findAllByIdInWithCategory(ids);
        Map<Long, Question> byId = fetched.stream()
                .collect(Collectors.toMap(Question::getId, Function.identity()));
        List<Question> ordered = ids.stream().map(byId::get).toList();

        Attempt attempt = attemptRepository.save(Attempt.builder()
                .user(user)
                .examType(req.examType())
                .status(AttemptStatus.IN_PROGRESS)
                .totalCount(ordered.size())
                .startedAt(LocalDateTime.now())
                .build());

        List<AttemptAnswer> answers = ordered.stream()
                .map(q -> AttemptAnswer.builder()
                        .attempt(attempt)
                        .question(q)
                        .selectedOption(null)
                        .isCorrect(false)
                        .build())
                .toList();
        attemptAnswerRepository.saveAll(answers);

        log.info("[Exam] 시작: attemptId={}, user={}, count={}", attempt.getId(), email, ordered.size());
        return toSessionResponse(attempt, answers);
    }

    /** 세션 조회 (이어풀기 또는 완료 후 응답지 확인). */
    public ExamSessionResponse resume(String email, UUID attemptId) {
        Attempt attempt = loadAndCheckOwnership(attemptId, email);
        List<AttemptAnswer> answers = attemptAnswerRepository.findByAttemptIdWithQuestion(attemptId);
        return toSessionResponse(attempt, answers);
    }

    /** 답안 일괄 제출 + 자동 채점 + WrongNote 생성. */
    @Transactional
    public ExamResultResponse submit(String email, UUID attemptId, ExamSubmitRequest req) {
        Attempt attempt = loadAndCheckOwnership(attemptId, email);
        if (attempt.getStatus() == AttemptStatus.COMPLETED) {
            throw new CustomException(ErrorCode.EXAM_ALREADY_COMPLETED);
        }

        List<AttemptAnswer> answers = attemptAnswerRepository.findByAttemptIdWithQuestion(attemptId);
        Map<Long, AttemptAnswer> answerByQid = answers.stream()
                .collect(Collectors.toMap(a -> a.getQuestion().getId(), Function.identity()));

        for (AnswerSubmission sub : req.answers()) {
            AttemptAnswer aa = answerByQid.get(sub.questionId());
            if (aa == null) {
                throw new CustomException(ErrorCode.INVALID_QUESTION_FOR_EXAM);
            }
            boolean correct = sub.selectedOption() != null
                    && sub.selectedOption().equals(aa.getQuestion().getAnswer());
            aa.updateSubmission(sub.selectedOption(), correct);
        }

        int correctCount = (int) answers.stream()
                .filter(a -> Boolean.TRUE.equals(a.getIsCorrect()))
                .count();
        int score = (int) Math.round(100.0 * correctCount / answers.size());
        attempt.complete(correctCount, score);

        autoCreateWrongNotes(attempt.getUser(), answers);

        log.info("[Exam] 제출 완료: attemptId={}, score={}/{} ({}점)",
                attempt.getId(), correctCount, answers.size(), score);
        return buildResult(attempt, answers);
    }

    /** 채점 결과 조회. */
    public ExamResultResponse result(String email, UUID attemptId) {
        Attempt attempt = loadAndCheckOwnership(attemptId, email);
        if (attempt.getStatus() != AttemptStatus.COMPLETED) {
            throw new CustomException(ErrorCode.EXAM_NOT_COMPLETED);
        }
        List<AttemptAnswer> answers = attemptAnswerRepository.findByAttemptIdWithQuestion(attemptId);
        return buildResult(attempt, answers);
    }

    // ────────────────────────────────────────────────────────────────────────
    // 내부 헬퍼
    // ────────────────────────────────────────────────────────────────────────

    private Attempt loadAndCheckOwnership(UUID attemptId, String email) {
        Attempt attempt = attemptRepository.findByIdWithUser(attemptId)
                .orElseThrow(() -> new CustomException(ErrorCode.EXAM_NOT_FOUND));
        if (!attempt.getUser().getEmail().equals(email)) {
            throw new CustomException(ErrorCode.EXAM_ACCESS_DENIED);
        }
        return attempt;
    }

    /**
     * 오답에 대해 WrongNote 자동 등록. 이미 등록된 문제는 건너뜀.
     * "오답"의 정의는 isCorrect=false — 미선택(selectedOption=null)도 포함.
     */
    private void autoCreateWrongNotes(User user, List<AttemptAnswer> answers) {
        for (AttemptAnswer a : answers) {
            if (Boolean.TRUE.equals(a.getIsCorrect())) continue;

            Long qId = a.getQuestion().getId();
            if (wrongNoteRepository.existsByUserIdAndQuestionId(user.getId(), qId)) continue;

            wrongNoteRepository.save(WrongNote.builder()
                    .user(user)
                    .question(a.getQuestion())
                    .isResolved(false)
                    .build());
        }
    }

    private ExamSessionResponse toSessionResponse(Attempt attempt, List<AttemptAnswer> answers) {
        return new ExamSessionResponse(
                attempt.getId(),
                attempt.getExamType(),
                attempt.getStatus(),
                attempt.getTotalCount(),
                attempt.getStartedAt(),
                answers.stream().map(QuestionInExam::from).toList()
        );
    }

    private ExamResultResponse buildResult(Attempt attempt, List<AttemptAnswer> answers) {
        // 카테고리별 [total, correct] 집계
        Map<String, long[]> byCategory = new LinkedHashMap<>();
        for (AttemptAnswer a : answers) {
            String cat = a.getQuestion().getCategory().getName();
            long[] stats = byCategory.computeIfAbsent(cat, k -> new long[]{0, 0});
            stats[0]++;
            if (Boolean.TRUE.equals(a.getIsCorrect())) stats[1]++;
        }
        List<CategoryScore> categoryStats = byCategory.entrySet().stream()
                .map(e -> new CategoryScore(e.getKey(), e.getValue()[0], e.getValue()[1]))
                .toList();

        return new ExamResultResponse(
                attempt.getId(),
                attempt.getExamType(),
                attempt.getCorrectCount(),
                attempt.getTotalCount(),
                attempt.getScore(),
                attempt.getCompletedAt(),
                categoryStats
        );
    }
}
