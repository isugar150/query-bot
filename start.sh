#!/usr/bin/env bash
set -e

JAVA_OPTS=${JAVA_OPTS:-"-Xms256m -Xmx512m"}

echo "Starting Query Bot backend..."
APP_DATA_DIR=${APP_DATA_DIR:-/app/data}
export APP_DATA_DIR
SERVER_PORT=${SERVER_PORT:-8080}

java $JAVA_OPTS -jar /app/backend/app.jar --server.port=$SERVER_PORT --APP_DATA_DIR=$APP_DATA_DIR &

echo "Starting nginx..."
nginx -g "daemon off;"
