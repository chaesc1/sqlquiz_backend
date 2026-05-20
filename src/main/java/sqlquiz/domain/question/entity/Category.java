package sqlquiz.domain.question.entity;

import jakarta.persistence.*;
import lombok.*;
import sqlquiz.domain.common.ExamType;

import java.time.LocalDateTime;

@Entity
@Table(name="categories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExamType examType;

    private LocalDateTime createdAt; // 굳이 updatedAt 이 필요없어서 BaseEntity 상속 X

    // Entity 가 DB 에 처음 저장 되기 직전에 자동으로 호출되는 생명 주기 콜백
    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
