package sqlquiz.domain.question.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sqlquiz.domain.common.ExamType;
import sqlquiz.domain.question.entity.Category;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByExamType(ExamType examType);
}
