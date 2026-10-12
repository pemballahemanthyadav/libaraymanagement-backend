package com.librarymanagement.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Library Management System - Spring Boot Application Entry Point
 *
 * @SpringBootApplication combines three annotations:
 *   - @Configuration: marks this as a Spring configuration class
 *   - @EnableAutoConfiguration: enables Spring Boot's auto-configuration
 *   - @ComponentScan: scans all classes under this package for Spring beans
 */
@SpringBootApplication
public class BackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }
}
