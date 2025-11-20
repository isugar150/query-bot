#!/usr/bin/env bash
set -e

JAVA_OPTS=${JAVA_OPTS:-"-Xms256m -Xmx512m"}

echo "Starting Query Bot backend..."
java $JAVA_OPTS -jar /app/backend/app.jar --server.port=8080 --spring.datasource.url=jdbc:sqlite:/app/data/querybot.db &

echo "Starting nginx..."
nginx -g "daemon off;"
