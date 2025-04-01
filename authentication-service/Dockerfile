FROM eclipse-temurin:21
LABEL authors="martinhachiya"

WORKDIR /app
COPY target/authentication-service.jar authentication-service.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "authentication-service.jar"]