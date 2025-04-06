#!/bin/bash
set -e

BASE_DIR=$(pwd)
SERVICES=(
  "eureka-service-registry"
  "api-gateway"
  "authentication-service"
  "image-processing-service"
  "trace-nutrition-service"
)

echo "=== Building all Nutria services ==="

for service in "${SERVICES[@]}"; do
  echo "🚀 Building $service..."
  cd "$BASE_DIR/$service"

  # Build JAR
  mvn clean package

  # Build Docker image (notice the dot at the end)
  docker build -t "nutria/${service}:latest" .
done

echo "✅ All services built successfully!"
echo "📦 Available Docker images:"
docker images | grep nutria/