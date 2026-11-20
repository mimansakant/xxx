package com.collective404.budget;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Budget Service Spring Boot application.
 *
 * <p>This class is intentionally neither final nor given a private
 * constructor: Spring must be able to subclass and instantiate a
 * {@code @SpringBootApplication} configuration class. The checkstyle
 * HideUtilityClassConstructor/DesignForExtension/FinalClass checks are
 * suppressed for this file for that reason.</p>
 */
@SpringBootApplication
public class BudgetApplication {

    /**
     * Starts the Budget Service application.
     *
     * @param args command line arguments
     */
    public static void main(final String[] args) {
        SpringApplication.run(BudgetApplication.class, args);
    }
}
