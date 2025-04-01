FROM eclipse-temurin:21
LABEL authors="martinhachiya"

WORKDIR /app
COPY target/api-gateway.jar api-gateway.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "api-gateway.jar"]