# Stage 1: Build frontend
FROM swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/node:22-alpine AS frontend
WORKDIR /app/frontend
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

# Stage 2: Build backend
FROM swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/gradle:8.13-jdk21 AS backend
WORKDIR /app
COPY backend/ ./
COPY --from=frontend /app/frontend/dist/ src/main/resources/static/
RUN gradle bootJar --no-daemon -x test

# Stage 3: Runtime image
FROM swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S domu && adduser -S domu -G domu
RUN mkdir -p /app/data/uploads && chown -R domu:domu /app/data

COPY --from=backend /app/build/libs/*.jar app.jar
RUN chown domu:domu app.jar

USER domu

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]
