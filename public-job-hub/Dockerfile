FROM node:22-alpine AS frontend-build
WORKDIR /workspace/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

FROM gradle:9.0.0-jdk17 AS backend-build
WORKDIR /workspace
COPY build.gradle settings.gradle ./
COPY src ./src
COPY --from=frontend-build /workspace/frontend/dist ./frontend/dist
RUN gradle bootJar -x buildFrontend --no-daemon

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S jobhub && adduser -S jobhub -G jobhub
COPY --from=backend-build /workspace/build/libs/public-job-hub-0.1.0.jar /app/jobhub.jar
USER jobhub
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -q --spider http://127.0.0.1:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-Duser.timezone=Asia/Seoul", "-jar", "/app/jobhub.jar"]
