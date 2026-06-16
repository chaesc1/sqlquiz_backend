package sqlquiz.domain.wrongnote.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sqlquiz.domain.common.Difficulty;
import sqlquiz.domain.common.ExamType;
import sqlquiz.domain.common.Role;
import sqlquiz.domain.question.entity.Category;
import sqlquiz.domain.question.entity.Question;
import sqlquiz.domain.question.repository.QuestionRepository;
import sqlquiz.domain.user.entity.User;
import sqlquiz.domain.user.repository.UserRepository;
import sqlquiz.domain.wrongnote.dto.WrongNoteRetryRequest;
import sqlquiz.domain.wrongnote.dto.WrongNoteRetryResponse;
import sqlquiz.domain.wrongnote.entity.WrongNote;
import sqlquiz.domain.wrongnote.repository.WrongNoteRepository;
import sqlquiz.global.exception.CustomException;
import sqlquiz.global.exception.ErrorCode;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WrongNoteServiceTest {

    @Mock WrongNoteRepository wrongNoteRepository;
    @Mock QuestionRepository questionRepository;
    @Mock UserRepository userRepository;

    @InjectMocks WrongNoteService wrongNoteService;

    private static final String EMAIL = "user@example.com";
    private User user;
    private Category category;
    private Question question;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(UUID.randomUUID())
                .email(EMAIL)
                .password("hash")
                .nickname("u")
                .role(Role.ROLE_USER)
                .build();
        category = Category.builder().id(1L).name("관리 구문").examType(ExamType.SQLD).build();
        question = Question.builder()
                .id(71L)
                .category(category)
                .content("일반 VIEW 에 대한 설명으로 옳은 것은?")
                .option1("물리적 저장 공간을 갖는다")
                .option2("가상 테이블이며 매번 정의된 SELECT 가 실행된다")
                .option3("인덱스를 만들 수 없다")
                .option4("UPDATE 가 절대 불가능하다")
                .answer(2)
                .explanation("일반 뷰는 가상 테이블이고 호출 시마다 정의 SELECT 가 실행된다.")
                .difficulty(Difficulty.EASY)
                .build();
    }

    @Nested
    @DisplayName("retry()")
    class Retry {

        @Test
        @DisplayName("정답을 고르면 isResolved=true 자동 전환 + selected_option 정답값으로 갱신")
        void retry_correct_marks_resolved() {
            WrongNote note = WrongNote.builder()
                    .id(10L).user(user).question(question)
                    .selectedOption(4).isResolved(false).build();
            when(wrongNoteRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(note));

            WrongNoteRetryResponse res =
                    wrongNoteService.retry(EMAIL, 10L, new WrongNoteRetryRequest(2));

            assertThat(res.isCorrect()).isTrue();
            assertThat(res.correctAnswer()).isEqualTo(2);
            assertThat(res.isResolved()).isTrue();
            assertThat(res.selectedOption()).isEqualTo(2);
            assertThat(res.explanation()).contains("가상 테이블");
            assertThat(note.getIsResolved()).isTrue();
            assertThat(note.getSelectedOption()).isEqualTo(2);
        }

        @Test
        @DisplayName("오답을 고르면 selected_option 만 갱신, isResolved 는 그대로")
        void retry_wrong_updates_selected_only() {
            WrongNote note = WrongNote.builder()
                    .id(11L).user(user).question(question)
                    .selectedOption(4).isResolved(false).build();
            when(wrongNoteRepository.findByIdWithDetails(11L)).thenReturn(Optional.of(note));

            WrongNoteRetryResponse res =
                    wrongNoteService.retry(EMAIL, 11L, new WrongNoteRetryRequest(3));

            assertThat(res.isCorrect()).isFalse();
            assertThat(res.correctAnswer()).isEqualTo(2);
            assertThat(res.isResolved()).isFalse();
            assertThat(res.selectedOption()).isEqualTo(3);
            assertThat(note.getIsResolved()).isFalse();
            assertThat(note.getSelectedOption()).isEqualTo(3);
        }

        @Test
        @DisplayName("타인 노트 ID 로 시도하면 WRONG_NOTE_NOT_FOUND")
        void retry_other_users_note() {
            User other = User.builder().id(UUID.randomUUID()).email("other@example.com")
                    .password("h").nickname("o").role(Role.ROLE_USER).build();
            WrongNote note = WrongNote.builder()
                    .id(12L).user(other).question(question)
                    .selectedOption(1).isResolved(false).build();
            when(wrongNoteRepository.findByIdWithDetails(12L)).thenReturn(Optional.of(note));

            assertThatThrownBy(() ->
                    wrongNoteService.retry(EMAIL, 12L, new WrongNoteRetryRequest(2)))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.WRONG_NOTE_NOT_FOUND);
        }

        @Test
        @DisplayName("존재하지 않는 노트 ID 면 WRONG_NOTE_NOT_FOUND")
        void retry_missing_note() {
            when(wrongNoteRepository.findByIdWithDetails(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    wrongNoteService.retry(EMAIL, 999L, new WrongNoteRetryRequest(2)))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.WRONG_NOTE_NOT_FOUND);
        }
    }
}
