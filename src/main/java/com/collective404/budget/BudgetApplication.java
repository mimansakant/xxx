package com.collective404.budget;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 *
 * BudgetApplication
 */
@SpringBootApplication
public final class BudgetApplication {

    /** private constructor to prevent instantiation */
    private BudgetApplication() {}

    /**
     * Application Entry Point
     * @param args command line arguments
     */
    public static void main(final String[] args) {
        SpringApplication.run(BudgetApplication.class, args);
    }
}
