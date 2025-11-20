FROM gradle:8.10-jdk17 AS backend-build
WORKDIR /workspace/backend
COPY backend/gradle backend/gradle
COPY backend/gradlew backend/gradlew
COPY backend/gradlew.bat backend/gradlew.bat
COPY backend/build.gradle backend/build.gradle
COPY backend/settings.gradle backend/settings.gradle
COPY backend/src backend/src
RUN ./gradlew bootJar --no-daemon

FROM node:22-alpine AS frontend-build
WORKDIR /workspace/frontend
COPY frontend/package*.json ./
COPY frontend/tsconfig*.json ./
COPY frontend/vite.config.ts ./
COPY frontend/eslint.config.js ./
COPY frontend/src ./src
COPY frontend/public ./public
RUN npm install
RUN npm run build

FROM eclipse-temurin:17-jre-jammy
ENV PORT=5213
WORKDIR /app

RUN apt-get update && apt-get install -y nginx && rm -rf /var/lib/apt/lists/*

COPY --from=backend-build /workspace/backend/build/libs/*.jar /app/backend/app.jar
COPY --from=frontend-build /workspace/frontend/dist /app/frontend
COPY nginx.conf /etc/nginx/nginx.conf
COPY start.sh /app/start.sh

RUN mkdir -p /var/log/query-bot && chmod +x /app/start.sh

EXPOSE 5213
ENTRYPOINT ["/app/start.sh"]
