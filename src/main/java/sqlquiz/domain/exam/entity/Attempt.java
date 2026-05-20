package sqlquiz.domain.exam.entity;

import jakarta.persistence.*;
import lombok.*;
import sqlquiz.domain.common.AttemptStatus;
import sqlquiz.domain.common.ExamType;
import sqlquiz.domain.user.entity.User;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "attempts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class Attempt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExamType examType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AttemptStatus status = AttemptStatus.IN_PROGRESS;

    @Column(nullable = false)
    private Integer totalCount;

    @Builder.Default
    private Integer correctCount = 0;

    @Builder.Default
    private Integer score = 0;

    @Column(nullable = false)
    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    // 시험 완료 처리 메서드
    public void complete(int correctCount, int score) {
        this.correctCount = correctCount;
        this.score = score;
        this.status = AttemptStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }
}
