# base 镜像前缀：默认华为云 swr（墙内本机 build 刚需）；GHA 墙外 runner 传
# --build-arg IMAGE_PREFIX= 切 docker.io 官方源。全局 ARG，仅在各 FROM 行使用。
ARG IMAGE_PREFIX=swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/

# Stage 1: Build frontend
FROM ${IMAGE_PREFIX}node:22-alpine AS frontend
WORKDIR /app/frontend
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

# Stage 2: Build backend
FROM ${IMAGE_PREFIX}gradle:8.13-jdk21 AS backend
WORKDIR /app
COPY backend/ ./
COPY --from=frontend /app/frontend/dist/ src/main/resources/static/
# 使用 BuildKit 缓存挂载 Gradle 依赖
RUN --mount=type=cache,target=/home/gradle/.gradle/caches \
    --mount=type=cache,target=/home/gradle/.gradle/wrapper \
    gradle bootJar --no-daemon -x test

# Stage 3: Runtime image
FROM ${IMAGE_PREFIX}eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S domu && adduser -S domu -G domu
RUN mkdir -p /app/data/uploads && chown -R domu:domu /app/data

COPY --from=backend /app/build/libs/*.jar app.jar
RUN chown domu:domu app.jar

USER domu

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]
