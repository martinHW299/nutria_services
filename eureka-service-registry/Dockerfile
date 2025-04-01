FROM eclipse-temurin:21
LABEL authors="martinhachiya"

WORKDIR /app
COPY target/eureka-service-registry.jar eureka-service.jar
EXPOSE 8761
ENTRYPOINT ["java", "-jar", "eureka-service.jar"]