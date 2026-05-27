package sqlquiz.domain.statistics.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sqlquiz.domain.common.ExamType;
import sqlquiz.domain.statistics.dto.*;
import sqlquiz.domain.statistics.repository.StatisticsRepository;
import sqlquiz.domain.user.entity.User;
import sqlquiz.domain.user.repository.UserRepository;
import sqlquiz.global.exception.CustomException;
import sqlquiz.global.exception.ErrorCode;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatisticsService {

    private final StatisticsRepository statisticsRepository;
    private final UserRepository userRepository;

    public SummaryResponse summary(String email) {
        SummaryView v = statisticsRepository.findSummaryByUserId(getUserId(email));
        return new SummaryResponse(
                v.getTotalAttempts(),
                v.getCompletedAttempts(),
                v.getAverageScore() == null ? 0.0 : v.getAverageScore(),
                v.getTotalQuestions() == null ? 0 : v.getTotalQuestions(),
                v.getTotalCorrect() == null ? 0 : v.getTotalCorrect()
        );
    }

    public List<CategoryStatResponse> categoryStats(String email) {
        return statisticsRepository.findCategoryStatsByUserId(getUserId(email)).stream()
                .map(this::toCategoryStatResponse)
                .toList();
    }

    public List<CategoryStatResponse> weakPoints(String email, int limit) {
        return statisticsRepository.findWeakPointsByUserId(getUserId(email), limit).stream()
                .map(this::toCategoryStatResponse)
                .toList();
    }

    public Page<HistoryItemResponse> history(String email, Pageable pageable) {
        return statisticsRepository.findHistoryByUserId(getUserId(email), pageable)
                .map(HistoryItemResponse::from);
    }

    // ────────────────────────────────────────────────────────────────────────

    private java.util.UUID getUserId(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        return user.getId();
    }

    private CategoryStatResponse toCategoryStatResponse(CategoryStatView v) {
        return new CategoryStatResponse(
                v.getCategoryId(),
                v.getCategoryName(),
                ExamType.valueOf(v.getExamType()),
                v.getTotalAttempted(),
                v.getTotalCorrect(),
                v.getAccuracy()
        );
    }
}
