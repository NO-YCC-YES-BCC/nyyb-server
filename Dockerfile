# 1단계: Gradle 빌드 (테스트는 CI에서 별도 수행 — 이미지 빌드 시 실DB 접근 방지)
FROM gradle:8.11-jdk21 AS build
WORKDIR /app
COPY build.gradle settings.gradle ./
COPY src ./src
RUN gradle bootJar -x test --no-daemon

# 2단계: 실행 이미지
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
