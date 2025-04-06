package com.nutria.app;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;


@SpringBootApplication
@EnableDiscoveryClient
@EnableJpaAuditing
@ComponentScan(basePackages = {
        "com.nutria.app",
        "com.nutria.common"
})
public class TraceNutritionServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(TraceNutritionServiceApplication.class, args);
    }
}