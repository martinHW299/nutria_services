FROM eclipse-temurin:21
LABEL authors="martinhachiya"

WORKDIR /app
COPY target/image-processing-service.jar image-processing-service.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "image-processing-service.jar"]