package com.demo.structured;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Java 25 Scoped Values Demo Application.
 *
 * Demonstrates 4 real-world use cases using:
 *   - ScopedValue (JEP 506 — Final in Java 25): immutable, request-scoped, zero-leak context
 *
 * Run with: ./mvnw spring-boot:run
 */
@SpringBootApplication
public class StructuredConcurrencyDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(StructuredConcurrencyDemoApplication.class, args);
    }
}
