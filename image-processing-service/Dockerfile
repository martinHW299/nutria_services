FROM eclipse-temurin:21
LABEL maintainer="martinhachiya"

WORKDIR /app

# Copy the pre-built JAR (from your host machine)
COPY target/*.jar app.jar

# Run as non-root user for security
RUN addgroup --system spring && \
    adduser --system --ingroup spring spring && \
    chown spring:spring /app

RUN mkdir -p /app/food-images && chmod -R 777 /app/food-images

USER spring:spring

EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]