package com.nutria.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.mongodb.config.EnableMongoAuditing;


@SpringBootApplication
@EnableDiscoveryClient
@EnableMongoAuditing
@EnableJpaAuditing
@ComponentScan(basePackages = {
        "com.nutria.app",
        "com.nutria.common"
})
public class ImageProcessingServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ImageProcessingServiceApplication.class, args);
    }
}