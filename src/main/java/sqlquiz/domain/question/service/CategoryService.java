package sqlquiz.domain.question.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sqlquiz.domain.common.ExamType;
import sqlquiz.domain.question.dto.CategoryResponse;
import sqlquiz.domain.question.entity.Category;
import sqlquiz.domain.question.repository.CategoryRepository;
import sqlquiz.global.exception.CustomException;
import sqlquiz.global.exception.ErrorCode;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;

    /** 카테고리 목록 — examType 필터 선택. */
    public List<CategoryResponse> list(ExamType examType) {
        List<Category> categories = (examType == null)
                ? categoryRepository.findAll()
                : categoryRepository.findByExamType(examType);
        return categories.stream().map(CategoryResponse::from).toList();
    }

    /** 내부에서 Question 생성/수정 시 categoryId → Category 변환에 사용. */
    public Category getOrThrow(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CustomException(ErrorCode.CATEGORY_NOT_FOUND));
    }
}
