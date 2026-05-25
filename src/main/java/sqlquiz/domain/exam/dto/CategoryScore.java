package sqlquiz.domain.exam.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "카테고리별 성적")
public record CategoryScore(
        String categoryName,
        long total,
        long correct
) {
    public double accuracy() {
        return total == 0 ? 0.0 : (double) correct / total * 100.0;
    }
}
