# syntax=docker/dockerfile:1.7
#
# 멀티 스테이지 빌드:
#   1) builder — JDK 21 + Gradle Wrapper 로 fat jar 빌드
#   2) runtime — JRE 21 슬림 베이스 + 빌드된 jar 만 복사
#
# 빌드 캐시 친화:
#   - Gradle 의존성을 먼저 받아내려 의존성 캐시 레이어를 안정화
#   - 소스 변경 시 ./gradlew bootJar 만 다시 실행

# ────────────────────────────────────────────────────────────────────────
# 1) builder
# ────────────────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace

# 의존성 캐시 레이어
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true

# 실제 소스 복사 후 빌드
COPY src ./src
RUN ./gradlew --no-daemon clean bootJar -x test

# ────────────────────────────────────────────────────────────────────────
# 2) runtime
# ────────────────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# 비-루트 유저로 실행
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# 빌드된 jar 만 복사
COPY --from=builder /workspace/build/libs/*.jar /app/app.jar

# 컨테이너 메타데이터
ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

EXPOSE 8080

# Healthcheck — Actuator health 200 응답 확인
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD wget -q -O - http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
