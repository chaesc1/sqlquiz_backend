package sqlquiz.domain.question.entity;

import jakarta.persistence.*;
import lombok.*;
import sqlquiz.domain.common.Difficulty;

@Entity
@Table(name = "questions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) // 문제 N , 카테고리 1 관계 , LAZY 설정한 이유 : Question 조회할때 Category 가 항상 필요 X ,(N+1) 방지
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String option1;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String option2;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String option3;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String option4;

    @Column(nullable = false)
    private Integer answer;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Difficulty difficulty;

    // 도메인 메서드: 수정 가능한 필드만 한 번에 갱신.
    // setter를 열어두면 외부에서 ID/createdAt 같은 불변값까지 건드릴 위험이 있어
    // 의도된 변경 경로를 한 곳으로 모음.
    public void update(Category category,
                       String content,
                       String option1, String option2, String option3, String option4,
                       Integer answer,
                       String explanation,
                       Difficulty difficulty) {
        this.category = category;
        this.content = content;
        this.option1 = option1;
        this.option2 = option2;
        this.option3 = option3;
        this.option4 = option4;
        this.answer = answer;
        this.explanation = explanation;
        this.difficulty = difficulty;
    }
}
