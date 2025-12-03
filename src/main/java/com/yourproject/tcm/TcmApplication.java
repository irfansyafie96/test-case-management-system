package com.yourproject.tcm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Application Class - Entry Point for the Test Case Management System
 *
 * This is the main class that starts the entire Spring Boot application.
 * SpringBootApplication annotation includes:
 * - @EnableAutoConfiguration: Automatically configures Spring based on dependencies
 * - @ComponentScan: Scans for Spring components in this package
 * - @Configuration: Allows to register additional beans
 *
 * When you run this application, it will:
 * 1. Start an embedded web server (Tomcat by default)
 * 2. Initialize all Spring components
 * 3. Set up database connections
 * 4. Configure security settings
 * 5. Make the API available at http://localhost:8080
 */
@SpringBootApplication
public class TcmApplication {

	/**
	 * Main method - This is where Java execution starts
	 * SpringApplication.run() bootstraps the Spring application
	 */
	public static void main(String[] args) {
		SpringApplication.run(TcmApplication.class, args);
	}

}