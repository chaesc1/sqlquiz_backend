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
}
