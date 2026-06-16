package sqlquiz.domain.exam.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sqlquiz.domain.common.AttemptStatus;
import sqlquiz.domain.common.Difficulty;
import sqlquiz.domain.common.ExamType;
import sqlquiz.domain.common.Role;
import sqlquiz.domain.exam.dto.AnswerSubmission;
import sqlquiz.domain.exam.dto.ExamResultResponse;
import sqlquiz.domain.exam.dto.ExamStartRequest;
import sqlquiz.domain.exam.dto.ExamSubmitRequest;
import sqlquiz.domain.exam.dto.QuestionResult;
import sqlquiz.domain.exam.entity.Attempt;
import sqlquiz.domain.exam.entity.AttemptAnswer;
import sqlquiz.domain.wrongnote.entity.WrongNote;
import sqlquiz.domain.exam.repository.AttemptAnswerRepository;
import sqlquiz.domain.exam.repository.AttemptRepository;
import sqlquiz.domain.question.entity.Category;
import sqlquiz.domain.question.entity.Question;
import sqlquiz.domain.question.repository.QuestionRepository;
import sqlquiz.domain.user.entity.User;
import sqlquiz.domain.user.repository.UserRepository;
import sqlquiz.domain.wrongnote.repository.WrongNoteRepository;
import sqlquiz.global.exception.CustomException;
import sqlquiz.global.exception.ErrorCode;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExamServiceTest {

    @Mock AttemptRepository attemptRepository;
    @Mock AttemptAnswerRepository attemptAnswerRepository;
    @Mock QuestionRepository questionRepository;
    @Mock WrongNoteRepository wrongNoteRepository;
    @Mock UserRepository userRepository;

    @InjectMocks ExamService examService;

    private static final String EMAIL = "user@example.com";
    private User user;
    private Category category;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(UUID.randomUUID())
                .email(EMAIL)
                .password("hash")
                .nickname("u")\
                .build();
        category = Category.builder().id(1L).name("SQL 기본").examType(ExamType.SQLD).build();
    }

    // ────────────────────────────────────────────────────────────────────────
    // start()
    // ────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("start()")
    class Start {

        @Test
        @DisplayName("요청 수만큼 문제가 추출되면 Attempt + AttemptAnswer 사전 생성")
        void start_success() {
            List<Long> ids = List.of(10L, 20L, 30L);
            List<Question> questions = ids.stream().map(id -> questionFixture(id, 1)).toList();
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(questionRepository.findRandomIdsByExamType("SQLD", null, 3)).thenReturn(ids);
            when(questionRepository.findAllByIdInWithCategory(ids)).thenReturn(questions);
            when(attemptRepository.save(any(Attempt.class))).thenAnswer(inv -> inv.getArgument(0));

            examService.start(EMAIL, new ExamStartRequest(ExamType.SQLD, 3, null));

            verify(attemptRepository).save(argThat(a ->
                    a.getStatus() == AttemptStatus.IN_PROGRESS
                            && a.getTotalCount() == 3
                            && a.getUser() == user));
            verify(attemptAnswerRepository).saveAll(argThat((List<AttemptAnswer> list) ->
                    list.size() == 3
                            && list.stream().allMatch(aa -> aa.getSelectedOption() == null)
                            && list.stream().allMatch(aa -> !aa.getIsCorrect())));
        }

        @Test
        @DisplayName("문제가 부족하면 NOT_ENOUGH_QUESTIONS")
        void start_not_enough_questions() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(questionRepository.findRandomIdsByExamType("SQLD", null, 10))
                    .thenReturn(List.of(1L, 2L));

            assertThatThrownBy(() ->
                    examService.start(EMAIL, new ExamStartRequest(ExamType.SQLD, 10, null)))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.NOT_ENOUGH_QUESTIONS);

            verify(attemptRepository, never()).save(any());
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // submit()
    // ────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("submit()")
    class Submit {

        @Test
        @DisplayName("정답률에 비례한 점수를 계산하고 COMPLETED 로 전환")
        void submit_calculates_score() {
            Attempt attempt = attemptFixture(AttemptStatus.IN_PROGRESS, 4);
            // 정답이 q.answer 인 4 문제 — 3개 정답 / 1개 오답으로 제출
            Question q1 = questionFixture(1L, 1);
            Question q2 = questionFixture(2L, 2);
            Question q3 = questionFixture(3L, 3);
            Question q4 = questionFixture(4L, 4);
            List<AttemptAnswer> preCreated = new ArrayList<>(List.of(
                    answerFixture(attempt, q1),
                    answerFixture(attempt, q2),
                    answerFixture(attempt, q3),
                    answerFixture(attempt, q4)
            ));
            when(attemptRepository.findByIdWithUser(attempt.getId())).thenReturn(Optional.of(attempt));
            when(attemptAnswerRepository.findByAttemptIdWithQuestion(attempt.getId())).thenReturn(preCreated);

            ExamSubmitRequest req = new ExamSubmitRequest(List.of(
                    new AnswerSubmission(1L, 1),   // 정답
                    new AnswerSubmission(2L, 2),   // 정답
                    new AnswerSubmission(3L, 3),   // 정답
                    new AnswerSubmission(4L, 1)    // 오답
            ));

            ExamResultResponse result = examService.submit(EMAIL, attempt.getId(), req);

            assertThat(result.correctCount()).isEqualTo(3);
            assertThat(result.totalCount()).isEqualTo(4);
            assertThat(result.score()).isEqualTo(75);   // 3/4 * 100
            assertThat(attempt.getStatus()).isEqualTo(AttemptStatus.COMPLETED);
            // 오답 1문제(q4)에 대해서만 WrongNote 분기 — 기존 노트 조회 1회.
            verify(wrongNoteRepository, times(1)).findByUserIdAndQuestionId(user.getId(), 4L);
            verify(wrongNoteRepository, never()).findByUserIdAndQuestionId(eq(user.getId()), eq(1L));
        }

        @Test
        @DisplayName("이미 완료된 시험에 제출하면 EXAM_ALREADY_COMPLETED")
        void submit_already_completed() {
            Attempt attempt = attemptFixture(AttemptStatus.COMPLETED, 5);
            when(attemptRepository.findByIdWithUser(attempt.getId())).thenReturn(Optional.of(attempt));

            assertThatThrownBy(() ->
                    examService.submit(EMAIL, attempt.getId(),
                            new ExamSubmitRequest(List.of(new AnswerSubmission(1L, 1)))))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.EXAM_ALREADY_COMPLETED);
        }

        @Test
        @DisplayName("다른 사용자의 attempt 제출 시 EXAM_ACCESS_DENIED")
        void submit_not_owner() {
            Attempt attempt = attemptFixture(AttemptStatus.IN_PROGRESS, 1);
            when(attemptRepository.findByIdWithUser(attempt.getId())).thenReturn(Optional.of(attempt));

            assertThatThrownBy(() ->
                    examService.submit("other@example.com", attempt.getId(),
                            new ExamSubmitRequest(List.of(new AnswerSubmission(1L, 1)))))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.EXAM_ACCESS_DENIED);
        }

        @Test
        @DisplayName("이미 오답노트에 있는 문제는 selected_option 만 최신 선택으로 갱신")
        void submit_updates_existing_wrong_note_selected_option() {
            Attempt attempt = attemptFixture(AttemptStatus.IN_PROGRESS, 1);
            Question q = questionFixture(1L, 2);
            List<AttemptAnswer> answers = new ArrayList<>(List.of(answerFixture(attempt, q)));
            WrongNote existing = WrongNote.builder()
                    .id(99L).user(user).question(q).selectedOption(3).isResolved(false).build();
            when(attemptRepository.findByIdWithUser(attempt.getId())).thenReturn(Optional.of(attempt));
            when(attemptAnswerRepository.findByAttemptIdWithQuestion(attempt.getId())).thenReturn(answers);
            when(wrongNoteRepository.findByUserIdAndQuestionId(user.getId(), 1L))
                    .thenReturn(Optional.of(existing));

            examService.submit(EMAIL, attempt.getId(),
                    new ExamSubmitRequest(List.of(new AnswerSubmission(1L, 1))));   // 오답 (정답 2)

            assertThat(existing.getSelectedOption()).isEqualTo(1);   // 3 → 1 로 덮어쓰기
            verify(wrongNoteRepository, never()).save(any());
        }

        @Test
        @DisplayName("채점 결과에 문항별 questionResults 가 포함된다")
        void submit_returns_question_results() {
            Attempt attempt = attemptFixture(AttemptStatus.IN_PROGRESS, 2);
            Question q1 = questionFixture(1L, 1);   // 정답 1
            Question q2 = questionFixture(2L, 2);   // 정답 2
            List<AttemptAnswer> answers = new ArrayList<>(List.of(
                    answerFixture(attempt, q1),
                    answerFixture(attempt, q2)
            ));
            when(attemptRepository.findByIdWithUser(attempt.getId())).thenReturn(Optional.of(attempt));
            when(attemptAnswerRepository.findByAttemptIdWithQuestion(attempt.getId())).thenReturn(answers);

            ExamResultResponse result = examService.submit(EMAIL, attempt.getId(),
                    new ExamSubmitRequest(List.of(
                            new AnswerSubmission(1L, 1),   // 정답
                            new AnswerSubmission(2L, 4)    // 오답
                    )));

            assertThat(result.questionResults()).hasSize(2);
            QuestionResult r1 = result.questionResults().get(0);
            assertThat(r1.questionId()).isEqualTo(1L);
            assertThat(r1.selectedOption()).isEqualTo(1);
            assertThat(r1.correctAnswer()).isEqualTo(1);
            assertThat(r1.isCorrect()).isTrue();
            QuestionResult r2 = result.questionResults().get(1);
            assertThat(r2.questionId()).isEqualTo(2L);
            assertThat(r2.selectedOption()).isEqualTo(4);
            assertThat(r2.correctAnswer()).isEqualTo(2);
            assertThat(r2.isCorrect()).isFalse();
        }

        @Test
        @DisplayName("신규 오답은 selected_option 을 담아 저장한다")
        void submit_new_wrong_note_persists_selected_option() {
            Attempt attempt = attemptFixture(AttemptStatus.IN_PROGRESS, 1);
            Question q = questionFixture(7L, 2);   // 정답 2
            List<AttemptAnswer> answers = new ArrayList<>(List.of(answerFixture(attempt, q)));
            when(attemptRepository.findByIdWithUser(attempt.getId())).thenReturn(Optional.of(attempt));
            when(attemptAnswerRepository.findByAttemptIdWithQuestion(attempt.getId())).thenReturn(answers);
            when(wrongNoteRepository.findByUserIdAndQuestionId(user.getId(), 7L))
                    .thenReturn(Optional.empty());

            examService.submit(EMAIL, attempt.getId(),
                    new ExamSubmitRequest(List.of(new AnswerSubmission(7L, 3))));   // 오답 (3 선택)

            verify(wrongNoteRepository).save(argThat(w ->
                    w.getQuestion() == q
                            && w.getUser() == user
                            && w.getSelectedOption() != null && w.getSelectedOption() == 3
                            && Boolean.FALSE.equals(w.getIsResolved())));
        }

        @Test
        @DisplayName("미선택(selectedOption=null) 문제도 오답노트에 등록된다")
        void submit_creates_wrong_note_for_unanswered_question() {
            Attempt attempt = attemptFixture(AttemptStatus.IN_PROGRESS, 1);
            Question q = questionFixture(8L, 4);
            List<AttemptAnswer> answers = new ArrayList<>(List.of(answerFixture(attempt, q)));
            when(attemptRepository.findByIdWithUser(attempt.getId())).thenReturn(Optional.of(attempt));
            when(attemptAnswerRepository.findByAttemptIdWithQuestion(attempt.getId())).thenReturn(answers);
            when(wrongNoteRepository.findByUserIdAndQuestionId(user.getId(), 8L))
                    .thenReturn(Optional.empty());

            // 미선택: selectedOption = null
            examService.submit(EMAIL, attempt.getId(),
                    new ExamSubmitRequest(List.of(new AnswerSubmission(8L, null))));

            verify(wrongNoteRepository).save(argThat(w ->
                    w.getQuestion() == q && w.getSelectedOption() == null));
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // helpers
    // ────────────────────────────────────────────────────────────────────────

    private Question questionFixture(Long id, int answer) {
        return Question.builder()
                .id(id)
                .category(category)
                .content("q" + id)
                .option1("a").option2("b").option3("c").option4("d")
                .answer(answer)
                .difficulty(Difficulty.EASY)
                .build();
    }

    private Attempt attemptFixture(AttemptStatus status, int totalCount) {
        return Attempt.builder()
                .id(UUID.randomUUID())
                .user(user)
                .examType(ExamType.SQLD)
                .status(status)
                .totalCount(totalCount)
                .correctCount(0)
                .score(0)
                .startedAt(LocalDateTime.now())
                .build();
    }

    private AttemptAnswer answerFixture(Attempt attempt, Question question) {
        return AttemptAnswer.builder()
                .attempt(attempt)
                .question(question)
                .selectedOption(null)
                .isCorrect(false)
                .build();
    }
}
