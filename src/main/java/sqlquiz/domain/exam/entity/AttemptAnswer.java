package sqlquiz.domain.exam.entity;

import jakarta.persistence.*;
import lombok.*;
import sqlquiz.domain.question.entity.Question;

import java.time.LocalDateTime;

@Entity
@Table(name = "attempt_answers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class AttemptAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_id", nullable = false)
    private Attempt attempt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    private Integer selectedOption;

    @Column(nullable = false)
    private Boolean isCorrect;

    @Builder.Default
    private LocalDateTime answeredAt = LocalDateTime.now();

    /**
     * 시험 제출 시점에 답안 갱신.
     * selectedOption 이 null 이면 미선택(자동 오답) 처리.
     */
    public void updateSubmission(Integer selectedOption, boolean isCorrect) {
        this.selectedOption = selectedOption;
        this.isCorrect = isCorrect;
        this.answeredAt = LocalDateTime.now();
    }
}
