package sqlquiz.domain.wrongnote.entity;

import jakarta.persistence.*;
import lombok.*;
import sqlquiz.domain.common.BaseEntity;
import sqlquiz.domain.question.entity.Question;
import sqlquiz.domain.user.entity.User;

@Entity
@Table(name = "wrong_notes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class WrongNote extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question", nullable = false)
    private Question question;

    @Column(columnDefinition = "TEXT")
    private String memo;

    @Builder.Default
    private Boolean isResolved = false;

    public void updateMemo(String memo) {
        this.memo = memo;
    }

    public void resolve() {
        this.isResolved = true;
    }

}
